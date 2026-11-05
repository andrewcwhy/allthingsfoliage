package net.karto.mc2.mc2_interactivefoliage.mixin.snowrealmagic;

//? forge {

/*import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.karto.mc2.mc2_interactivefoliage.gpu.GpuFoliageSplit;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/^*
 * Makes the GPU renderer's decision for the plant inside Snow! Real Magic's snow when Embeddium meshes the chunk.
 * <p>
 * Embeddium draws a model of Fabric's rendering API, such as Snow! Real Magic's snow, through Indigo's block context,
 * which it extends; the snow draws the plant inside through the plant's own model, handed the snow's model data, so the
 * plant's model cannot decide for itself whether the GPU renderer draws it. The decision is made here as the block
 * starts, the same way as for any plant -- and only in a chunk build: Indigo draws falling and moved blocks through
 * here too, which always keep their plant. See IndigoBlockPositionMixin for the same on vanilla's chunk builds.
 * <p>
 * Indigo is not compiled against: the class is named, and without it nothing here applies.
 ^/
@Pseudo
@Mixin(targets = "net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderContext", remap = false)
public abstract class IndigoBlockRenderMixin {

	@Inject(method = "render(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;JILnet/minecraftforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V",
			at = @At("HEAD"), require = 0, remap = false)
	private void mc2$decideSnowPlant(BlockAndTintGetter level, BakedModel model, BlockState state, BlockPos pos,
			PoseStack pose, VertexConsumer buffer, boolean cull, RandomSource random, long seed, int overlay, ModelData data,
			RenderType layer, CallbackInfo ci) {
		GpuFoliageSplit.beginIndigoBlock(level, state, pos);
	}

	@Inject(method = "render(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;JILnet/minecraftforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V",
			at = @At("RETURN"), require = 0, remap = false)
	private void mc2$forgetSnowPlant(BlockAndTintGetter level, BakedModel model, BlockState state, BlockPos pos,
			PoseStack pose, VertexConsumer buffer, boolean cull, RandomSource random, long seed, int overlay, ModelData data,
			RenderType layer, CallbackInfo ci) {
		GpuFoliageSplit.endIndigoBlock();
	}
}
*///?}
