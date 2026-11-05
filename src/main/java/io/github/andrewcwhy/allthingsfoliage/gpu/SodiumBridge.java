package io.github.andrewcwhy.allthingsfoliage.gpu;

//? >=1.20.1 {

import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collection;

/**
 * What the mod needs from Sodium when Sodium owns the terrain: whether a box survived its occlusion
 * culling, and a way to ask it to build a chunk again.
 * <p>
 * Sodium has no public API for either. They are public methods on an internal class, but they have kept
 * the same signatures from Sodium 0.5 through 0.9, and Sodium relies on them itself -- only the package moved,
 * from {@code me.jellysquid} to {@code net.caffeinemc} with Sodium 0.6. They are reached by
 * reflection so the mod neither compiles against Sodium nor needs it; without them, culling falls back to
 * the camera frustum alone and rebuilds fall back to vanilla's own route.
 * <p>
 * A section Sodium holds no geometry for is reported visible, so foliage standing on its own is never
 * hidden by mistake -- it only misses out on being culled.
 */
// Public for the mixin that reports Sodium's uploads.
public final class SodiumBridge {

    //? >=1.21.1 {
    private static final String SODIUM = "net.caffeinemc.mods.sodium.client.";
    //?} else {
    /*private static final String SODIUM = "me.jellysquid.mods.sodium.client.";
     */ //?}
    private static final String RENDERER =
        SODIUM + "render.SodiumWorldRenderer";
    /** On Forge it is Embeddium, which keeps Sodium 0.5's classes under a mod id of its own. */
    //? forge {
    /*private static final String MOD_ID = "embeddium";
     */ //?} else {
    private static final String MOD_ID = "sodium";
    //?}

    private static final MethodHandle INSTANCE;
    private static final MethodHandle IS_BOX_VISIBLE;
    private static final MethodHandle SCHEDULE_REBUILD;

    static {
        MethodHandle instance = null;
        MethodHandle isBoxVisible = null;
        MethodHandle scheduleRebuild = null;
        if (AllThingsFoliage.xplat().isModLoaded(MOD_ID)) {
            try {
                Class<?> renderer = Class.forName(RENDERER);
                MethodHandles.Lookup lookup = MethodHandles.publicLookup();
                instance = lookup
                    .findStatic(
                        renderer,
                        "instanceNullable",
                        MethodType.methodType(renderer)
                    )
                    .asType(MethodType.methodType(Object.class));
                isBoxVisible = lookup
                    .findVirtual(
                        renderer,
                        "isBoxVisible",
                        MethodType.methodType(
                            boolean.class,
                            double.class,
                            double.class,
                            double.class,
                            double.class,
                            double.class,
                            double.class
                        )
                    )
                    .asType(
                        MethodType.methodType(
                            boolean.class,
                            Object.class,
                            double.class,
                            double.class,
                            double.class,
                            double.class,
                            double.class,
                            double.class
                        )
                    );
                scheduleRebuild = lookup
                    .findVirtual(
                        renderer,
                        "scheduleRebuildForChunk",
                        MethodType.methodType(
                            void.class,
                            int.class,
                            int.class,
                            int.class,
                            boolean.class
                        )
                    )
                    .asType(
                        MethodType.methodType(
                            void.class,
                            Object.class,
                            int.class,
                            int.class,
                            int.class,
                            boolean.class
                        )
                    );
            } catch (ReflectiveOperationException e) {
                AllThingsFoliage.LOGGER.warn(
                    "Sodium is installed but could not be reached; foliage will be culled by " +
                        "the camera frustum only, and near foliage may keep being drawn by the chunk mesh " +
                        "until something else rebuilds it",
                    e
                );
                instance = null;
                isBoxVisible = null;
                scheduleRebuild = null;
            }
        }
        INSTANCE = instance;
        IS_BOX_VISIBLE = isBoxVisible;
        SCHEDULE_REBUILD = scheduleRebuild;
    }

    private SodiumBridge() {}

