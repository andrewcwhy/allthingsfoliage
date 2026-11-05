package io.github.andrewcwhy.allthingsfoliage;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
 */ //?}

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
//? >=1.21.11 {
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

/**
 * The blocks Sway does not bend that the GPU renderer still draws and waves: they sway in calm weather and move with
 * the weather's wind, and nothing pushes them.
 * <p>
 * Chosen by the author, not the player, and laid out by group rather than by mod: each group gathers vanilla's blocks
 * and every mod's together, so a group can be switched on or off as a whole. A block of a mod that is not installed, or
 * of a version that does not have it, is skipped. Tags get their own section, apart from the blocks named one by one.
 */
public final class WavingWhitelist {

    /**
     * The groups of the whitelist, each with how hard its blocks wave, and how.
     * <p>
     * A plant waves more the higher above its base, so small blocks that sit low, like petals, barely move at a plant's
     * strength and are given more. A field group waves as a whole instead: every vertex by where it is in the world, the
     * same wherever it is shared, so blocks that touch -- a tree's leaves -- move together and never part.
     */
    public enum Group {
        /** Ground plants and other small blocks: always on. Waves like a plant, twice as hard. */
        GENERAL(2.0F, false),
        /**
         * Leaves: the player can switch them off (waving leaves). Waves as a field, a fifth as far as a plant's tip. The
         * shaders' MC2_LEAVES_WAVE must match it: blocks hanging on the leaves move with them at that weight.
         */
        LEAVES(0.20F, true);

        /** How hard the group's blocks wave: a multiple of a plant's sway, or for a field, of its tip's. */
        private final float intensity;
        /** Whether the group's blocks wave as a field rather than from their base. */
        private final boolean field;

        Group(float intensity, boolean field) {
            this.intensity = intensity;
            this.field = field;
        }

        /** Whether the group's blocks wave now: leaves only while they do; see leavesWave(). */
        boolean enabled() {
            return this != LEAVES || leavesWave();
        }
    }

    private static final Map<Block, Group> BLOCKS = new IdentityHashMap<>();
    /** The blocks that follow the leaves; see followLeaves(). */
    private static final Set<Block> FOLLOWERS = Collections.newSetFromMap(
        new IdentityHashMap<>()
    );
    /**
     * Whether the game draws leaves see-through, as its Fancy leaves do, as the renderer last saw: Fast leaves are drawn
     * solid, and leaves the renderer waved would show their holes, so they are left to the game then -- unless the
     * renderer can draw them solid too; see {@link #opaqueLeavesDrawn}.
     */
    private static volatile boolean cutoutLeaves = true;
    /**
     * Whether the renderer can draw leaves solid, as the game's Fast leaves are, with what draws the chunks now: only where
     * it has a pipeline that keeps every pixel for each way it may draw them. As the renderer last saw.
     */
    private static volatile boolean opaqueLeavesDrawn;
    /** The blocks that rest on the leaves; see restOnLeaves(). */
    private static final Set<Block> RESTING = Collections.newSetFromMap(
        new IdentityHashMap<>()
    );
    /** Set once the lists are read; models are baked on several threads at once, so they are read under a lock. */
    private static volatile boolean resolved;

    private WavingWhitelist() {}

    /** Whether the GPU renderer waves this block now, although Sway does not bend it: listed, and its group on. */
    public static boolean contains(Block block) {
        if (!listed(block)) {
            return false;
        }
        return BLOCKS.get(block).enabled();
    }

    /**
     * Whether this block is on the whitelist, its group on or not. Asked as models are baked, when every block of every
     * mod is registered: a listed block's models are wrapped either way, so a group can be switched on or off in a world
     * without baking them again.
     */
    public static boolean listed(Block block) {
        if (!resolved) {
            resolve();
        }
        return BLOCKS.containsKey(block);
    }

