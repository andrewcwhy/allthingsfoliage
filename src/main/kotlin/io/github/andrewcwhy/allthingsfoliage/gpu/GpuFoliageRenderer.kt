package io.github.andrewcwhy.allthingsfoliage.gpu

import com.github.razorplay01.sway.api.SwayAPI
import com.github.razorplay01.sway.api.behavior.BehaviorPipeline
import com.github.razorplay01.sway.api.behavior.contributors.DeformationContributor
import com.github.razorplay01.sway.api.behavior.contributors.MultiBlockContributor
import com.github.razorplay01.sway.client.behavior.multiblock.HangingVineMultiblockBehavior
import com.github.razorplay01.sway.config.SwayConfig
import com.mojang.blaze3d.buffers.Std140Builder
import com.mojang.blaze3d.buffers.Std140SizeCalculator
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.MeshData
import com.mojang.renderpearl.api.GpuFormat
import com.mojang.renderpearl.api.buffers.GpuBuffer
import com.mojang.renderpearl.api.buffers.GpuBufferSlice
import com.mojang.renderpearl.api.commands.RenderPass
import com.mojang.renderpearl.api.pipeline.BindGroupLayout
import com.mojang.renderpearl.api.pipeline.ColorTargetState
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline
import com.mojang.renderpearl.api.pipeline.DepthStencilState
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import com.mojang.renderpearl.api.pipeline.UniformType
import com.mojang.renderpearl.api.textures.FilterMode
import com.mojang.renderpearl.api.textures.GpuTexture
import com.mojang.renderpearl.api.textures.GpuTextureView
import com.mojang.renderpearl.api.vertex.VertexFormat
import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage
import io.github.andrewcwhy.allthingsfoliage.client.renderer.WavingWhitelist
import io.github.andrewcwhy.allthingsfoliage.Config
import io.github.andrewcwhy.allthingsfoliage.gpu.compat.IrisCompat
import io.github.andrewcwhy.allthingsfoliage.gpu.compat.SnowRealMagicCompat
import io.github.andrewcwhy.allthingsfoliage.gpu.compat.SodiumBridge
import io.github.andrewcwhy.allthingsfoliage.gpu.compat.TerrainSlabsCompat
import io.github.andrewcwhy.allthingsfoliage.gpu.shader.IrisVertexExtension
import io.github.andrewcwhy.allthingsfoliage.gpu.shader.SodiumFoliageShader
import io.github.andrewcwhy.allthingsfoliage.gpu.shader.TerrainFoliageShader
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.objects.Reference2BooleanOpenHashMap
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import java.nio.ByteBuffer
import java.util.Locale
import java.util.Optional
import java.util.OptionalDouble
import java.util.function.Predicate
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.sign
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.BindGroupLayouts
import net.minecraft.client.renderer.DynamicGpuData
import net.minecraft.client.renderer.ProjectionMatrixBuffer
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.block.BlockQuadOutput
import net.minecraft.client.renderer.block.BlockStateModelSet
import net.minecraft.client.renderer.block.ModelBlockRenderer
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.client.renderer.state.level.LevelRenderState
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.SectionPos
import net.minecraft.data.AtlasIds
import net.minecraft.resources.Identifier
import net.minecraft.util.Mth
import net.minecraft.world.level.block.LeavesBlock
import net.minecraft.world.level.block.PipeBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.chunk.LevelChunkSection
import net.minecraft.world.level.chunk.status.ChunkStatus
import net.minecraft.world.level.levelgen.structure.BoundingBox
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.FrustumIntersection
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil

/**
 * Meshes nearby foliage into region buffers and submits it through vanilla or shader-pack
 * pipelines.
 *
 * Mutable renderer state and native-buffer ownership belong to the render thread. Uploaded section
 * snapshots remain separate from replacement meshes until the region is uploaded again.
 */
object GpuFoliageRenderer {
    private const val BUILD_MARGIN: Int = 2
    private const val CELL_BIAS: Int = 64
    private const val COLUMN_CURVE_BLOCKS: Float = 2.0f
    private const val EXPOSURE_SCALE: Float = 15.0f
    private const val FULL_NEAR_SHARE: Float = 0.5f
    private const val FULL_NEAR_SHARE_UP_TO: Int = 8
    private const val HANGING_CURVE: Float = 3.0f
    private const val HANGING_EXPOSURE_SCALE: Float = 7.0f
    private const val INITIAL_SCRATCH_QUADS: Int = 1024
    private const val LENGTH_DAMPING_PER_BLOCK: Float = 0.2f
    private const val LIGHT_STREAM_MILLIS: Long = 3_000L
    private const val LONG_STRAND_SWAY: Float = 2.0f
    private const val MAX_LENGTH_DAMPING: Float = 0.8f
    private const val MIN_NEAR_RADIUS: Int = 2
    private const val MIN_NEAR_SHARE: Float = 0.25f
    private const val MOST_VERTICES_A_DRAW: Int = 65_536
    private const val NEAR_SHARE_DROP_PER_CHUNK: Float = 0.025f
    private const val OUT_OF_VIEW: Double = 1.0E12
    private const val PUSH_REACH_PAST_RADIUS: Float = 4.0f
    private const val PUSH_SCALE: Float = 255.0f
    private const val REBUILD_BUDGET: Int = 16
    private const val REBUILD_BUDGET_FRAME_SHARE: Long = 4L
    private const val REBUILD_BUDGET_MAX_NANOS: Long = 16_000_000L
    private const val REBUILD_BUDGET_NANOS: Long = 4_000_000L
    private const val REGION_HEIGHT_SHIFT: Int = 2
    private const val REGION_HEIGHT: Int = (1 shl REGION_HEIGHT_SHIFT)
    private const val REGION_UPLOAD_MAX_WAIT_MILLIS: Long = 500L
    private const val REGION_UPLOAD_STARVE_MILLIS: Long = 2_000L
    private const val REGION_WIDTH_SHIFT: Int = 3
    private const val REGION_WIDTH: Int = (1 shl REGION_WIDTH_SHIFT)
    private const val SECTIONS_PER_REGION: Int = REGION_WIDTH * REGION_HEIGHT * REGION_WIDTH
    private const val SECTION_SIZE: Int = SectionPos.SECTION_SIZE
    private const val SHELTER_RECHECK_BLOCKS: Double = 2.0
    private const val SHORT_HANGING_STRAND: Int = 2
    private const val SNOW_LADEN_SWAY: Float = 0.20f
    private const val SNOW_LADEN_WIND: Float = 0.80f
    private const val STALK_CURVE: Float = 2.0f
    private const val SWAY_MARGIN: Double = 5.0
    private const val SWAY_SETTINGS_UNIFORM: String = "FoliageSway"
    private const val SWAY_SPAN: Float = 3.0f
    private const val UNBOUNDED_REACH: Float = 100_000.0f
    private const val UPLOAD_BUDGET_SHARE: Long = 2L
    private const val URGENT_BUDGET_MAX_NANOS: Long = 24_000_000L
    private const val URGENT_BUDGET_MIN_NANOS: Long = 8_000_000L
    private const val VINE_SPAN: Float = 4.0f
    private const val VINE_TIP_SWAY: Float = 0.6f
    private const val WEATHER_WIND_FADE_BLOCKS: Float = 4.0f
    private const val WEIGHT_BYTES: Int = Float.SIZE_BYTES * 2
    private const val WEIGHT_SCALE: Float = 1023.0f
    private const val WIND_EDGE_JUMP: Double = 48.0
    private const val WIND_EDGE_SPEED: Double = 16.0

    // Pipeline construction follows vertex-layout and bind-group dependencies.
    private val foliageVertexFormat: VertexFormat = foliageFormat()
    private val swaySettingsLayout: BindGroupLayout =
        BindGroupLayout.builder()
            .withUniform(SWAY_SETTINGS_UNIFORM, UniformType.UNIFORM_BUFFER)
            .build()
    private val swaySettingsSize: Int =
        Std140SizeCalculator().putFloat().putFloat().putVec4().putVec4().get()
    private val defaultPipeline: RenderPipeline =
        modernPipeline("pipeline/foliage", true, GpuFormat.RGBA8_UNORM)
    private val defaultOpaquePipeline: RenderPipeline =
        modernPipeline("pipeline/foliage_opaque", false, GpuFormat.RGBA8_UNORM)
    private val sodiumCutoutPipeline: RenderPipeline =
        sodiumPipeline("pipeline/foliage_sodium", GpuFormat.RGBA8_UNORM)
    private val sodiumOpaquePipeline: RenderPipeline =
        sodiumPipeline("pipeline/foliage_sodium_opaque", GpuFormat.RGBA8_UNORM)
    private val terrainCutoutPipeline: RenderPipeline =
        terrainPipeline("pipeline/foliage_terrain", true, GpuFormat.RGBA8_UNORM)
    private val terrainOpaquePipeline: RenderPipeline =
        terrainPipeline("pipeline/foliage_terrain_opaque", false, GpuFormat.RGBA8_UNORM)
    private val defaultLevelPipelines: LevelPipelines =
        LevelPipelines(
            defaultPipeline,
            defaultOpaquePipeline,
            sodiumCutoutPipeline,
            sodiumOpaquePipeline,
            terrainCutoutPipeline,
            terrainOpaquePipeline,
        )
    private var active: Boolean = false
    private val animatedSpriteCache: Reference2BooleanOpenHashMap<TextureAtlasSprite> =
        Reference2BooleanOpenHashMap()
    private val blockVertexBytes: Int = DefaultVertexFormat.BLOCK.vertexSize
    private var burstAnchorNanos: Long = 0L
    private var burstAsked: Int = 0
    private var burstFrames: Int = 0
    private var burstLightQueued: Int = 0
    private var burstLightStreamed: Int = 0
    private var burstMeshMaxNanos: Long = 0L
    private var burstMeshNanos: Long = 0L
    private var burstPatchNanos: Long = 0L
    private var burstPatched: Int = 0
    private var burstPeakQueue: Int = 0
    private var burstRebuilt: Int = 0
    private var burstRebuiltHidden: Int = 0
    private var burstStartNanos: Long = 0L
    private var burstUploadMaxNanos: Long = 0L
    private var burstUploadNanos: Long = 0L
    private var burstUploads: Int = 0
    private var burstWaitMaxMillis: Long = 0L
    private var burstWeightNanos: Long = 0L
    private val directions = Direction.entries
    private val dirtyRegions = linkedSetOf<Region>()
    private val dirtySections = linkedSetOf<Long>()
    private val drawnRegions = mutableListOf<Region>()
    private var draws: IntArray = IntArray(256 * 3)
    private var drawsSize: Int = 0
    private val failedPipelines: MutableMap<RenderPipeline, CompiledRenderPipeline?> =
        Reference2ObjectOpenHashMap<RenderPipeline, CompiledRenderPipeline>()
    private val foliagePredicate: Predicate<BlockState> = Predicate(GpuFoliageSplit::mayHoldFoliage)
    private var frameBudgetNanos: Long = REBUILD_BUDGET_NANOS
    private var lastFrameNanos: Long = 0L
    private var lastSprite: TextureAtlasSprite? = null
    private var levelFog: GpuBufferSlice? = null
    private val levelModelView: Matrix4f = Matrix4f()
    private val lightChangedAt: Long2LongOpenHashMap = Long2LongOpenHashMap()
    private val measureToLog: Boolean = Path("mc2-foliage-timing").exists()
    private var meshedForShaderPack: Boolean = false
    private var meshedPackGeneration: Int = 0
    private var modelRenderer: ModelBlockRenderer? = null
    private var opaqueDraws: IntArray = IntArray(64 * 3)
    private var opaqueDrawsSize: Int = 0
    private var pendingDraw: ((RenderPass) -> Unit)? = null
    private var pendingLevelUniforms: (() -> Unit)? = null
    private var pipelineRefused: Boolean = false
    private val pipelinesByColorFormat: MutableMap<GpuFormat, LevelPipelines> = mutableMapOf()
    private val regionsByKey = hashMapOf<Long, Region>()
    private val regionsWithQueuedSections: LongOpenHashSet = LongOpenHashSet()
    private var reseedPending: Boolean = false
    private val sectionSprites = ReferenceOpenHashSet<TextureAtlasSprite>()
    private lateinit var shaderPackOpaquePipeline: RenderPipeline
    private lateinit var shaderPackPipeline: RenderPipeline
    private var shaderPackSetUp: Boolean = false
    private var shadowProjectionUniform: ProjectionMatrixBuffer? = null
    private var shelterActive: Boolean = false
    private var shelterCheckedReach: Float = Float.NaN
    private var shelterCheckedX: Double = 0.0
    private var shelterCheckedZ: Double = 0.0
    private var swaySettingsBuffer: GpuBuffer? = null
    private var uncullingRenderer: ModelBlockRenderer? = null
    private var uploadStaging: ByteBuffer? = null
    private var uploadedCalmSway: Float = Float.NaN
    private val uploadedEdge: Vector4f = Vector4f(Float.NaN)
    private var uploadedIntensity: Float = Float.NaN
    private val uploadedWeather: Vector4f = Vector4f(Float.NaN)
    private val urgentRegions: LongOpenHashSet = LongOpenHashSet()
    private val urgentSections: LongOpenHashSet = LongOpenHashSet()
    private lateinit var vertexScratch: ByteBufferBuilder
    private var warnedForeignVertexFormat: Boolean = false
    private var warnedWeightCountMismatch: Boolean = false
    private lateinit var weightScratch: ByteBuffer
    private val windEdge: DoubleArray =
        doubleArrayOf(Double.NaN, Double.NaN, Double.NaN, Double.NaN)
    private var windEdgeMovedNanos: Long = 0L

    private val buildRadius: Int
        get() {
            return if (GpuFoliageSplit.hasArea) {
                GpuFoliageSplit.radius + BUILD_MARGIN
            } else {
                0
            }
        }

    private val calmSway: Float
        get() {
            return if (Config.wavingFoliage) 1.0f else 0.0f
        }

    private val canDrawOpaqueLeaves: Boolean
        get() {
            return !IrisCompat.isShaderPackInUse || IrisCompat.hasOpaquePrograms
        }

