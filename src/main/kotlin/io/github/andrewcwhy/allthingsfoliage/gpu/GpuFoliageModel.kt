package io.github.andrewcwhy.allthingsfoliage.gpu

//? fabric {
import java.util.function.Predicate
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter
import net.minecraft.client.renderer.block.BlockAndTintGetter
import net.minecraft.client.renderer.block.dispatch.BlockStateModel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.state.BlockState

/**
 * Excludes GPU-rendered foliage from chunk meshes through Fabric's rendering API.
 *
 * Wraps Sway's model so geometry retained by the chunk mesh still receives Sway's processing.
 */
class GpuFoliageModel(wrapped: BlockStateModel) : WrapperBlockStateModel(wrapped) {
    override fun emitQuads(
        emitter: QuadEmitter,
        level: BlockAndTintGetter,
        pos: BlockPos,
        state: BlockState,
        random: RandomSource,
        cullTest: Predicate<Direction?>,
    ) {
        if (
            GpuFoliageSplit.isFoliageAt(level, pos, state) &&
            GpuFoliageSplit.leaveToGpu(level, pos)
        ) {
            return
        }

        super.emitQuads(emitter, level, pos, state, random, cullTest)
    }
}
//?}
