package io.github.andrewcwhy.allthingsfoliage.mixin.render

import io.github.andrewcwhy.allthingsfoliage.gpu.GpuFoliageRenderer
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/** Keeps GPU foliage geometry synchronized with client-side block updates. */
@Mixin(ClientLevel::class)
abstract class ClientLevelMixin {
    @Inject(
        method = ["setBlocksDirty"],
        at = [At("HEAD")],
    )
    private fun onBlockChanged(
        pos: BlockPos,
        oldState: BlockState,
        newState: BlockState,
        callback: CallbackInfo,
    ) {
        GpuFoliageRenderer.onBlockChanged(pos)
    }

    /** Includes block updates that bypass the state-change notification. */
    @Inject(
        method = ["sendBlockUpdated"],
        at = [At("HEAD")],
    )
    private fun onBlockUpdated(
        pos: BlockPos,
        oldState: BlockState,
        newState: BlockState,
        flags: Int,
        callback: CallbackInfo,
    ) {
        GpuFoliageRenderer.onBlockUpdated(pos, newState)
    }
}
