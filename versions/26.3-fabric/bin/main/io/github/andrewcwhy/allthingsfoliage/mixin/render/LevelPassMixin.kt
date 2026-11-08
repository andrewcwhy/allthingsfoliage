package io.github.andrewcwhy.allthingsfoliage.mixin.render

import com.mojang.blaze3d.resource.GraphicsResourceAllocator
import com.mojang.renderpearl.api.buffers.GpuBufferSlice
import com.mojang.renderpearl.api.commands.RenderPass
import io.github.andrewcwhy.allthingsfoliage.gpu.GpuFoliageRenderer
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher
import net.minecraft.client.renderer.state.level.CameraRenderState
import org.joml.Vector4f
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/**
 * Prepares foliage uniforms and draws foliage in vanilla's opaque world pass,
 * after opaque terrain and before solid features.
 */
@Mixin(LevelRenderer::class)
@Suppress("UNUSED_PARAMETER")
abstract class LevelPassMixin {

    @Inject(
        method = ["executeSolid"],
        at = [
            At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher\$PreparedFrame;" +
                        "executeSolid(Lcom/mojang/renderpearl/api/commands/RenderPass;)V",
            ),
        ],
    )
    private fun drawFoliage(
        chunkSectionsToRender: ChunkSectionsToRender,
        featureFrame: FeatureRenderDispatcher.PreparedFrame,
        renderPass: RenderPass,
        callbackInfo: CallbackInfo,
    ) {
        GpuFoliageRenderer.drawIntoLevelPass(renderPass)
    }

    /**
     * Captures the final view after Iris applies camera bobbing and preserves
     * terrain fog for shadow rendering before any render pass opens.
     */
    @Inject(method = ["render"], at = [At("HEAD")])
    private fun writeFoliageUniforms(
        resourceAllocator: GraphicsResourceAllocator,
        renderOutline: Boolean,
        cameraState: CameraRenderState,
        terrainFog: GpuBufferSlice,
        fogColor: Vector4f,
        renderSky: Boolean,
        consistentDepthRequired: Boolean,
        callbackInfo: CallbackInfo,
    ) {
        GpuFoliageRenderer.writeLevelUniforms(
            cameraState.viewRotationMatrix,
            terrainFog,
        )
    }
}
