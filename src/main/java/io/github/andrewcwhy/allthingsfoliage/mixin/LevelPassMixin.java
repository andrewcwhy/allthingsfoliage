package io.github.andrewcwhy.allthingsfoliage.mixin;

import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import io.github.andrewcwhy.allthingsfoliage.gpu.GpuFoliageRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hands the GPU foliage the render pass vanilla draws the opaque world in, and the view it draws the level with.
 * <p>
 * From 26.3 the opaque terrain and the solid features are drawn inside one pass vanilla opens itself, and nothing
 * outside it is handed that pass. The foliage goes in once the opaque terrain is drawn and before the solid features,
 * the moment Fabric's opaque-terrain event marks. Everything it needs was written while the frame was prepared,
 * except what holds the view, which is written as the level starts to be drawn; in the pass it is only bound and drawn.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelPassMixin {

    /**
     * The view is final only here, before any pass is open: while a shader pack is loaded, Iris moves the camera's
     * bobbing out of the projection and into the view just before the level is drawn. The terrain's fog is kept for the
     * shadow map a shader pack draws next, before vanilla sets it.
     */
    @Inject(method = "render", at = @At("HEAD"))
    private void mc2$writeFoliageUniforms(
        GraphicsResourceAllocator resourceAllocator,
        boolean renderOutline,
        CameraRenderState cameraState,
        GpuBufferSlice terrainFog,
        Vector4f fogColor,
        boolean shouldRenderSky,
        boolean consistentDepthRequired,
        CallbackInfo ci
    ) {
        GpuFoliageRenderer.writeLevelUniforms(
            cameraState.viewRotationMatrix,
            terrainFog
        );
    }

    @Inject(
        method = "executeSolid",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;executeSolid(Lcom/mojang/renderpearl/api/commands/RenderPass;)V"
        )
    )
    private void mc2$drawFoliage(
        ChunkSectionsToRender chunkSectionsToRender,
        FeatureRenderDispatcher.PreparedFrame featureFrame,
        RenderPass renderPass,
        CallbackInfo ci
    ) {
        GpuFoliageRenderer.drawIntoLevelPass(renderPass);
    }
}
