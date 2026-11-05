package io.github.andrewcwhy.allthingsfoliage.gpu;

//? >=1.21.11 {
import java.util.function.Predicate;
//? fabric {

//? >=1.21.11 {
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
//?} else {
/*import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.resources.model.BakedModel;
*/ //?}
//? >=26.1.2 {
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
//?} elif >=1.21.11 {
/*import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
 */ //?}
//? >=26.1.2 {
import net.minecraft.client.renderer.block.BlockAndTintGetter;
//?} else {
/*import net.minecraft.world.level.BlockAndTintGetter;
 */ //?}
//? >=26.1.2 {
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
//?} elif >=1.21.11 {
/*import net.minecraft.client.renderer.block.model.BlockStateModel;
 */ //?}
import net.minecraft.core.BlockPos;
//? >=1.21.11 {
import net.minecraft.core.Direction;
//?}
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

//?} else {
/*import java.util.function.Supplier;
 */ //?}

/**
 * Wraps the model of every foliage block so a chunk mesher leaves out the foliage the GPU renderer draws.
 * <p>
 * It sits outside Sway's own model wrapper, so everything it does not withhold still goes through Sway.
 * Only the call through Fabric's rendering API is involved: it is how meshers ask for a block's geometry,
 * and it is the call that carries the block's position.
 */
//? >=1.21.11 {
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
//?} else {
/*public final class GpuFoliageModel extends ForwardingBakedModel {

	public GpuFoliageModel(BakedModel wrapped) {
		this.wrapped = wrapped;
	}

	/^*
	 * Never a plain vanilla model, whatever it wraps: a mesher only calls {@code emitBlockQuads} on models that
	 * say so, and that is the one call that carries the block's position. Sway does not wrap every variant of
	 * every block on this version, so the model underneath may well be vanilla's own.
	 ^/
	@Override
	public boolean isVanillaAdapter() {
		return false;
	}

	@Override
	public void emitBlockQuads(BlockAndTintGetter level, BlockState state, BlockPos pos,
			Supplier<RandomSource> random, RenderContext context) {
		// A block whose whitelist group the player has switched off, or snow off the leaves, stays with the chunk mesh.
		if (GpuFoliageSplit.isFoliageAt(level, pos, state) && GpuFoliageSplit.leaveToGpu(level, pos)) {
			return;
		}
		super.emitBlockQuads(level, state, pos, random, context);
	}
}
*/ //?}
//?}
