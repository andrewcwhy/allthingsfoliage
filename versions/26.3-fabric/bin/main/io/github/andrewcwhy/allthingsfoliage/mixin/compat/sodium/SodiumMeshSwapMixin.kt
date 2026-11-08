
package io.github.andrewcwhy.allthingsfoliage.mixin.compat.sodium

import io.github.andrewcwhy.allthingsfoliage.gpu.compat.SodiumBridge
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Pseudo
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Coerce
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/**
 * Notifies the GPU foliage renderer when Sodium replaces chunk meshes.
 *
 * Intercepts mesh uploads after completion to ensure the renderer observes
 * updated geometry. Sodium is optional, so the target is referenced by name.
 */
@Pseudo
@Mixin(
    targets = ["net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegionManager"],
    remap = false
)
abstract class SodiumMeshSwapMixin {

    /**
     * Synchronizes GPU foliage after Sodium uploads updated section meshes.
     */
    @Inject(
        method = ["uploadResults(Ljava/util/Collection;Lnet/caffeinemc/mods/sodium/client/render/chunk/UniformBufferManager;)V"],
        at = [At("RETURN")],
        require = 0,
        remap = false
    )
    private fun `allthingsfoliage$onChunkMeshesUploaded`(
        results: Collection<*>,
        @Coerce uniforms: Any?,
        callback: CallbackInfo
    ) {
        SodiumBridge.onResultsUploaded(results)
    }
}
