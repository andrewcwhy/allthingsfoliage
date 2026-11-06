package io.github.andrewcwhy.allthingsfoliage.mixin.targeting;

import io.github.andrewcwhy.allthingsfoliage.FoliageSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
abstract class MinecraftAttackMixin {

    @Shadow
    public HitResult hitResult;

    @Shadow
    public MultiPlayerGameMode gameMode;

    @Inject(method = "startAttack", at = @At("HEAD"))
    private void allthingsfoliage$delayBlockBreak(CallbackInfoReturnable<Boolean> cir) {
        if (!FoliageSettings.targetAliveOnly() || this.gameMode == null) return;
        // Prevents breaking a block without collision (like tall grass) when hitting an entity through it and the
        // entity dies or goes out of range. Vanilla doesn't update the block breaking delay, so the block would be
        // broken on the next tick, once it becomes the new pick result.
        if (this.hitResult instanceof EntityHitResult entityHit && entityHit.getEntity().isAlive()) {
            ((MultiPlayerGameModeAccessor) this.gameMode).allthingsfoliage$setDestroyDelay(5);
        }
    }
}