    private static synchronized void resolve() {
        if (resolved) {
            return;
        }
        general();
        leaves();
        followLeaves();
        restOnLeaves();
        tags();
        Map<Group, Integer> counts = new EnumMap<>(Group.class);
        for (Group group : BLOCKS.values()) {
            counts.merge(group, 1, Integer::sum);
        }
        AllThingsFoliage.LOGGER.info(
            "Waving whitelist: {}, following the leaves: {}, resting on them: {}",
            counts,
            FOLLOWERS.size(),
            RESTING.size()
        );
        resolved = true;
    }

    /**
     * How hard this block waves: its group's intensity, the leaves' for a block resting on them (only drawn while it
     * does), or 1 for a block not on the whitelist.
     */
    public static float intensityOf(Block block) {
        if (restsOnLeaves(block)) {
            return Group.LEAVES.intensity;
        }
        Group group = contains(block) ? BLOCKS.get(block) : null;
        return group == null ? 1.0F : group.intensity;
    }

    /** Whether this block waves as a field, as a tree's leaves do, and what rests on them. */
    public static boolean wavesAsField(Block block) {
        if (restsOnLeaves(block)) {
            return true;
        }
        Group group = contains(block) ? BLOCKS.get(block) : null;
        return group != null && group.field;
    }

    /** Whether this block follows the leaves: waves as hard as they do while they wave, and not at all else. */
    public static boolean followsLeaves(Block block) {
        if (!resolved) {
            resolve();
        }
        return FOLLOWERS.contains(block);
    }

    /**
     * Whether this block rests on the leaves: lying on top of one while they wave, it is drawn and waved with them, as
     * a field; anywhere else it stays as it is.
     */
    public static boolean restsOnLeaves(Block block) {
        if (!resolved) {
            resolve();
        }
        return RESTING.contains(block);
    }

    /**
     * Whether the leaves wave now: the player has them on, and the game draws them see-through (Fancy leaves) or the
     * renderer can draw them solid as the game does (Fast leaves).
     */
    public static boolean leavesWave() {
        return (
            FoliageSettings.wavingLeaves() &&
            (cutoutLeaves || opaqueLeavesDrawn)
        );
    }

    /** Whether the leaves the renderer draws are drawn solid, as the game's Fast leaves are: every pixel kept. */
    public static boolean opaqueLeaves() {
        return !cutoutLeaves && opaqueLeavesDrawn;
    }

    /** Whether the leaves can wave with the game's leaves as they are now: Fancy, or Fast where the renderer draws them. */
    public static boolean leavesCanWave() {
        return gameCutoutLeaves() || opaqueLeavesDrawn;
    }

    /** Whether the game draws leaves see-through now, as its Fancy leaves do. Read on the render thread. */
    public static boolean gameCutoutLeaves() {
        //? >=1.21.11 {
        return net.minecraft.client.Minecraft.getInstance()
            .options.cutoutLeaves()
            .get();
        //?} else {
        /*// Sodium and Embeddium have leaves of their own, Default, Fancy or Fast, which decide over the graphics setting.
		Boolean sodium = sodiumFancyLeaves();
		return sodium != null ? sodium : net.minecraft.client.Minecraft.useFancyGraphics();
		*/ //?}
    }

