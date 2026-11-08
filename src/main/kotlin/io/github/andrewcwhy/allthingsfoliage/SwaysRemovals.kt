package io.github.andrewcwhy.allthingsfoliage

import com.github.razorplay01.sway.api.SwayAPI
import kotlin.jvm.optionals.getOrNull
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier

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
    private fun removeBlocks(vararg blockIds: String) {
        for (blockId in blockIds) {
            val identifier = Identifier.tryParse(blockId) ?: continue
            val block = BuiltInRegistries.BLOCK.getOptional(identifier).getOrNull()
                ?: continue

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
            "minecraft:glow_lichen",
            "minecraft:moss_carpet",
            "minecraft:spore_blossom",
        )

        // Rigid or connected blocks that should not bend independently.
        removeBlocks(
            "minecraft:big_dripleaf",
            "minecraft:big_dripleaf_stem",
            "minecraft:chorus_flower",
            "minecraft:chorus_plant",
        )
    }
}
