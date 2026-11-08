package io.github.andrewcwhy.allthingsfoliage.passability

import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage
import net.minecraft.core.registries.Registries
import net.minecraft.tags.TagKey
import net.minecraft.world.level.block.Block

object FoliageTags {
    val passables: TagKey<Block> = TagKey.create(Registries.BLOCK, AllThingsFoliage.id("passables"))
}
