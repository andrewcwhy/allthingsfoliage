package io.github.andrewcwhy.allthingsfoliage.gpu

import com.github.razorplay01.sway.api.SwayAPI
import io.github.andrewcwhy.allthingsfoliage.client.renderer.WavingWhitelist
import io.github.andrewcwhy.allthingsfoliage.gpu.compat.SnowRealMagicCompat
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap
import it.unimi.dsi.fastutil.longs.LongArrayList
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.LongConsumer
import kotlin.math.abs
import net.minecraft.client.renderer.block.BlockAndTintGetter
import net.minecraft.client.renderer.block.dispatch.BlockStateModel
import net.minecraft.client.renderer.chunk.RenderSectionRegion
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState

/**
 * Coordinates foliage ownership between GPU rendering and chunk meshes.
 *
 * Meshing threads report decisions through an ordered queue. Ownership changes when the
 * corresponding chunk mesh is uploaded, preventing missing or duplicated foliage.
 *
 * Mutable section tracking belongs to the render thread unless explicitly concurrent.
 */
object GpuFoliageSplit {
    private const val HAND_BACK_TIMEOUT_MILLIS = 5_000L
    private const val REMESH_TIMEOUT_MILLIS = 2_000L

    @Volatile
    private var area: Area? = null

    private val awaitingGroup = ConcurrentHashMap.newKeySet<Long>()

    @Volatile
    private var decisionGeneration = 0

    private val decisions = ConcurrentLinkedQueue<MeshEvent>()

    @Volatile
    private var foliage: Set<Block>? = null

    @Volatile
    private var foliageGeneration = 0

    private val gpuModels = Reference2ObjectOpenHashMap<BlockState, BlockStateModel>()
    private val gpuReady = ConcurrentHashMap.newKeySet<Long>()
    private val handingBack = Long2LongOpenHashMap()
    private val lastDecision = ThreadLocal.withInitial { LastDecision() }
    private val meshedWithoutFoliage = LongOpenHashSet()
    private val needsRebuild = LongOpenHashSet()
    private val rebuildNow = LongArrayList()
    private val remeshNow = LongArrayList()
    private val remeshOnSwap = Long2LongOpenHashMap()
    private val snowPlantLeft = ThreadLocal.withInitial { false }
    private val sodiumLevelSlice =
        findClass("net.caffeinemc.mods.sodium.client.world.LevelSlice")
    private val takingOver = LongOpenHashSet()

    val centerX: Int
        get() {
            return area?.centerX ?: 0
        }

    val centerZ: Int
        get() {
            return area?.centerZ ?: 0
        }

    val hasArea: Boolean
        get() {
            return area != null
        }

    val listGeneration: Int
        get() {
            return foliageGeneration
        }

    val radius: Int
        get() {
            return area?.radius ?: 0
        }

    /** Published as one immutable value so meshing threads see a complete area update. */
    private data class Area(
        val centerX: Int,
        val centerZ: Int,
        val radius: Int,
    ) {
        fun contains(chunkX: Int, chunkZ: Int): Boolean {
            return maxOf(abs(chunkX - centerX), abs(chunkZ - centerZ)) <= radius
        }
    }

    /** Avoids recording the same section decision for every foliage block in a build. */
    private class LastDecision {
        var generation = -1
        var leftFoliage = false
        var sectionKey = 0L
    }

    private sealed interface MeshEvent

    private data class MeshDecision(
        val sectionKey: Long,
        val leftFoliage: Boolean,
    ) : MeshEvent

    private data class MeshSwap(val sectionKey: Long) : MeshEvent

