package io.github.andrewcwhy.allthingsfoliage.mixin.targeting;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeAccessor {

    @Accessor("destroyDelay")
    void allthingsfoliage$setDestroyDelay(int delay);
}