    //? fabric && <1.21.1 {
    /*private static boolean warnedWithoutIndium;

	/^*
	 * Whether the chunk mesher can be told to leave foliage out. Sodium 0.5 meshes a block without Fabric's rendering
	 * API unless Indium is installed to provide it, and that API is the one call a foliage model can withhold its
	 * geometry through. Without it the chunk mesh keeps all of the foliage, and the GPU renderer drawing it as well
	 * would show every plant twice -- so it stays out of the way, as if switched off.
	 ^/
	static boolean canSplitChunkMeshes() {
		if (!AllThingsFoliage.xplat().isModLoaded("sodium") || AllThingsFoliage.xplat().isModLoaded("indium")) {
			return true;
		}
		if (!warnedWithoutIndium) {
			warnedWithoutIndium = true;
			AllThingsFoliage.LOGGER.warn("Sodium is installed without Indium, so the chunk mesh cannot leave foliage to the "
					+ "GPU renderer; foliage is drawn by the chunk mesh alone. Install Indium to use the GPU renderer.");
		}
		return false;
	}
	*/ //?} else {
    static boolean canSplitChunkMeshes() {
        return true;
    }

    //?}

    /** Whether Sodium draws the chunks, and so decides what the terrain around the foliage is drawn with. */
    /**
     * Sodium only moves on the animated textures something marks as on screen each frame, which its chunk meshes do for
     * theirs. Foliage the chunk mesh left out is drawn by the mod, so the mod marks its textures instead: without it a
     * firefly bush's glow stood still unless another one was in view further out, drawn by Sodium.
     * <p>
     * This is Sodium's public API, the same from 0.5 to 0.9; Embeddium has only the class it grew out of.
     */
    private static final Object SPRITE_UTIL;
    private static final MethodHandle MARK_SPRITE_ACTIVE;
    private static final MethodHandle SPRITE_HAS_ANIMATION;

    static {
        Object util = null;
        MethodHandle mark = null;
        MethodHandle animates = null;
        if (AllThingsFoliage.xplat().isModLoaded(MOD_ID)) {
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            try {
                Class<?> api = Class.forName(
                    "net.caffeinemc.mods.sodium.api.texture.SpriteUtil"
                );
                util = api.getField("INSTANCE").get(null);
                mark = lookup
                    .findVirtual(
                        api,
                        "markSpriteActive",
                        MethodType.methodType(
                            void.class,
                            net.minecraft.client.renderer.texture.TextureAtlasSprite.class
                        )
                    )
                    .asType(
                        MethodType.methodType(
                            void.class,
                            Object.class,
                            net.minecraft.client.renderer.texture.TextureAtlasSprite.class
                        )
                    );
                animates = lookup
                    .findVirtual(
                        api,
                        "hasAnimation",
                        MethodType.methodType(
                            boolean.class,
                            net.minecraft.client.renderer.texture.TextureAtlasSprite.class
                        )
                    )
                    .asType(
                        MethodType.methodType(
                            boolean.class,
                            Object.class,
                            net.minecraft.client.renderer.texture.TextureAtlasSprite.class
                        )
                    );
            } catch (ReflectiveOperationException | LinkageError e) {
                util = null;
                try {
                    Class<?> old = Class.forName(
                        "me.jellysquid.mods.sodium.client.render.texture.SpriteUtil"
                    );
                    mark = lookup.findStatic(
                        old,
                        "markSpriteActive",
                        MethodType.methodType(
                            void.class,
                            net.minecraft.client.renderer.texture.TextureAtlasSprite.class
                        )
                    );
                    animates = lookup.findStatic(
                        old,
                        "hasAnimation",
                        MethodType.methodType(
                            boolean.class,
                            net.minecraft.client.renderer.texture.TextureAtlasSprite.class
                        )
                    );
                } catch (ReflectiveOperationException | LinkageError missing) {
                    mark = null;
                    animates = null;
                    AllThingsFoliage.LOGGER.warn(
                        "Sodium is installed, but not a version whose animated textures the GPU foliage " +
                            "renderer can keep moving; animated foliage textures may stand still near the player"
                    );
                }
            }
        }
        SPRITE_UTIL = util;
        MARK_SPRITE_ACTIVE = mark;
        SPRITE_HAS_ANIMATION = animates;
    }

