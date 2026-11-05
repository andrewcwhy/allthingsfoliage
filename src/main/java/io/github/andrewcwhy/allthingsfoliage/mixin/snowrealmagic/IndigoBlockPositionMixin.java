package net.karto.mc2.mc2_interactivefoliage.mixin.snowrealmagic;

//? forge {

/*import com.github.razorplay01.sway.SwayRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import net.karto.mc2.mc2_interactivefoliage.gpu.GpuFoliageSplit;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/^*
 * Tells Sway which block is being drawn while Indigo draws one, as Sway itself does for vanilla's block renderer.
 * <p>
 * On Forge, Sway bends a plant in {@code getQuads}, which is not handed the block's position; it reads it from what it
 * noted as {@code ModelBlockRenderer.tesselateBlock} began. Snow! Real Magic draws its snow, and the plant inside it,
 * through the copy of Fabric's Indigo renderer it carries, which skips that method -- so Sway never knew where the
 * plant inside was, and never bent it. Here the same note is taken as Indigo starts a block, and dropped when it ends.
 * <p>
 * The same goes for the GPU renderer's decision to draw a plant itself, which the plant's model takes from the data
 * it is handed -- the snow's, in there: it is made here for the snow's position; see GpuFoliageSplit.beginIndigoBlock.
 * <p>
 * Indigo is not compiled against: the class is named, and without it nothing here applies.
 ^/
@Pseudo
@Mixin(targets = "net.fabricmc.fabric.impl.client.indigo.renderer.render.TerrainRenderContext", remap = false)
public abstract class IndigoBlockPositionMixin {

	@Inject(method = "tessellateBlock(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/client/resources/model/BakedModel;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraftforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V",
			at = @At("HEAD"), require = 0, remap = false)
	private void mc2$noteBlockForSway(BlockState state, BlockPos pos, BakedModel model, PoseStack pose, ModelData data,
			RenderType layer, CallbackInfo ci) {
		SwayRenderContext.setCurrentBlockPos(pos);
		GpuFoliageSplit.beginIndigoBlock(state, pos);
	}

	@Inject(method = "tessellateBlock(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/client/resources/model/BakedModel;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraftforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V",
			at = @At("RETURN"), require = 0, remap = false)
	private void mc2$forgetBlockForSway(BlockState state, BlockPos pos, BakedModel model, PoseStack pose, ModelData data,
			RenderType layer, CallbackInfo ci) {
		SwayRenderContext.clear();
		GpuFoliageSplit.endIndigoBlock();
	}
}
*///?}
