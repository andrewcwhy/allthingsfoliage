package io.github.andrewcwhy.allthingsfoliage.gpu.shader

import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.renderpearl.api.GpuFormat
import com.mojang.renderpearl.api.buffers.GpuBuffer
import com.mojang.renderpearl.api.pipeline.BindGroupLayout
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import com.mojang.renderpearl.api.vertex.VertexFormat
import com.mojang.renderpearl.backend.api.BackendRenderPipeline
import com.mojang.renderpearl.backend.opengl.GlBuffer
import com.mojang.renderpearl.backend.opengl.GlProgram
import com.mojang.renderpearl.backend.opengl.GlRenderPipeline
import com.mojang.renderpearl.backend.opengl.GlStateManager
import com.mojang.renderpearl.backend.opengl.VertexArray
import com.mojang.renderpearl.frontend.FrontendRenderPipeline
import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage
import io.github.andrewcwhy.allthingsfoliage.gpu.compat.IrisCompat
import io.github.andrewcwhy.allthingsfoliage.mixin.compat.iris.IrisRenderingPipelineAccessor
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
import java.io.IOException
import kotlin.jvm.optionals.getOrNull
import net.irisshaders.iris.Iris
import net.irisshaders.iris.api.v0.IrisApi
import net.irisshaders.iris.api.v0.IrisProgram
import net.irisshaders.iris.api.v0.IrisShadowProgram
import net.irisshaders.iris.gl.IrisRenderSystem
import net.irisshaders.iris.mixinterface.GlRenderPipelineAccess
import net.irisshaders.iris.pipeline.IrisRenderingPipeline
import net.irisshaders.iris.pipeline.WorldRenderingPhase
import net.irisshaders.iris.pipeline.programs.ShaderKey
import net.irisshaders.iris.pipeline.programs.ShaderSupplier
import net.irisshaders.iris.pipeline.transform.Patch
import net.irisshaders.iris.shaderpack.loading.ProgramId
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder
import net.irisshaders.iris.vertices.IrisVertexFormats
import net.minecraft.world.level.block.state.BlockState
import org.joml.Matrix4f
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL20C
import org.lwjgl.opengl.GL31
import org.lwjgl.opengl.GL33C

/**
 * Builds foliage variants of a shader pack's terrain and shadow programs through Iris.
 *
 * Only programs built by this integration receive vertex changes. Failures retain chunk-mesh
 * rendering without modifying the pack's own programs.
 *
 * Rendering state belongs to the render thread; program-building state is thread-local.
 */
object IrisFoliageShaders {
    private const val MAX_PROGRAM_LOG_LENGTH = 32_768
    private const val SWAYED_POSITION = "atf_swayedPosition"

    private val building = ThreadLocal.withInitial { false }
    private val foliagePipelines =
        Reference2ObjectOpenHashMap<CompiledRenderPipeline, FrontendRenderPipeline>()

    private val mainPattern = Regex(
        """\bvoid\s+main\s*\(\s*(?:void\s*)?\)"""
    )

    private val positionInputPattern = Regex(
        """\bin\s+vec3\s+iris_Position\s*;"""
    )

    private val positionPattern = Regex("""\biris_Position\b""")

    private val renamedPositionInputPattern = Regex(
        """\bin\s+vec3\s+${Regex.escape(SWAYED_POSITION)}\s*;"""
    )

    private val swayShader = """
        // Added by All Things Foliage.
        in float SwayCell;
        in float SwayWeights;

        layout(std140) uniform iris_FoliageSway {
            float atf_SwayIntensity;
            float atf_CalmSway;
            vec4 atf_SwayEdge;
            vec4 atf_Weather;
        };

        #define ATF_MAX_CELLS 128

        layout(std140) uniform iris_FoliageInteraction {
            int atf_CellCount;
            vec4 atf_CellBoundsMin;
            vec4 atf_CellBoundsMax;
            vec4 atf_CellPosition[ATF_MAX_CELLS];
            vec4 atf_CellForce[ATF_MAX_CELLS];
        };
    """.trimIndent() + "\n" + readSway() + "\n" + """
        void main() {
            vec3 atf_offset = iris_transforms.ModelOffset;
            atf_swayedPosition = atf_sway(iris_Position + atf_offset, atf_offset, iris_globalInfo.CameraBlockPos,
                    iris_globalInfo.CameraOffset, iris_globalInfo.GameTime, SwayCell, SwayWeights) - atf_offset;
            atf_packMain();
        }
    """.trimIndent() + "\n"

    private val vertexArrays = mutableListOf<VertexArray>()

