package io.github.andrewcwhy.allthingsfoliage

import com.github.razorplay01.sway.api.SwayAPI
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks

/**
 * Removes unsupported vanilla interactions through Sway's public API.
 *
 * Must run after [ModCompatRegistry] initialization and before model baking.
 */
object SwayRemovals {
    private var initialized = false
    private var removedCount = 0

    /** Applies the compatibility workaround once. */
    @JvmStatic
    fun initialize() {
        if (initialized) {
            return
        }

        initialized = true
        removeVanillaBlocks()

        AllThingsFoliage.logger.info(
            "Sway removals: {} blocks taken out of Sway",
            removedCount,
        )
    }

    /** Removes interactive registrations and clears their behavior pipelines. */
    private fun removeBlocks(vararg blocks: Block) {
        for (block in blocks) {
            if (!SwayAPI.isInteractive(block)) {
                continue
            }

            SwayAPI.getRegistry().remove(block)
            SwayAPI.setBlockPipeline(block, emptyList())
            removedCount++
        }
    }

    private fun removeVanillaBlocks() {
        // Ground-cover, wall-attached, and ceiling-attached blocks.
        removeBlocks(
            Blocks.GLOW_LICHEN,
            Blocks.MOSS_CARPET,
            Blocks.SPORE_BLOSSOM,
        )

        // Rigid or connected blocks that should not bend independently.
        removeBlocks(
            Blocks.BIG_DRIPLEAF,
            Blocks.BIG_DRIPLEAF_STEM,
            Blocks.CHORUS_FLOWER,
            Blocks.CHORUS_PLANT,
        )
    }
}
