package net.karto.mc2.mc2_interactivefoliage.mixin.polytone;

//? >=1.21.1 && <26.3 {
/*import net.karto.mc2.mc2_interactivefoliage.gpu.GpuFoliageRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/^*
 * Draws the GPU foliage into Polytone's shadow map, as {@link ShadowMapRendererMixin} does, for the Polytone releases
 * that moved it -- 5.1 on 1.21.1, 6.8 on 26.1.2, 6.9 on 26.2: the terrain is drawn into the map by a static
 * ShadowCasterRenderer.render, which is handed the map itself, and the entities after. The map's own accessors give
 * the sun's projection times its view and, from 1.21.11, its two targets; they are reached by name, since Polytone is
 * not compiled against. Both mixins are listed, and each applies only where its class exists.
 ^/
@Mixin(targets = "net.mehvahdjukaar.polytone.content.shaders.shadow.ShadowCasterRenderer", remap = false)
public abstract class ShadowCasterRendererMixin {

	private static MethodHandle mc2$shadowMatrix;
	//? >=1.21.11 {
	private static MethodHandle mc2$color;
	private static MethodHandle mc2$depth;
	//?}
	private static boolean mc2$failed;

	@Inject(method = "render", at = @At(value = "INVOKE",
			target = "Lnet/mehvahdjukaar/polytone/content/shaders/shadow/ShadowMapSettings;renderEntities()Z", ordinal = 0))
	//? >=26.1.2 {
	private static void mc2$drawFoliageShadow(Minecraft minecraft, ClientLevel level, Camera camera, float partialTick,
			@Coerce Object map, @Coerce Object settings, Matrix4f cameraViewProjection,
			com.mojang.blaze3d.buffers.GpuBufferSlice fog, CallbackInfo ci) {
	//?} else {
	/^private static void mc2$drawFoliageShadow(Minecraft minecraft, ClientLevel level, Camera camera, float partialTick,
			@Coerce Object map, @Coerce Object settings, Matrix4f cameraViewProjection, CallbackInfo ci) {
	^///?}
		if (mc2$failed) {
			return;
		}
		try {
			if (mc2$shadowMatrix == null) {
				MethodHandles.Lookup lookup = MethodHandles.publicLookup();
				Class<?> type = map.getClass();
				mc2$shadowMatrix = lookup.findVirtual(type, "shadowMatrix", MethodType.methodType(Matrix4f.class));
				//? >=1.21.11 {
				mc2$color = lookup.findVirtual(type, "colorTextureView",
						MethodType.methodType(com.mojang.blaze3d.textures.GpuTextureView.class));
				mc2$depth = lookup.findVirtual(type, "depthTextureView",
						MethodType.methodType(com.mojang.blaze3d.textures.GpuTextureView.class));
				//?}
			}
			Matrix4f shadowMatrix = (Matrix4f) mc2$shadowMatrix.invoke(map);
			//? >=1.21.11 {
			GpuFoliageRenderer.drawPolytoneShadow(shadowMatrix,
					(com.mojang.blaze3d.textures.GpuTextureView) mc2$color.invoke(map),
					(com.mojang.blaze3d.textures.GpuTextureView) mc2$depth.invoke(map));
			//?} else {
			/^GpuFoliageRenderer.drawPolytoneShadow(shadowMatrix);
			^///?}
		} catch (Throwable e) {
			// A Polytone whose shadow map reads otherwise: its shadows go without the foliage, the game goes on.
			mc2$failed = true;
			net.karto.mc2.mc2_interactivefoliage.ModTemplate.LOGGER.warn(
					"Polytone's shadow map could not be read; the GPU foliage casts no shadow into it", e);
		}
	}
}
*///?}
