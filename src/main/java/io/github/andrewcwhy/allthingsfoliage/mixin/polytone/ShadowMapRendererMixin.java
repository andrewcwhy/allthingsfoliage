package net.karto.mc2.mc2_interactivefoliage.mixin.polytone;

//? >=1.21.1 && <26.3 {
/*import net.karto.mc2.mc2_interactivefoliage.gpu.GpuFoliageRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/^*
 * Draws the GPU foliage into Polytone's shadow map, which Polytone fills by drawing the chunk meshes again from the sun:
 * the chunk mesh leaves the foliage near the player out, so without this those plants cast no shadow. Drawn once the
 * terrain is in the shadow map and before the entities, whether Polytone drew the terrain itself or through Sodium, and
 * only when it draws the map at all.
 * <p>
 * Polytone is not compiled against: the class is named by its name, and only what every version with a shadow map
 * shares is read -- the sun's matrix, and from 1.21.11 its two targets. The method drawing the map has been renamed
 * between releases, so both names are listed. Applied only with Polytone installed, by {@link PolytoneMixinPlugin}; the
 * configuration is not required, so a Polytone release that changes these leaves its shadows without the foliage rather
 * than stopping the game.
 ^/
@Mixin(targets = "net.mehvahdjukaar.polytone.content.shaders.ShadowMapRenderer", remap = false)
public abstract class ShadowMapRendererMixin {

	/^* The sun's projection times its view, set before anything is drawn into the map. ^/
	@Shadow
	@Final
	private Matrix4f shadowMatrix;
	//? >=1.21.11 {
	@Shadow
	private com.mojang.blaze3d.textures.GpuTextureView colorTextureView;
	@Shadow
	private com.mojang.blaze3d.textures.GpuTextureView depthTextureView;
	//?}

	@Inject(method = {"renderShadowMap", "render"}, at = @At(value = "INVOKE",
			target = "Lnet/mehvahdjukaar/polytone/content/shaders/ShadowMapSettings;renderEntities()Z", ordinal = 0))
	private void mc2$drawFoliageShadow(CallbackInfo ci) {
		//? >=1.21.11 {
		GpuFoliageRenderer.drawPolytoneShadow(shadowMatrix, colorTextureView, depthTextureView);
		//?} else {
		/^GpuFoliageRenderer.drawPolytoneShadow(shadowMatrix);
		^///?}
	}
}
*///?}