    private val pushRadius: Int
        get() {
            return ceil(
                ((SwayConfig.INSTANCE.maxDistance + PUSH_REACH_PAST_RADIUS) / SECTION_SIZE)
                    .toDouble()
            ).toInt()
        }

    private val swayIntensity: Float
        get() {
            return if (Config.wavingFoliage) {
                Config.wavingIntensity
            } else {
                Config.DEFAULT_WAVING_INTENSITY
            }
        }

    private val weatherWindReach: Float
        get() {
            return when (Config.weatherWindDistance) {
                Config.GpuDistance.PERFORMANCE -> SwayConfig.INSTANCE.maxDistance
                Config.GpuDistance.HALF ->
                    ((GpuFoliageSplit.radius + 0.5f) * SECTION_SIZE) / 2.0f

                else -> UNBOUNDED_REACH
            }
        }

    private fun adaptiveRadius(renderDistance: Int): Int {
        if (renderDistance <= FULL_NEAR_SHARE_UP_TO) {
            return renderDistance / 2
        }
        val share: Float =
            maxOf(
                MIN_NEAR_SHARE,
                FULL_NEAR_SHARE -
                        (renderDistance - FULL_NEAR_SHARE_UP_TO) * NEAR_SHARE_DROP_PER_CHUNK,
            )
        return Math.round(renderDistance * share)
    }

    private fun addDraw(regionIndex: Int, firstVertexValue: Int, vertexCountValue: Int) {
        var firstVertex = firstVertexValue
        var vertexCount = vertexCountValue
        while (vertexCount > 0) {
            val piece: Int = minOf(vertexCount, MOST_VERTICES_A_DRAW)
            if (drawsSize + 3 > draws.size) {
                draws = draws.copyOf(draws.size * 2)
            }
            draws[drawsSize++] = regionIndex
            draws[drawsSize++] = firstVertex
            draws[drawsSize++] = piece
            firstVertex += piece
            vertexCount -= piece
        }
    }

    private fun addOpaqueDraw(regionIndex: Int, firstVertexValue: Int, vertexCountValue: Int) {
        var firstVertex = firstVertexValue
        var vertexCount = vertexCountValue
        while (vertexCount > 0) {
            val piece: Int = minOf(vertexCount, MOST_VERTICES_A_DRAW)
            if (opaqueDrawsSize + 3 > opaqueDraws.size) {
                opaqueDraws = opaqueDraws.copyOf(opaqueDraws.size * 2)
            }
            opaqueDraws[opaqueDrawsSize++] = regionIndex
            opaqueDraws[opaqueDrawsSize++] = firstVertex
            opaqueDraws[opaqueDrawsSize++] = piece
            firstVertex += piece
            vertexCount -= piece
        }
    }

    private fun animates(sprite: TextureAtlasSprite): Boolean {
        if (!animatedSpriteCache.containsKey(sprite)) {
            animatedSpriteCache.put(sprite, SodiumBridge.animates(sprite))
        }
        return animatedSpriteCache.getBoolean(sprite)
    }

    private fun buildNearest(minecraft: Minecraft, camera: Vec3, frustum: Frustum?) {
        val clientLevel = minecraft.level
        val nearest: LongArray = LongArray(REBUILD_BUDGET)
        val distances: DoubleArray = DoubleArray(REBUILD_BUDGET)
        distances.fill(Double.MAX_VALUE)
        var found: Int = 0
        val sodium = SodiumBridge.renderer()
        val pending = dirtySections.iterator()

        while (pending.hasNext()) {
            val key: Long = pending.next()

            if (!withinRange(minecraft, key)) {
                pending.remove()
                urgentSections.remove(key)
                continue
            }

            var distance: Double = distanceSqTo(key, camera)

            if (!inView(key, frustum, sodium)) {
                distance += OUT_OF_VIEW
            }

            for (slot in 0 until REBUILD_BUDGET) {
                if (distance < distances[slot]) {
                    val tail: Int = REBUILD_BUDGET - slot - 1
                    distances.copyInto(distances, slot + 1, slot, slot + tail)
                    nearest.copyInto(nearest, slot + 1, slot, slot + tail)
                    distances[slot] = distance
                    nearest[slot] = key
                    found = minOf(found + 1, REBUILD_BUDGET)
                    break
                }
            }
        }

        val begin: Long = System.nanoTime()
        var hiddenBuilt: Int = 0
        val budgetEnd: Long = begin + frameBudgetNanos
        val urgentEnd: Long =
            begin +
                    maxOf(
                        URGENT_BUDGET_MIN_NANOS,
                        minOf(frameBudgetNanos * 2L, URGENT_BUDGET_MAX_NANOS),
                    )
        for (i in 0 until found) {
            val key: Long = nearest[i]
            val urgent: Boolean = distances[i] < OUT_OF_VIEW && urgentSections.contains(key)
            val now: Long = System.nanoTime()
            val hidden: Boolean = distances[i] >= OUT_OF_VIEW
            if (hidden && hiddenBuilt > 0) {
                break
            }
            if (i > 0 && now >= (if (urgent) urgentEnd else budgetEnd)) {
                if (now >= urgentEnd) {
                    break
                }
                continue
            }
            dirtySections.remove(key)
            if (urgentSections.remove(key)) {
                urgentRegions.add(regionKeyOf(key))
            }
            val start: Long = System.nanoTime()
            rebuild(minecraft, requireNotNull(clientLevel), key)
            val taken: Long = System.nanoTime() - start
            burstRebuilt++
            if (hidden) {
                hiddenBuilt++
                burstRebuiltHidden++
            }
            burstMeshNanos += taken
            burstMeshMaxNanos = maxOf(burstMeshMaxNanos, taken)
        }
    }

    private fun collectDraws(frustum: Frustum?, sodium: Any?): Boolean {
        val drawn: MutableList<Region> = drawnRegions
        drawn.clear()
        drawsSize = 0
        opaqueDrawsSize = 0
        for (region in regionsByKey.values) {
            if (frustum != null && !frustum.isVisible(region.bounds)) {
                continue
            }
            val sectionFrustum: Frustum? =
                if (
                    frustum != null &&
                    frustum.cubeInFrustum(region.blockBounds) == FrustumIntersection.INSIDE
                )
                    null
                else frustum
            val regionIndex: Int = drawn.size
            val drawsBefore: Int = drawsSize
            val opaqueDrawsBefore: Int = opaqueDrawsSize
            var runStart: Int = -1
            var runEnd: Int = -1
            var opaqueStart: Int = -1
            var opaqueEnd: Int = -1
            for (i in 0 until region.occupiedCount) {
                val slot: Int = region.occupied[i]
                val section = requireNotNull(region.uploadedSections[slot])
                if (!isVisible(section, sectionFrustum, sodium)) {
                    if (runStart >= 0) {
                        addDraw(regionIndex, runStart, runEnd - runStart)
                        runStart = -1
                    }
                    if (opaqueStart >= 0) {
                        addOpaqueDraw(regionIndex, opaqueStart, opaqueEnd - opaqueStart)
                        opaqueStart = -1
                    }
                    continue
                }
                val animatedSprites = section.animatedSprites
                if (animatedSprites != null) {
                    SodiumBridge.markActive(animatedSprites)
                }
                if (runStart < 0) {
                    runStart = region.firstVertex[slot]
                }
                runEnd = region.firstVertex[slot] + section.cutoutVertexCount
                if (opaqueStart < 0) {
                    opaqueStart = region.firstOpaqueVertex[slot]
                }
                opaqueEnd = region.firstOpaqueVertex[slot] + section.opaqueVertexCount
            }
            if (runStart >= 0) {
                addDraw(regionIndex, runStart, runEnd - runStart)
            }
            if (opaqueStart >= 0) {
                addOpaqueDraw(regionIndex, opaqueStart, opaqueEnd - opaqueStart)
            }
            if (drawsSize > drawsBefore || opaqueDrawsSize > opaqueDrawsBefore) {
                drawn.add(region)
            }
        }
        return !drawn.isEmpty()
    }

    private fun colorTarget(format: GpuFormat): ColorTargetState {
        return ColorTargetState(
            ColorTargetState.DEFAULT.blendFunction(),
            format,
            ColorTargetState.DEFAULT.writeMask(),
        )
    }

    private fun compiledPipeline(requested: RenderPipeline): CompiledRenderPipeline? {
        val terrain: CompiledRenderPipeline? =
            RenderSystem.getCompiledPipelineNullable(RenderPipelines.SOLID_TERRAIN)
        if (failedPipelines.containsKey(requested) && failedPipelines.get(requested) === terrain) {
            return null
        }

        var compiled: CompiledRenderPipeline? = null
        try {
            compiled = RenderSystem.getCompiledPipelineNullable(requested)
        } catch (e: RuntimeException) {
            AllThingsFoliage.logger.error(
                "The GPU foliage pipeline did not compile; the foliage it holds is not drawn",
                e,
            )
            compiled = null
        }

        if (compiled == null) {
            failedPipelines.put(requested, terrain)
        }

        return compiled
    }

    private fun deactivate(minecraft: Minecraft) {
        active = false
        if (GpuFoliageSplit.hasArea) {
            val centerX: Int = GpuFoliageSplit.centerX
            val centerZ: Int = GpuFoliageSplit.centerZ
            val radius: Int = GpuFoliageSplit.radius
            GpuFoliageSplit.clearArea()
            for (chunkX in centerX - radius..centerX + radius) {
                for (chunkZ in centerZ - radius..centerZ + radius) {
                    remeshColumn(minecraft, chunkX, chunkZ, false)
                }
            }
        }
        discardAll()
    }

    fun discardAll() {
        regionsByKey.values.forEach(Region::free)
        regionsByKey.clear()
        dirtySections.clear()
        dirtyRegions.clear()
        urgentSections.clear()
        urgentRegions.clear()
        lightChangedAt.clear()
        animatedSpriteCache.clear()
        modelRenderer = null
        uncullingRenderer = null
        reseedPending = true
        GpuFoliageInteraction.reset()
    }

    fun discoverChunk(level: ClientLevel, chunk: LevelChunk) {
        if (!active) {
            return
        }

        val chunkX = chunk.pos.x
        val chunkZ = chunk.pos.z
        val sections: Array<LevelChunkSection> = chunk.sections

        for (index in 0 until sections.size) {
            val sectionY: Int = level.getSectionYFromSectionIndex(index)

            if (mayHoldFoliage(sections[index])) {
                dirtySections.add(SectionPos.asLong(chunkX, sectionY, chunkZ))
            }

            if (shelterActive) {
                markIfHeld(SectionPos.asLong(chunkX - 1, sectionY, chunkZ))
            }
        }
    }

    private fun distanceSqTo(key: Long, camera: Vec3): Double {
        return camera.distanceToSqr(
            SectionPos.sectionToBlockCoord(SectionPos.x(key)) + SECTION_SIZE / 2.0,
            SectionPos.sectionToBlockCoord(SectionPos.y(key)) + SECTION_SIZE / 2.0,
            SectionPos.sectionToBlockCoord(SectionPos.z(key)) + SECTION_SIZE / 2.0,
        )
    }

    fun draw(levelState: LevelRenderState) {
        val cameraState: CameraRenderState? = levelState.cameraRenderState
        val hasCamera: Boolean = cameraState != null
        pendingDraw = null
        pendingLevelUniforms = null
        if (hasCamera) {
            levelModelView.set(cameraState.viewRotationMatrix)
        }
        val frustum: Frustum? = if (hasCamera) cameraState.cullFrustum else null
        drawFrame(if (hasCamera) cameraState.pos else null, frustum)
    }

    private fun drawFrame(camera: Vec3?, frustum: Frustum?) {
        val minecraft: Minecraft = Minecraft.getInstance()
        val player = minecraft.player
        val clientLevel = minecraft.level
        if (clientLevel == null) {
            return
        }
        GpuFoliageSplit.applyMeshDecisions()
        if (WavingWhitelist.followGameLeaves(canDrawOpaqueLeaves)) {
            foliageListChanged()
        }
        if (!Config.gpuRenderer || !SodiumBridge.canSplitChunkMeshes()) {
            if (active) {
                deactivate(minecraft)
            }
            return
        }
        val shaderPack: Boolean = IrisCompat.isShaderPackInUse
        if (shaderPack) {
            setUpShaderPack()
        }
        if (shaderPack && !IrisCompat.programsReady()) {
            if (active) {
                deactivate(minecraft)
            }
            return
        }
        val packGeneration: Int = if (shaderPack) IrisCompat.programGeneration else 0
        if (
            active && (shaderPack != meshedForShaderPack || packGeneration != meshedPackGeneration)
        ) {
            deactivate(minecraft)
        }
        if (!active) {
            active = true
            reseedPending = true
            meshedForShaderPack = shaderPack
            meshedPackGeneration = packGeneration
        }
        if (camera == null) {
            return
        }
        updateNearArea(minecraft)
        followWindEdge()
        followRain(minecraft)
        GpuFoliageSplit.forEachRebuildDue({ key ->
            rebuildChunkMesh(minecraft, SectionPos.x(key), SectionPos.y(key), SectionPos.z(key))
        })
        GpuFoliageSplit.forEachRemeshDue(dirtySections::add)
        if (reseedPending && player != null) {
            reseedPending = false
            reseedLoadedChunks(minecraft, requireNotNull(clientLevel))
        }
        measureFrame()
        buildNearest(minecraft, camera, frustum)
        uploadDueRegion(camera)
        measureBurst()
        val regions = regionsByKey.entries.iterator()
        while (regions.hasNext()) {
            val entry = regions.next()
            if (!regionWithinRange(minecraft, entry.key)) {
                entry.value.free()
                regions.remove()
                dirtyRegions.remove(entry.value)
            }
        }
        val anything: Boolean = collectDraws(frustum, SodiumBridge.renderer())
        if (anything) {
            submitDraws(minecraft, camera, null, null, null, null)
        }
    }

    fun drawIntoLevelPass(pass: RenderPass) {
        val draw: ((RenderPass) -> Unit)? = pendingDraw
        pendingDraw = null
        if (draw != null) {
            draw(pass)
        }
    }