    /** Applies queued build decisions and mesh replacements in their reported order. */
    fun applyMeshDecisions() {
        while (true) {
            val event = decisions.poll() ?: break

            when (event) {
                is MeshDecision -> {
                    val key = event.sectionKey

                    if (event.leftFoliage) {
                        // The old chunk mesh remains visible until the new mesh is uploaded.
                        if (!meshedWithoutFoliage.contains(key)) {
                            takingOver.add(key)
                        }

                        needsRebuild.remove(key)
                    } else {
                        takingOver.remove(key)

                        if (meshedWithoutFoliage.remove(key)) {
                            handingBack.put(
                                key,
                                System.currentTimeMillis() + HAND_BACK_TIMEOUT_MILLIS,
                            )
                        }

                        if (
                            isNear(SectionPos.x(key), SectionPos.z(key)) &&
                            gpuReady.contains(key)
                        ) {
                            needsRebuild.add(key)
                        }
                    }
                }

                is MeshSwap -> {
                    val key = event.sectionKey

                    handingBack.remove(key)

                    if (remeshOnSwap.remove(key) != 0L) {
                        remeshNow.add(key)
                    }

                    if (takingOver.remove(key)) {
                        meshedWithoutFoliage.add(key)
                    }

                    if (
                        needsRebuild.remove(key) &&
                        isNear(SectionPos.x(key), SectionPos.z(key)) &&
                        !meshedWithoutFoliage.contains(key)
                    ) {
                        rebuildNow.add(key)
                    }
                }
            }
        }
    }

    /** Keeps newly enabled foliage groups in the chunk mesh until GPU geometry is uploaded. */
    fun awaitGroup(sectionKey: Long) {
        awaitingGroup.add(sectionKey)
    }

    fun beginIndigoBlock(state: BlockState, pos: BlockPos) {
        snowPlantLeft.set(
            SnowRealMagicCompat.isSnowBlock(state) && decide(pos)
        )
    }

    /** Restricts contained-plant suppression to actual chunk builds. */
    fun beginIndigoBlock(
        level: BlockAndTintGetter,
        state: BlockState,
        pos: BlockPos,
    ) {
        snowPlantLeft.set(
            SnowRealMagicCompat.isSnowBlock(state) && leaveToGpu(level, pos)
        )
    }

    /** Excludes blocks Sway pushes without contributing any deformation of their own. */
    fun bends(block: Block): Boolean {
        return SwayAPI.isInteractive(block) &&
                SwayAPI.getBehaviorPipeline(block).deformationContributors.isNotEmpty()
    }

    /** Includes sections still waiting for a replacement chunk mesh to reclaim their foliage. */
    fun chunkMeshLeftFoliage(sectionKey: Long): Boolean {
        if (meshedWithoutFoliage.contains(sectionKey)) {
            return true
        }

        val deadline = handingBack.get(sectionKey)

        if (deadline == 0L) {
            return false
        }

        if (System.currentTimeMillis() > deadline) {
            handingBack.remove(sectionKey)
            return false
        }

        return true
    }

    /** Clears the active area so subsequent chunk builds retain their foliage. */
    fun clearArea() {
        area = null
        handingBack.clear()
        needsRebuild.clear()
        rebuildNow.clear()
        takingOver.clear()
        remeshOnSwap.clear()
        remeshNow.clear()
        awaitingGroup.clear()
        decisionGeneration++
    }

    private fun decide(pos: BlockPos): Boolean {
        val sectionX = SectionPos.blockToSectionCoord(pos.x)
        val sectionZ = SectionPos.blockToSectionCoord(pos.z)
        val sectionKey = SectionPos.asLong(
            sectionX,
            SectionPos.blockToSectionCoord(pos.y),
            sectionZ,
        )

        val last = lastDecision.get()
        val currentGeneration = decisionGeneration

        if (
            last.sectionKey == sectionKey &&
            last.generation == currentGeneration
        ) {
            return last.leftFoliage
        }

        val currentArea = area
        val near = currentArea != null && currentArea.contains(sectionX, sectionZ)
        val leave = near && gpuReady.contains(sectionKey)

        last.sectionKey = sectionKey
        last.leftFoliage = leave
        last.generation = currentGeneration

        decisions.add(MeshDecision(sectionKey, leave))

        return leave
    }