    private var assigned = false
    private var configured = false
    private var foliageShadowDraw: IrisCompat.ShadowDraw? = null
    private var interactionBinding = -1
    private var irisUnsupported = false
    private var layouts: List<BindGroupLayout> = emptyList()
    private var mainProgram: GlProgram? = null
    private var mainSolidProgram: GlProgram? = null
    private var opaquePipeline: RenderPipeline? = null
    private var owner: IrisRenderingPipeline? = null
    private var pipeline: RenderPipeline? = null
    private var shadowCallbackRegistered = false
    private var shadowPipeline: RenderPipeline? = null
    private var shadowProgram: GlProgram? = null
    private var shadowSolidProgram: GlProgram? = null
    private var swayBinding = -1

    /** Changes when a new pack pipeline requires foliage geometry to be rebuilt. */
    var generation = 0
        private set

    val hasOpaquePrograms: Boolean
        get() {
            return owner != null &&
                    mainSolidProgram != null &&
                    (shadowProgram == null || shadowSolidProgram != null)
        }

    val hasShadowProgram: Boolean
        get() {
            return shadowProgram != null
        }

    val isBuilding: Boolean
        get() {
            return building.get()
        }

    val isRenderingShadows: Boolean
        get() {
            return IrisApi.getInstance().isRenderingShadowPass()
        }

    val isShaderPackInUse: Boolean
        get() {
            return IrisApi.getInstance().isShaderPackInUse()
        }

    /** Iris's terrain attributes followed by the two foliage-specific attributes. */
    val vertexFormat: VertexFormat = createVertexFormat()

    /** Supplies block identifiers, light emission, and block centers to Iris's vertex builder. */
    fun beginBlock(
        builder: BufferBuilder,
        state: BlockState,
        x: Int,
        y: Int,
        z: Int,
    ) {
        val ids = WorldRenderingSettings.INSTANCE.blockStateIds ?: return

        // Iris adds this interface to BufferBuilder through Mixin.
        val blocks = (builder as Any) as? BlockSensitiveBufferBuilder ?: return

        blocks.beginBlock(
            ids.getOrDefault(state, -1),
            0.toByte(),
            state.lightEmission.toByte(),
            x,
            y,
            z,
        )
    }

    /** Returns the previous phase so callers can restore it after drawing. */
    fun beginTerrainPhase(): WorldRenderingPhase? {
        val current = Iris.getPipelineManager().pipelineNullable ?: return null
        val previous = current.phase

        current.phase = WorldRenderingPhase.TERRAIN_CUTOUT

        return previous
    }

    private fun bindBlock(program: Int, name: String, binding: Int) {
        val index = GL31.glGetUniformBlockIndex(program, name)

        if (index != GL31.GL_INVALID_INDEX) {
            GL31.glUniformBlockBinding(program, index, binding)
        }
    }

    /** Binds foliage buffers to the uniform binding points reserved during program linking. */
    fun bindSwayBlocks(settings: GpuBuffer, interaction: GpuBuffer) {
        if (swayBinding < 0) {
            return
        }

        GL31.glBindBufferBase(
            GL31.GL_UNIFORM_BUFFER,
            swayBinding,
            (settings as GlBuffer).handle(),
        )

        GL31.glBindBufferBase(
            GL31.GL_UNIFORM_BUFFER,
            interactionBinding,
            (interaction as GlBuffer).handle(),
        )
    }

    private fun build(created: IrisRenderingPipeline) {
        // The accessor interface is supplied by this mod's Mixin.
        val access = (created as Any) as IrisRenderingPipelineAccessor
        building.set(true)

        try {
            val terrain = access.resolver()
                .resolve(ProgramId.TerrainCutout)
                .getOrNull()

            if (terrain == null) {
                AllThingsFoliage.logger.warn(
                    "The shader pack has no terrain program; foliage near the player " +
                            "is left to the chunk mesh while it is loaded"
                )

                return
            }

            mainProgram = finish(
                access.createShader(
                    "atf_terrain",
                    ShaderKey.TERRAIN_CUTOUT,
                    terrain,
                    ProgramId.TerrainCutout,
                    ShaderKey.TERRAIN_CUTOUT.alphaTest,
                    vertexFormat,
                    ShaderKey.TERRAIN_CUTOUT.fogMode,
                    false,
                    false,
                    false,
                    false,
                    false,
                    Patch.VANILLA,
                )
            )

            if (access.shadowRenderTargets() != null) {
                val shadowSource = access.resolver()
                    .resolve(ProgramId.ShadowCutout)
                    .getOrNull()

                if (shadowSource != null) {
                    shadowProgram = finish(
                        access.createShadowShader(
                            "atf_shadow",
                            ShaderKey.SHADOW_TERRAIN_CUTOUT,
                            shadowSource,
                            ProgramId.ShadowCutout,
                            ShaderKey.SHADOW_TERRAIN_CUTOUT.alphaTest,
                            vertexFormat,
                            false,
                            false,
                            false,
                            false,
                            Patch.VANILLA,
                        )
                    )
                }
            }

            buildSolid(access)
        } catch (exception: Throwable) {
            closePrograms()

            AllThingsFoliage.logger.error(
                "Could not build the foliage programs for this shader pack; foliage near " +
                        "the player is left to the chunk mesh while it is loaded",
                exception,
            )
        } finally {
            building.set(false)
        }
    }

