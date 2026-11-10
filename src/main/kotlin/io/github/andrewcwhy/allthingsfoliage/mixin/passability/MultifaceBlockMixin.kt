@file:JvmName("MultifaceBlockMixin")
@file:Mixin(MultifaceBlock::class)

package io.github.andrewcwhy.allthingsfoliage.mixin.passability

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import io.github.andrewcwhy.allthingsfoliage.passability.FoliagePassability
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.MultifaceBlock
import net.minecraft.world.level.block.state.BlockState
import org.spongepowered.asm.mixin.Mixin

@WrapMethod(
    method = [
        "canAttachTo(" +
                "Lnet/minecraft/world/level/BlockGetter;" +
                "Lnet/minecraft/core/Direction;" +
                "Lnet/minecraft/core/BlockPos;" +
                "Lnet/minecraft/world/level/block/state/BlockState;" +
                ")Z",
    ],
)
private fun preserveAttachment(
    level: BlockGetter,
    direction: Direction,
    pos: BlockPos,
    state: BlockState,
    original: Operation<Boolean>,
): Boolean {
    if (!FoliagePassability.isPassable(state)) {
        return original.call(level, direction, pos, state)
    }

    return FoliagePassability.withOriginalCollision { original.call(level, direction, pos, state) }
}