    /** Whether the textures of what the renderer draws have to be marked for Sodium to animate them. */
    static boolean tracksSprites() {
        return MARK_SPRITE_ACTIVE != null && SPRITE_HAS_ANIMATION != null;
    }

    static boolean animates(
        net.minecraft.client.renderer.texture.TextureAtlasSprite sprite
    ) {
        try {
            return SPRITE_UTIL != null
                ? (boolean) SPRITE_HAS_ANIMATION.invokeExact(
                      SPRITE_UTIL,
                      sprite
                  )
                : (boolean) SPRITE_HAS_ANIMATION.invokeExact(sprite);
        } catch (Throwable e) {
            return false;
        }
    }

    /** Marks the animated textures of a section drawn this frame as on screen. */
    static void markActive(
        net.minecraft.client.renderer.texture.TextureAtlasSprite[] sprites
    ) {
        try {
            for (net.minecraft.client.renderer.texture.TextureAtlasSprite sprite : sprites) {
                if (SPRITE_UTIL != null) {
                    MARK_SPRITE_ACTIVE.invokeExact(SPRITE_UTIL, sprite);
                } else {
                    MARK_SPRITE_ACTIVE.invokeExact(sprite);
                }
            }
        } catch (Throwable e) {
            // Left as it is: the texture stands still, which is where this started.
        }
    }

    static boolean drawsChunks() {
        return AllThingsFoliage.xplat().isModLoaded(MOD_ID);
    }

