@file:JvmName("WalkNodeEvaluatorMixin")
@file:Mixin(WalkNodeEvaluator::class)

package io.github.andrewcwhy.allthingsfoliage.mixin.pathfinding

import io.github.andrewcwhy.allthingsfoliage.Config
import io.github.andrewcwhy.allthingsfoliage.duck.FoliageBlock
import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.pathfinder.PathType
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Inject(method = ["getPathTypeFromState"], at = [At("HEAD")], cancellable = true)
private fun useFoliagePathType(
    level: BlockGetter,
    pos: BlockPos,
    callback: CallbackInfoReturnable<PathType>,
) {
    if (!Config.passability().modifyPathFinding()) {
        return
    }
    val state = level.getBlockState(pos)
    val block = state.block as? FoliageBlock ?: return
    if (block.foliagePassable && state.fluidState.isEmpty) {
        callback.returnValue = PathType.OPEN
    }
}