    fun endIndigoBlock() {
        snowPlantLeft.set(false)
    }

    private fun findClass(name: String): Class<*>? {
        try {
            return Class.forName(
                name,
                false,
                GpuFoliageSplit::class.java.classLoader,
            )
        } catch (_: ClassNotFoundException) {
            return null
        }
    }

    /** Invalidates block classification after a waving group changes. */
    fun foliageChanged() {
        foliage = null
        foliageGeneration++
    }

    /** Removes unloaded-section tracking so its next build can establish ownership again. */
    fun forgetSection(sectionKey: Long) {
        handingBack.remove(sectionKey)
        remeshOnSwap.remove(sectionKey)
        needsRebuild.remove(sectionKey)
        takingOver.remove(sectionKey)

        if (meshedWithoutFoliage.remove(sectionKey)) {
            decisionGeneration++
        }
    }

    /** Delivers rebuild requests after the preceding chunk builds have reached the screen. */
    fun forEachRebuildDue(rebuild: LongConsumer) {
        for (index in 0 until rebuildNow.size) {
            rebuild.accept(rebuildNow.getLong(index))
        }

        rebuildNow.clear()
    }

    /** Delivers GPU remesh requests after chunk replacement or the fallback timeout. */
    fun forEachRemeshDue(remesh: LongConsumer) {
        if (!remeshOnSwap.isEmpty()) {
            val now = System.currentTimeMillis()
            val entries = remeshOnSwap.long2LongEntrySet().fastIterator()

            while (entries.hasNext()) {
                val entry = entries.next()

                if (now > entry.longValue) {
                    remeshNow.add(entry.longKey)
                    entries.remove()
                }
            }
        }

        for (index in 0 until remeshNow.size) {
            remesh.accept(remeshNow.getLong(index))
        }

        remeshNow.clear()
    }

    /** Releases a newly enabled group only after matching GPU geometry has been uploaded. */
    fun groupTakenOver(sectionKey: Long, meshedGeneration: Int): Boolean {
        return meshedGeneration == foliageGeneration && awaitingGroup.remove(sectionKey)
    }

    /** Publishes a complete identity-based classification for meshing threads. */
    fun isFoliage(state: BlockState): Boolean {
        val known = foliage ?: ReferenceOpenHashSet<Block>().apply {
            for (block in BuiltInRegistries.BLOCK) {
                if (waves(block)) {
                    add(block)
                }
            }
        }.also {
            foliage = it
        }

        return state.block in known
    }

    /** Includes position-dependent foliage, such as snow resting on waving leaves. */
    fun isFoliageAt(
        level: BlockGetter,
        pos: BlockPos,
        state: BlockState,
    ): Boolean {
        val block = state.block

        if (
            awaitingGroup.isNotEmpty() &&
            (WavingWhitelist.isLeaves(block) || WavingWhitelist.restsOnLeaves(block)) &&
            awaitingGroup.contains(
                SectionPos.asLong(
                    SectionPos.blockToSectionCoord(pos.x),
                    SectionPos.blockToSectionCoord(pos.y),
                    SectionPos.blockToSectionCoord(pos.z),
                )
            )
        ) {
            return false
        }

        if (isFoliage(state)) {
            return true
        }

        if (!WavingWhitelist.leavesWave() || !WavingWhitelist.restsOnLeaves(block)) {
            return false
        }

        val currentArea = area ?: return false

        return currentArea.contains(
            SectionPos.blockToSectionCoord(pos.x),
            SectionPos.blockToSectionCoord(pos.z),
        ) && WavingWhitelist.isLeaves(level.getBlockState(pos.below()).block)
    }

    fun isGpuReady(sectionKey: Long): Boolean {
        return gpuReady.contains(sectionKey)
    }

    fun isNear(chunkX: Int, chunkZ: Int): Boolean {
        val currentArea = area ?: return false
        return currentArea.contains(chunkX, chunkZ)
    }

