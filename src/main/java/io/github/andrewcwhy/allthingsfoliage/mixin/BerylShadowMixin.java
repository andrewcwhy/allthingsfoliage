package io.github.andrewcwhy.allthingsfoliage.mixin;

import io.github.andrewcwhy.allthingsfoliage.gpu.GpuFoliageRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the GPU foliage into Beryl's shadow map, as {@code ShadowMapRendererMixin} does for Polytone's.
 * <p>
 * Beryl draws its shadow map in {@code renderShadowMap}: its terrain first, from its own chunk meshes, then the
 * entities through the game's own pipelines. The call that sets the light's projection for those entities is the one
 * place between the two, so the foliage goes in there, over the terrain it stands on.
 * <p>
 * Beryl is optional, and not on the compile classpath: the target is named, and does nothing when it is absent or
 * written another way.
 */
@Pseudo
@Mixin(targets = "net.beryl.render.ShadowMap", remap = false)
public abstract class BerylShadowMixin {

    @Inject(
        method = "renderShadowMap",
        at = @At(
            value = "INVOKE",
            target = "Lnet/vulkanmod/vulkan/VRenderSystem;applyProjectionMatrix(Lorg/joml/Matrix4f;)V"
        ),
        require = 0,
        remap = false
    )
    private void mc2$foliageIntoShadowMap(CallbackInfo ci) {
        GpuFoliageRenderer.drawBerylShadow();
    }
}