    //? <1.21.11 {
    /*/^* Where Sodium keeps its leaves quality: its options, their quality settings, the leaves' and how it is asked. ^/
	private static boolean sodiumSought;
	private static java.lang.reflect.Method sodiumOptions;
	private static java.lang.reflect.Field sodiumQuality;
	private static java.lang.reflect.Field sodiumLeaves;
	private static java.lang.reflect.Method sodiumIsFancy;

	/^*
	 * Whether Sodium (or Embeddium, which keeps Sodium's names) draws leaves Fancy, its own setting read against the
	 * graphics setting as Sodium reads it; null without either, or where it is not where it is looked for.
	 ^/
	private static Boolean sodiumFancyLeaves() {
		if (!sodiumSought) {
			sodiumSought = true;
			for (String name : new String[] {"net.caffeinemc.mods.sodium.client.SodiumClientMod",
					"me.jellysquid.mods.sodium.client.SodiumClientMod"}) {
				try {
					Class<?> mod = Class.forName(name, false, WavingWhitelist.class.getClassLoader());
					java.lang.reflect.Method options = mod.getMethod("options");
					java.lang.reflect.Field quality = options.getReturnType().getField("quality");
					java.lang.reflect.Field leaves = quality.getType().getField("leavesQuality");
					java.lang.reflect.Method isFancy = leaves.getType().getMethod("isFancy",
							net.minecraft.client.GraphicsStatus.class);
					sodiumOptions = options;
					sodiumQuality = quality;
					sodiumLeaves = leaves;
					sodiumIsFancy = isFancy;
					break;
				} catch (ReflectiveOperationException | LinkageError e) {
					// Not this one.
				}
			}
		}
		if (sodiumIsFancy == null) {
			return null;
		}
		try {
			Object leaves = sodiumLeaves.get(sodiumQuality.get(sodiumOptions.invoke(null)));
			return (Boolean) sodiumIsFancy.invoke(leaves,
					net.minecraft.client.Minecraft.getInstance().options.graphicsMode().get());
		} catch (ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}
	*/ //?}

    /**
     * Follows the game's leaves, Fancy or Fast, and whether the renderer can draw them solid, once a frame; returns whether
     * either changed, and with it which leaves the renderer draws, or how.
     */
    public static boolean followGameLeaves(boolean canDrawOpaque) {
        boolean cutout = gameCutoutLeaves();
        if (cutout == cutoutLeaves && canDrawOpaque == opaqueLeavesDrawn) {
            return false;
        }
        cutoutLeaves = cutout;
        opaqueLeavesDrawn = canDrawOpaque;
        return true;
    }

    /** Whether this block is one of the leaves group's, whether they wave now or not. */
    public static boolean isLeaves(Block block) {
        return listed(block) && BLOCKS.get(block) == Group.LEAVES;
    }

    // ==================================================================
    // Group: GENERAL -- ground plants and other small blocks, always on
    // ==================================================================

    private static void general() {
        // Vanilla
        add(Group.GENERAL, "minecraft:wildflowers", "minecraft:pink_petals");
        // Biomes O' Plenty
        add(
            Group.GENERAL,
            "biomesoplenty:white_petals",
            "biomesoplenty:purple_wildflowers",
            "biomesoplenty:wildflower",
            "biomesoplenty:clover",
            "biomesoplenty:huge_clover_petal"
        );
        // No Mans Land
        add(Group.GENERAL, "nomansland:clover_patch");
    }

    // ==================================================================
    // Group: LEAVES -- a tree's leaves
    // ==================================================================

    private static void leaves() {
        // Vanilla and mods come in through #minecraft:leaves, in the tags below. Name a mod's leaves here only if it
        // does not add them to that tag.
        add(Group.LEAVES);
    }

    // ==================================================================
    // Follow the leaves -- blocks that hang on leaves, like vines
    // ==================================================================
    // Block by block: with leaves behind it a block moves exactly as they do, with anything else solid behind it it stands
    // still, and below both the strand hangs free, straight as a strip of cloth, in calm weather and in the weather's wind.
    // Only the part hanging free is pushed. See GpuFoliageRenderer's SwayAnchor.hangVine.

    private static void followLeaves() {
        // Vanilla
        follow("minecraft:vine");
        // Biomes O' Plenty
        follow("biomesoplenty:willow_vine");
    }

    // ==================================================================
    // Rest on the leaves -- thin layers lying on top of leaves, like snow or a carpet
    // ==================================================================
    // Only on top of a leaf, and only while waving leaves is on: they then move exactly as the leaf under them, all
    // over, so they never part from it. Anywhere else -- on the ground, on a roof -- they stay with the chunk mesh as
    // they always have. Only vanilla's own tag is read for the carpets, which a mod's carpets join to be dyed, walked on
    // and burnt as carpets are.

    private static void restOnLeaves() {
        // Vanilla
        rest(
            "minecraft:snow",
            "minecraft:moss_carpet",
            "minecraft:pale_moss_carpet",
            "biomesoplenty:glowing_moss_carpet"
        );
        restTag("minecraft:wool_carpets");
    }

