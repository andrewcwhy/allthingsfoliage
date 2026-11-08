@file:JvmName("FlowingFluidMixin")
@file:Mixin(value = [FlowingFluid::class], priority = 1100)

package io.github.andrewcwhy.allthingsfoliage.mixin.passability

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import io.github.andrewcwhy.allthingsfoliage.passability.FoliagePassability
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.FlowingFluid
import org.spongepowered.asm.mixin.Mixin

@WrapMethod(method = ["canPassThroughWall"])
private fun preserveFluidCollision(
    direction: Direction,
    level: BlockGetter,
    sourcePos: BlockPos,
    sourceState: BlockState,
    targetPos: BlockPos,
    targetState: BlockState,
    original: Operation<Boolean>,
): Boolean {
    return FoliagePassability.withOriginalCollision {
        original.call(direction, level, sourcePos, sourceState, targetPos, targetState)
    }
}
