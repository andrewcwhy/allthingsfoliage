package io.github.andrewcwhy.allthingsfoliage.mixin.passability

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import io.github.andrewcwhy.allthingsfoliage.passability.FoliagePassability
import net.minecraft.core.BlockPos
import net.minecraft.util.RandomSource
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.LeavesBlock
import net.minecraft.world.level.block.state.BlockState
import org.spongepowered.asm.mixin.Mixin

@Mixin(LeavesBlock::class)
abstract class LeavesBlockMixin {
    @WrapMethod(method = ["animateTick"])
    private fun preserveLeafParticles(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        random: RandomSource,
        original: Operation<Void?>,
    ) {
        FoliagePassability.withOriginalCollision { original.call(state, level, pos, random) }
    }
}