    /** Sodium's world renderer, or null when there is none to ask. */
    static Object renderer() {
        if (INSTANCE == null) {
            return null;
        }
        try {
            return (Object) INSTANCE.invokeExact();
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * Asks Sodium to build a section again, which vanilla's own {@code setSectionDirty} does not do while
     * Sodium owns the terrain. Without this a section keeps whatever the mesher decided about its foliage
     * until something else happens to rebuild it -- a block placed, or the light changing.
     */
    static void scheduleRebuild(int sectionX, int sectionY, int sectionZ) {
        Object renderer = renderer();
        if (renderer == null || SCHEDULE_REBUILD == null) {
            return;
        }
        try {
            SCHEDULE_REBUILD.invokeExact(
                renderer,
                sectionX,
                sectionY,
                sectionZ,
                false
            );
        } catch (Throwable e) {
            // Nothing to do about it: vanilla's route was asked as well, and one of the two is enough.
        }
    }

    /**
     * Reports each section whose new chunk mesh Sodium has just uploaded, which is the moment it replaces the old one
     * on screen. Only meshing results count: a translucency sort uploads too, but keeps the geometry it had.
     */
    public static void onResultsUploaded(Collection<?> results) {
        if (MeshSwaps.BUILD_OUTPUT == null) {
            return;
        }
        try {
            for (Object output : results) {
                if (!MeshSwaps.BUILD_OUTPUT.isInstance(output)) {
                    continue;
                }
                //? forge {
                /*// Embeddium sends a translucency sort through the same upload, as an output with no geometry in it.
				if (MeshSwaps.INDEX_ONLY != null && (boolean) MeshSwaps.INDEX_ONLY.invokeExact(output)) {
					continue;
				}
				*/ //?}
                Object section = (Object) MeshSwaps.SECTION.invokeExact(output);
                GpuFoliageSplit.onChunkMeshSwapped(
                    (int) MeshSwaps.CHUNK_X.invokeExact(section),
                    (int) MeshSwaps.CHUNK_Y.invokeExact(section),
                    (int) MeshSwaps.CHUNK_Z.invokeExact(section)
                );
            }
        } catch (Throwable e) {
            // Nothing to do about it: a section not reported is let go of by GpuFoliageSplit's safety net.
        }
    }

    /**
     * Sodium's build results, resolved on first use: the output of a meshing build, the section it belongs to, and
     * where that section is. Public members of internal classes, under the same names through each release line.
     */
    private static final class MeshSwaps {

        /**
         * The class declaring a build output's section: a common base from Sodium 0.6, the meshing output itself before.
         */
        //? >=1.21.1 {
        private static final String SECTION_OWNER =
            SODIUM + "render.chunk.compile.BuilderTaskOutput";
        //?} else {
        /*private static final String SECTION_OWNER = SODIUM + "render.chunk.compile.ChunkBuildOutput";
         */ //?}
        /** The field holding a build output's section: {@code render} through Sodium 0.8, {@code section} from 0.9. */
        //? >=26.1.2 {
        private static final String SECTION_FIELD = "section";
        //?} else {
        /*private static final String SECTION_FIELD = "render";
         */ //?}

        static final Class<?> BUILD_OUTPUT;
        static final MethodHandle SECTION;
        static final MethodHandle CHUNK_X;
        static final MethodHandle CHUNK_Y;
        static final MethodHandle CHUNK_Z;

        //? forge {
        /*static final MethodHandle INDEX_ONLY = indexOnly();

		private static MethodHandle indexOnly() {
			try {
				return MethodHandles.publicLookup().findVirtual(Class.forName(SODIUM + "render.chunk.compile.ChunkBuildOutput"),
						"isIndexOnlyUpload", MethodType.methodType(boolean.class))
						.asType(MethodType.methodType(boolean.class, Object.class));
			} catch (ReflectiveOperationException e) {
				return null;
			}
		}
		*/ //?}

        static {
            Class<?> buildOutput = null;
            MethodHandle section = null;
            MethodHandle chunkX = null;
            MethodHandle chunkY = null;
            MethodHandle chunkZ = null;
            try {
                Class<?> taskOutput = Class.forName(SECTION_OWNER);
                Class<?> renderSection = Class.forName(
                    SODIUM + "render.chunk.RenderSection"
                );
                MethodHandles.Lookup lookup = MethodHandles.publicLookup();
                section = lookup
                    .findGetter(taskOutput, SECTION_FIELD, renderSection)
                    .asType(MethodType.methodType(Object.class, Object.class));
                MethodType coordinate = MethodType.methodType(int.class);
                MethodType erased = MethodType.methodType(
                    int.class,
                    Object.class
                );
                chunkX = lookup
                    .findVirtual(renderSection, "getChunkX", coordinate)
                    .asType(erased);
                chunkY = lookup
                    .findVirtual(renderSection, "getChunkY", coordinate)
                    .asType(erased);
                chunkZ = lookup
                    .findVirtual(renderSection, "getChunkZ", coordinate)
                    .asType(erased);
                buildOutput = Class.forName(
                    SODIUM + "render.chunk.compile.ChunkBuildOutput"
                );
            } catch (ReflectiveOperationException e) {
                AllThingsFoliage.LOGGER.warn(
                    "Sodium's chunk build results could not be read; foliage handed back to the " +
                        "chunk mesh may blink out for a moment",
                    e
                );
                buildOutput = null;
            }
            BUILD_OUTPUT = buildOutput;
            SECTION = section;
            CHUNK_X = chunkX;
            CHUNK_Y = chunkY;
            CHUNK_Z = chunkZ;
        }
    }

    /** Whether the box is visible to Sodium. Any failure answers yes, so nothing is hidden wrongly. */
    static boolean isVisible(
        Object renderer,
        double minX,
        double minY,
        double minZ,
        double maxX,
        double maxY,
        double maxZ
    ) {
        try {
            return (boolean) IS_BOX_VISIBLE.invokeExact(
                renderer,
                minX,
                minY,
                minZ,
                maxX,
                maxY,
                maxZ
            );
        } catch (Throwable e) {
            return true;
        }
    }
}
//?}
