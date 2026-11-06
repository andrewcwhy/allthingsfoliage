package io.github.andrewcwhy.allthingsfoliage.platform.neoforge;
//? neoforge {
/*
import io.github.andrewcwhy.allthingsfoliage.gpu.GpuFoliageSplit;

import io.github.andrewcwhy.allthingsfoliage.gpu.SnowRealMagicCompat;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.sprite.Material;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/^*
 * Wraps the model of every foliage block so a chunk mesher leaves out the foliage the GPU renderer draws.
 * <p>
 * NeoForge has no rendering API of its own, so it hands the block's position to the model through its
 * {@code collectParts} extension, and that is the call this answers. It sits inside Sway's wrapper rather
 * than outside it: Sway deforms here too, so the renderer meshes through this model to get plain geometry,
 * which its shader then moves itself.
 * <p>
 * Everything else is passed straight through. A model carries rather more of that from 26.1.2 on, where a
 * part knows its material and the flags that go with it; before then a model only names its particle
 * texture.
 ^/
public final class NeoforgeFoliageModel implements BlockStateModel {

	private final BlockStateModel parent;

	public NeoforgeFoliageModel(BlockStateModel parent) {
		this.parent = parent;
	}

	@Override
	public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
			List<BlockStateModelPart> parts) {
		// A block whose whitelist group the player has switched off, or snow off the leaves, stays with the chunk mesh.
		if (GpuFoliageSplit.isFoliageAt(level, pos, state) && GpuFoliageSplit.leaveToGpu(level, pos)) {
			return;
		}
		// Snow! Real Magic picks a part's snowy variant only as it is drawn through its own emitter, and Sway, which
		// wraps this model, hands on only the parts' quads, the plain ones: the plant lost its snow as it was pushed, and
		// a tall plant's top half on the snow never had it. So the snowy parts are handed over from here.
		int from = parts.size();
		parent.collectParts(level, pos, state, random, parts);
		SnowRealMagicCompat.snowyParts(level, pos, state, parts.subList(from, parts.size()));
	}

	@Override
	public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
		parent.collectParts(random, parts);
	}

	@Override
	public Material.Baked particleMaterial() {
		return parent.particleMaterial();
	}

	@Override
	public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
		return parent.particleMaterial(level, pos, state);
	}

	@Override
	public int materialFlags() {
		return parent.materialFlags();
	}

	@Override
	public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) {
		return parent.materialFlags(level, pos, state);
	}

	@Override
	public boolean hasMaterialFlag(int flag) {
		return parent.hasMaterialFlag(flag);
	}

	@Override
	public boolean hasMaterialFlag(BlockAndTintGetter level, BlockPos pos, BlockState state, int flag) {
		return parent.hasMaterialFlag(level, pos, state, flag);
	}

	@Override
	public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
		return parent.createGeometryKey(level, pos, state, random);
	}
}
*/
//?}