    private fun drawIntoShadowMap(
        minecraft: Minecraft,
        camera: Vec3,
        modelView: Matrix4f,
        projection: Matrix4f,
    ) {
        val interaction: GpuBuffer? = GpuFoliageInteraction.uploadedBuffer
        val fog: GpuBufferSlice? = levelFog
        if (interaction == null || fog == null || compiledPipeline(shaderPackPipeline) == null) {
            return
        }
        val regions: Array<Region> = drawnRegions.toTypedArray()
        for (region in regions) {
            if (region.vertices == null) {
                return
            }
        }
        val noModulation: Vector4f = Vector4f(1.0f, 1.0f, 1.0f, 1.0f)
        val noTextureTransform: Matrix4f = Matrix4f()
        val transforms = arrayOfNulls<DynamicGpuData.Transform>(regions.size)
        for (i in 0 until regions.size) {
            val origin: BlockPos = regions[i].origin
            transforms[i] =
                DynamicGpuData.Transform(
                    modelView,
                    noModulation,
                    Vector3f(
                        ((origin.x - camera.x)).toFloat(),
                        ((origin.y - camera.y)).toFloat(),
                        ((origin.z - camera.z)).toFloat(),
                    ),
                    noTextureTransform,
                )
        }
        val regionBlocks =
            RenderSystem.getDynamicUniforms().writeTransforms(*transforms.requireNoNulls())
        val drawList: IntArray = draws.copyOf(drawsSize)
        val opaqueList: IntArray = opaqueDraws.copyOf(opaqueDrawsSize)
        val solidShadow: Boolean =
            opaqueList.size > 0 &&
                    IrisCompat.hasOpaquePrograms &&
                    compiledPipeline(shaderPackOpaquePipeline) != null
        var longestDraw: Int = 0
        for (i in (2 until drawList.size) step 3) {
            longestDraw = maxOf(longestDraw, drawList[i])
        }
        for (i in (2 until opaqueList.size) step 3) {
            longestDraw = maxOf(longestDraw, opaqueList[i])
        }
        val indices: RenderSystem.AutoStorageIndexBuffer =
            RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS)
        val indexBuffer: GpuBuffer? = indices.getBuffer(indexCountFor(longestDraw))
        val settings: GpuBuffer = uploadSwaySettings(camera)
        val sunProjection: GpuBufferSlice = shadowProjectionBuffer().getBuffer(projection)
        val atlas: AbstractTexture = minecraft.atlasManager.getAtlasOrThrow(AtlasIds.BLOCKS)
        val target: RenderTarget = minecraft.gameRenderer.mainRenderTarget()
        RenderSystem.getDevice()
            .createCommandEncoder()
            .createRenderPass(
                { "All Things Foliage shadow" },
                requireNotNull(target.getColorTextureView()),
                Optional.empty(),
                target.getDepthTextureView(),
                OptionalDouble.empty(),
            )
            .use { pass ->
                RenderSystem.bindDefaultUniforms(pass)
                for (kind in 0 until 2) {
                    val list: IntArray = if (kind == 0) opaqueList else drawList
                    if (list.size == 0) {
                        continue
                    }
                    val drawnSolid: Boolean = kind == 0 && solidShadow
                    pass.setPipeline(
                        IrisCompat.withFoliageProgram(
                            RenderSystem.getCompiledPipeline(
                                if (drawnSolid) shaderPackOpaquePipeline else shaderPackPipeline
                            ),
                            drawnSolid,
                        )
                    )
                    pass.setUniform("Projection", sunProjection)
                    pass.setUniform("Fog", fog)
                    pass.setUniform("Sampler0", atlas.textureView, atlas.sampler)
                    pass.setUniform(
                        "Sampler2",
                        minecraft.gameRenderer.lightmap(),
                        RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR),
                    )
                    pass.setUniform(SWAY_SETTINGS_UNIFORM, settings)
                    pass.setUniform(GpuFoliageInteraction.UNIFORM, interaction)
                    pass.setIndexBuffer(requireNotNull(indexBuffer), indices.type())
                    IrisCompat.bindSwayBlocks(settings, interaction)
                    var boundRegion: Int = -1
                    for (i in (0 until list.size) step 3) {
                        val regionIndex: Int = list[i]
                        if (regionIndex != boundRegion) {
                            pass.setUniform(
                                "DynamicTransforms",
                                requireNotNull(regionBlocks[regionIndex]),
                            )
                            pass.setVertexBuffer(
                                0,
                                requireNotNull(regions[regionIndex].vertices).slice(),
                            )
                            boundRegion = regionIndex
                        }
                        pass.drawIndexed(indexCountFor(list[i + 2]), 1, 0, list[i + 1], 0)
                    }
                }
            }
    }

    fun drawPolytoneShadow(shadowMatrix: Matrix4f, color: GpuTextureView, depth: GpuTextureView) {
        val minecraft: Minecraft = Minecraft.getInstance()
        val clientLevel = minecraft.level
        if (!active || meshedForShaderPack || clientLevel == null) {
            return
        }
        val camera: Vec3 = minecraft.gameRenderer.mainCamera().position()
        if (collectDraws(null, null)) {
            submitDraws(minecraft, camera, Matrix4f(), shadowMatrix, color, depth)
        }
    }

    private fun drawShadow(
        modelView: Matrix4f,
        projection: Matrix4f,
        cameraX: Double,
        cameraY: Double,
        cameraZ: Double,
    ) {
        val minecraft: Minecraft = Minecraft.getInstance()
        val clientLevel = minecraft.level
        if (
            !active || !meshedForShaderPack || !IrisCompat.hasShadowProgram || clientLevel == null
        ) {
            return
        }
        val anything: Boolean = collectDraws(null, null)
        if (anything) {
            drawIntoShadowMap(minecraft, Vec3(cameraX, cameraY, cameraZ), modelView, projection)
        }
    }

    private fun foliageFormat(): VertexFormat {
        val builder: VertexFormat.Builder = VertexFormat.builder(0)
        for (element in DefaultVertexFormat.BLOCK.elements) {
            builder.addAttribute(element.name(), element.format())
        }
        return builder
            .addAttribute("SwayCell", GpuFormat.R32_FLOAT)
            .addAttribute("SwayWeights", GpuFormat.R32_FLOAT)
            .build()
    }

    fun foliageListChanged() {
        GpuFoliageSplit.foliageChanged()
        val minecraft: Minecraft = Minecraft.getInstance()
        val clientLevel = minecraft.level
        val level = clientLevel
        if (!active || level == null || !GpuFoliageSplit.hasArea) {
            return
        }
        val centerX: Int = GpuFoliageSplit.centerX
        val centerZ: Int = GpuFoliageSplit.centerZ
        val radius: Int = GpuFoliageSplit.radius
        val takingOver: Boolean = WavingWhitelist.leavesWave()
        for (chunkX in centerX - radius..centerX + radius) {
            for (chunkZ in centerZ - radius..centerZ + radius) {
                val chunk: LevelChunk? =
                    level.chunkSource.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false)
                if (chunk == null || !GpuFoliageSplit.isNear(chunkX, chunkZ)) {
                    continue
                }
                val sections: Array<LevelChunkSection> = chunk.sections
                for (index in 0 until sections.size) {
                    val sectionY: Int = level.getSectionYFromSectionIndex(index)
                    val key: Long = SectionPos.asLong(chunkX, sectionY, chunkZ)
                    if (
                        !sections[index].maybeHas({ state ->
                            WavingWhitelist.listed(state.block) ||
                                    WavingWhitelist.restsOnLeaves(state.block)
                        })
                    ) {
                        if (
                            sections[index].maybeHas({ state ->
                                WavingWhitelist.followsLeaves(state.block) ||
                                        HangingVineMultiblockBehavior.isHangingVine(state)
                            })
                        ) {
                            markIfHeld(key)
                        }
                        continue
                    }
                    if (GpuFoliageSplit.isGpuReady(key)) {
                        if (takingOver) {
                            GpuFoliageSplit.awaitGroup(key)
                            dirtySections.add(key)
                        } else {
                            rebuildChunkMesh(minecraft, chunkX, sectionY, chunkZ)
                            GpuFoliageSplit.remeshOnSwap(key)
                        }
                    } else {
                        dirtySections.add(key)
                    }
                }
            }
        }
    }

    private fun followRain(minecraft: Minecraft) {
        val player = minecraft.player
        val clientLevel = minecraft.level
        val raining: Boolean =
            Config.weatherWind && clientLevel != null && clientLevel.getRainLevel(1.0f) > 0.0f
        if (raining != shelterActive) {
            shelterActive = raining
            shelterCheckedReach = Float.NaN
        }
        if (!shelterActive || player == null) {
            return
        }
        val reach: Float = weatherWindReach
        val x: Double = player.x
        val z: Double = player.z
        if (
            reach == shelterCheckedReach &&
            (reach >= UNBOUNDED_REACH ||
                    abs(x - shelterCheckedX) + abs(z - shelterCheckedZ) < SHELTER_RECHECK_BLOCKS)
        ) {
            return
        }
        shelterCheckedReach = reach
        shelterCheckedX = x
        shelterCheckedZ = z
        for (region in regionsByKey.values) {
            for (section in region.sections) {
                if (
                    section != null &&
                    !section.sheltered &&
                    withinReach(section.origin, x, z, reach)
                ) {
                    dirtySections.add(section.key)
                }
            }
        }
    }

    private fun followWindEdge() {
        val now: Long = System.nanoTime()
        val step: Double = WIND_EDGE_SPEED * minOf((now - windEdgeMovedNanos) / 1.0E9, 0.1)
        windEdgeMovedNanos = now
        if (!GpuFoliageSplit.hasArea) {
            windEdge.fill(Double.NaN)
            return
        }
        val radius: Int = GpuFoliageSplit.radius
        val target: DoubleArray =
            doubleArrayOf(
                SectionPos.sectionToBlockCoord(GpuFoliageSplit.centerX - radius).toDouble(),
                SectionPos.sectionToBlockCoord(GpuFoliageSplit.centerZ - radius).toDouble(),
                SectionPos.sectionToBlockCoord(GpuFoliageSplit.centerX + radius + 1).toDouble(),
                SectionPos.sectionToBlockCoord(GpuFoliageSplit.centerZ + radius + 1).toDouble(),
            )
        var jump: Boolean = false
        for (i in 0 until target.size) {
            jump = jump or !(abs(target[i] - windEdge[i]) <= WIND_EDGE_JUMP)
        }
        for (i in 0 until target.size) {
            val gap: Double = target[i] - windEdge[i]
            windEdge[i] =
                if (jump || abs(gap) <= step) target[i] else windEdge[i] + sign(gap) * step
        }
    }

    fun forgetChunk(level: ClientLevel, chunk: LevelChunk) {
        val chunkX = chunk.pos.x
        val chunkZ = chunk.pos.z

        for (sectionY in level.minSectionY..level.maxSectionY) {
            val key: Long = SectionPos.asLong(chunkX, sectionY, chunkZ)
            dirtySections.remove(key)
            removeSection(key)
            GpuFoliageSplit.forgetSection(key)
        }
    }

    private fun holdsLeavesStill(level: ClientLevel, pos: BlockPos, state: BlockState): Boolean {
        if (state.isAir || state.block is LeavesBlock || WavingWhitelist.isLeaves(state.block)) {
            return false
        }
        if (WavingWhitelist.restsOnLeaves(state.block)) {
            return !WavingWhitelist.isLeaves(level.getBlockState(pos.below()).block)
        }
        return state.isCollisionShapeFullBlock(level, pos)
    }

    private fun inView(key: Long, frustum: Frustum?, sodium: Any?): Boolean {
        val x: Int = SectionPos.sectionToBlockCoord(SectionPos.x(key))
        val y: Int = SectionPos.sectionToBlockCoord(SectionPos.y(key))
        val z: Int = SectionPos.sectionToBlockCoord(SectionPos.z(key))
        if (
            frustum != null &&
            !frustum.isVisible(
                AABB(
                    x - SWAY_MARGIN,
                    y - SWAY_MARGIN,
                    z - SWAY_MARGIN,
                    x + SECTION_SIZE + SWAY_MARGIN,
                    y + SECTION_SIZE + SWAY_MARGIN,
                    z + SECTION_SIZE + SWAY_MARGIN,
                )
            )
        ) {
            return false
        }
        return (sodium == null ||
                SodiumBridge.isVisible(
                    sodium,
                    (x + 1).toDouble(),
                    (y + 1).toDouble(),
                    (z + 1).toDouble(),
                    (x + SECTION_SIZE - 1).toDouble(),
                    (y + SECTION_SIZE - 1).toDouble(),
                    (z + SECTION_SIZE - 1).toDouble(),
                ))
    }

    private fun indexCountFor(vertexCount: Int): Int {
        return (vertexCount / 4) * 6
    }

    private fun isVisible(section: Section, frustum: Frustum?, sodium: Any?): Boolean {
        if (!GpuFoliageSplit.chunkMeshLeftFoliage(section.key)) {
            return false
        }
        if (frustum != null && !frustum.isVisible(section.bounds)) {
            return false
        }
        if (sodium == null) {
            return true
        }
        val origin: BlockPos = section.origin
        return SodiumBridge.isVisible(
            sodium,
            (origin.x + 1).toDouble(),
            (origin.y + 1).toDouble(),
            (origin.z + 1).toDouble(),
            (origin.x + SECTION_SIZE - 1).toDouble(),
            (origin.y + SECTION_SIZE - 1).toDouble(),
            (origin.z + SECTION_SIZE - 1).toDouble(),
        )
    }

    private fun levelPipelines(minecraft: Minecraft): LevelPipelines {
        val color: GpuTexture? = minecraft.gameRenderer.mainRenderTarget().getColorTexture()
        val format: GpuFormat = if (color == null) GpuFormat.RGBA8_UNORM else color.format
        if (format == GpuFormat.RGBA8_UNORM) {
            return defaultLevelPipelines
        }
        return pipelinesByColorFormat.getOrPut(format) {
            val wanted = format
            LevelPipelines(
                modernPipeline("pipeline/foliage", true, wanted),
                modernPipeline("pipeline/foliage_opaque", false, wanted),
                sodiumPipeline("pipeline/foliage_sodium", wanted),
                sodiumPipeline("pipeline/foliage_sodium_opaque", wanted),
                terrainPipeline("pipeline/foliage_terrain", true, wanted),
                terrainPipeline("pipeline/foliage_terrain_opaque", false, wanted),
            )
        }
    }

    private fun markIfHeld(key: Long) {
        val region: Region? = regionsByKey.get(regionKeyOf(key))
        if (region != null && region.sections[slotOf(key)] != null) {
            dirtySections.add(key)
        }
    }

    private fun mayHoldFoliage(section: LevelChunkSection): Boolean {
        return !section.hasOnlyAir() && section.maybeHas(foliagePredicate)
    }

    private fun mayHoldFoliage(level: ClientLevel, key: Long): Boolean {
        val sectionY: Int = SectionPos.y(key)
        if (sectionY < level.minSectionY || sectionY > level.maxSectionY) {
            return false
        }
        return mayHoldFoliage(
            level
                .getChunk(SectionPos.x(key), SectionPos.z(key))
                .getSection(level.getSectionIndexFromSectionY(sectionY))
        )
    }

    private fun measureBurst() {
        if (!measureToLog && !AllThingsFoliage.logger.isDebugEnabled()) {
            return
        }
        if (!dirtySections.isEmpty() || !dirtyRegions.isEmpty()) {
            if (burstStartNanos == 0L) {
                burstStartNanos = System.nanoTime()
            }
            burstFrames++
            burstPeakQueue = maxOf(burstPeakQueue, dirtySections.size)
            return
        }
        if (burstRebuilt > 0) {
            if (burstStartNanos == 0L) {
                burstStartNanos = System.nanoTime()
                burstFrames = 1
            }
            val report: String =
                String.format(
                    Locale.ROOT,
                    "Foliage burst: %.0f ms over %d frames; %d sections rebuilt (%d queued by light, %d of them by a light " +
                            "on the move, %d asked for by another route, peak queue %d), " +
                            "meshing %.1f ms in all, %.1f ms the slowest; %d region uploads, %.1f ms in all, %.1f ms the " +
                            "slowest, longest wait before upload %d ms; %d sections written over their own, %.1f ms in all; %d of the sections rebuilt were out of view; of the meshing, " +
                            "%.1f ms setting each block up and %.1f ms on the vertices' weights",
                    (System.nanoTime() - burstStartNanos) / 1.0E6,
                    burstFrames,
                    burstRebuilt,
                    burstLightQueued,
                    burstLightStreamed,
                    burstAsked,
                    burstPeakQueue,
                    burstMeshNanos / 1.0E6,
                    burstMeshMaxNanos / 1.0E6,
                    burstUploads,
                    burstUploadNanos / 1.0E6,
                    burstUploadMaxNanos / 1.0E6,
                    burstWaitMaxMillis,
                    burstPatched,
                    burstPatchNanos / 1.0E6,
                    burstRebuiltHidden,
                    burstAnchorNanos / 1.0E6,
                    burstWeightNanos / 1.0E6,
                )
            if (measureToLog) {
                AllThingsFoliage.logger.info(report)
            } else {
                AllThingsFoliage.logger.debug(report)
            }
        }
        burstStartNanos = 0L
        burstFrames = 0
        burstLightQueued = 0
        burstLightStreamed = 0
        burstAsked = 0
        burstPatched = 0
        burstRebuiltHidden = 0
        burstAnchorNanos = 0L
        burstWeightNanos = 0L
        burstPatchNanos = 0L
        burstPeakQueue = 0
        burstRebuilt = 0
        burstMeshNanos = 0L
        burstMeshMaxNanos = 0L
        burstUploads = 0
        burstUploadNanos = 0L
        burstUploadMaxNanos = 0L
        burstWaitMaxMillis = 0L
    }

    private fun measureFrame() {
        val now: Long = System.nanoTime()
        val frame: Long = if (lastFrameNanos == 0L) 0L else now - lastFrameNanos
        lastFrameNanos = now
        frameBudgetNanos =
            maxOf(
                REBUILD_BUDGET_NANOS,
                minOf(frame / REBUILD_BUDGET_FRAME_SHARE, REBUILD_BUDGET_MAX_NANOS),
            )
    }

    private fun meshSection(minecraft: Minecraft, level: ClientLevel, key: Long) {
        val player = minecraft.player
        if (!mayHoldFoliage(level, key)) {
            removeSection(key)
            return
        }
        if (modelRenderer == null) {
            modelRenderer =
                ModelBlockRenderer(
                    minecraft.options.ambientOcclusion().get(),
                    true,
                    minecraft.blockColors,
                )
            uncullingRenderer =
                ModelBlockRenderer(
                    minecraft.options.ambientOcclusion().get(),
                    false,
                    minecraft.blockColors,
                )
        }
        if (!::vertexScratch.isInitialized) {
            vertexScratch = ByteBufferBuilder(blockVertexBytes * 4 * INITIAL_SCRATCH_QUADS)
            weightScratch = MemoryUtil.memAlloc(WEIGHT_BYTES * 4 * INITIAL_SCRATCH_QUADS)
        }
        vertexScratch.clear()
        weightScratch.clear()
        val origin: BlockPos =
            BlockPos(
                SectionPos.sectionToBlockCoord(SectionPos.x(key)),
                SectionPos.sectionToBlockCoord(SectionPos.y(key)),
                SectionPos.sectionToBlockCoord(SectionPos.z(key)),
            )
        val regionOrigin: BlockPos = regionOrigin(regionKeyOf(key))
        val shelter: WindShelter? =
            if (
                shelterActive &&
                player != null &&
                withinReach(origin, player.x, player.z, weatherWindReach)
            )
                WindShelter(level, origin)
            else null
        val offsetX: Int = origin.x - regionOrigin.x
        val offsetY: Int = origin.y - regionOrigin.y
        val offsetZ: Int = origin.z - regionOrigin.z
        val builder: BufferBuilder =
            BufferBuilder(vertexScratch, PrimitiveTopology.QUADS, DefaultVertexFormat.BLOCK)
        val anchor: SwayAnchor = SwayAnchor()
        sectionSprites.clear()
        lastSprite = null
        anchor.fieldShelter = if (shelter == null) null else WindShelter(level, origin, true)
        val output: BlockQuadOutput = { x, y, z, quad, instance ->
            builder.putBlockBakedQuad(x, y, z, quad, instance)
            noteSprite(quad.materialInfo().sprite())
            reserveWeights(WEIGHT_BYTES * 4)
            val cell: Float =
                packCell(
                    anchor.cellX - regionOrigin.x,
                    anchor.cellY - regionOrigin.y,
                    anchor.cellZ - regionOrigin.z,
                )
            for (vertex in 0 until 4) {
                val localY: Float = quad.position(vertex).y()
                val worldY: Float = regionOrigin.y + y + localY
                weightScratch.putFloat(cell)
                val worldX: Float = regionOrigin.x + x + quad.position(vertex).x()
                val worldZ: Float = regionOrigin.z + z + quad.position(vertex).z()
                weightScratch.putFloat(
                    packWeights(
                        anchor.waveAt(worldY) * anchor.pinAt(worldX, worldY, worldZ),
                        anchor.pushAt(localY, worldY),
                        anchor.still,
                        anchor.steady,
                        anchor.exposureAt(worldX, worldY, worldZ),
                        anchor.leafHeldAt(worldX, worldY, worldZ),
                    )
                )
            }
        }
        val models: BlockStateModelSet = minecraft.modelManager.blockStateModelSet
        val blocks: LevelChunkSection =
            level
                .getChunk(SectionPos.x(key), SectionPos.z(key))
                .getSection(level.getSectionIndexFromSectionY(SectionPos.y(key)))
        val pos: BlockPos.MutableBlockPos = BlockPos.MutableBlockPos()
        val neighbour: BlockPos.MutableBlockPos = BlockPos.MutableBlockPos()
        val opaqueLeaves: Boolean = WavingWhitelist.opaqueLeaves()
        var cutoutVertices: Int = 0
        for (walk in 0 until 2) {
            if (walk == 1) {
                cutoutVertices = weightScratch.position() / WEIGHT_BYTES
                if (!opaqueLeaves) {
                    break
                }
            }
            for (dx in 0 until SECTION_SIZE) {
                for (dy in 0 until SECTION_SIZE) {
                    for (dz in 0 until SECTION_SIZE) {
                        var state: BlockState = blocks.getBlockState(dx, dy, dz)
                        var raised: Float = 0.0f
                        var inSnow: Boolean = false
                        pos.set(origin.x + dx, origin.y + dy, origin.z + dz)
                        if (
                            !GpuFoliageSplit.isFoliage(state) &&
                            !GpuFoliageSplit.restsOnWavingLeaves(level, pos, state)
                        ) {
                            if (!SnowRealMagicCompat.isSnowBlock(state)) {
                                continue
                            }
                            val plant = SnowRealMagicCompat.plantIn(level, pos, state)
                            if (plant == null || !GpuFoliageSplit.isFoliage(plant)) {
                                continue
                            }
                            state = plant
                            raised = SnowRealMagicCompat.liftOf(plant)
                            inSnow = true
                        }
                        if (opaqueLeaves && state.block is LeavesBlock != (walk == 1)) {
                            continue
                        }
                        pos.set(origin.x + dx, origin.y + dy, origin.z + dz)
                        val anchorStart: Long = if (measureToLog) System.nanoTime() else 0L
                        anchor.prepare(state, pos, level)
                        anchor.exposure =
                            if (shelter == null) 1.0f
                            else shelter.exposureAt(pos.x, anchor.cellY, pos.z, anchor.length)
                        anchor.still = false
                        val laden: Boolean =
                            inSnow || SnowRealMagicCompat.standsOnSnow(level, pos, state)
                        anchor.waving = if (laden) SNOW_LADEN_SWAY else 1.0f
                        anchor.waving *= WavingWhitelist.intensityOf(state.block)
                        anchor.field = WavingWhitelist.wavesAsField(state.block)
                        anchor.restY =
                            if (WavingWhitelist.restsOnLeaves(state.block)) pos.y else Int.MIN_VALUE
                        anchor.steady = anchor.field
                        if (laden && !anchor.field) {
                            anchor.waving *= SNOW_LADEN_WIND / SNOW_LADEN_SWAY
                            anchor.still = true
                        }
                        if (anchor.field) {
                            anchor.exposure = 1.0f
                        }
                        anchor.hangsOnLeaves = false
                        anchor.pushless = false
                        anchor.vineFree = false
                        anchor.vineOnLeaves = false
                        if (anchor.hanging && WavingWhitelist.followsLeaves(state.block)) {
                            anchor.hangVine(level, pos, state)
                        } else if (
                            anchor.hanging &&
                            WavingWhitelist.leavesWave() &&
                            anchor.hangOnLeaves(level)
                        ) {
                            anchor.hangsOnLeaves = true
                            anchor.still = true
                            anchor.steady = true
                            anchor.exposure = 1.0f
                        }
                        if (measureToLog) {
                            burstAnchorNanos += System.nanoTime() - anchorStart
                        }
                        val lift: Float = TerrainSlabsCompat.offsetY(level, pos, state) + raised
                        if (meshedForShaderPack) {
                            IrisCompat.beginBlock(
                                builder,
                                state,
                                offsetX + dx,
                                offsetY + dy,
                                offsetZ + dz,
                            )
                        }
                        requireNotNull(
                            if (
                                anchor.field &&
                                partsFromNeighbour(
                                    level,
                                    blocks,
                                    dx,
                                    dy,
                                    dz,
                                    pos,
                                    neighbour,
                                )
                            )
                                uncullingRenderer
                            else modelRenderer
                        )
                            .tesselateBlock(
                                output,
                                (offsetX + dx).toFloat(),
                                offsetY + dy + lift,
                                (offsetZ + dz).toFloat(),
                                level,
                                pos,
                                state,
                                SnowRealMagicCompat.snowyVariant(
                                    GpuFoliageSplit.modelFor(state, models.get(state)),
                                    state,
                                    level,
                                    pos,
                                    inSnow,
                                ),
                                state.getSeed(pos),
                            )
                    }
                }
            }
        }
        if (meshedForShaderPack) {
            IrisCompat.endBlock(builder)
        }
        val mesh: MeshData? = builder.build()
        if (mesh == null) {
            removeSection(key)
            return
        }
        mesh.use {
            val expected: VertexFormat =
                if (meshedForShaderPack) IrisCompat.shaderPackMeshFormat
                else DefaultVertexFormat.BLOCK
            if (expected != mesh.drawState().format()) {
                warnForeignVertexFormat(mesh.drawState().format())
                removeSection(key)
                return
            }
            val vertexBytes: Int = expected.vertexSize
            val meshVertices: ByteBuffer = mesh.vertexBuffer()
            val vertexCount: Int = mesh.drawState().vertexCount()
            weightScratch.flip()
            if (weightScratch.remaining() != vertexCount * WEIGHT_BYTES) {
                warnWeightCountMismatch(vertexCount, weightScratch.remaining() / WEIGHT_BYTES)
                removeSection(key)
                return
            }
            val stride: Int = vertexBytes + WEIGHT_BYTES
            val meshBase: Int = meshVertices.position()
            val data: ByteBuffer = MemoryUtil.memAlloc(vertexCount * stride)
            for (vertex in 0 until vertexCount) {
                data.put(
                    vertex * stride,
                    meshVertices,
                    meshBase + vertex * vertexBytes,
                    vertexBytes,
                )
                data.put(
                    vertex * stride + vertexBytes,
                    weightScratch,
                    vertex * WEIGHT_BYTES,
                    WEIGHT_BYTES,
                )
            }
            val section =
                Section(key, origin, data, vertexCount, vertexCount - cutoutVertices, stride)
            section.animatedSprites =
                if (sectionSprites.isEmpty()) null else sectionSprites.toTypedArray()
            section.sheltered = shelter != null
            section.listGeneration = GpuFoliageSplit.listGeneration
            putSection(key, section)
        }
    }

    private fun modernPipeline(
        location: String,
        cutout: Boolean,
        format: GpuFormat,
    ): RenderPipeline {
        val builder: RenderPipeline.Builder =
            RenderPipeline.builder()
                .withLocation(pipelineLocation(location, format))
                .withVertexShader(
                    Identifier.fromNamespaceAndPath(AllThingsFoliage.MOD_ID, "core/foliage")
                )
                .withFragmentShader(
                    Identifier.fromNamespaceAndPath(AllThingsFoliage.MOD_ID, "core/foliage")
                )
                .withVertexBinding(0, foliageVertexFormat)
                .withPrimitiveTopology(PrimitiveTopology.QUADS)
                .withDepthStencilState(DepthStencilState.DEFAULT)
                .withColorTargetState(colorTarget(format))
                .withBindGroupLayout(BindGroupLayouts.GLOBALS)
                .withBindGroupLayout(BindGroupLayouts.FOG)
                .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                .withBindGroupLayout(BindGroupLayouts.SAMPLER2)
                .withBindGroupLayout(BindGroupLayouts.PROJECTION)
                .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
                .withBindGroupLayout(swaySettingsLayout)
                .withBindGroupLayout(GpuFoliageInteraction.LAYOUT)
        if (cutout) {
            builder.withShaderDefine("ALPHA_CUTOUT", 0.5f)
        }
        return builder.build()
    }

    fun nearRadius(renderDistance: Int): Int {
        val radius: Int =
            when (Config.gpuDistance) {
                Config.GpuDistance.PERFORMANCE -> pushRadius
                Config.GpuDistance.ADAPTIVE -> adaptiveRadius(renderDistance)
                Config.GpuDistance.HALF -> renderDistance / 2
                Config.GpuDistance.FULL -> renderDistance
            }
        return maxOf(MIN_NEAR_RADIUS, radius)
    }

    private fun noteSprite(sprite: TextureAtlasSprite) {
        if (sprite === lastSprite || !SodiumBridge.tracksSprites()) {
            return
        }
        lastSprite = sprite
        if (animates(sprite)) {
            sectionSprites.add(sprite)
        }
    }

    fun onBlockChanged(pos: BlockPos) {
        if (!active) {
            return
        }
        val sectionX: Int = SectionPos.blockToSectionCoord(pos.x)
        val sectionY: Int = SectionPos.blockToSectionCoord(pos.y)
        val sectionZ: Int = SectionPos.blockToSectionCoord(pos.z)
        val localX: Int = (pos.x and (SECTION_SIZE - 1))
        val localZ: Int = (pos.z and (SECTION_SIZE - 1))
        for (dy in -1..1) {
            queueChanged(SectionPos.asLong(sectionX, sectionY + dy, sectionZ))
        }
        if (localX == 0) {
            queueChanged(SectionPos.asLong(sectionX - 1, sectionY, sectionZ))
        } else if (localX == SECTION_SIZE - 1) {
            queueChanged(SectionPos.asLong(sectionX + 1, sectionY, sectionZ))
        }
        if (localZ == 0) {
            queueChanged(SectionPos.asLong(sectionX, sectionY, sectionZ - 1))
        } else if (localZ == SECTION_SIZE - 1) {
            queueChanged(SectionPos.asLong(sectionX, sectionY, sectionZ + 1))
        }
        if (!shelterActive) {
            return
        }
        val westX: Int = SectionPos.blockToSectionCoord(pos.x - WindShelter.REACH)
        val lowY: Int =
            SectionPos.blockToSectionCoord(
                pos.y - maxOf(WindShelter.MAX_WALL_HEIGHT, WindShelter.MAX_CEILING_HEIGHT)
            )
        for (x in westX..sectionX) {
            for (y in lowY..sectionY) {
                markIfHeld(SectionPos.asLong(x, y, sectionZ))
            }
        }
    }

    fun onBlockUpdated(pos: BlockPos, state: BlockState) {
        if (SnowRealMagicCompat.isSnowBlock(state)) {
            onBlockChanged(pos)
        }
    }

    fun onLightChanged(sectionX: Int, sectionY: Int, sectionZ: Int) {
        val key: Long = SectionPos.asLong(sectionX, sectionY, sectionZ)
        val now: Long = System.currentTimeMillis()
        val last: Long = lightChangedAt.put(key, now)
        if (lightChangedAt.size > 8192) {
            lightChangedAt.clear()
        }
        if (now - last < LIGHT_STREAM_MILLIS) {
            val region: Region? = regionsByKey.get(regionKeyOf(key))
            if (region != null && region.sections[slotOf(key)] != null && dirtySections.add(key)) {
                burstLightQueued++
                burstLightStreamed++
            }
            return
        }
        if (queueUrgent(key)) {
            burstLightQueued++
        }
    }

    fun onSectionAsked(sectionX: Int, sectionY: Int, sectionZ: Int) {
        if (!active) {
            return
        }
        val key: Long = SectionPos.asLong(sectionX, sectionY, sectionZ)
        val region: Region? = regionsByKey.get(regionKeyOf(key))
        if (region != null && region.sections[slotOf(key)] != null && dirtySections.add(key)) {
            burstAsked++
        }
    }

    /** Encodes biased cell coordinates as an exactly representable 24-bit float payload. */
    private fun packCell(x: Int, y: Int, z: Int): Float {
        val cx: Int = Mth.clamp(x + CELL_BIAS, 0, 255)
        val cy: Int = Mth.clamp(y + CELL_BIAS, 0, 255)
        val cz: Int = Mth.clamp(z + CELL_BIAS, 0, 255)
        return ((cx * 256 + cy) * 256 + cz).toFloat()
    }

    /** Packs sway weights and flags in the bit layout decoded by the foliage shaders. */
    private fun packWeights(
        wave: Float,
        push: Float,
        still: Boolean,
        steady: Boolean,
        exposure: Float,
        leafHeld: Boolean,
    ): Float {
        val w: Int = Math.round(Mth.clamp(wave, 0.0f, 1.0f) * WEIGHT_SCALE)
        val p: Int = Math.round(Mth.clamp(push, 0.0f, 1.0f) * PUSH_SCALE)
        val e: Int =
            if (still && steady)
                Math.round(Mth.clamp(exposure, 0.0f, 1.0f) * HANGING_EXPOSURE_SCALE) * 2 +
                        (if (leafHeld) 0 else 1)
            else Math.round(Mth.clamp(exposure, 0.0f, 1.0f) * EXPOSURE_SCALE)
        return (((((w * 256 + p) * 2 + (if (still) 1 else 0)) * 2 + (if (steady) 1 else 0)) * 16 +
                e))
            .toFloat()
    }

    private fun partsFromNeighbour(
        level: ClientLevel,
        blocks: LevelChunkSection,
        dx: Int,
        dy: Int,
        dz: Int,
        pos: BlockPos,
        at: BlockPos.MutableBlockPos,
    ): Boolean {
        for (side in directions) {
            val x: Int = dx + side.getStepX()
            val y: Int = dy + side.getStepY()
            val z: Int = dz + side.getStepZ()
            at.setWithOffset(pos, side)
            val beside: BlockState =
                if (
                    (((x or y) or z)) >= 0 &&
                    x < SECTION_SIZE &&
                    y < SECTION_SIZE &&
                    z < SECTION_SIZE
                )
                    blocks.getBlockState(x, y, z)
                else level.getBlockState(at)
            if (
                !beside.isAir &&
                !WavingWhitelist.isLeaves(beside.block) &&
                !holdsLeavesStill(level, at, beside)
            ) {
                return true
            }
        }
        return false
    }

    private fun pipelineLocation(location: String, format: GpuFormat): Identifier {
        return Identifier.fromNamespaceAndPath(
            AllThingsFoliage.MOD_ID,
            if (format == GpuFormat.RGBA8_UNORM) location
            else location + "/" + format.name.lowercase(),
        )
    }

    private fun putSection(key: Long, section: Section) {
        val region = regionsByKey.getOrPut(regionKeyOf(key)) { Region(regionKeyOf(key)) }
        val slot: Int = slotOf(key)
        val shown: Section? = region.uploadedSections[slot]
        val sameSize: Boolean =
            shown != null &&
                    region.vertices != null &&
                    shown.stride == section.stride &&
                    shown.vertexCount == section.vertexCount &&
                    shown.opaqueVertexCount == section.opaqueVertexCount
        region.put(slot, section)
        if (sameSize) {
            val start: Long = System.nanoTime()
            if (region.patch(slot, section)) {
                region.uploadedSections[slot] = section
                burstPatched++
                burstPatchNanos += System.nanoTime() - start
                return
            }
        }
        if (dirtyRegions.add(region)) {
            region.pendingSince = System.currentTimeMillis()
        }
    }

    private fun queueChanged(key: Long) {
        if (!queueUrgent(key)) {
            dirtySections.add(key)
        }
    }

    private fun queueUrgent(key: Long): Boolean {
        val region: Region? = regionsByKey.get(regionKeyOf(key))
        if (region == null || region.sections[slotOf(key)] == null) {
            return false
        }
        dirtySections.add(key)
        return urgentSections.add(key)
    }

    private fun rebuild(minecraft: Minecraft, level: ClientLevel, key: Long) {
        val wasSkipping: Boolean =
            if (meshedForShaderPack) IrisVertexExtension.allow() else IrisVertexExtension.begin()
        try {
            meshSection(minecraft, level, key)
        } finally {
            IrisVertexExtension.end(wasSkipping)
        }
    }

    private fun rebuildChunkMesh(
        minecraft: Minecraft,
        sectionX: Int,
        sectionY: Int,
        sectionZ: Int,
    ) {
        minecraft.levelExtractor.setSectionDirty(sectionX, sectionY, sectionZ)
        SodiumBridge.scheduleRebuild(sectionX, sectionY, sectionZ)
    }

    private fun regionFormat(): VertexFormat {
        return if (meshedForShaderPack) IrisCompat.shaderPackFormat else foliageVertexFormat
    }

    private fun regionKeyOf(sectionKey: Long): Long {
        return SectionPos.asLong(
            (SectionPos.x(sectionKey) shr REGION_WIDTH_SHIFT),
            (SectionPos.y(sectionKey) shr REGION_HEIGHT_SHIFT),
            (SectionPos.z(sectionKey) shr REGION_WIDTH_SHIFT),
        )
    }

    private fun regionOrigin(regionKey: Long): BlockPos {
        return BlockPos(
            SectionPos.sectionToBlockCoord((SectionPos.x(regionKey) shl REGION_WIDTH_SHIFT)),
            SectionPos.sectionToBlockCoord((SectionPos.y(regionKey) shl REGION_HEIGHT_SHIFT)),
            SectionPos.sectionToBlockCoord((SectionPos.z(regionKey) shl REGION_WIDTH_SHIFT)),
        )
    }

    private fun regionWithinRange(minecraft: Minecraft, regionKey: Long): Boolean {
        val player = minecraft.player
        if (player == null) {
            return false
        }
        val minX: Int = (SectionPos.x(regionKey) shl REGION_WIDTH_SHIFT)
        val minY: Int = (SectionPos.y(regionKey) shl REGION_HEIGHT_SHIFT)
        val minZ: Int = (SectionPos.z(regionKey) shl REGION_WIDTH_SHIFT)
        return withinRange(
            minecraft,
            SectionPos.asLong(
                Mth.clamp(
                    SectionPos.blockToSectionCoord(player.blockX),
                    minX,
                    minX + REGION_WIDTH - 1,
                ),
                Mth.clamp(
                    SectionPos.blockToSectionCoord(player.blockY),
                    minY,
                    minY + REGION_HEIGHT - 1,
                ),
                Mth.clamp(
                    SectionPos.blockToSectionCoord(player.blockZ),
                    minZ,
                    minZ + REGION_WIDTH - 1,
                ),
            ),
        )
    }

    private fun remeshColumn(minecraft: Minecraft, chunkX: Int, chunkZ: Int, nowNear: Boolean) {
        val clientLevel = minecraft.level
        val level = clientLevel ?: return
        val chunk: LevelChunk? = level.chunkSource.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false)
        if (chunk == null) {
            return
        }
        val sections: Array<LevelChunkSection> = chunk.sections
        for (index in 0 until sections.size) {
            if (!mayHoldFoliage(sections[index])) {
                continue
            }
            val sectionY: Int = level.getSectionYFromSectionIndex(index)
            val key: Long = SectionPos.asLong(chunkX, sectionY, chunkZ)
            if (!nowNear || GpuFoliageSplit.isGpuReady(key)) {
                rebuildChunkMesh(minecraft, chunkX, sectionY, chunkZ)
            } else {
                dirtySections.add(key)
            }
        }
    }

    private fun removeSection(key: Long) {
        val regionKey: Long = regionKeyOf(key)
        val region: Region? = regionsByKey.get(regionKey)
        if (region == null || !region.remove(slotOf(key))) {
            return
        }
        if (region.sectionCount == 0) {
            region.free()
            regionsByKey.remove(regionKey)
            dirtyRegions.remove(region)
        } else if (dirtyRegions.add(region)) {
            region.pendingSince = System.currentTimeMillis()
        }
    }

    private fun reseedLoadedChunks(minecraft: Minecraft, level: ClientLevel) {
        val player = minecraft.player ?: return
        val radius: Int = buildRadius
        val centerX: Int = SectionPos.blockToSectionCoord(player.blockX)
        val centerZ: Int = SectionPos.blockToSectionCoord(player.blockZ)
        for (chunkX in centerX - radius..centerX + radius) {
            for (chunkZ in centerZ - radius..centerZ + radius) {
                val chunk: LevelChunk? =
                    level.chunkSource.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false)
                if (chunk != null) {
                    discoverChunk(level, chunk)
                }
            }
        }
    }

    private fun reserveWeights(bytes: Int) {
        if (weightScratch.remaining() >= bytes) {
            return
        }
        val grown: ByteBuffer =
            MemoryUtil.memAlloc(
                maxOf(weightScratch.capacity() * 2, weightScratch.position() + bytes)
            )
        weightScratch.flip()
        grown.put(weightScratch)
        MemoryUtil.memFree(weightScratch)
        weightScratch = grown
    }

    private fun setUpShaderPack() {
        if (shaderPackSetUp) {
            return
        }
        shaderPackSetUp = true
        shaderPackPipeline =
            RenderPipeline.builder(RenderPipelines.BLOCK_SNIPPET)
                .withLocation(
                    Identifier.fromNamespaceAndPath(
                        AllThingsFoliage.MOD_ID,
                        "pipeline/foliage_shader_pack",
                    )
                )
                .withVertexShader(
                    Identifier.fromNamespaceAndPath(AllThingsFoliage.MOD_ID, "core/foliage")
                )
                .withFragmentShader(
                    Identifier.fromNamespaceAndPath(AllThingsFoliage.MOD_ID, "core/foliage")
                )
                .withVertexBinding(0, IrisCompat.shaderPackFormat)
                .withColorTargetState(ColorTargetState.DEFAULT)
                .withBindGroupLayout(swaySettingsLayout)
                .withBindGroupLayout(GpuFoliageInteraction.LAYOUT)
                .withShaderDefine("ALPHA_CUTOUT", 0.5f)
                .build()
        IrisCompat.setUp(
            shaderPackPipeline,
            null,
            listOf(swaySettingsLayout, GpuFoliageInteraction.LAYOUT),
            GpuFoliageRenderer::drawShadow,
        )
        shaderPackOpaquePipeline =
            RenderPipeline.builder(RenderPipelines.BLOCK_SNIPPET)
                .withLocation(
                    Identifier.fromNamespaceAndPath(
                        AllThingsFoliage.MOD_ID,
                        "pipeline/foliage_shader_pack_opaque",
                    )
                )
                .withVertexShader(
                    Identifier.fromNamespaceAndPath(AllThingsFoliage.MOD_ID, "core/foliage")
                )
                .withFragmentShader(
                    Identifier.fromNamespaceAndPath(AllThingsFoliage.MOD_ID, "core/foliage")
                )
                .withVertexBinding(0, IrisCompat.shaderPackFormat)
                .withColorTargetState(ColorTargetState.DEFAULT)
                .withBindGroupLayout(swaySettingsLayout)
                .withBindGroupLayout(GpuFoliageInteraction.LAYOUT)
                .build()
        IrisCompat.setUpOpaque(shaderPackOpaquePipeline)
    }

    private fun shadowProjectionBuffer(): ProjectionMatrixBuffer {
        if (shadowProjectionUniform == null) {
            shadowProjectionUniform = ProjectionMatrixBuffer("All Things Foliage shadow")
        }
        return requireNotNull(shadowProjectionUniform)
    }

    private fun slotOf(sectionKey: Long): Int {
        val x: Int = (SectionPos.x(sectionKey) and (REGION_WIDTH - 1))
        val y: Int = (SectionPos.y(sectionKey) and (REGION_HEIGHT - 1))
        val z: Int = (SectionPos.z(sectionKey) and (REGION_WIDTH - 1))
        return (y * REGION_WIDTH + z) * REGION_WIDTH + x
    }

    private fun sodiumPipeline(location: String, format: GpuFormat): RenderPipeline {
        return RenderPipeline.builder()
            .withLocation(pipelineLocation(location, format))
            .withVertexShader(SodiumFoliageShader.shaderId)
            .withFragmentShader(SodiumFoliageShader.shaderId)
            .withVertexBinding(0, foliageVertexFormat)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(DepthStencilState.DEFAULT)
            .withColorTargetState(colorTarget(format))
            .withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withBindGroupLayout(BindGroupLayouts.FOG)
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withBindGroupLayout(
                BindGroupLayout.builder()
                    .withUniform(
                        SodiumFoliageShader.BLOCK_TEXTURE,
                        UniformType.COMBINED_IMAGE_SAMPLER,
                    )
                    .withUniform(
                        SodiumFoliageShader.LIGHT_TEXTURE,
                        UniformType.COMBINED_IMAGE_SAMPLER,
                    )
                    .build()
            )
            .withBindGroupLayout(swaySettingsLayout)
            .withBindGroupLayout(GpuFoliageInteraction.LAYOUT)
            .build()
    }

    private fun submitDraws(
        minecraft: Minecraft,
        camera: Vec3,
        shadowModelView: Matrix4f?,
        shadowProjection: Matrix4f?,
        shadowColor: GpuTextureView?,
        shadowDepth: GpuTextureView?,
    ) {
        val shaderPack: Boolean = meshedForShaderPack
        val sodium: Boolean = !shaderPack && SodiumBridge.drawsChunks()
        val level: LevelPipelines = levelPipelines(minecraft)
        val sodiumPipeline: CompiledRenderPipeline? =
            if (sodium) SodiumFoliageShader.compiled(level.sodium, true) else null
        val terrainPipeline: CompiledRenderPipeline? =
            if (shaderPack || sodium) null else TerrainFoliageShader.compiled(level.terrain)
        val sodiumShaders: Boolean = sodiumPipeline != null
        val terrainShaders: Boolean = terrainPipeline != null
        val pipeline: CompiledRenderPipeline? =
            if (shaderPack) compiledPipeline(shaderPackPipeline)
            else if (sodiumShaders) sodiumPipeline
            else if (terrainShaders) terrainPipeline else compiledPipeline(level.own)
        if (pipeline == null) {
            return
        }
        val solid: CompiledRenderPipeline? =
            if (opaqueDrawsSize == 0) null
            else if (shaderPack) compiledPipeline(shaderPackOpaquePipeline)
            else if (sodiumShaders) SodiumFoliageShader.compiled(level.sodiumOpaque, false)
            else if (terrainShaders) TerrainFoliageShader.compiled(level.terrainOpaque)
            else compiledPipeline(level.ownOpaque)
        val opaquePipeline: CompiledRenderPipeline = if (solid != null) solid else pipeline
        val regions: Array<Region> = drawnRegions.toTypedArray()
        val atlas: AbstractTexture = minecraft.atlasManager.getAtlasOrThrow(AtlasIds.BLOCKS)
        val regionBlocks: Array<GpuBufferSlice?> = arrayOfNulls<GpuBufferSlice>(regions.size)
        val terrainBlock: Array<GpuBufferSlice?> = arrayOfNulls<GpuBufferSlice>(1)
        pendingLevelUniforms = prepareUniforms@{
            if (terrainShaders) {
                terrainBlock[0] =
                    RenderSystem.getDynamicUniforms()
                        .writeTerrainTransform(
                            levelModelView,
                            atlas.textureView.getWidth(0),
                            atlas.textureView.getHeight(0),
                        )
                for (i in 0 until regions.size) {
                    regionBlocks[i] = regions[i].chunkSection().slice()
                }
                return@prepareUniforms
            }
            val noModulation: Vector4f = Vector4f(1.0f, 1.0f, 1.0f, 1.0f)
            val noTextureTransform: Matrix4f = Matrix4f()
            val transforms = arrayOfNulls<DynamicGpuData.Transform>(regions.size)
            for (i in 0 until regions.size) {
                val origin: BlockPos = regions[i].origin
                transforms[i] =
                    DynamicGpuData.Transform(
                        levelModelView,
                        noModulation,
                        Vector3f(
                            ((origin.x - camera.x)).toFloat(),
                            ((origin.y - camera.y)).toFloat(),
                            ((origin.z - camera.z)).toFloat(),
                        ),
                        noTextureTransform,
                    )
            }
            RenderSystem.getDynamicUniforms()
                .writeTransforms(*transforms.requireNoNulls())
                .copyInto(regionBlocks)
        }
        val regionUniform: String = if (terrainShaders) "ChunkSection" else "DynamicTransforms"
        val drawList: IntArray = draws.copyOf(drawsSize)
        val opaqueList: IntArray = opaqueDraws.copyOf(opaqueDrawsSize)
        var longestDraw: Int = 0
        for (i in (2 until drawList.size) step 3) {
            longestDraw = maxOf(longestDraw, drawList[i])
        }
        for (i in (2 until opaqueList.size) step 3) {
            longestDraw = maxOf(longestDraw, opaqueList[i])
        }
        val indicesNeeded: Int = indexCountFor(longestDraw)
        val indices: RenderSystem.AutoStorageIndexBuffer =
            RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS)
        indices.requestIndexCount(indicesNeeded)
        val settings: GpuBuffer = uploadSwaySettings(camera)
        val interaction: GpuBuffer = GpuFoliageInteraction.upload(camera)
        val draw: (RenderPass) -> Unit = drawFoliage@{ pass ->
            val indexBuffer: GpuBuffer? = indices.getBuffer()
            if (
                indexBuffer == null ||
                !indices.hasStorage(indicesNeeded) ||
                (regions.size > 0 && regionBlocks[0] == null)
            ) {
                return@drawFoliage
            }
            for (kind in 0 until 2) {
                val list: IntArray = if (kind == 0) opaqueList else drawList
                if (list.size == 0) {
                    continue
                }
                val drawnSolid: Boolean = kind == 0 && solid != null
                try {
                    if (shaderPack) {
                        IrisCompat.setTerrainPhase(drawnSolid)
                        pass.setPipeline(
                            IrisCompat.withFoliageProgram(
                                RenderSystem.getCompiledPipeline(
                                    if (drawnSolid) shaderPackOpaquePipeline else shaderPackPipeline
                                ),
                                drawnSolid,
                            )
                        )
                    } else {
                        pass.setPipeline(if (kind == 0) opaquePipeline else pipeline)
                    }
                } catch (e: IllegalStateException) {
                    if (!pipelineRefused) {
                        pipelineRefused = true
                        AllThingsFoliage.logger.error(
                            "The level's pass refused the GPU foliage pipeline; the foliage it " +
                                    "holds is not drawn while it does",
                            e,
                        )
                    }
                    return@drawFoliage
                }
                if (terrainBlock[0] != null) {
                    pass.setUniform("TerrainUniform", requireNotNull(terrainBlock[0]))
                }
                pass.setUniform(
                    if (sodiumShaders) SodiumFoliageShader.BLOCK_TEXTURE else "Sampler0",
                    atlas.textureView,
                    if (sodiumShaders || terrainShaders)
                        RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR, true)
                    else atlas.sampler,
                )
                pass.setUniform(
                    if (sodiumShaders) SodiumFoliageShader.LIGHT_TEXTURE else "Sampler2",
                    minecraft.gameRenderer.lightmap(),
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR),
                )
                pass.setUniform(SWAY_SETTINGS_UNIFORM, settings)
                pass.setUniform(GpuFoliageInteraction.UNIFORM, interaction)
                pass.setIndexBuffer(requireNotNull(indexBuffer), indices.type())
                if (shaderPack) {
                    IrisCompat.bindSwayBlocks(settings, interaction)
                }
                var boundRegion: Int = -1
                for (i in (0 until list.size) step 3) {
                    val regionIndex: Int = list[i]
                    if (regionIndex != boundRegion) {
                        pass.setUniform(regionUniform, requireNotNull(regionBlocks[regionIndex]))
                        pass.setVertexBuffer(
                            0,
                            requireNotNull(regions[regionIndex].vertices).slice(),
                        )
                        boundRegion = regionIndex
                    }
                    pass.drawIndexed(indexCountFor(list[i + 2]), 1, 0, list[i + 1], 0)
                }
            }
        }
        pendingDraw =
            if (shaderPack) { pass -> IrisCompat.inTerrainPhase({ draw(pass) }) } else draw
    }

    private fun terrainPipeline(
        location: String,
        cutout: Boolean,
        format: GpuFormat,
    ): RenderPipeline {
        val builder: RenderPipeline.Builder =
            RenderPipeline.builder()
                .withLocation(pipelineLocation(location, format))
                .withVertexShader(TerrainFoliageShader.VERTEX)
                .withFragmentShader(TerrainFoliageShader.TERRAIN)
                .withVertexBinding(0, foliageVertexFormat)
                .withPrimitiveTopology(PrimitiveTopology.QUADS)
                .withDepthStencilState(DepthStencilState.DEFAULT)
                .withColorTargetState(colorTarget(format))
                .withBindGroupLayout(BindGroupLayouts.GLOBALS)
                .withBindGroupLayout(BindGroupLayouts.FOG)
                .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                .withBindGroupLayout(BindGroupLayouts.SAMPLER2)
                .withBindGroupLayout(BindGroupLayouts.PROJECTION)
                .withBindGroupLayout(BindGroupLayouts.CHUNK_SECTION)
                .withBindGroupLayout(BindGroupLayouts.TERRAIN_INFO)
                .withBindGroupLayout(swaySettingsLayout)
                .withBindGroupLayout(GpuFoliageInteraction.LAYOUT)
        if (cutout) {
            builder.withShaderDefine("ALPHA_CUTOUT", 0.5f)
        }
        return builder.build()
    }

    private fun updateNearArea(minecraft: Minecraft) {
        val player = minecraft.player
        if (player == null) {
            return
        }
        val centerX: Int = SectionPos.blockToSectionCoord(player.blockX)
        val centerZ: Int = SectionPos.blockToSectionCoord(player.blockZ)
        val radius: Int = nearRadius(minecraft.options.renderDistance().get())
        val hadArea: Boolean = GpuFoliageSplit.hasArea
        val oldX: Int = GpuFoliageSplit.centerX
        val oldZ: Int = GpuFoliageSplit.centerZ
        val oldRadius: Int = GpuFoliageSplit.radius
        if (hadArea && centerX == oldX && centerZ == oldZ && radius == oldRadius) {
            return
        }
        GpuFoliageSplit.setArea(centerX, centerZ, radius)
        if (hadArea) {
            for (chunkX in oldX - oldRadius..oldX + oldRadius) {
                for (chunkZ in oldZ - oldRadius..oldZ + oldRadius) {
                    if (!GpuFoliageSplit.isNear(chunkX, chunkZ)) {
                        remeshColumn(minecraft, chunkX, chunkZ, false)
                    }
                }
            }
        }
        for (chunkX in centerX - radius..centerX + radius) {
            for (chunkZ in centerZ - radius..centerZ + radius) {
                val wasNear: Boolean =
                    hadArea && maxOf(abs(chunkX - oldX), abs(chunkZ - oldZ)) <= oldRadius
                if (!wasNear) {
                    remeshColumn(minecraft, chunkX, chunkZ, true)
                }
            }
        }
    }

    private fun uploadDueRegion(camera: Vec3) {
        uploadUrgentRegions()
        val budgetEnd: Long = System.nanoTime() + frameBudgetNanos / UPLOAD_BUDGET_SHARE
        while (uploadOneDueRegion(camera) && System.nanoTime() < budgetEnd) {
            // The condition performs each upload and stops when the frame budget is exhausted.
        }
    }

    private fun uploadMeasured(region: Region, now: Long) {
        val start: Long = System.nanoTime()
        burstWaitMaxMillis = maxOf(burstWaitMaxMillis, now - region.pendingSince)
        region.upload()
        val taken: Long = System.nanoTime() - start
        burstUploads++
        burstUploadNanos += taken
        burstUploadMaxNanos = maxOf(burstUploadMaxNanos, taken)
    }

    private fun uploadOneDueRegion(camera: Vec3): Boolean {
        if (dirtyRegions.isEmpty()) {
            return false
        }
        val now: Long = System.currentTimeMillis()
        val oldest: Region = dirtyRegions.iterator().next()
        if (now - oldest.pendingSince >= REGION_UPLOAD_STARVE_MILLIS) {
            dirtyRegions.remove(oldest)
            uploadMeasured(oldest, now)
            return true
        }
        regionsWithQueuedSections.clear()
        for (key in dirtySections) {
            regionsWithQueuedSections.add(regionKeyOf(key))
        }
        var nearest: Region? = null
        var nearestDistance: Double = Double.MAX_VALUE
        for (region in dirtyRegions) {
            if (
                now - region.pendingSince < REGION_UPLOAD_MAX_WAIT_MILLIS &&
                regionsWithQueuedSections.contains(region.key)
            ) {
                continue
            }
            val distance: Double = region.distanceSqTo(camera)
            if (distance < nearestDistance) {
                nearestDistance = distance
                nearest = region
            }
        }
        if (nearest != null) {
            dirtyRegions.remove(nearest)
            uploadMeasured(nearest, now)
            return true
        }
        return false
    }

    private fun uploadScratch(bytes: Int): ByteBuffer {
        if (uploadStaging == null || requireNotNull(uploadStaging).capacity() < bytes) {
            if (uploadStaging != null) {
                MemoryUtil.memFree(uploadStaging)
            }
            uploadStaging = MemoryUtil.memAlloc(maxOf(bytes, 64 * 1024))
        }
        return requireNotNull(uploadStaging).clear().limit(bytes)
    }

    /** Keeps the std140 field order aligned with the shaders’ FoliageSway uniform block. */
    private fun uploadSwaySettings(camera: Vec3): GpuBuffer {
        if (swaySettingsBuffer == null) {
            swaySettingsBuffer =
                RenderSystem.getDevice()
                    .createBuffer(
                        { "All Things Foliage sway settings" },
                        (GpuBuffer.USAGE_UNIFORM or GpuBuffer.USAGE_COPY_DST),
                        swaySettingsSize.toLong(),
                    )
            uploadedIntensity = Float.NaN
        }
        val intensity: Float = swayIntensity
        val calm: Float = calmSway
        val edge: Vector4f = windEdgeFrom(camera)
        val weather: Vector4f = weather()
        if (
            intensity != uploadedIntensity ||
            calm != uploadedCalmSway ||
            edge != uploadedEdge ||
            weather != uploadedWeather
        ) {
            MemoryStack.stackPush().use { stack ->
                val data: ByteBuffer =
                    Std140Builder.onStack(stack, swaySettingsSize)
                        .putFloat(intensity)
                        .putFloat(calm)
                        .putVec4(edge)
                        .putVec4(weather)
                        .get()
                RenderSystem.getDevice()
                    .createCommandEncoder()
                    .writeToBuffer(requireNotNull(swaySettingsBuffer).slice(), data)
            }
            uploadedIntensity = intensity
            uploadedCalmSway = calm
            uploadedEdge.set(edge)
            uploadedWeather.set(weather)
        }
        return requireNotNull(swaySettingsBuffer)
    }

    private fun uploadUrgentRegions() {
        if (urgentRegions.isEmpty()) {
            return
        }
        val now: Long = System.currentTimeMillis()
        val regions = urgentRegions.iterator()
        while (regions.hasNext()) {
            val region: Region? = regionsByKey.get(regions.nextLong())
            regions.remove()
            if (region != null && dirtyRegions.remove(region)) {
                uploadMeasured(region, now)
            }
        }
    }

    private fun warnForeignVertexFormat(format: Any) {
        if (warnedForeignVertexFormat) {
            return
        }
        warnedForeignVertexFormat = true
        AllThingsFoliage.logger.warn(
            "Foliage was meshed in {} rather than the block format, so the GPU renderer " +
                    "left it to the chunk mesh. Another mod is widening the mod's vertex buffer.",
            format,
        )
    }

    private fun warnWeightCountMismatch(vertices: Int, weights: Int) {
        if (warnedWeightCountMismatch) {
            return
        }
        warnedWeightCountMismatch = true
        AllThingsFoliage.logger.warn(
            "Foliage was meshed with {} vertices but {} sway weights, so the GPU renderer " +
                    "left it to the chunk mesh. Another mod is writing vertices the mod cannot see.",
            vertices,
            weights,
        )
    }

    private fun weather(): Vector4f {
        val minecraft: Minecraft = Minecraft.getInstance()
        val clientLevel = minecraft.level

        if (clientLevel == null) {
            return Vector4f()
        }

        val partialTick: Float = minecraft.deltaTracker.getGameTimeDeltaPartialTick(false)

        if (!Config.weatherWind) {
            return Vector4f()
        }

        return Vector4f(
            clientLevel.getRainLevel(partialTick),
            clientLevel.getThunderLevel(partialTick),
            weatherWindReach,
            WEATHER_WIND_FADE_BLOCKS,
        )
    }

    private fun windEdgeFrom(camera: Vec3): Vector4f {
        return Vector4f(
            ((windEdge[0] - camera.x)).toFloat(),
            ((windEdge[1] - camera.z)).toFloat(),
            ((windEdge[2] - camera.x)).toFloat(),
            ((windEdge[3] - camera.z)).toFloat(),
        )
    }

    private fun withinRange(minecraft: Minecraft, key: Long): Boolean {
        val player = minecraft.player
        if (player == null) {
            return false
        }
        val limit: Int = buildRadius
        val dx: Int = SectionPos.x(key) - SectionPos.blockToSectionCoord(player.blockX)
        val dz: Int = SectionPos.z(key) - SectionPos.blockToSectionCoord(player.blockZ)
        val dy: Int = SectionPos.y(key) - SectionPos.blockToSectionCoord(player.blockY)
        return (abs(dx) <= limit && abs(dz) <= limit && abs(dy) <= limit)
    }

    private fun withinReach(sectionOrigin: BlockPos, x: Double, z: Double, reach: Float): Boolean {
        if (reach >= UNBOUNDED_REACH) {
            return true
        }
        val dx: Double =
            maxOf(0.0, maxOf(sectionOrigin.x - x, x - (sectionOrigin.x + SECTION_SIZE)))
        val dz: Double =
            maxOf(0.0, maxOf(sectionOrigin.z - z, z - (sectionOrigin.z + SECTION_SIZE)))
        return dx * dx + dz * dz <= reach * (reach).toDouble()
    }

    fun writeLevelUniforms(view: Matrix4f, terrainFog: GpuBufferSlice) {
        levelFog = terrainFog
        val write: (() -> Unit)? = pendingLevelUniforms
        pendingLevelUniforms = null
        if (write != null) {
            levelModelView.set(view)
            write()
        }
    }

    private data class LevelPipelines(
        val own: RenderPipeline,
        val ownOpaque: RenderPipeline,
        val sodium: RenderPipeline,
        val sodiumOpaque: RenderPipeline,
        val terrain: RenderPipeline,
        val terrainOpaque: RenderPipeline,
    )

    /**
     * Packs section geometry into shared buffers while retaining the currently uploaded snapshot.
     */
    private class Region(val key: Long) {
        var chunkSection: GpuBuffer? = null
        val firstOpaqueVertex = IntArray(SECTIONS_PER_REGION)
        val firstVertex = IntArray(SECTIONS_PER_REGION)
        val occupied = IntArray(SECTIONS_PER_REGION)
        var occupiedCount = 0
        val origin = regionOrigin(key)
        val blockBounds =
            BoundingBox(
                origin.x,
                origin.y,
                origin.z,
                origin.x + REGION_WIDTH * SECTION_SIZE - 1,
                origin.y + REGION_HEIGHT * SECTION_SIZE - 1,
                origin.z + REGION_WIDTH * SECTION_SIZE - 1,
            )
        val bounds =
            AABB(
                origin.x - SWAY_MARGIN,
                origin.y - SWAY_MARGIN,
                origin.z - SWAY_MARGIN,
                origin.x + REGION_WIDTH * SECTION_SIZE + SWAY_MARGIN,
                origin.y + REGION_HEIGHT * SECTION_SIZE + SWAY_MARGIN,
                origin.z + REGION_WIDTH * SECTION_SIZE + SWAY_MARGIN,
            )
        var pendingSince = 0L
        var sectionCount = 0
        val sections = arrayOfNulls<Section>(SECTIONS_PER_REGION)
        val uploadedSections = arrayOfNulls<Section>(SECTIONS_PER_REGION)
        var vertices: GpuBuffer? = null

        fun chunkSection(): GpuBuffer {
            if (chunkSection == null) {
                val data: ByteBuffer = MemoryUtil.memAlloc(DynamicGpuData.CHUNK_SECTION_UBO_SIZE)
                try {
                    Std140Builder.intoBuffer(data)
                        .putIVec3(origin.x, origin.y, origin.z)
                        .putFloat(1.0f)
                    data.position(0)
                    chunkSection =
                        RenderSystem.getDevice()
                            .createBuffer(
                                { "All Things Foliage chunk section" },
                                GpuBuffer.USAGE_UNIFORM,
                                data,
                            )
                } finally {
                    MemoryUtil.memFree(data)
                }
            }
            return requireNotNull(chunkSection)
        }

        fun closeBuffers() {
            vertices?.close()
            vertices = null
            chunkSection?.close()
            chunkSection = null
            pendingDraw = null
            pendingLevelUniforms = null
        }

        fun distanceSqTo(camera: Vec3): Double {
            return camera.distanceToSqr(
                origin.x + REGION_WIDTH * SECTION_SIZE / 2.0,
                origin.y + REGION_HEIGHT * SECTION_SIZE / 2.0,
                origin.z + REGION_WIDTH * SECTION_SIZE / 2.0,
            )
        }

        fun free() {
            closeBuffers()
            for (slot in 0 until SECTIONS_PER_REGION) {
                remove(slot)
            }
        }

        fun markSectionsReady() {
            val minecraft: Minecraft = Minecraft.getInstance()
            for (i in 0 until occupiedCount) {
                val key: Long = requireNotNull(sections[occupied[i]]).key
                if (
                    GpuFoliageSplit.markGpuReady(key) &&
                    GpuFoliageSplit.isNear(SectionPos.x(key), SectionPos.z(key))
                ) {
                    rebuildChunkMesh(
                        minecraft,
                        SectionPos.x(key),
                        SectionPos.y(key),
                        SectionPos.z(key),
                    )
                } else if (
                    GpuFoliageSplit.groupTakenOver(
                        key,
                        requireNotNull(sections[occupied[i]]).listGeneration,
                    )
                ) {
                    rebuildChunkMesh(
                        minecraft,
                        SectionPos.x(key),
                        SectionPos.y(key),
                        SectionPos.z(key),
                    )
                }
            }
        }

        fun patch(slot: Int, section: Section): Boolean {
            val vertexBuffer = requireNotNull(vertices)
            val stride: Int = section.stride
            val cutoutBytes: Int = section.cutoutVertexCount * stride
            val opaqueBytes: Int = section.opaqueVertexCount * stride
            if (cutoutBytes + opaqueBytes == 0) {
                return true
            }
            val encoder = RenderSystem.getDevice().createCommandEncoder()
            if (cutoutBytes > 0) {
                encoder.writeToBuffer(
                    vertexBuffer.slice((firstVertex[slot] * stride).toLong(), cutoutBytes.toLong()),
                    section.data.slice(0, cutoutBytes),
                )
            }
            if (opaqueBytes > 0) {
                encoder.writeToBuffer(
                    vertexBuffer.slice(
                        (firstOpaqueVertex[slot] * stride).toLong(),
                        opaqueBytes.toLong(),
                    ),
                    section.data.slice(cutoutBytes, opaqueBytes),
                )
            }
            return true
        }

        fun put(slot: Int, section: Section) {
            if (sections[slot] == null) {
                sectionCount++
            } else {
                requireNotNull(sections[slot]).free()
            }
            sections[slot] = section
        }

        fun remove(slot: Int): Boolean {
            val section = sections[slot]
            if (section == null) {
                return false
            }
            GpuFoliageSplit.unmarkGpuReady(section.key)
            section.free()
            sections[slot] = null
            sectionCount--
            return true
        }

        fun upload() {
            closeBuffers()
            var vertexCount: Int = 0
            occupiedCount = 0
            for (slot in 0 until SECTIONS_PER_REGION) {
                uploadedSections[slot] = sections[slot]
                if (sections[slot] != null) {
                    firstVertex[slot] = vertexCount
                    vertexCount += requireNotNull(sections[slot]).cutoutVertexCount
                    occupied[occupiedCount++] = slot
                }
            }
            for (i in 0 until occupiedCount) {
                firstOpaqueVertex[occupied[i]] = vertexCount
                vertexCount += requireNotNull(sections[occupied[i]]).opaqueVertexCount
            }
            val stride: Int =
                if (occupiedCount == 0) blockVertexBytes + WEIGHT_BYTES
                else requireNotNull(sections[occupied[0]]).stride
            val vertexData: ByteBuffer = uploadScratch(vertexCount * stride)
            for (i in 0 until occupiedCount) {
                val slot: Int = occupied[i]
                val section = requireNotNull(sections[slot])
                val cutoutBytes: Int = section.cutoutVertexCount * stride
                vertexData.put(firstVertex[slot] * stride, section.data, 0, cutoutBytes)
                if (section.opaqueVertexCount > 0) {
                    vertexData.put(
                        firstOpaqueVertex[slot] * stride,
                        section.data,
                        cutoutBytes,
                        section.opaqueVertexCount * stride,
                    )
                }
            }
            vertices =
                RenderSystem.getDevice()
                    .createBuffer(
                        { "All Things Foliage vertices" },
                        (GpuBuffer.USAGE_VERTEX or GpuBuffer.USAGE_COPY_DST),
                        vertexData,
                    )
            markSectionsReady()
        }
    }

    /** Owns one section's interleaved native vertex data until replaced or unloaded. */
    private class Section(
        val key: Long,
        val origin: BlockPos,
        val data: ByteBuffer,
        val vertexCount: Int,
        val opaqueVertexCount: Int,
        val stride: Int,
    ) {
        var animatedSprites: Array<TextureAtlasSprite>? = null
        val bounds =
            AABB(
                origin.x - SWAY_MARGIN,
                origin.y - SWAY_MARGIN,
                origin.z - SWAY_MARGIN,
                origin.x + SECTION_SIZE + SWAY_MARGIN,
                origin.y + SECTION_SIZE + SWAY_MARGIN,
                origin.z + SECTION_SIZE + SWAY_MARGIN,
            )
        val cutoutVertexCount: Int
            get() {
                return vertexCount - opaqueVertexCount
            }

        var listGeneration = 0
        var sheltered = false

        fun free() {
            MemoryUtil.memFree(data)
        }
    }

    private class SwayAnchor {
        var anchorY: Float = 0.0f
        var cellX: Int = 0
        var cellY: Int = 0
        var cellZ: Int = 0
        var deformation: DeformationContributor? = null
        var deformationScale: Float = 0.0f
        var exposure: Float = 1.0f
        var field: Boolean = false
        val fieldPins: Long2FloatOpenHashMap = Long2FloatOpenHashMap()
        var fieldShelter: WindShelter? = null
        val freeByAnchor: Long2ObjectOpenHashMap<FloatArray> = Long2ObjectOpenHashMap()
        var freeLength: Int = 0
        var freeTop: Float = 0.0f
        var hanging: Boolean = false
        var hangsOnLeaves: Boolean = false
        val holdByAnchor: Long2ObjectOpenHashMap<IntArray> = Long2ObjectOpenHashMap()
        var length: Int = 1
        val lengthByAnchor = hashMapOf<BlockPos, Int>()
        lateinit var level: ClientLevel
        val pinCursor: BlockPos.MutableBlockPos = BlockPos.MutableBlockPos()
        lateinit var pos: BlockPos
        var pushless: Boolean = false
        var restY: Int = Int.MIN_VALUE
        lateinit var state: BlockState
        var steady: Boolean = false
        var still: Boolean = false
        val strandExposureByAnchor: Long2FloatOpenHashMap = Long2FloatOpenHashMap()
        var vineFree: Boolean = false
        var vineOnLeaves: Boolean = false
        var waving: Float = 1.0f

        private fun cornerPin(worldX: Float, worldY: Float, worldZ: Float): Float {
            val x: Int = Math.round(worldX)
            val y: Int = cornerY(worldY)
            val z: Int = Math.round(worldZ)
            val key: Long = BlockPos.asLong(x, y, z)
            if (fieldPins.containsKey(key)) {
                return fieldPins.get(key)
            }
            var pin: Float = 1.0f
            for (dy in -1..0) {
                if (!(pin > 0.0f)) {
                    break
                }
                for (dz in -1..0) {
                    if (!(pin > 0.0f)) {
                        break
                    }
                    for (dx in -1..0) {
                        pinCursor.set(x + dx, y + dy, z + dz)
                        if (holdsLeavesStill(level, pinCursor, level.getBlockState(pinCursor))) {
                            pin = 0.0f
                            break
                        }
                    }
                }
            }
            fieldPins.put(key, pin)
            return pin
        }

        private fun cornerY(worldY: Float): Int {
            return if (restY != NOT_RESTING) restY else Math.round(worldY)
        }

        fun exposureAt(worldX: Float, worldY: Float, worldZ: Float): Float {
            if (!(field || hangsOnLeaves || vineFree || vineOnLeaves) || fieldShelter == null) {
                return exposure
            }
            if (
                (vineFree || (hangsOnLeaves && freeLength > 0)) &&
                worldY < freeTop - FREE_PART_MARGIN
            ) {
                return strandExposure()
            }
            return requireNotNull(fieldShelter)
                .cornerExposure(
                    Math.round(worldX),
                    cornerY(worldY),
                    Math.round(worldZ),
                )
        }

        fun hangOnLeaves(level: ClientLevel): Boolean {
            freeLength = 0
            if (!hanging) {
                return false
            }
            val anchor: Long = BlockPos.asLong(cellX, cellY, cellZ)
            var known = freeByAnchor.get(anchor)
            if (known == null) {
                known = measureHang(level)
                freeByAnchor.put(anchor, known)
            }
            if (known[0] == 0.0f) {
                return false
            }
            freeTop = known[1]
            freeLength = (known[2]).toInt()
            return true
        }

        fun hangVine(level: ClientLevel, pos: BlockPos, state: BlockState) {
            val anchor: Long = BlockPos.asLong(cellX, cellY, cellZ)
            var hold = holdByAnchor.get(anchor)
            if (hold == null) {
                hold = measureHold(level)
                holdByAnchor.put(anchor, hold)
            }
            val tipY: Int = cellY - length + 1
            freeTop = hold[0].toFloat()
            freeLength = hold[0] - tipY
            val leavesWave: Boolean = WavingWhitelist.leavesWave()
            exposure = 1.0f
            if (pos.y >= hold[0]) {
                waving = 0.0f
                pushless = true
                vineOnLeaves = leavesWave && behind(level, pos, state) != BEHIND_OTHER
                still = vineOnLeaves
                steady = vineOnLeaves
                return
            }
            vineFree = true
            vineOnLeaves = leavesWave && hold[1] != 0
            still = vineOnLeaves
            steady = true
        }

        fun leafHeldAt(worldX: Float, worldYValue: Float, worldZ: Float): Boolean {
            var worldY = worldYValue
            if (vineFree) {
                worldY = freeTop
            }
            return ((hangsOnLeaves || vineOnLeaves) &&
                    still &&
                    steady &&
                    cornerPin(worldX, worldY, worldZ) == 0.0f)
        }

        private fun lengthOf(
            multiblock: MultiBlockContributor,
            anchor: BlockPos,
            level: ClientLevel,
        ): Int {
            val cached: Int? = lengthByAnchor.get(anchor)
            if (cached != null) {
                return cached
            }
            val anchorState: BlockState = SnowRealMagicCompat.plantAt(level, anchor)
            val value: Int = multiblock.getLinkedBlocks(anchor, anchorState, level).size + 1
            lengthByAnchor.put(anchor.immutable(), value)
            return value
        }

        private fun measureHang(level: ClientLevel): FloatArray {
            val at: BlockPos.MutableBlockPos = BlockPos.MutableBlockPos(cellX, cellY + 1, cellZ)
            if (!WavingWhitelist.isLeaves(level.getBlockState(at).block)) {
                return floatArrayOf(0.0f, 0.0f, 0.0f)
            }
            if (length <= SHORT_HANGING_STRAND) {
                return floatArrayOf(1.0f, 0.0f, 0.0f)
            }
            val tipY: Int = cellY - length + 1
            var heldY: Int = cellY + 1
            for (y in cellY downTo tipY) {
                at.set(cellX, y, cellZ)
                if (behind(level, at, level.getBlockState(at)) != BEHIND_NOTHING) {
                    heldY = y
                }
            }
            return if (heldY > cellY) floatArrayOf(1.0f, anchorY, (length).toFloat())
            else floatArrayOf(1.0f, (heldY).toFloat(), (heldY - tipY).toFloat())
        }

        private fun measureHold(level: ClientLevel): IntArray {
            val at: BlockPos.MutableBlockPos = BlockPos.MutableBlockPos()
            for (y in cellY - length + 1..cellY) {
                at.set(cellX, y, cellZ)
                val behind: Int = behind(level, at, level.getBlockState(at))
                if (behind != BEHIND_NOTHING) {
                    return intArrayOf(y, if (behind == BEHIND_LEAVES) 1 else 0)
                }
            }
            at.set(cellX, cellY + 1, cellZ)
            return intArrayOf(
                cellY + 1,
                if (WavingWhitelist.isLeaves(level.getBlockState(at).block)) 1 else 0,
            )
        }

        fun pinAt(worldX: Float, worldY: Float, worldZ: Float): Float {
            return if (field) cornerPin(worldX, worldY, worldZ) else 1.0f
        }

        fun prepare(state: BlockState, pos: BlockPos, level: ClientLevel) {
            this.level = level
            hanging = HangingVineMultiblockBehavior.isHangingVine(state)
            var anchor: BlockPos = pos
            var multiblock: MultiBlockContributor? = null
            val pipeline: BehaviorPipeline? = SwayAPI.getBehaviorPipeline(state.block)
            if (pipeline != null) {
                for (contributor in pipeline.getMultiBlockContributors()) {
                    if (contributor.appliesTo(state)) {
                        multiblock = contributor
                        anchor = contributor.getAnchorPosition(pos, state)
                        break
                    }
                }
            }
            deformation = null
            if (pipeline != null) {
                for (contributor in pipeline.getDeformationContributors()) {
                    if (contributor.appliesTo(state)) {
                        deformation = contributor
                        break
                    }
                }
            }
            deformationScale = deformation?.getDeformationScale(state, pos) ?: 0.0f
            this.state = state
            this.pos = pos
            anchorY =
                if (hanging) (anchor.y + 1).toFloat()
                else
                    anchor.y +
                            TerrainSlabsCompat.offsetY(
                                level,
                                anchor,
                                if (anchor == pos) state
                                else SnowRealMagicCompat.plantAt(level, anchor),
                            )
            cellX = anchor.x
            cellY = anchor.y
            cellZ = anchor.z
            length = if (multiblock == null) 1 else lengthOf(multiblock, anchor, level)
        }

        fun pushAt(localY: Float, worldY: Float): Float {
            if (pushless) {
                return 0.0f
            }
            return if (vineFree) pushWeightAt(localY) * Mth.clamp(freeTop - worldY, 0.0f, 1.0f)
            else pushWeightAt(localY)
        }

        fun pushWeightAt(localY: Float): Float {
            val contributor = deformation ?: return 0.0f
            return contributor.getVertexWeight(localY, state, pos) * deformationScale
        }

        private fun strandExposure(): Float {
            val anchor: Long = BlockPos.asLong(cellX, cellY, cellZ)
            if (strandExposureByAnchor.containsKey(anchor)) {
                return strandExposureByAnchor.get(anchor)
            }
            val middle: Int = Math.round(freeTop - freeLength * 0.5f)
            var sum: Float = 0.0f
            for (dx in 0..1) {
                for (dz in 0..1) {
                    sum +=
                        requireNotNull(fieldShelter).cornerExposure(cellX + dx, middle, cellZ + dz)
                }
            }
            val reach: Float = sum / 4.0f
            strandExposureByAnchor.put(anchor, reach)
            return reach
        }

        fun waveAt(worldY: Float): Float {
            if (field) {
                return waving
            }
            if (hangsOnLeaves) {
                return if (freeLength == 0) 0.0f
                else curveAt(freeTop - worldY, freeLength, true) * waving
            }
            if (vineFree) {
                return (VINE_TIP_SWAY *
                        Mth.clamp(
                            (freeTop - worldY) / maxOf(VINE_SPAN, freeLength.toFloat()),
                            0.0f,
                            1.0f,
                        ) *
                        waving)
            }
            return weightAt(worldY) * waving
        }

        fun weightAt(worldY: Float): Float {
            val distance: Float = if (hanging) anchorY - worldY else worldY - anchorY
            return curveAt(distance, length, hanging)
        }

        companion object {
            private const val BEHIND_LEAVES: Int = 1
            private const val BEHIND_LEAVES_RANK: Int = 3
            private const val BEHIND_NOTHING: Int = 0
            private const val BEHIND_OTHER: Int = 2
            private const val FREE_PART_MARGIN: Float = 0.01f
            private const val NOT_RESTING: Int = Int.MIN_VALUE

            private fun behind(level: ClientLevel, pos: BlockPos, state: BlockState): Int {
                var clings: Boolean = false
                var found: Int = BEHIND_NOTHING
                val beside: BlockPos.MutableBlockPos = BlockPos.MutableBlockPos()
                for (side in Direction.Plane.HORIZONTAL) {
                    val face = PipeBlock.PROPERTY_BY_DIRECTION.get(side) ?: continue
                    if (state.hasProperty(face) && state.getValue(face)) {
                        clings = true
                        found =
                            maxOf(
                                found,
                                supportAt(level, beside.setWithOffset(pos, side), side.getAxis()),
                            )
                    }
                }
                if (!clings) {
                    for (side in Direction.Plane.HORIZONTAL) {
                        found =
                            maxOf(
                                found,
                                supportAt(level, beside.setWithOffset(pos, side), side.getAxis()),
                            )
                    }
                }
                return if (found == BEHIND_LEAVES_RANK) BEHIND_LEAVES else found
            }

            private fun curveAt(distance: Float, length: Int, hanging: Boolean): Float {
                val along: Float =
                    Mth.clamp(distance / maxOf(SWAY_SPAN, length.toFloat()), 0.0f, 1.0f)
                val curving: Float =
                    Mth.clamp((length - SWAY_SPAN) / COLUMN_CURVE_BLOCKS, 0.0f, 1.0f)
                val eased: Float = curving * curving * (3.0f - 2.0f * curving)
                val exponent: Float =
                    Mth.lerp(eased, 1.0f, if (hanging) HANGING_CURVE else STALK_CURVE)
                val damping: Float =
                    1.0f - minOf(MAX_LENGTH_DAMPING, (length - 1) * LENGTH_DAMPING_PER_BLOCK)
                return (((along).toDouble().pow((exponent).toDouble())).toFloat() *
                        damping *
                        Mth.lerp(eased, 1.0f, LONG_STRAND_SWAY))
            }

            private fun supportAt(level: ClientLevel, pos: BlockPos, axis: Direction.Axis): Int {
                val state: BlockState = level.getBlockState(pos)
                if (state.isAir) {
                    return BEHIND_NOTHING
                }
                if (WavingWhitelist.isLeaves(state.block)) {
                    return BEHIND_LEAVES_RANK
                }
                return if (WindShelter.isSolidSide(level, pos, state, axis)) BEHIND_OTHER
                else BEHIND_NOTHING
            }
        }
    }
}
