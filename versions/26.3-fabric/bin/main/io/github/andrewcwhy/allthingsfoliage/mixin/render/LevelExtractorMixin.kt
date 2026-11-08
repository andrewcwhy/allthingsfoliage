package io.github.andrewcwhy.allthingsfoliage.mixin.render

import io.github.andrewcwhy.allthingsfoliage.gpu.GpuFoliageRenderer
import net.minecraft.client.renderer.extract.LevelExtractor
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/**
 * Keeps foliage geometry synchronized with lighting, resource reloads,
 * world changes, and rendering options.
 *
 * Block changes are tracked separately because Sodium replaces vanilla's
 * section invalidation paths.
 */
@Mixin(LevelExtractor::class)
@Suppress("UNUSED_PARAMETER")
abstract class LevelExtractorMixin {

    @Inject(method = ["allChanged"], at = [At("TAIL")])
    private fun onAllChanged(callbackInfo: CallbackInfo) {
        GpuFoliageRenderer.discardAll()
    }

    @Inject(method = ["setSectionDirty(III)V"], at = [At("TAIL")])
    private fun onLightChanged(
        sectionX: Int,
        sectionY: Int,
        sectionZ: Int,
        callbackInfo: CallbackInfo,
    ) {
        GpuFoliageRenderer.onLightChanged(sectionX, sectionY, sectionZ)
    }

    /**
     * Schedules a foliage rebuild, including requests that bypass the public overload.
     * The public overload's light notification subsequently prioritizes the rebuild.
     */
    @Inject(
        method = ["setSectionDirty(IIIZ)V"],
        at = [At("HEAD")],
        require = 0,
    )
    private fun onSectionRebuildRequested(
        sectionX: Int,
        sectionY: Int,
        sectionZ: Int,
        onMainThread: Boolean,
        callbackInfo: CallbackInfo,
    ) {
        GpuFoliageRenderer.onSectionAsked(sectionX, sectionY, sectionZ)
    }
}
