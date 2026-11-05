package net.karto.mc2.mc2_interactivefoliage.mixin.snowrealmagic.neoforge;

//? neoforge && <1.21.11 {

/*import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.karto.mc2.mc2_interactivefoliage.ModTemplate;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/^*
 * Hands each model Snow! Real Magic draws inside its snow the model data of its own, so Sway bends the plant in there,
 * and lets that plant be tinted once.
 * <p>
 * On NeoForge Sway bends a plant by the model data its model gives for the block's position: getModelData puts the push
 * there, and getQuads deforms by it. The mod's own wrapper decides there too whether the GPU renderer draws the plant,
 * and whether it is drawn snowy. Snow! Real Magic draws its snow, and the plant inside it, through Fabric's rendering
 * API, handing every model it draws the snow's model data -- which carries none of that. Here each part is handed what
 * its own model makes of the snow's data at that position, as NeoForge's chunk compiler does for every block:
 * <ul>
 *   <li>through Indigo, the copy Snow! Real Magic carries, as the part is prepared (prepareForBlock);</li>
 *   <li>through Sodium's implementation, which prepares nothing, pushed on its context around the part's drawing.</li>
 * </ul>
 * Indigo also multiplies a tinted quad by the block's color after Snow! Real Magic's transform has painted it with that
 * color already: the plant came out a darker green than the plants around it. Only there is the color it paints taken
 * to be white, so Indigo's tint is the only one; Sodium tints by the snow block, which has no color, and keeps it.
 * <p>
 * Snow! Real Magic, Indigo and Sodium are not compiled against: they are named, and without them nothing here applies.
 ^/
@Pseudo
@Mixin(targets = "snownee.snow.client.FabricRendererRenderAPI", remap = false)
public abstract class SnowRenderApiMixin {

	@Shadow
	@Final
	private BlockAndTintGetter level;

	@Shadow
	@Final
	private BlockPos pos;

	/^* Whether Indigo prepared the part being drawn: set as it does, for the rest of that part. ^/
	@Unique
	private boolean mc2$indigo;
	/^* Whether model data was pushed on the context for the part being drawn, to be popped after it. ^/
	@Unique
	private boolean mc2$pushed;

	@Unique
	private static Field mc2$contextField;
	@Unique
	private static Method mc2$getModelData;
	@Unique
	private static Method mc2$pushModelData;
	@Unique
	private static Method mc2$popModelData;
	@Unique
	private static Class<?> mc2$contextClass;
	@Unique
	private static boolean mc2$unknown;

	@Inject(method = "render", at = @At("HEAD"), require = 0, remap = false)
	private void mc2$beginPart(CallbackInfoReturnable<Boolean> cir) {
		mc2$indigo = false;
		mc2$pushed = false;
	}

	@ModifyArg(method = "render", require = 0, remap = false,
			at = @At(value = "INVOKE", remap = false,
					target = "Lnet/fabricmc/fabric/impl/client/indigo/renderer/render/BlockRenderInfo;prepareForBlock(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;ZLnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V"),
			index = 3)
	private ModelData mc2$ownModelData(BlockState state, BlockPos at, boolean ao, ModelData snowData, RenderType layer,
			@Local(argsOnly = true) BakedModel model) {
		mc2$indigo = true;
		return model.getModelData(level, pos, state, snowData);
	}

	@Inject(method = "render", require = 0, remap = false,
			at = @At(value = "INVOKE", remap = false,
					target = "Lnet/fabricmc/fabric/api/renderer/v1/model/FabricBakedModel;emitBlockQuads(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Ljava/util/function/Supplier;Lnet/fabricmc/fabric/api/renderer/v1/render/RenderContext;)V"))
	private void mc2$pushOwnModelData(CallbackInfoReturnable<Boolean> cir, @Local(argsOnly = true) BlockState state,
			@Local(argsOnly = true) BakedModel model) {
		if (mc2$indigo) {
			return;
		}
		Object context = mc2$context();
		if (context == null) {
			return;
		}
		try {
			ModelData snowData = (ModelData) mc2$getModelData.invoke(context);
			ModelData own = model.getModelData(level, pos, state, snowData != null ? snowData : ModelData.EMPTY);
			if (own != snowData) {
				mc2$pushModelData.invoke(context, own);
				mc2$pushed = true;
			}
		} catch (ReflectiveOperationException | RuntimeException e) {
			mc2$fail(e);
		}
	}

	@Inject(method = "render", require = 0, remap = false,
			at = @At(value = "INVOKE", shift = At.Shift.AFTER, remap = false,
					target = "Lnet/fabricmc/fabric/api/renderer/v1/model/FabricBakedModel;emitBlockQuads(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Ljava/util/function/Supplier;Lnet/fabricmc/fabric/api/renderer/v1/render/RenderContext;)V"))
	private void mc2$popOwnModelData(CallbackInfoReturnable<Boolean> cir) {
		if (!mc2$pushed) {
			return;
		}
		mc2$pushed = false;
		try {
			mc2$popModelData.invoke(mc2$context());
		} catch (ReflectiveOperationException | RuntimeException e) {
			mc2$fail(e);
		}
	}

	@ModifyExpressionValue(method = "lambda$render$0", require = 0, remap = false,
			at = @At(value = "INVOKE", remap = false,
					target = "Lnet/minecraft/client/color/block/BlockColors;getColor(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;I)I"))
	private int mc2$tintOnce(int color) {
		return mc2$indigo ? -1 : color;
	}

	/^*
	 * The rendering context the part is drawn into, when it is one that takes model data pushed on it (Sodium's), or
	 * null. Found by name: Fabric's rendering API is not compiled against.
	 ^/
	@Unique
	private Object mc2$context() {
		if (mc2$unknown) {
			return null;
		}
		try {
			if (mc2$contextField == null) {
				mc2$contextField = getClass().getDeclaredField("context");
				mc2$contextField.setAccessible(true);
			}
			Object context = mc2$contextField.get(this);
			if (context == null) {
				return null;
			}
			if (context.getClass() != mc2$contextClass) {
				mc2$getModelData = context.getClass().getMethod("getModelData");
				mc2$pushModelData = context.getClass().getMethod("pushModelData", ModelData.class);
				mc2$popModelData = context.getClass().getMethod("popModelData");
				mc2$contextClass = context.getClass();
			}
			return context;
		} catch (NoSuchMethodException e) {
			// A context that takes no pushed model data (an older Fabric rendering API): the plant keeps the snow's.
			return null;
		} catch (ReflectiveOperationException | RuntimeException e) {
			mc2$fail(e);
			return null;
		}
	}

	@Unique
	private static void mc2$fail(Exception e) {
		if (!mc2$unknown) {
			mc2$unknown = true;
			ModTemplate.LOGGER.warn("Could not hand the plant in Snow! Real Magic's snow its own model data: it is drawn "
					+ "as the snow draws it", e);
		}
	}
}
*///?}
