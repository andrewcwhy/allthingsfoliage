package io.github.andrewcwhy.allthingsfoliage.mixin.compat.sodium

import io.github.andrewcwhy.allthingsfoliage.gpu.compat.SodiumBridge
import net.caffeinemc.mods.sodium.client.render.chunk.UniformBufferManager
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Pseudo
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/**
 * Reports completed Sodium section-mesh uploads to the GPU foliage renderer.
 *
 * The full descriptor selects the batch upload method, avoiding duplicate per-region reports.
 */
@Pseudo
@Mixin(
    targets = [
        "net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegionManager"
    ],
    remap = false,
)
abstract class SodiumMeshSwapMixin {
    @Inject(
        method = [
            "uploadResults(" +
                    "Ljava/util/Collection;" +
                    "Lnet/caffeinemc/mods/sodium/client/render/chunk/UniformBufferManager;" +
                    ")V"
        ],
        at = [At("RETURN")],
        require = 0,
        remap = false,
    )
    private fun onChunkMeshesUploaded(
        results: Collection<*>,
        uniforms: UniformBufferManager,
        callback: CallbackInfo,
    ) {
        SodiumBridge.onResultsUploaded(results)
    }
}
