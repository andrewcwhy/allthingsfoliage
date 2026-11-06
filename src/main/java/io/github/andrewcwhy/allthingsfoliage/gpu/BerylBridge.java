package io.github.andrewcwhy.allthingsfoliage.gpu;

import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lights the foliage as Beryl lights the terrain while Beryl's shaders are on, and draws it into Beryl's shadow map.
 * <p>
 * Beryl is a shader renderer for VulkanMod with one set of shaders of its own, which work the light, the shadows and
 * the fog out themselves. It draws the game's entities and particles through them by putting one of its Vulkan
 * pipelines under the game's render pipeline for as long as its shaders are on; the mod's pipeline is not among those,
 * so the foliage was drawn by the mod's own shader, in none of Beryl's light. The mod does for its pipeline what Beryl
 * does for the game's: it builds a Vulkan pipeline from Beryl's terrain shaders, the vertex one with the sway spliced in
 * ({@link BerylFoliageShader}), and puts it under its own while Beryl draws the terrain, and takes it away again
 * otherwise.
 * <p>
 * Beryl draws its shadow map from its own chunk meshes, which the foliage was left out of, so a plant cast no shadow.
 * While Beryl draws its shadow map, the pipeline under the mod's is one built the same way from the shaders Beryl draws
 * the terrain into it with, and the foliage is drawn with it once Beryl's terrain is in.
 * <p>
 * The pipelines are built as Beryl builds its own. The blocks Beryl's shaders share are filled by VulkanMod from
 * values it looks up by name, as they are for the terrain; the four the sway reads are bound from what the mod hands the
 * render pass, which VulkanMod finds by the name it gives a block declared by its size alone: {@code "UBO "} and its
 * binding. VulkanMod lays a pipeline's bindings out by their number, so those four take the one Beryl's block of section
 * offsets leaves free and the three after Beryl's last. The textures are found by name too, and the shadow map read is
 * the one Beryl has bound by then.
 * <p>
 * Neither mod is compiled against: everything is reached by name, on Beryl 0.2.1 and VulkanMod 0.6.8. Whatever is not
 * found, or does not compile, leaves the foliage on the mod's own shader and out of the shadow map, as it is without
 * this class.
 */
final class BerylBridge {

    /** The name VulkanMod finds the region's transform by in what the render pass was handed. */
    static final String REGION = "UBO " + BerylFoliageShader.REGION_BINDING;

    private static final boolean LOADED =
        AllThingsFoliage.xplat().isModLoaded("beryl") &&
        AllThingsFoliage.xplat().isModLoaded("vulkanmod");
    /** Vertex shader stage alone, as Vulkan numbers it. */
    private static final int VERTEX_STAGE = 1;
    /** The name Beryl's terrain pipeline reads the lightmap by, and the one its shadow pipeline does. */
    private static final String LIT_LIGHT_TEXTURE = "LightTexture";
    private static final String SHADOW_LIGHT_TEXTURE = "Sampler2";

    /** What is under each of the mod's render pipelines at the moment, for those with one of these under them. */
    private static final Map<Object, Object> UNDER = new IdentityHashMap<>();
    private static Object litPipeline;
    private static Object shadowPipeline;
    /** Which of Beryl's options the pipelines were built for; they change what its shaders declare. */
    private static String builtFor;
    /** The first of the three bindings after Beryl's own in each pipeline, where it reads the sway's other blocks. */
    private static int litFreeBinding;
    private static int shadowFreeBinding;
    /** The same, and the lightmap's name, for the pipeline put under the mod's last. */
    private static int freeBinding;
    private static String lightTexture = LIT_LIGHT_TEXTURE;
    /** Where the normal, if any, and the first of the sway's two values are among the foliage's vertex inputs. */
    private static int normalLocation;
    private static int swayLocation;
    private static boolean litFailed;
    private static boolean shadowFailed;

    private BerylBridge() {}

