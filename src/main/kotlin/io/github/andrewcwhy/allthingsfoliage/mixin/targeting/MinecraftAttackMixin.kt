package io.github.andrewcwhy.allthingsfoliage.mixin.targeting

import io.github.andrewcwhy.allthingsfoliage.Config
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.MultiPlayerGameMode
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(Minecraft::class)
abstract class MinecraftAttackMixin {
    @Shadow
    @JvmField
    var gameMode: MultiPlayerGameMode? = null

    @Shadow
    @JvmField
    var hitResult: HitResult? = null

    /** Delays block breaking after targeting a living entity to avoid breaking foliage behind it. */
    @Inject(
        method = ["startAttack"],
        at = [At("HEAD")],
    )
    private fun delayBlockBreak(callback: CallbackInfoReturnable<Boolean>) {
        if (!Config.targetAliveOnly) {
            return
        }

        val gameMode = gameMode ?: return
        val entityHit = hitResult as? EntityHitResult ?: return

        if (entityHit.entity.isAlive) {
            (gameMode as MultiPlayerGameModeAccessor).setDestroyDelay(5)
        }
    }
}
