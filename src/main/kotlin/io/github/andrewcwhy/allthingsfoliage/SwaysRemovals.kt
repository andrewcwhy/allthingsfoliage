package io.github.andrewcwhy.allthingsfoliage

import com.github.razorplay01.sway.api.SwayAPI
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier

/**
 * Removes vanilla blocks that Sway incorrectly registers as interactive.
 *
 * These blocks retain their original behavior without foliage interactions in either renderer. The
 * GPU renderer leaves their geometry to the standard chunk renderer.
 *
 * This is a temporary compatibility workaround until Sway removes these registrations upstream. It
 * uses only Sway's public API and runs after [ModCompatRegistry] initialization, before model
 * baking.
 */
object SwayRemovals {

    private var initialized = false
    private var removedCount = 0

    /**
     * Removes unsupported vanilla blocks from Sway's interaction registry.
     *
     * Initialization is performed only once.
     */
    @JvmStatic
    fun initialize() {
        if (initialized) {
            return
        }

        initialized = true
        removeVanillaBlocks()

        AllThingsFoliage.logger.info("Sway removals: {} blocks taken out of Sway", removedCount)
    }

    /** Removes vanilla blocks that should not respond to foliage interactions. */
    private fun removeVanillaBlocks() {
        // Ground-cover, wall-attached, and ceiling-attached blocks.
        removeBlocks("minecraft:moss_carpet", "minecraft:glow_lichen", "minecraft:spore_blossom")

        // Rigid or structurally connected blocks that should not bend independently.
        removeBlocks(
                "minecraft:chorus_flower",
                "minecraft:chorus_plant",
                "minecraft:big_dripleaf",
                "minecraft:big_dripleaf_stem"
        )
    }

    /**
     * Removes interactive blocks from Sway's registry and clears their behavior pipelines.
     *
     * Skips invalid identifiers, missing blocks, and non-interactive blocks.
     *
     * @param blockIds Namespaced identifiers of the blocks to remove.
     */
    private fun removeBlocks(vararg blockIds: String) {
        for (blockId in blockIds) {
            val identifier = Identifier.tryParse(blockId)

            if (identifier == null) {
                continue
            }

            BuiltInRegistries.BLOCK.getOptional(identifier).ifPresent { block ->
                if (SwayAPI.isInteractive(block)) {
                    SwayAPI.getRegistry().remove(block)
                    SwayAPI.setBlockPipeline(block, emptyList())
                    removedCount++
                }
            }
        }
    }
}
