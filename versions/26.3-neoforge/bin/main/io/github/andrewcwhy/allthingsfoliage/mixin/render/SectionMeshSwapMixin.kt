package io.github.andrewcwhy.allthingsfoliage.mixin.render

import com.mojang.blaze3d.systems.RenderSystem
import io.github.andrewcwhy.allthingsfoliage.gpu.GpuFoliageSplit
import net.minecraft.client.renderer.chunk.SectionMesh
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher
import net.minecraft.core.SectionPos
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

/**
 * Notifies the GPU foliage renderer when vanilla installs a section's new mesh.
 */
@Mixin(SectionRenderDispatcher.RenderSection::class)
abstract class SectionMeshSwapMixin {

    @Inject(method = ["setSectionMesh"], at = [At("RETURN")])
    @Suppress("UNUSED_PARAMETER")
    private fun onSectionMeshSwapped(
        mesh: SectionMesh,
        callbackInfo: CallbackInfoReturnable<SectionMesh>,
    ) {
        val sectionNode = (this as SectionRenderDispatcher.RenderSection).sectionNode
        val sectionX = SectionPos.x(sectionNode)
        val sectionY = SectionPos.y(sectionNode)
        val sectionZ = SectionPos.z(sectionNode)

        // Empty meshes can finish on a meshing thread.
        if (RenderSystem.isOnRenderThread()) {
            GpuFoliageSplit.onChunkMeshSwapped(sectionX, sectionY, sectionZ)
        } else {
            GpuFoliageSplit.onChunkMeshSwappedElsewhere(sectionX, sectionY, sectionZ)
        }
    }
}