    /**
     * Suppresses geometry only during chunk meshing.
     *
     * Moving blocks and the GPU renderer's own meshing requests retain their full models.
     */
    fun leaveToGpu(level: BlockAndTintGetter, pos: BlockPos): Boolean {
        if (
            level !is RenderSectionRegion &&
            sodiumLevelSlice?.isInstance(level) != true
        ) {
            return false
        }

        return decide(pos)
    }

    /** Marks uploaded geometry as available and schedules any necessary chunk ownership update. */
    fun markGpuReady(sectionKey: Long): Boolean {
        if (!gpuReady.add(sectionKey)) {
            return false
        }

        decisionGeneration++

        if (
            isNear(SectionPos.x(sectionKey), SectionPos.z(sectionKey)) &&
            !meshedWithoutFoliage.contains(sectionKey) &&
            !takingOver.contains(sectionKey)
        ) {
            needsRebuild.add(sectionKey)
        }

        return true
    }

    fun mayHoldFoliage(state: BlockState): Boolean {
        return isFoliage(state) ||
                SnowRealMagicCompat.isSnowBlock(state) ||
                (
                        WavingWhitelist.leavesWave() &&
                                WavingWhitelist.restsOnLeaves(state.block)
                        )
    }

    /** Includes disabled waving groups so their models are ready when those groups are enabled. */
    fun mayWave(block: Block): Boolean {
        return bends(block) ||
                WavingWhitelist.listed(block) ||
                WavingWhitelist.restsOnLeaves(block)
    }

    /** Uses the model inside Sway's wrapper to avoid baking deformation twice. */
    fun modelFor(
        state: BlockState,
        fallback: BlockStateModel,
    ): BlockStateModel {
        return gpuModels.get(state) ?: fallback
    }

    /** Queues a render-thread chunk replacement after its recorded meshing decisions. */
    fun onChunkMeshSwapped(sectionX: Int, sectionY: Int, sectionZ: Int) {
        val key = SectionPos.asLong(sectionX, sectionY, sectionZ)

        if (
            meshedWithoutFoliage.contains(key) ||
            handingBack.containsKey(key) ||
            takingOver.contains(key) ||
            isNear(sectionX, sectionZ)
        ) {
            decisions.add(MeshSwap(key))
        }
    }

    /** Includes empty meshes replaced off-thread when a section contains only GPU foliage. */
    fun onChunkMeshSwappedElsewhere(sectionX: Int, sectionY: Int, sectionZ: Int) {
        val key = SectionPos.asLong(sectionX, sectionY, sectionZ)

        if (gpuReady.contains(key)) {
            decisions.add(MeshSwap(key))
        }
    }

    fun registerGpuModel(state: BlockState, model: BlockStateModel) {
        gpuModels.put(state, model)
    }

    /** Defers GPU remeshing until the corresponding chunk mesh has been replaced. */
    fun remeshOnSwap(sectionKey: Long) {
        remeshOnSwap.put(
            sectionKey,
            System.currentTimeMillis() + REMESH_TIMEOUT_MILLIS,
        )
    }

    fun restsOnWavingLeaves(
        level: BlockGetter,
        pos: BlockPos,
        state: BlockState,
    ): Boolean {
        return WavingWhitelist.leavesWave() &&
                WavingWhitelist.restsOnLeaves(state.block) &&
                WavingWhitelist.isLeaves(level.getBlockState(pos.below()).block)
    }

    fun setArea(centerX: Int, centerZ: Int, radius: Int) {
        area = Area(centerX, centerZ, radius)
        decisionGeneration++
    }

    fun unmarkGpuReady(sectionKey: Long) {
        if (gpuReady.remove(sectionKey)) {
            decisionGeneration++
        }

        awaitingGroup.remove(sectionKey)
    }

    fun waves(block: Block): Boolean {
        return bends(block) || WavingWhitelist.contains(block)
    }
}