    /** Builds solid variants independently so a failure does not discard cutout programs. */
    private fun buildSolid(access: IrisRenderingPipelineAccessor) {
        var solid: GlProgram? = null
        var solidShadow: GlProgram? = null

        try {
            val terrain = access.resolver()
                .resolve(ProgramId.TerrainSolid)
                .getOrNull()
                ?: return

            solid = finish(
                access.createShader(
                    "atf_terrain_solid",
                    ShaderKey.TERRAIN_SOLID,
                    terrain,
                    ProgramId.TerrainSolid,
                    ShaderKey.TERRAIN_SOLID.alphaTest,
                    vertexFormat,
                    ShaderKey.TERRAIN_SOLID.fogMode,
                    false,
                    false,
                    false,
                    false,
                    false,
                    Patch.VANILLA,
                )
            )

            if (shadowProgram != null) {
                val shadowSource = access.resolver()
                    .resolve(ProgramId.ShadowSolid)
                    .getOrNull()
                    ?: return

                // Iris uses the cutout shadow key, but the source and alpha test are solid.
                solidShadow = finish(
                    access.createShadowShader(
                        "atf_shadow_solid",
                        ShaderKey.SHADOW_TERRAIN_CUTOUT,
                        shadowSource,
                        ProgramId.ShadowSolid,
                        ShaderKey.TERRAIN_SOLID.alphaTest,
                        vertexFormat,
                        false,
                        false,
                        false,
                        false,
                        Patch.VANILLA,
                    )
                )
            }

            mainSolidProgram = solid
            shadowSolidProgram = solidShadow

            // Ownership transfers to the cached program fields.
            solid = null
            solidShadow = null
        } catch (exception: Throwable) {
            closeSolidPrograms()

            AllThingsFoliage.logger.warn(
                "Could not build the solid foliage programs for this shader pack; " +
                        "Fast leaves near the player are left to the chunk mesh while it is loaded",
                exception,
            )
        } finally {
            solid?.close()
            solidShadow?.close()
        }
    }

    private fun closePrograms() {
        // Closing each pipeline would delete its shared program repeatedly.
        foliagePipelines.clear()

        for (vertexArray in vertexArrays) {
            vertexArray.close()
        }

        vertexArrays.clear()
        closeSolidPrograms()

        mainProgram?.close()
        mainProgram = null

        shadowProgram?.close()
        shadowProgram = null
    }

    private fun closeSolidPrograms() {
        mainSolidProgram?.close()
        mainSolidProgram = null

        shadowSolidProgram?.close()
        shadowSolidProgram = null
    }

    private fun createVertexFormat(): VertexFormat {
        val builder = VertexFormat.builder(0)

        for (element in IrisVertexFormats.TERRAIN.elements) {
            builder.addAttribute(element.name(), element.format())
        }

        return builder
            .addAttribute("SwayCell", GpuFormat.R32_FLOAT)
            .addAttribute("SwayWeights", GpuFormat.R32_FLOAT)
            .build()
    }

    /** Draws only when the programs still belong to Iris's active pack pipeline. */
    fun drawShadow(
        modelView: Matrix4f,
        projection: Matrix4f,
        cameraX: Double,
        cameraY: Double,
        cameraZ: Double,
    ) {
        val callback = foliageShadowDraw ?: return

        if (
            owner != null &&
            shadowProgram != null &&
            Iris.getPipelineManager().pipelineNullable === owner
        ) {
            callback.draw(
                modelView,
                projection,
                cameraX,
                cameraY,
                cameraZ,
            )
        }
    }

    fun endBlock(builder: BufferBuilder) {
        val blocks = (builder as Any) as? BlockSensitiveBufferBuilder ?: return
        blocks.endBlock()
    }

