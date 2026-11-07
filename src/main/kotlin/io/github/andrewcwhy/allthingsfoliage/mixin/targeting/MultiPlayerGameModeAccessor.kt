package io.github.andrewcwhy.allthingsfoliage.mixin.targeting

import net.minecraft.client.multiplayer.MultiPlayerGameMode
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(MultiPlayerGameMode::class)
interface MultiPlayerGameModeAccessor {

    @Accessor("destroyDelay")
    fun setDestroyDelay(delay: Int)
}
