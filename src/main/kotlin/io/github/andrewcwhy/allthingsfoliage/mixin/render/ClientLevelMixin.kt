
package io.github.andrewcwhy.allthingsfoliage.mixin.render

import io.github.andrewcwhy.allthingsfoliage.gpu.GpuFoliageRenderer
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/**
 * Tracks client-side block changes to keep GPU foliage geometry synchronized.
 *
 * Intercepts state changes through `setBlocksDirty`, which Sodium does not
 * override, and block entity updates through `sendBlockUpdated`.
 */
@Mixin(ClientLevel::class)
abstract class ClientLevelMixin {

    /**
     * Notifies the renderer when a block's state changes.
     */
    @Inject(
        method = ["setBlocksDirty"],
        at = [At("HEAD")]
    )
    private fun `allthingsfoliage$onBlockChanged`(
        pos: BlockPos,
        oldState: BlockState,
        newState: BlockState,
        callback: CallbackInfo
    ) {
        GpuFoliageRenderer.onBlockChanged(pos)
    }

    /**
     * Notifies the renderer when a block update reaches the client,
     * including updates that bypass `setBlocksDirty`.
     */
    @Inject(
        method = ["sendBlockUpdated"],
        at = [At("HEAD")]
    )
    private fun `allthingsfoliage$onBlockUpdated`(
        pos: BlockPos,
        oldState: BlockState,
        newState: BlockState,
        flags: Int,
        callback: CallbackInfo
    ) {
        GpuFoliageRenderer.onBlockUpdated(pos, newState)
    }
}
