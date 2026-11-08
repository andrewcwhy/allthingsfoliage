package io.github.andrewcwhy.allthingsfoliage.passability

import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.util.Unit as MinecraftUnit
import net.minecraft.world.item.enchantment.Enchantment

object FoliageEnchantments {
    val leafWalker: ResourceKey<Enchantment> =
        ResourceKey.create(Registries.ENCHANTMENT, AllThingsFoliage.id("leaf_walker"))
    val leafWalkerEffect: DataComponentType<MinecraftUnit> =
        DataComponentType.builder<MinecraftUnit>().persistent(MinecraftUnit.CODEC).build()
}
