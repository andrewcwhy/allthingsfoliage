package io.github.andrewcwhy.allthingsfoliage.gpu.compat

import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.renderpearl.api.buffers.GpuBuffer
import com.mojang.renderpearl.api.pipeline.BindGroupLayout
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import com.mojang.renderpearl.api.vertex.VertexFormat
import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage
import io.github.andrewcwhy.allthingsfoliage.gpu.shader.IrisFoliageShaders
import net.irisshaders.iris.vertices.IrisVertexFormats
import net.minecraft.world.level.block.state.BlockState
import org.joml.Matrix4f

/**
 * Provides optional Iris integration without exposing Iris types to renderer callers.
 *
 * Operations guard against Iris being absent. Vertex formats require Iris to be installed.
 */
object IrisCompat {
    private val irisLoaded = AllThingsFoliage.platform.isModLoaded("iris")

    val hasOpaquePrograms: Boolean
        get() {
            return irisLoaded && IrisFoliageShaders.hasOpaquePrograms
        }

    val hasShadowProgram: Boolean
        get() {
            return irisLoaded && IrisFoliageShaders.hasShadowProgram
        }

    val isRenderingShadows: Boolean
        get() {
            return irisLoaded && IrisFoliageShaders.isRenderingShadows
        }

    val isShaderPackInUse: Boolean
        get() {
            return irisLoaded && IrisFoliageShaders.isShaderPackInUse
        }

    /** Identifies pack changes that require foliage geometry to be rebuilt. */
    val programGeneration: Int
        get() {
            if (!irisLoaded) {
                return 0
            }

            return IrisFoliageShaders.generation
        }

    /** Vertex format used by foliage region buffers when rendering through a shader pack. */
    val shaderPackFormat: VertexFormat
        get() {
            check(irisLoaded) {
                "Iris must be installed to access the shader-pack vertex format"
            }

            return IrisFoliageShaders.vertexFormat
        }

    /** Iris's terrain vertex format used while meshing sections. */
    val shaderPackMeshFormat: VertexFormat
        get() {
            check(irisLoaded) {
                "Iris must be installed to access the shader-pack meshing format"
            }

            return IrisVertexFormats.TERRAIN
        }

    /** Draws foliage using Iris's shadow-pass matrices and camera position. */
    fun interface ShadowDraw {
        fun draw(
            modelView: Matrix4f,
            projection: Matrix4f,
            cameraX: Double,
            cameraY: Double,
            cameraZ: Double,
        )
    }

    fun beginBlock(
        builder: BufferBuilder,
        state: BlockState,
        x: Int,
        y: Int,
        z: Int,
    ) {
        if (irisLoaded) {
            IrisFoliageShaders.beginBlock(builder, state, x, y, z)
        }
    }

    /** Binds foliage settings and interaction buffers to the shader-pack programs. */
    fun bindSwayBlocks(
        settings: GpuBuffer,
        interaction: GpuBuffer,
    ) {
        if (irisLoaded) {
            IrisFoliageShaders.bindSwayBlocks(settings, interaction)
        }
    }

    fun endBlock(builder: BufferBuilder) {
        if (irisLoaded) {
            IrisFoliageShaders.endBlock(builder)
        }
    }

    /** Runs a draw in the cutout terrain phase, restoring the previous phase even on failure. */
    fun inTerrainPhase(draw: () -> Unit) {
        if (!irisLoaded) {
            draw()
            return
        }

        val previousPhase = IrisFoliageShaders.beginTerrainPhase()

        try {
            draw()
        } finally {
            IrisFoliageShaders.endTerrainPhase(previousPhase)
        }
    }

    /** Builds the current pack's programs when necessary and reports whether they are ready. */
    fun programsReady(): Boolean {
        return irisLoaded && IrisFoliageShaders.ready()
    }

    /** Selects solid or cutout terrain within an active terrain phase. */
    fun setTerrainPhase(solid: Boolean) {
        if (irisLoaded) {
            IrisFoliageShaders.setTerrainPhase(solid)
        }
    }

    /** Supplies foliage pipelines, additional bind-group layouts, and shadow rendering. */
    fun setUp(
        pipeline: RenderPipeline,
        shadowPipeline: RenderPipeline?,
        layouts: List<BindGroupLayout>,
        drawShadow: ShadowDraw,
    ) {
        if (irisLoaded) {
            IrisFoliageShaders.setUp(
                pipeline,
                shadowPipeline,
                layouts,
                drawShadow,
            )
        }
    }

    fun setUpOpaque(pipeline: RenderPipeline) {
        if (irisLoaded) {
            IrisFoliageShaders.setUpOpaque(pipeline)
        }
    }

    /** Selects the foliage program variant, preserving the supplied pipeline when Iris is absent. */
    fun withFoliageProgram(
        iris: CompiledRenderPipeline,
        solid: Boolean,
    ): CompiledRenderPipeline {
        if (!irisLoaded) {
            return iris
        }

        return IrisFoliageShaders.withFoliageProgram(iris, solid)
    }
}
