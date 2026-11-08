package io.github.andrewcwhy.allthingsfoliage.mixin.blockgetter

//? neoforge {

import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Pseudo

/**
 * Declares level-view targets whose block-getter interface is added by [BlockGetterMixinPlugin].
 *
 * References Sodium's level slice by name to avoid a compile-time dependency.
 */
@Pseudo
@Mixin(
    targets = [
        "net.caffeinemc.mods.sodium.client.world.LevelSlice",
        "net.minecraft.client.renderer.chunk.RenderSectionRegion",
        "net.minecraft.world.level.Level",
    ],
    remap = false,
)
abstract class LevelViewBlockGetterMixin {}

//?}