    fun endTerrainPhase(previous: WorldRenderingPhase?) {
        val current = Iris.getPipelineManager().pipelineNullable ?: return
        val phase = previous ?: return

        current.phase = phase
    }

    /** Verifies linking and assigns the foliage uniform blocks to reserved binding points. */
    private fun finish(supplier: ShaderSupplier): GlProgram {
        val program = supplier.id().program()

        check(GlStateManager.glGetProgrami(program, GL20C.GL_LINK_STATUS) != 0) {
            "Linking failed: ${
                GlStateManager.glGetProgramInfoLog(program, MAX_PROGRAM_LOG_LENGTH)
            }"
        }

        if (swayBinding < 0) {
            val points = GL31.glGetInteger(GL31.GL_MAX_UNIFORM_BUFFER_BINDINGS)
            swayBinding = points - 1
            interactionBinding = points - 2
        }

        bindBlock(program, "iris_FoliageSway", swayBinding)
        bindBlock(program, "iris_FoliageInteraction", interactionBinding)

        return supplier.shader().get()
    }

    /** Replaces position reads while retaining the input name Iris uses for attribute binding. */
    fun patchVertex(vertex: String): String {
        check(positionInputPattern.findAll(vertex).count() == 1) {
            "Expected one position input in the translated vertex shader"
        }

        check(mainPattern.findAll(vertex).count() == 1) {
            "Expected one main function in the translated vertex shader"
        }

        var patched = positionPattern.replace(vertex, SWAYED_POSITION)

        patched = renamedPositionInputPattern.replaceFirst(
            patched,
            "in vec3 iris_Position;\nvec3 $SWAYED_POSITION;",
        )

        patched = mainPattern.replaceFirst(
            patched,
            "void atf_packMain()",
        )

        return "$patched\n$swayShader"
    }

    /** Returns a foliage program only for a pipeline owned by the currently active pack. */
    fun programFor(requested: RenderPipeline?): GlProgram? {
        if (
            requested == null ||
            Iris.getPipelineManager().pipelineNullable !== owner
        ) {
            return null
        }

        return when {
            requested === pipeline -> mainProgram
            requested === opaquePipeline -> mainSolidProgram
            requested === shadowPipeline -> shadowProgram
            else -> null
        }
    }

    /**
     * Builds programs once per active pack pipeline.
     *
     * Called during render-thread frame preparation, after renderer configuration.
     */
    fun ready(): Boolean {
        if (irisUnsupported) {
            return false
        }

        val current = Iris.getPipelineManager().pipelineNullable

        if (!configured || current !is IrisRenderingPipeline) {
            if (owner != null) {
                closePrograms()
                owner = null
            }

            return false
        }

        if (current !== owner) {
            closePrograms()
            owner = current
            generation++
            build(current)
        }

        return mainProgram != null
    }

    private fun readSway(): String {
        val path = "/assets/${AllThingsFoliage.MOD_ID}/shaders/include/sway.glsl"

        try {
            val stream = checkNotNull(
                IrisFoliageShaders::class.java.getResourceAsStream(path)
            ) {
                "Missing $path"
            }

            return stream.bufferedReader(Charsets.UTF_8).use { reader ->
                reader.readText()
            }
        } catch (exception: IOException) {
            throw IllegalStateException("Could not read $path", exception)
        }
    }

    fun setTerrainPhase(solid: Boolean) {
        val current = Iris.getPipelineManager().pipelineNullable ?: return

        current.phase = if (solid) {
            WorldRenderingPhase.TERRAIN_SOLID
        } else {
            WorldRenderingPhase.TERRAIN_CUTOUT
        }
    }

    /** Registers foliage pipelines and the shadow callback without duplicating Iris assignments. */
    fun setUp(
        foliagePipeline: RenderPipeline,
        foliageShadowPipeline: RenderPipeline?,
        foliageLayouts: List<BindGroupLayout>,
        shadowDraw: IrisCompat.ShadowDraw,
    ) {
        pipeline = foliagePipeline
        shadowPipeline = foliageShadowPipeline
        layouts = foliageLayouts.toList()
        configured = true
        foliageShadowDraw = shadowDraw

        val api = IrisApi.getInstance()

        if (!assigned) {
            assigned = true

            api.assignPipeline(
                foliagePipeline,
                IrisProgram.TERRAIN_CUTOUT,
            )

            api.assignPipelineShadow(
                foliagePipeline,
                IrisShadowProgram.SHADOW_TERRAIN_CUTOUT,
            )
        }

        if (!shadowCallbackRegistered) {
            shadowCallbackRegistered = true

            try {
                api.registerShadowRenderCallback {
                        modelView,
                        projection,
                        cameraX,
                        cameraY,
                        cameraZ,
                        _,
                    ->
                    IrisFoliageShaders.drawShadow(
                        modelView,
                        projection,
                        cameraX,
                        cameraY,
                        cameraZ,
                    )
                }
            } catch (exception: LinkageError) {
                irisUnsupported = true

                AllThingsFoliage.logger.warn(
                    "The installed Iris lacks the shadow callback required for GPU foliage; " +
                            "foliage is left to the chunk mesh while a shader pack is loaded",
                    exception,
                )
            }
        }
    }

    /** Registers the opaque-leaf pipeline using Iris's solid terrain pass. */
    fun setUpOpaque(foliageOpaquePipeline: RenderPipeline) {
        if (opaquePipeline != null) {
            return
        }

        opaquePipeline = foliageOpaquePipeline

        val api = IrisApi.getInstance()

        api.assignPipeline(
            foliageOpaquePipeline,
            IrisProgram.TERRAIN_SOLID,
        )

        api.assignPipelineShadow(
            foliageOpaquePipeline,
            IrisShadowProgram.SHADOW_TERRAIN_CUTOUT,
        )
    }

    /** Adds the foliage layouts without erasing or casting their element type. */
    fun withFoliageLayouts(irisLayouts: List<BindGroupLayout>): List<BindGroupLayout> {
        return irisLayouts + layouts
    }

    /**
     * Reuses Iris's pipeline state with a foliage program and matching vertex attributes.
     *
     * Replacement pipelines share programs; their vertex arrays are released separately.
     */
    fun withFoliageProgram(
        iris: CompiledRenderPipeline,
        solid: Boolean,
    ): CompiledRenderPipeline {
        val program = if (isRenderingShadows) {
            if (solid) shadowSolidProgram else shadowProgram
        } else {
            if (solid) mainSolidProgram else mainProgram
        } ?: return iris

        val frontend = iris as? FrontendRenderPipeline ?: return iris
        val irisPipeline = frontend.backendRenderPipeline() as? GlRenderPipeline
            ?: return iris

        foliagePipelines.get(iris)?.let { cached ->
            return cached
        }

        // Iris adds this interface to the OpenGL pipeline through Mixin.
        val original = ((irisPipeline as Any) as GlRenderPipelineAccess).createInfo

        val vertexBuffers =
            mutableListOf<BackendRenderPipeline.CreateInfo.VertexBuffer>()

        val attributes =
            mutableListOf<BackendRenderPipeline.CreateInfo.AttribBinding>()

        val formats = frontend.vertexFormats()

        for (slot in formats.indices) {
            val format = formats[slot] ?: continue

            vertexBuffers.add(
                BackendRenderPipeline.CreateInfo.VertexBuffer(
                    slot,
                    format.vertexSize,
                    format.stepRate,
                )
            )

            for (element in format.elements) {
                var location = GL33C.glGetAttribLocation(
                    program.programId,
                    "iris_${element.name()}",
                )

                if (location == -1) {
                    location = GL33C.glGetAttribLocation(
                        program.programId,
                        element.name(),
                    )
                }

                if (location != -1) {
                    attributes.add(
                        BackendRenderPipeline.CreateInfo.AttribBinding(
                            slot,
                            location,
                            element.offset(),
                            element.format(),
                        )
                    )
                }
            }
        }

        val createInfo = BackendRenderPipeline.CreateInfo(
            original.name(),
            original.shaders(),
            vertexBuffers,
            attributes,
            original.uniforms(),
            original.pushConstantsSize(),
            original.depthStencilState(),
            original.polygonMode(),
            original.cull(),
            original.colorTargetStates(),
            original.primitiveTopology(),
        )

        val vertexArray = VertexArray.createSource(
            GL.getCapabilities(),
            mutableSetOf(),
        ).apply(program, createInfo)

        vertexArrays.add(vertexArray)

        val built = FrontendRenderPipeline(
            frontend.name(),
            GlRenderPipeline(
                IrisRenderSystem.getGlDevice(),
                createInfo,
                program,
                vertexArray,
            ),
            formats,
            frontend.uniformIndices(),
            frontend.uniforms(),
            frontend.colorTargetStates(),
            frontend.wantsDepthTexture(),
            frontend.pushConstantSize(),
        )

        foliagePipelines.put(iris, built)

        return built
    }
}
