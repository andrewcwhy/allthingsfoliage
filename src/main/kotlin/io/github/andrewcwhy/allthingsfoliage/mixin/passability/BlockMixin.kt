package io.github.andrewcwhy.allthingsfoliage.mixin.passability

import io.github.andrewcwhy.allthingsfoliage.duck.FoliageBlock
import net.minecraft.world.level.block.Block
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique

@Mixin(Block::class)
abstract class BlockMixin : FoliageBlock {
    @field:Unique override var foliagePassable: Boolean = false
}
