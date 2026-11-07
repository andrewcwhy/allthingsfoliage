package io.github.andrewcwhy.allthingsfoliage.gpu;
//? fabric {

import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Wraps the model of every foliage block so a chunk mesher leaves out the foliage the GPU renderer draws.
 * <p>
 * It sits outside Sway's own model wrapper, so everything it does not withhold still goes through Sway.
 * Only the call through Fabric's rendering API is involved: it is how meshers ask for a block's geometry,
 * and it is the call that carries the block's position.
 */

public final class GpuFoliageModel extends WrapperBlockStateModel {

    public GpuFoliageModel(BlockStateModel wrapped) {
        super(wrapped);
    }

    @Override
    public void emitQuads(
        QuadEmitter emitter,
        BlockAndTintGetter level,
        BlockPos pos,
        BlockState state,
        RandomSource random,
        Predicate<Direction> cullTest
    ) {
        // A block whose whitelist group the player has switched off, or snow off the leaves, stays with the chunk mesh.
        if (
            GpuFoliageSplit.isFoliageAt(level, pos, state) &&
            GpuFoliageSplit.leaveToGpu(level, pos)
        ) {
            return;
        }
        super.emitQuads(emitter, level, pos, state, random, cullTest);
    }
}
//?}
