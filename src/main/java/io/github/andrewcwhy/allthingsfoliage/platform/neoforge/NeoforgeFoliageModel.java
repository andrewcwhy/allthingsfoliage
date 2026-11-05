package io.github.andrewcwhy.allthingsfoliage.platform.neoforge;

//? neoforge && >=1.21.1 {

/*import io.github.andrewcwhy.allthingsfoliage.gpu.GpuFoliageSplit;
//? >=26.1.2 {
import io.github.andrewcwhy.allthingsfoliage.gpu.SnowRealMagicCompat;
//?}
//? <1.21.11 {
/^import com.github.razorplay01.sway.platform.neoforge.util.SwayModel;
import io.github.andrewcwhy.allthingsfoliage.gpu.SnowRealMagicCompat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
^///?}
//? >=26.1.2 {
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.sprite.Material;
//?} elif >=1.21.11 {
/^import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.BlockAndTintGetter;
^///?}
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

//? >=1.21.11 {
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

	//? >=26.1.2 {
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
	//?} elif >=1.21.11 {
	/^@Override
	public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
			List<BlockModelPart> parts) {
		// A block whose whitelist group the player has switched off, or snow off the leaves, stays with the chunk mesh.
		if (GpuFoliageSplit.isFoliageAt(level, pos, state) && GpuFoliageSplit.leaveToGpu(level, pos)) {
			return;
		}
		parent.collectParts(level, pos, state, random, parts);
	}

	@Override
	public void collectParts(RandomSource random, List<BlockModelPart> parts) {
		parent.collectParts(random, parts);
	}

	@Override
	public TextureAtlasSprite particleIcon() {
		return parent.particleIcon();
	}

	@Override
	public TextureAtlasSprite particleIcon(BlockAndTintGetter level, BlockPos pos, BlockState state) {
		return parent.particleIcon(level, pos, state);
	}
	^///?}

	@Override
	public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
		return parent.createGeometryKey(level, pos, state, random);
	}
}
//?} else {
/^// Wraps the model of every foliage block so a chunk mesher leaves out the foliage the GPU renderer draws.
//
// Before 1.21.11 a model is not handed the block's position while its quads are asked for. It is handed it
// just before, in getModelData, which vanilla's chunk compiler and Sodium both call for every block. So the
// decision is made there and carried in the model data to the calls that follow, which then hand over
// nothing for a block the GPU renderer draws.
//
// It sits outside Sway's own wrapper, unlike on newer versions: Sway's getModelData does not ask the model
// inside it, so a wrapper in there would never see the position. Everything it does not withhold goes to
// Sway as before. The renderer meshes from the model as it was before Sway wrapped it, which the hooks keep.
public final class NeoforgeFoliageModel extends BakedModelWrapper<BakedModel> {

	// Set in the model data of a block whose foliage the GPU renderer draws.
	private static final ModelProperty<Boolean> LEFT_TO_GPU = new ModelProperty<>();
	// Set in the model data of a plant Snow! Real Magic draws snowy: held in its snow, or a tall plant's top half on it.
	private static final ModelProperty<Boolean> SNOWY = new ModelProperty<>();

	// The snowy variant last drawn, and Sway's wrapper around it, which bends it as it bends the plant.
	private BakedModel snowyVariant;
	private BakedModel swayedSnowyVariant;

	public NeoforgeFoliageModel(BakedModel swayModel) {
		super(swayModel);
	}

	// Asked for a plant of its own by the chunk compiler, and for a plant held in Snow! Real Magic's snow as the snow
	// draws it (see SnowRenderApiMixin), at the snow's position either way.
	@Override
	public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
		// A block whose whitelist group the player has switched off, or snow off the leaves, stays with the chunk mesh.
		if (GpuFoliageSplit.isFoliageAt(level, pos, state) && GpuFoliageSplit.leaveToGpu(level, pos)) {
			return modelData.derive().with(LEFT_TO_GPU, Boolean.TRUE).build();
		}
		ModelData data = super.getModelData(level, pos, state, modelData);
		if (SnowRealMagicCompat.drawnSnowy(level, pos, state)) {
			data = data.derive().with(SNOWY, Boolean.TRUE).build();
		}
		return data;
	}

	// No layer at all for a block left to the GPU renderer, so a mesher skips it outright.
	@Override
	public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
		if (data.has(LEFT_TO_GPU)) {
			return ChunkRenderTypeSet.none();
		}
		return super.getRenderTypes(state, rand, data);
	}

	// And no quads, for a mesher that asks regardless.
	@Override
	public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData extraData,
			RenderType renderType) {
		if (extraData.has(LEFT_TO_GPU)) {
			return List.of();
		}
		// Where Snow! Real Magic draws the plant snowy. It picks its snowy variant only when asked through Fabric's
		// rendering API, which Sway's wrapper and this one do not pass on, so the variant is drawn from here -- through
		// Sway's own wrapper, so it bends as the plant would.
		if (state != null && extraData.has(SNOWY)) {
			BakedModel variant = SnowRealMagicCompat.snowyVariantOf(this);
			if (variant != null) {
				if (variant != snowyVariant) {
					swayedSnowyVariant = new SwayModel(variant);
					snowyVariant = variant;
				}
				return swayedSnowyVariant.getQuads(state, side, rand, extraData, renderType);
			}
		}
		return super.getQuads(state, side, rand, extraData, renderType);
	}
}
^///?}
*/ //?}
