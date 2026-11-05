package io.github.andrewcwhy.allthingsfoliage.mixin;

//? >=1.20.1 {

import io.github.andrewcwhy.allthingsfoliage.gpu.VulkanBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tells the GPU foliage renderer the moment VulkanMod's chunk mesh of a section is replaced by a newly built one, as
 * {@link SodiumMeshSwapMixin} does for Sodium.
 * <p>
 * {@code doSectionUpdate} is where VulkanMod uploads a finished build's geometry in place of the old, on the render
 * thread, one section at a time; it has kept its name and its one parameter from 0.5 to 0.6.8.
 * <p>
 * VulkanMod is optional, and not on the compile classpath: the target is named, and does nothing when it is absent.
 */
@Pseudo
@Mixin(
    targets = "net.vulkanmod.render.chunk.build.task.TaskDispatcher",
    remap = false
)
public abstract class VulkanMeshSwapMixin {

    @Inject(
        method = "doSectionUpdate",
        at = @At("RETURN"),
        require = 0,
        remap = false
    )
    private void mc2$foliageChunkMeshSwapped(
        @Coerce Object result,
        CallbackInfo ci
    ) {
        VulkanBridge.onSectionUpdated(result);
    }
}
//?}
