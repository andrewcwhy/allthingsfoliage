package net.karto.mc2.mc2_interactivefoliage.mixin.iris;

//? iris && <26.1.2 {
/*import net.irisshaders.iris.shadows.ShadowRenderer;
import net.irisshaders.iris.uniforms.CameraUniforms;
import net.karto.mc2.mc2_interactivefoliage.gpu.IrisFoliageShaders;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/^*
 * Draws the foliage into a shader pack's shadow map before 26.1.2, where Iris has no shadow render callback: at the
 * point it would call one from 26.1.2 on, once the terrain is in the shadow map and before the entities, with the sun's
 * matrices and the camera the shadow pass renders from. Listed by IrisMixinPlugin only on these versions.
 ^/
@Mixin(value = ShadowRenderer.class, remap = false)
public abstract class ShadowRendererMixin {

	//? >=1.21.11 {
	@Inject(method = "renderShadows", at = @At(value = "FIELD",
			target = "Lnet/irisshaders/iris/pipeline/WorldRenderingPhase;ENTITIES:Lnet/irisshaders/iris/pipeline/WorldRenderingPhase;",
			ordinal = 0))
	//?} else {
	/^// Before 1.21.11 the pass sets no phases; the entities start where the viewport is set back to the shadow map's.
	@Inject(method = "renderShadows", at = @At(value = "INVOKE",
			target = "Lcom/mojang/blaze3d/systems/RenderSystem;viewport(IIII)V", ordinal = 0, remap = true))
	^///?}
	private void mc2$drawFoliageShadow(CallbackInfo ci) {
		Vector3d camera = CameraUniforms.getUnshiftedCameraPosition();
		IrisFoliageShaders.drawShadow(ShadowRenderer.MODELVIEW, ShadowRenderer.PROJECTION, camera.x(), camera.y(), camera.z());
	}
}
*///?}