    // ==================================================================
    // Tags -- every block a tag names, from Minecraft and from each mod
    // ==================================================================

    private static void tags() {
        tag(Group.LEAVES, "minecraft:leaves");
    }

    // ------------------------------------------------------------------
    // Helpers. Every lookup is optional; a block named twice keeps its first group.
    // ------------------------------------------------------------------

    private static void add(Group group, String... ids) {
        for (String id : ids) {
            //? >=1.21.11 {
            Identifier key = Identifier.tryParse(id);
            //?} else {
            /*ResourceLocation key = ResourceLocation.tryParse(id);
             */ //?}
            if (key != null) {
                BuiltInRegistries.BLOCK.getOptional(key).ifPresent(block ->
                    BLOCKS.putIfAbsent(block, group)
                );
            }
        }
    }

    private static void rest(String... ids) {
        for (String id : ids) {
            //? >=1.21.11 {
            Identifier key = Identifier.tryParse(id);
            //?} else {
            /*ResourceLocation key = ResourceLocation.tryParse(id);
             */ //?}
            if (key != null) {
                BuiltInRegistries.BLOCK.getOptional(key).ifPresent(
                    RESTING::add
                );
            }
        }
    }

    private static void follow(String... ids) {
        for (String id : ids) {
            //? >=1.21.11 {
            Identifier key = Identifier.tryParse(id);
            //?} else {
            /*ResourceLocation key = ResourceLocation.tryParse(id);
             */ //?}
            if (key != null) {
                BuiltInRegistries.BLOCK.getOptional(key).ifPresent(
                    FOLLOWERS::add
                );
            }
        }
    }

    /**
     * Adds every block the tags name. The world's tags only arrive with the world, long after the models are baked, so
     * the tag files are read where they come from instead: Minecraft's own and every mod's, each adding to the others.
     */
    private static void tag(Group group, String... tags) {
        for (String tag : tags) {
            List<String> ids = new ArrayList<>();
            readTag(tag, ids, new HashSet<>());
            add(group, ids.toArray(String[]::new));
        }
    }

    private static void restTag(String... tags) {
        for (String tag : tags) {
            List<String> ids = new ArrayList<>();
            readTag(tag, ids, new HashSet<>());
            rest(ids.toArray(String[]::new));
        }
    }

    /** Gathers the blocks a tag names, following the tags it names in turn. */
    private static void readTag(
        String tag,
        List<String> ids,
        Set<String> seen
    ) {
        if (!seen.add(tag)) {
            return;
        }
        int colon = tag.indexOf(':');
        String namespace = colon < 0 ? "minecraft" : tag.substring(0, colon);
        String path = colon < 0 ? tag : tag.substring(colon + 1);
        //? >=1.21.1 {
        String file = "data/" + namespace + "/tags/block/" + path + ".json";
        //?} else {
        /*String file = "data/" + namespace + "/tags/blocks/" + path + ".json";
         */ //?}
        try {
            for (byte[] source : AllThingsFoliage.xplat().readAll(file)) {
                try (
                    Reader reader = new InputStreamReader(
                        new java.io.ByteArrayInputStream(source),
                        StandardCharsets.UTF_8
                    )
                ) {
                    JsonElement values = JsonParser.parseReader(reader)
                        .getAsJsonObject()
                        .get("values");
                    if (values == null) {
                        continue;
                    }
                    for (JsonElement value : values.getAsJsonArray()) {
                        String entry = value.isJsonObject()
                            ? ((JsonObject) value).get("id").getAsString()
                            : value.getAsString();
                        if (entry.startsWith("#")) {
                            readTag(entry.substring(1), ids, seen);
                        } else {
                            ids.add(entry);
                        }
                    }
                }
            }
        } catch (Exception e) {
            AllThingsFoliage.LOGGER.warn(
                "Could not read the block tag {} for the waving whitelist",
                tag,
                e
            );
        }
    }
}
