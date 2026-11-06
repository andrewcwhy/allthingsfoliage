package io.github.andrewcwhy.allthingsfoliage.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.andrewcwhy.allthingsfoliage.gpu.GpuFoliageSplit;

import net.minecraft.client.renderer.chunk.SectionMesh;

import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;

import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tells the GPU foliage renderer the moment vanilla's chunk mesh of a section is replaced by a newly built one.
 * <p>
 * The hooked method puts a section's new mesh in place once every layer of it is uploaded, and from then on vanilla
 * draws the new mesh. A build that was superseded is cancelled before it gets there, so an older mesh never reports
 * after a newer build has begun. The method is {@code setCompiled} before 1.21.11 and {@code setSectionMesh} after.
 * Before 1.21.1 a chunk has no such method: its build sets the new mesh itself and tells the level renderer straight
 * after, so that call is the one heard.
 */

@Mixin(SectionRenderDispatcher.RenderSection.class)

public abstract class SectionMeshSwapMixin {

    @Inject(method = "setSectionMesh", at = @At("RETURN"))
    private void mc2$foliageChunkMeshSwapped(
        SectionMesh mesh,
        CallbackInfoReturnable<SectionMesh> cir
    ) {
        long node = (
            (SectionRenderDispatcher.RenderSection) (Object) this
        ).getSectionNode();
        mc2$reportSwap(
            SectionPos.x(node),
            SectionPos.y(node),
            SectionPos.z(node)
        );
    }

    @Unique
    private void mc2$reportSwap(int sectionX, int sectionY, int sectionZ) {
        // A build with no layers at all to upload finishes on its meshing thread, before 1.21.11 and from 26.1.2 on.
        if (RenderSystem.isOnRenderThread()) {
            GpuFoliageSplit.onChunkMeshSwapped(sectionX, sectionY, sectionZ);
        } else {
            GpuFoliageSplit.onChunkMeshSwappedElsewhere(
                sectionX,
                sectionY,
                sectionZ
            );
        }
    }
}