    /**
     * Puts one of Beryl's pipelines under the mod's render pipelines: the lit one while Beryl draws the terrain with its
     * shaders, the shadow one while it draws its shadow map, and none otherwise. Returns whether one is under them now,
     * so the caller hands the pass what it reads. Render thread only, and before the pass is opened.
     *
     * @param vertexFormat the format of the foliage's vertices
     * @param normalAt the place of the normal among its elements, or -1 where it has none
     * @param swayAt the place of the first of the sway's two values among them
     * @param swaySize the size of the block of sway settings
     * @param interactionSize the size of the block of pushes
     * @param pipelines the render pipelines the foliage is about to be drawn with, each already compiled
     */
    static boolean light(
        Object vertexFormat,
        int normalAt,
        int swayAt,
        int swaySize,
        int interactionSize,
        Object... pipelines
    ) {
        if (!LOADED) {
            return false;
        }
        normalLocation = normalAt;
        swayLocation = swayAt;
        boolean shadow = false;
        try {
            String stage = Names.stage();
            shadow = "SHADOW_MAP".equals(stage);
            boolean terrain = "TERRAIN".equals(stage);
            if (!((shadow && !shadowFailed) || (terrain && !litFailed))) {
                unlight();
                return false;
            }
            String options = Names.options();
            if (!options.equals(builtFor)) {
                // Beryl's options changed: its shaders are other ones now.
                unlight();
                for (Object built : new Object[] {
                    litPipeline,
                    shadowPipeline,
                }) {
                    if (built != null) {
                        Names.SCHEDULE_CLEAN_UP.invoke(built);
                    }
                }
                litPipeline = null;
                shadowPipeline = null;
                builtFor = options;
            }
            Object under;
            if (shadow) {
                if (shadowPipeline == null) {
                    shadowPipeline = Names.buildShadow(
                        vertexFormat,
                        swaySize,
                        interactionSize
                    );
                }
                under = shadowPipeline;
                freeBinding = shadowFreeBinding;
                lightTexture = SHADOW_LIGHT_TEXTURE;
            } else {
                if (litPipeline == null) {
                    litPipeline = Names.buildLit(
                        vertexFormat,
                        swaySize,
                        interactionSize
                    );
                }
                under = litPipeline;
                freeBinding = litFreeBinding;
                lightTexture = LIT_LIGHT_TEXTURE;
            }
            if (under == null) {
                fail(shadow, null);
                return false;
            }
            // All of them or none: a pipeline of Beryl's drawn without what it reads has nothing to fall back on. Beryl
            // keeps what was under a render pipeline the first time, to put it back, so that has to be there already --
            // which it is from the first time the pipeline was drawn with.
            for (Object pipeline : pipelines) {
                if (
                    !UNDER.containsKey(pipeline) &&
                    Names.GET_PIPELINE.invoke(pipeline) == null
                ) {
                    unlight();
                    return false;
                }
            }
            for (Object pipeline : pipelines) {
                if (UNDER.put(pipeline, under) != under) {
                    Names.REDIRECT.invoke(pipeline, under);
                }
            }
            return true;
        } catch (Throwable e) {
            fail(shadow, e);
            return false;
        }
    }

    private static void fail(boolean shadow, Throwable cause) {
        if (shadow) {
            shadowFailed = true;
        } else {
            litFailed = true;
        }
        String what = shadow
            ? "foliage casts no shadow under Beryl"
            : "foliage is drawn without Beryl's light and shadows";
        if (cause == null) {
            AllThingsFoliage.LOGGER.warn(
                "Beryl's shaders are not the ones the GPU foliage renderer knows how to follow; " +
                    what
            );
        } else {
            AllThingsFoliage.LOGGER.warn(
                "The GPU foliage renderer can't draw through Beryl's shaders; " +
                    what,
                cause
            );
        }
        try {
            unlight();
        } catch (Throwable e) {
            UNDER.clear();
        }
    }

    /** The names VulkanMod finds the game's globals, the sway's settings and the pushes by, as {@link #REGION}. */
    static String globals() {
        return "UBO " + freeBinding;
    }

    static String sway() {
        return "UBO " + (freeBinding + 1);
    }

    static String interaction() {
        return "UBO " + (freeBinding + 2);
    }

