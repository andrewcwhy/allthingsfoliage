package io.github.andrewcwhy.allthingsfoliage.mixin;

import io.github.andrewcwhy.allthingsfoliage.gpu.SodiumBridge;
import java.util.Collection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tells the GPU foliage renderer the moment Sodium's chunk mesh of a section is replaced by a newly built one.
 * <p>
 * {@code uploadResults} is where Sodium puts a finished build's geometry in place of the old, on the render thread.
 * Sodium never builds one section twice at a time and drops a result older than what is already uploaded, so the
 * results it uploads for a section always come in the order they were built.
 * <p>
 * The method is named with its parameters: a private overload of the same name uploads each region's share, and
 * would report every section twice. Its parameters differ between Sodium's release lines, and before Sodium 0.6 --
 * still under its old package on 1.20.1 -- it is called {@code uploadMeshes}.
 * <p>
 * Sodium is optional, and not on the compile classpath: the target is named, and does nothing when it is absent.
 */
@Pseudo

@Mixin(
    targets = "net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegionManager",
    remap = false
)

public abstract class SodiumMeshSwapMixin {

    @Inject(
        method = "uploadResults(Ljava/util/Collection;Lnet/caffeinemc/mods/sodium/client/render/chunk/UniformBufferManager;)V",
        at = @At("RETURN"),
        require = 0,
        remap = false
    )
    private void mc2$foliageChunkMeshesSwapped(
        Collection<?> results,
        @Coerce Object uniforms,
        CallbackInfo ci
    ) {
        SodiumBridge.onResultsUploaded(results);
    }
    
}
