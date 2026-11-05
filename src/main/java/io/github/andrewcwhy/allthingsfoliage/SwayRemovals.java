package io.github.andrewcwhy.allthingsfoliage;

import com.github.razorplay01.sway.api.SwayAPI;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
 */ //?}

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
//? >=1.21.11 {
import net.minecraft.resources.Identifier;

/**
 * Blocks Sway registers that it should not, taken back out of its registry: they stay plain vanilla blocks, with no
 * interaction in either renderer, and the GPU renderer, which only draws what Sway bends, leaves them to the chunk.
 * <p>
 * Provisional: Sway's author has been asked to drop these himself, and this list goes once a Sway release does. Uses
 * Sway's own public API, never a patch of its code. Runs right after ModCompatRegistry, before any model is baked.
 */
public final class SwayRemovals {

    private static boolean initialized;
    private static int removed;

    private SwayRemovals() {}

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        vanilla();
        AllThingsFoliage.LOGGER.info(
            "Sway removals: {} blocks taken out of Sway",
            removed
        );
    }

    // ------------------------------------------------------------------
    // Vanilla
    // ------------------------------------------------------------------

    private static void vanilla() {
        // Flat on the ground, or clinging to walls and ceilings.
        remove(
            "minecraft:moss_carpet",
            "minecraft:glow_lichen",
            "minecraft:spore_blossom"
        );
        // Solid enough to be stood on, or built from blocks that are; each block bent on its own.
        remove(
            "minecraft:chorus_flower",
            "minecraft:chorus_plant",
            "minecraft:big_dripleaf",
            "minecraft:big_dripleaf_stem"
        );
    }

    // ------------------------------------------------------------------
    // Helper. Every lookup is optional.
    // ------------------------------------------------------------------

    private static void remove(String... ids) {
        for (String id : ids) {
            //? >=1.21.11 {
            Identifier key = Identifier.tryParse(id);
            //?} else {
            /*ResourceLocation key = ResourceLocation.tryParse(id);
             */ //?}
            if (key == null) {
                continue;
            }
            BuiltInRegistries.BLOCK.getOptional(key).ifPresent(block -> {
                if (SwayAPI.isInteractive(block)) {
                    SwayAPI.getRegistry().remove(block);
                    SwayAPI.setBlockPipeline(block, List.of());
                    removed++;
                }
            });
        }
    }
}
