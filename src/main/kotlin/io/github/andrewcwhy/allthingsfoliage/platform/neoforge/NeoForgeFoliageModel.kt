package io.github.andrewcwhy.allthingsfoliage.platform.neoforge

//? neoforge {
/*
import io.github.andrewcwhy.allthingsfoliage.gpu.GpuFoliageSplit
import io.github.andrewcwhy.allthingsfoliage.gpu.compat.SnowRealMagicCompat
import net.minecraft.client.renderer.block.BlockAndTintGetter
import net.minecraft.client.renderer.block.dispatch.BlockStateModel
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart
import net.minecraft.client.resources.model.sprite.Material
import net.minecraft.core.BlockPos
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.state.BlockState

/^*
 * Excludes GPU-rendered foliage from chunk meshes while preserving the wrapped model's behavior.
 *
 * Sits inside Sway's wrapper so GPU meshing can obtain geometry before Sway deforms it.
 ^/
class NeoForgeFoliageModel(
    private val parent: BlockStateModel,
) : BlockStateModel {

    override fun collectParts(
        level: BlockAndTintGetter,
        pos: BlockPos,
        state: BlockState,
        random: RandomSource,
        parts: MutableList<BlockStateModelPart>,
    ) {
        if (
            GpuFoliageSplit.isFoliageAt(level, pos, state) &&
            GpuFoliageSplit.leaveToGpu(level, pos)
        ) {
            return
        }

        val firstAddedPart = parts.size
        parent.collectParts(level, pos, state, random, parts)

        // Preserve snowy variants before Sway reduces the model to its parts' quads.
        SnowRealMagicCompat.snowyParts(
            level,
            pos,
            state,
            parts.subList(firstAddedPart, parts.size),
        )
    }

    override fun collectParts(
        random: RandomSource,
        parts: MutableList<BlockStateModelPart>,
    ) {
        parent.collectParts(random, parts)
    }

    override fun createGeometryKey(
        level: BlockAndTintGetter,
        pos: BlockPos,
        state: BlockState,
        random: RandomSource,
    ): Any? {
        return parent.createGeometryKey(level, pos, state, random)
    }

    override fun hasMaterialFlag(flag: Int): Boolean {
        return parent.hasMaterialFlag(flag)
    }

    override fun hasMaterialFlag(
        level: BlockAndTintGetter,
        pos: BlockPos,
        state: BlockState,
        flag: Int,
    ): Boolean {
        return parent.hasMaterialFlag(level, pos, state, flag)
    }

    override fun materialFlags(): Int {
        return parent.materialFlags()
    }

    override fun materialFlags(
        level: BlockAndTintGetter,
        pos: BlockPos,
        state: BlockState,
    ): Int {
        return parent.materialFlags(level, pos, state)
    }

    override fun particleMaterial(): Material.Baked {
        return parent.particleMaterial()
    }

    override fun particleMaterial(
        level: BlockAndTintGetter,
        pos: BlockPos,
        state: BlockState,
    ): Material.Baked {
        return parent.particleMaterial(level, pos, state)
    }
}
*/
//?}