    /** The name the pipeline put under the mod's last reads the lightmap by. */
    static String lightTexture() {
        return lightTexture;
    }

    /**
     * Whether Beryl is turning the colours written into a buffer builder from the screen's curve into plain light, as it
     * does for as long as it draws a frame with its shaders. Its shaders do the same to the colours of the terrain, which
     * are not written through a buffer builder, so a plant meshed in the middle of a frame would have it done twice --
     * and come out darker than one meshed outside it.
     */
    static boolean straightensColours() {
        if (!LOADED || litFailed) {
            return false;
        }
        try {
            return (boolean) Shared.SRGB_COLOURS.invoke(null);
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * A colour that Beryl's straightening brings back to the one given: each of red, green and blue raised to the
     * power that undoes it. Alpha is left alone, as Beryl leaves it.
     */
    static int beforeStraightening(int packed) {
        return (
            (packed & 0xFF000000) |
            (CURVED[(packed >> 16) & 0xFF] << 16) |
            (CURVED[(packed >> 8) & 0xFF] << 8) |
            CURVED[packed & 0xFF]
        );
    }

    private static final int[] CURVED = new int[256];

    static {
        for (int i = 0; i < CURVED.length; i++) {
            CURVED[i] = (int) Math.round(
                Math.pow(i / 255.0, 1.0 / 2.2) * 255.0
            );
        }
    }

    /**
     * The id Beryl gives a block, which it picks the block's material by, where the mesher writes it above a vertex's
     * light; nothing where Beryl is not installed or gives the block none.
     */
    static int materialBits(Object blockState) {
        if (!LOADED) {
            return 0;
        }
        try {
            Method blockId = Shared.blockId(blockState);
            return blockId == null
                ? 0
                : Math.max((int) blockId.invoke(null, blockState), 0) <<
                      BerylFoliageShader.LIGHT_BITS;
        } catch (Throwable e) {
            return 0;
        }
    }

    /** Beryl's lighting for the versions where the foliage is drawn through VulkanMod itself; see legacyLit. */
    private static Object legacyPipeline;
    private static String legacyBuiltFor;

    /**
     * The pipeline the foliage is lit by Beryl with where it is drawn through VulkanMod's own pipelines (before
     * 1.21.11; see VulkanLegacyDraw), or null while Beryl is not drawing the terrain with its shaders: there is no
     * pipeline of the game's to put it under there, so the renderer binds this one in place of its own. It is built
     * from Beryl's terrain shaders as on later versions, with what the sway reads declared as named fields, which is
     * how VulkanMod feeds a pipeline there.
     */
    static Object legacyLit(Object vertexFormat, int normalAt, int swayAt) {
        if (!LOADED || litFailed) {
            return null;
        }
        try {
            if (
                !((boolean) Shared.USING_SHADERS.invoke(null)) ||
                !"TERRAIN".equals(String.valueOf(Shared.STAGE.invoke(null)))
            ) {
                return null;
            }
            String options = Shared.options();
            if (!options.equals(legacyBuiltFor)) {
                if (legacyPipeline != null) {
                    legacyPipeline
                        .getClass()
                        .getMethod("scheduleCleanUp")
                        .invoke(legacyPipeline);
                }
                legacyPipeline = Shared.buildLegacyLit(
                    vertexFormat,
                    normalAt,
                    swayAt,
                    options
                );
                legacyBuiltFor = options;
                if (legacyPipeline == null) {
                    fail(false, null);
                }
            }
            return legacyPipeline;
        } catch (Throwable e) {
            legacyPipeline = null;
            fail(false, e);
            return null;
        }
    }

    private static Object legacyShadowPipeline;

    /**
     * The pipeline the foliage is drawn into Beryl's shadow map with on the same versions, or null where there is
     * none: Beryl's own shadow shaders, copied as its terrain's are. Nothing else may be drawn into the shadow map.
     */
    static Object legacyShadow(Object vertexFormat, int normalAt, int swayAt) {
        if (!LOADED || shadowFailed) {
            return null;
        }
        try {
            if (
                !((boolean) Shared.USING_SHADERS.invoke(null)) ||
                !"SHADOW_MAP".equals(String.valueOf(Shared.STAGE.invoke(null)))
            ) {
                return null;
            }
            if (legacyShadowPipeline == null) {
                legacyShadowPipeline = Shared.buildLegacyShadow(
                    vertexFormat,
                    normalAt,
                    swayAt
                );
                if (legacyShadowPipeline == null) {
                    fail(true, null);
                }
            }
            return legacyShadowPipeline;
        } catch (Throwable e) {
            legacyShadowPipeline = null;
            fail(true, e);
            return null;
        }
    }

    /** Puts back what each render pipeline had under it. */
    private static void unlight() throws ReflectiveOperationException {
        for (Object pipeline : UNDER.keySet()) {
            Names.RESET.invoke(pipeline);
        }
        UNDER.clear();
    }

    /**
     * What is asked of Beryl whichever way the foliage is drawn, found by name the first time it is: these have been
     * the same in every Beryl so far, 0.2.0 for 1.21.1 included.
     */
    private static final class Shared {

        static final Method USING_SHADERS;
        static final Method STAGE;
        static final Method SRGB_COLOURS;
        /** Beryl 0.2.0's options, each a define of its terrain shaders, in its own order. */
        static final Method[] OPTIONS;
        private static final String[] DEFINES = {
            "COLORED_SHADOWS",
            "TEXTURED_SUN",
            "WATER_WAVING",
        };
        private static Method blockId;
        private static boolean blockIdSought;

        static {
            try {
                Class<?> rendering = Class.forName(
                    "net.beryl.render.RenderingPipeline"
                );
                USING_SHADERS = rendering.getMethod("isUsingShaderPipeline");
                STAGE = rendering.getMethod("getRenderingStage");
                SRGB_COLOURS = rendering.getMethod("useSrgbColors");
                OPTIONS = new Method[] {
                    rendering.getMethod("coloredShadows"),
                    rendering.getMethod("texturedSun"),
                    rendering.getMethod("wavingWater"),
                };
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        static String options() throws ReflectiveOperationException {
            StringBuilder options = new StringBuilder();
            for (Method option : OPTIONS) {
                options.append((boolean) option.invoke(null) ? '1' : '0');
            }
            return options.toString();
        }

        /** Beryl's lookup of a block state's id; it has one of a fluid state's by the same name. */
        static Method blockId(Object blockState)
            throws ReflectiveOperationException {
            if (!blockIdSought) {
                blockIdSought = true;
                for (Method method : Class.forName(
                    "net.beryl.render.ShaderRendererResources"
                ).getMethods()) {
                    if (
                        method.getName().equals("getBlockId") &&
                        method.getParameterCount() == 1 &&
                        method.getParameterTypes()[0].isInstance(blockState)
                    ) {
                        blockId = method;
                    }
                }
            }
            return blockId;
        }

        /**
         * Beryl's terrain pipeline over the foliage's vertices for VulkanMod's own way of drawing, or null where its
         * vertex shader is not one this knows how to copy.
         */
        static Object buildLegacyLit(
            Object vertexFormat,
            int normalAt,
            int swayAt,
            String options
        ) throws ReflectiveOperationException {
            List<String> defines = new ArrayList<>();
            for (int i = 0; i < DEFINES.length; i++) {
                if (options.charAt(i) == '1') {
                    defines.add(DEFINES[i]);
                }
            }
            boolean coloredShadows = options.charAt(0) == '1';
            Class<?> loader = Class.forName(
                "net.vulkanmod.render.shader.ShaderLoadUtil"
            );
            String shaders = (String) Class.forName(
                "net.beryl.render.util.SUtil"
            )
                .getField("SHADERS_PATH")
                .get(null);
            Object path = loader
                .getMethod("resolveShaderPath", String.class, String.class)
                .invoke(null, shaders, "terrain");
            Method load = loader.getMethod(
                "loadShader",
                String.class,
                String.class,
                List.class
            );
            String terrainVertex = (String) load.invoke(
                null,
                path,
                "terrain.vsh",
                defines
            );
            String fragment = (String) load.invoke(
                null,
                path,
                "terrain.fsh",
                defines
            );
            if (terrainVertex == null || fragment == null) {
                return null;
            }
            // Beryl's two blocks are at 0 and 1 and its textures follow them: three, or five with coloured shadows.
            int free = coloredShadows ? 7 : 5;
            String vertex = BerylFoliageShader.legacyVertex(
                terrainVertex,
                VulkanLegacyDraw.swayDeclarations(free),
                VulkanFoliageShader.SWAYED,
                normalAt,
                swayAt,
                true
            );
            if (vertex == null) {
                return null;
            }
            Class<?> configs = Class.forName(
                "net.beryl.render.shader.BerylPipelineConfigs"
            );
            Object[] blocks = {
                configs.getField("COMMON_UB0").get(null),
                configs.getField("COMMON_UB1").get(null),
            };
            Object[][] textures = coloredShadows
                ? new Object[][] {
                      { 2, "Sampler0", 0 },
                      { 3, LIT_LIGHT_TEXTURE, 2 },
                      { 4, "ShadowMap", 3 },
                      { 5, "ShadowMap1", 4 },
                      { 6, "ShadowColor", 5 },
                  }
                : new Object[][] {
                      { 2, "Sampler0", 0 },
                      { 3, LIT_LIGHT_TEXTURE, 2 },
                      { 4, "ShadowMap", 3 },
                  };
            return VulkanLegacyDraw.berylPipeline(
                vertexFormat,
                blocks,
                free,
                textures,
                vertex,
                fragment
            );
        }

        /**
         * Beryl's shadow map pipeline over the foliage's vertices, the same way. Its blocks are the ones its own
         * pipeline is told in a file beside the shaders, which hold what these do.
         */
        static Object buildLegacyShadow(
            Object vertexFormat,
            int normalAt,
            int swayAt
        ) throws ReflectiveOperationException {
            Class<?> loader = Class.forName(
                "net.vulkanmod.render.shader.ShaderLoadUtil"
            );
            String shaders = (String) Class.forName(
                "net.beryl.render.util.SUtil"
            )
                .getField("SHADERS_PATH")
                .get(null);
            Object path = loader
                .getMethod("resolveShaderPath", String.class, String.class)
                .invoke(null, shaders, "shadow");
            Method load = loader.getMethod(
                "loadShader",
                String.class,
                String.class,
                List.class
            );
            String shadowVertex = (String) load.invoke(
                null,
                path,
                "shadow.vsh",
                null
            );
            String fragment = (String) load.invoke(
                null,
                path,
                "shadow.fsh",
                null
            );
            if (shadowVertex == null || fragment == null) {
                return null;
            }
            // Its two blocks are at 0 and 1, the texture at 2 and the lightmap, which it declares and never reads, at 3.
            int free = 4;
            String vertex = BerylFoliageShader.legacyVertex(
                shadowVertex,
                VulkanLegacyDraw.swayDeclarations(free),
                VulkanFoliageShader.SWAYED,
                normalAt,
                swayAt,
                false
            );
            if (vertex == null) {
                return null;
            }
            Object[] blocks = {
                VulkanLegacyDraw.block(0, 1, new String[][] {
                    { "mat4", "MVP" },
                    { "mat4", "ModelViewMat" },
                    { "mat4", "LightSpaceMat" },
                }),
                VulkanLegacyDraw.block(1, 31, new String[][] {
                    { "vec4", "ColorModulator" },
                    { "vec4", "FogColor" },
                    { "float", "FogStart" },
                    { "float", "FogEnd" },
                    { "float", "ShadowDistortion" },
                }),
            };
            Object[][] textures = {
                { 2, "Sampler0", 0 },
                { 3, "Sampler2", 2 },
            };
            return VulkanLegacyDraw.berylPipeline(
                vertexFormat,
                blocks,
                free,
                textures,
                vertex,
                fragment
            );
        }
    }

    /** Found the first time the foliage is drawn with Beryl installed, when both mods' classes are certainly loaded. */
    private static final class Names {

        private static final String VULKAN = "net.vulkanmod.vulkan.shader.";
        static final Method USING_SHADERS;
        static final Method STAGE;
        static final Method[] OPTIONS;
        static final Method SRGB_COLOURS;
        static final Method GET_PIPELINE;
        static final Method REDIRECT;
        static final Method RESET;
        static final Method SCHEDULE_CLEAN_UP;

        static {
            try {
                Class<?> rendering = Class.forName(
                    "net.beryl.render.RenderingPipeline"
                );
                USING_SHADERS = rendering.getMethod("isUsingShaderPipeline");
                STAGE = rendering.getMethod("getRenderingStage");
                SRGB_COLOURS = rendering.getMethod("useSrgbColors");
                OPTIONS = new Method[] {
                    rendering.getMethod("coloredShadows"),
                    rendering.getMethod("texturedSun"),
                    rendering.getMethod("wavingWater"),
                    rendering.getMethod("SSREnabled"),
                };
                Class<?> graphics = Class.forName(VULKAN + "GraphicsPipeline");
                GET_PIPELINE = Class.forName(
                    "net.vulkanmod.interfaces.shader.ExtendedRenderPipeline"
                ).getMethod("getPipeline");
                Class<?> redirecting = Class.forName(
                    "net.beryl.render.ShaderRenderPipeline"
                );
                REDIRECT = redirecting.getMethod("redirectPipeline", graphics);
                RESET = redirecting.getMethod("resetPipeline");
                SCHEDULE_CLEAN_UP = Class.forName(
                    VULKAN + "Pipeline"
                ).getMethod("scheduleCleanUp");
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        private static Method blockId;
        private static boolean blockIdSought;

        /** Beryl's lookup of a block state's id; it has one of a fluid state's by the same name. */
        static Method blockId(Object blockState)
            throws ReflectiveOperationException {
            if (!blockIdSought) {
                blockIdSought = true;
                for (Method method : Class.forName(
                    "net.beryl.render.ShaderRendererResources"
                ).getMethods()) {
                    if (
                        method.getName().equals("getBlockId") &&
                        method.getParameterCount() == 1 &&
                        method.getParameterTypes()[0].isInstance(blockState)
                    ) {
                        blockId = method;
                    }
                }
            }
            return blockId;
        }

        /** The defines Beryl compiles its terrain shaders with, in its own order. */
        private static final String[] DEFINES = {
            "COLORED_SHADOWS",
            "TEXTURED_SUN",
            "WATER_WAVING",
            "SSR",
        };

        /** What Beryl is drawing with its shaders, by its own name for it, or null while they are off. */
        static String stage() throws ReflectiveOperationException {
            return (boolean) USING_SHADERS.invoke(null)
                ? String.valueOf(STAGE.invoke(null))
                : null;
        }

        static String options() throws ReflectiveOperationException {
            StringBuilder options = new StringBuilder();
            for (Method option : OPTIONS) {
                options.append((boolean) option.invoke(null) ? '1' : '0');
            }
            return options.toString();
        }

        /** Beryl's terrain pipeline over the foliage's vertices, or null where its vertex shader is not the one known. */
        static Object buildLit(
            Object vertexFormat,
            int swaySize,
            int interactionSize
        ) throws ReflectiveOperationException {
            String options = options();
            List<String> defines = new ArrayList<>();
            for (int i = 0; i < DEFINES.length; i++) {
                if (options.charAt(i) == '1') {
                    defines.add(DEFINES[i]);
                }
            }
            boolean coloredShadows = options.charAt(0) == '1';

            // The sources, read and given their defines as Beryl reads its own.
            Object path = shaderPath("terrain");
            String terrainVertex = load(path, "terrain.vsh", defines);
            String fragment = load(path, "terrain.fsh", defines);
            if (terrainVertex == null || fragment == null) {
                return null;
            }
            // Beryl's textures are at 3 to 5, and with coloured shadows two more follow them.
            int free = coloredShadows ? 8 : 6;
            String vertex = BerylFoliageShader.vertex(
                terrainVertex,
                readSway(),
                free,
                normalLocation,
                swayLocation
            );
            if (vertex == null) {
                return null;
            }

            // What the pipeline reads, as Beryl's terrain pipeline says it: its two blocks, then the sway's four by
            // their size alone, and its textures.
            Class<?> configs = Class.forName(
                "net.beryl.render.shader.BerylPipelineConfigs"
            );
            Object config = Class.forName(VULKAN + "PipelineConfig")
                .getMethod("builder")
                .invoke(null);
            Method addBlock = config
                .getClass()
                .getMethod(
                    "addUB",
                    Class.forName(VULKAN + "PipelineConfig$UB")
                );
            addBlock.invoke(config, configs.getField("COMMON_UB0").get(null));
            addBlock.invoke(config, configs.getField("COMMON_UB1").get(null));
            addSwayBlocks(config, free, swaySize, interactionSize);
            Method addTexture = config
                .getClass()
                .getMethod(
                    "addImageDescriptor",
                    int.class,
                    String.class,
                    String.class,
                    int.class
                );
            addTexture.invoke(config, 3, "sampler2D", "Sampler0", 0);
            addTexture.invoke(config, 4, "sampler2D", LIT_LIGHT_TEXTURE, 2);
            addTexture.invoke(config, 5, "sampler2D", "ShadowMap", 3);
            if (coloredShadows) {
                addTexture.invoke(config, 6, "sampler2D", "ShadowMap1", 4);
                addTexture.invoke(config, 7, "sampler2D", "ShadowColor", 5);
            }
            litFreeBinding = free;
            return pipeline(
                vertexFormat,
                "mc2_foliage_beryl",
                config,
                vertex,
                fragment
            );
        }

        /**
         * The pipeline Beryl draws the terrain into its shadow map with, over the foliage's vertices, or null where it
         * is not the one known. What it reads is taken from the file Beryl says it in, less its block of section
         * offsets, whose place the region's transform takes.
         */
        static Object buildShadow(
            Object vertexFormat,
            int swaySize,
            int interactionSize
        ) throws ReflectiveOperationException {
            Object path = shaderPath("shadow");
            String shadowVertex = load(path, "shadow.vsh", null);
            String fragment = load(path, "shadow.fsh", null);
            Class<?> loader = Class.forName(
                "net.vulkanmod.render.shader.ShaderLoadUtil"
            );
            Object json = loader
                .getMethod("getJsonConfig", String.class, String.class)
                .invoke(null, path, "shadow");
            if (shadowVertex == null || fragment == null || json == null) {
                return null;
            }
            Class<?> configClass = Class.forName(VULKAN + "PipelineConfig");
            Object beryls = null;
            for (Method method : configClass.getMethods()) {
                if (
                    method.getName().equals("fromJson") &&
                    method.getParameterCount() == 2
                ) {
                    beryls = method.invoke(null, "shadow", json);
                }
            }
            if (beryls == null) {
                return null;
            }

            Class<?> block = Class.forName(VULKAN + "PipelineConfig$UB");
            Object config = configClass.getMethod("builder").invoke(null);
            Method addBlock = config.getClass().getMethod("addUB", block);
            int last = -1;
            boolean regionFree = false;
            for (Object one : (List<?>) configClass
                .getField("ubs")
                .get(beryls)) {
                int binding = block.getField("binding").getInt(one);
                if (((List<?>) block.getField("uniforms").get(one)).isEmpty()) {
                    // Declared by its size alone: the section offsets.
                    if (binding != BerylFoliageShader.REGION_BINDING) {
                        return null;
                    }
                    regionFree = true;
                    continue;
                }
                addBlock.invoke(config, one);
                last = Math.max(last, binding);
            }
            for (Object texture : (List<?>) configClass
                .getField("imageDescriptors")
                .get(beryls)) {
                config
                    .getClass()
                    .getMethod("addImageDescriptor", texture.getClass())
                    .invoke(config, texture);
                last = Math.max(
                    last,
                    (int) texture
                        .getClass()
                        .getMethod("binding")
                        .invoke(texture)
                );
            }
            if (!regionFree || last < BerylFoliageShader.REGION_BINDING) {
                return null;
            }
            int free = last + 1;
            String vertex = BerylFoliageShader.shadowVertex(
                shadowVertex,
                readSway(),
                free,
                normalLocation,
                swayLocation
            );
            if (vertex == null) {
                return null;
            }
            addSwayBlocks(config, free, swaySize, interactionSize);
            shadowFreeBinding = free;
            return pipeline(
                vertexFormat,
                "mc2_foliage_beryl_shadow",
                config,
                vertex,
                fragment
            );
        }

        private static Object shaderPath(String folder)
            throws ReflectiveOperationException {
            String shaders = (String) Class.forName(
                "net.beryl.render.util.SUtil"
            )
                .getField("SHADERS_PATH")
                .get(null);
            return Class.forName("net.vulkanmod.render.shader.ShaderLoadUtil")
                .getMethod("resolveShaderPath", String.class, String.class)
                .invoke(null, shaders, folder);
        }

        private static String load(
            Object path,
            String name,
            List<String> defines
        ) throws ReflectiveOperationException {
            return (String) Class.forName(
                "net.vulkanmod.render.shader.ShaderLoadUtil"
            )
                .getMethod("loadShader", String.class, String.class, List.class)
                .invoke(null, path, name, defines);
        }

        /** The four blocks the sway reads, each declared by its size alone, in the order of their bindings. */
        private static void addSwayBlocks(
            Object config,
            int free,
            int swaySize,
            int interactionSize
        ) throws ReflectiveOperationException {
            Class<?> block = Class.forName(VULKAN + "PipelineConfig$UB");
            Method addBlock = config.getClass().getMethod("addUB", block);
            Method blockBuilder = block.getMethod(
                "builder",
                int.class,
                int.class
            );
            int[][] sized = {
                {
                    BerylFoliageShader.REGION_BINDING,
                    BerylFoliageShader.REGION_SIZE,
                },
                { free, BerylFoliageShader.GLOBALS_SIZE },
                { free + 1, swaySize },
                { free + 2, interactionSize },
            };
            for (int[] one : sized) {
                Object builder = blockBuilder.invoke(
                    null,
                    one[0],
                    VERTEX_STAGE
                );
                builder
                    .getClass()
                    .getMethod("setSize", int.class)
                    .invoke(builder, one[1]);
                addBlock.invoke(
                    config,
                    builder.getClass().getMethod("build").invoke(builder)
                );
            }
        }

        private static Object pipeline(
            Object vertexFormat,
            String name,
            Object config,
            String vertex,
            String fragment
        ) throws ReflectiveOperationException {
            Object built = config.getClass().getMethod("build").invoke(config);
            Class<?> builderClass = Class.forName(VULKAN + "Pipeline$Builder");
            Object builder = null;
            for (var constructor : builderClass.getConstructors()) {
                Class<?>[] parameters = constructor.getParameterTypes();
                if (parameters.length == 2 && parameters[1] == String.class) {
                    builder = constructor.newInstance(vertexFormat, name);
                }
            }
            if (builder == null) {
                return null;
            }
            builderClass
                .getMethod("applyConfig", built.getClass())
                .invoke(builder, built);
            Class<?> kind = Class.forName(VULKAN + "SPIRVUtils$ShaderKind");
            Method setSource = builderClass.getMethod(
                "setShaderSrc",
                kind,
                String.class
            );
            setSource.invoke(builder, enumValue(kind, "VERTEX_SHADER"), vertex);
            setSource.invoke(
                builder,
                enumValue(kind, "FRAGMENT_SHADER"),
                fragment
            );
            return builderClass
                .getMethod("createGraphicsPipeline")
                .invoke(builder);
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        private static Object enumValue(Class<?> type, String name) {
            return Enum.valueOf((Class) type, name);
        }

        private static String readSway() {
            String path =
                "/assets/" +
                AllThingsFoliage.MOD_ID +
                "/shaders/include/sway.glsl";
            try (InputStream in = BerylBridge.class.getResourceAsStream(path)) {
                if (in == null) {
                    throw new IllegalStateException("Missing " + path);
                }
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("Could not read " + path, e);
            }
        }
    }
}
