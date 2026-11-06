package io.github.andrewcwhy.allthingsfoliage.mixin;

import io.github.andrewcwhy.allthingsfoliage.gpu.GpuFoliageRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Follows block changes on the client for the mod's foliage geometry.
 * <p>
 * {@code Level.setBlock} calls {@code setBlocksDirty} whenever a block's state actually changes,
 * whatever update flags were passed, and Sodium leaves it alone -- unlike the section-marking
 * methods further down the chain, which it overwrites.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    @Inject(method = "setBlocksDirty", at = @At("HEAD"))
    private void mc2$foliageBlockChanged(
        BlockPos pos,
        BlockState oldState,
        BlockState newState,
        CallbackInfo ci
    ) {
        GpuFoliageRenderer.onBlockChanged(pos);
    }

    /** A block entity's data reaching the client, or changing there, is told this way, and not through setBlocksDirty. */
    @Inject(method = "sendBlockUpdated", at = @At("HEAD"))
    private void mc2$foliageBlockUpdated(
        BlockPos pos,
        BlockState oldState,
        BlockState newState,
        int flags,
        CallbackInfo ci
    ) {
        GpuFoliageRenderer.onBlockUpdated(pos, newState);
    }
}
