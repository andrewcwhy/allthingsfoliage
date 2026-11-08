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

    /**
     * Prevents an attack on a living entity from immediately breaking
     * non-colliding blocks when the entity is no longer targeted.
     */
    @Inject(
        method = ["startAttack"],
        at = [At("HEAD")]
    )
    private fun `allthingsfoliage$delayBlockBreak`(
        callback: CallbackInfoReturnable<Boolean>
    ) {
        if (!Config.targetAliveOnly()) {
            return
        }

        val gameMode = gameMode ?: return
        val entityHit = hitResult as? EntityHitResult ?: return

        if (entityHit.entity.isAlive) {
            (gameMode as MultiPlayerGameModeAccessor).setDestroyDelay(5)
        }
    }
}
