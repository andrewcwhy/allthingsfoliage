package io.github.andrewcwhy.allthingsfoliage.gpu.compat

import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.DoublePlantBlock
import net.minecraft.world.level.block.SlabBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf
import net.minecraft.world.level.block.state.properties.SlabType

/**
 * Applies Terrain Slabs' plant displacement when GPU meshing bypasses its model wrapper.
 */
object TerrainSlabsCompat {

    private const val HELPER_CLASS = "net.countered.terrainslabs.util.OnTopHelper"
    private const val MOD_ID = "terrain_slabs"
    private const val SLAB_DROP = -0.5f
    private const val VALID_ON_TOP_METHOD = "terrain_slabs\$isStateValidOnTop"

    private val loweringRule: MethodHandle? = findLoweringRule()

    private fun findLoweringRule(): MethodHandle? {
        if (!AllThingsFoliage.platform.isModLoaded(MOD_ID)) {
            return null
        }

        try {
            val helperClass = Class.forName(
                HELPER_CLASS,
                false,
                TerrainSlabsCompat::class.java.classLoader,
            )

            return MethodHandles.publicLookup().findStatic(
                helperClass,
                VALID_ON_TOP_METHOD,
                MethodType.methodType(Boolean::class.java, BlockState::class.java),
            )
        } catch (exception: Throwable) {
            if (
                exception !is ReflectiveOperationException &&
                exception !is LinkageError
            ) {
                throw exception
            }

            AllThingsFoliage.logger.warn(
                "Terrain Slabs is installed, but not a version the GPU renderer knows: " +
                        "plants on its slabs may float while the renderer draws them",
                exception,
            )

            return null
        }
    }

    /**
     * Returns the plant's vertical displacement, following the lower half for tall plants.
     */
    @JvmStatic
    fun offsetY(
        level: BlockGetter,
        pos: BlockPos,
        state: BlockState,
    ): Float {
        val rule = loweringRule ?: return 0.0f

        val isUpperHalf =
            state.block is DoublePlantBlock &&
                    state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER

        val ground = level.getBlockState(pos.below(if (isUpperHalf) 2 else 1))

        if (
            ground.block !is SlabBlock ||
            ground.getValue(SlabBlock.TYPE) != SlabType.BOTTOM
        ) {
            return 0.0f
        }

        try {
            if (rule.invoke(state) as Boolean) {
                return SLAB_DROP
            }

            return 0.0f
        } catch (_: Throwable) {
            return 0.0f
        }
    }
}
