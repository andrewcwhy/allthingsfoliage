package io.github.andrewcwhy.allthingsfoliage.gpu.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Util;
import org.apache.commons.io.IOUtils;

/**
 * Draws foliage with the shaders Sodium draws its chunks with -- Sodium's own, or the ones a resource pack puts in
 * their place -- so foliage looks exactly as the chunk mesh around it while Sodium owns the terrain. Vanilla's terrain
 * shaders are not what the chunks are drawn with then, so {@link TerrainFoliageShader} would not match.
 * <p>
 * Sodium's shaders are written for Sodium's renderer and cannot be compiled as they are: they read Sodium's compressed
 * vertices, and uniforms the mod's draw doesn't hold. The mod builds a copy of each when the pipeline is compiled, from
 * the sources Sodium itself would read, so a resource pack's replacements are the ones read:
 * <ul>
 * <li>The include that decodes Sodium's vertices is replaced by one reading the mod's, which fills in the same values --
 * with the position already swayed. Everything the shader does from there on is left as it is.</li>
 * <li>Sodium's uniforms are pointed at the game's uniform blocks holding the same values: the matrices, the region's
 * offset, the fog, the texture filtering. Chunks are never fading in.</li>
 * </ul>
 * Sodium loads its shaders itself through 26.1.2, and resource packs reach them through Sodium Core Shader Support; from
 * 26.2 they are resources like the game's own, with the game's imports. What the shaders declare changed apart from that:
 * Sodium 0.8 (1.21.11) sets loose uniforms and a block of chunk fade-in times, and Sodium 0.9 (26.1.2) keeps most of
 * them in one block of globals and reads fade-in times from a buffer. On 26.1.2 a pack may still be written either way.
 * <p>
 * On 26.3 the shaders include rather than import and name a location for every input, and 26.3 compiles them all for
 * Vulkan, so Sodium's region offset and time always come in push constants. The game compiles only shaders it reads
 * from resources, so the mod compiles its copy itself, and closes it again when the shaders reload.
 * <p>
 * A shader that cannot be read that way, or that does not compile once it is, leaves the renderer on the terrain's shaders
 * -- on 26.3, on the mod's own.
 */
public final class SodiumFoliageShader {

    /** Both of the pipeline's shaders; the source hands over the vertex or the fragment copy as asked. */
    public static final Identifier SHADER = Identifier.fromNamespaceAndPath(
        AllThingsFoliage.MOD_ID,
        "core/foliage_sodium"
    );

    public static final String BLOCK_TEXTURE = "u_BlockTex";
    public static final String LIGHT_TEXTURE = "u_LightTex";

    private static final boolean SODIUM = AllThingsFoliage.xplat().isModLoaded(
        "sodium"
    );

    private static final String OPAQUE_VERTEX = "blocks/block_layer_opaque.vsh";
    private static final String OPAQUE_FRAGMENT =
        "blocks/block_layer_opaque.fsh";

    /** The game's own import, by namespace and path under that namespace's shader includes, or relative to the file. */

    private static final Pattern IMPORT = Pattern.compile(
        "^\\s*#include\\s*(?:<([\\w.-]+):([\\w/.-]+)>|\"([\\w/.-]+)\")\\s*$",
        Pattern.MULTILINE
    );

    /** The include that decodes Sodium's vertices, replaced by {@link #VERTEX_INPUTS}. */
    private static final String CHUNK_VERTEX =
        "sodium:shaders/include/chunk_vertex.glsl";
    /**
     * Sodium's block of globals, left out: the uniforms it declares are pointed at the game's instead, all of them, as the
     * block itself is gone.
     */
    private static final String GLOBALS = "sodium:shaders/include/globals.glsl";

    /** What Sodium compiles its cutout chunk program with: its vertex layout (the mod's include ignores it), fog, and the alpha test. */
    private static final String[] DEFINES = {
        "USE_VERTEX_COMPRESSION",
        "USE_FOG",
        "ALPHA_CUTOUT 0.5",
    };

    /** Sodium 0.9's per-section fade-in times, read from a buffer; every one of the mod's is fully in. */
    private static final Pattern SECTION_TIME = Pattern.compile(
        "\\btexelFetch\\s*\\(\\s*u_SectionTimeInfo\\b"
    );
    /** Sodium 0.8's block of per-chunk fade-in times, which a pack written for it may still declare, and its reads. */
    private static final Pattern CHUNK_FADES_BLOCK = Pattern.compile(
        "\\blayout\\s*\\(\\s*std140\\s*\\)\\s*uniform\\s+ChunkData\\s*\\{[^}]*\\}\\s*\\w*\\s*;"
    );
    private static final Pattern CHUNK_FADE = Pattern.compile(
        "\\bu_chunkFades\\s*\\[[^\\]]*\\]\\s*\\[[^\\]]*\\]"
    );
    /** Sodium's push constant block, which only Vulkan compiles; see {@link #withoutSodiumDrawState}. */
    private static final Pattern PUSH_CONSTANTS = Pattern.compile(
        "\\blayout\\s*\\(\\s*push_constant\\s*\\)\\s*uniform\\s+\\w+\\s*\\{[^}]*\\}\\s*\\w*\\s*;"
    );

    private static final Pattern VERSION = Pattern.compile(
        "^\\s*#version[^\\n]*\\n",
        Pattern.MULTILINE
    );

    // 26.3 reordered the members of DynamicTransforms and Globals. Read in another version's order, the region's offset
    // would come from the wrong bytes, and nothing would say so.

    private static final String DYNAMIC_TRANSFORMS = """
    layout(std140) uniform DynamicTransforms {
        mat4 mc2_ModelViewMat;
        mat4 mc2_TextureMat;
        vec4 mc2_ColorModulator;
        vec3 mc2_ModelOffset;
    };
    """;
    private static final String GLOBALS_BLOCK = """
    layout(std140) uniform Globals {
        ivec3 mc2_CameraBlockPos;
        float mc2_GlintAlpha;
        vec3 mc2_CameraOffset;
        float mc2_GameTime;
        vec2 mc2_ScreenSize;
        int mc2_MenuBlurRadius;
        int mc2_UseRgss;
    };
    """;

    /**
     * The game's uniform blocks the replacements below read, laid out as vanilla declares them. Their members are renamed:
     * only a block's name and layout have to match what the game binds, and Sodium's shaders use some of vanilla's names
     * for functions of their own.
     */
    private static final String GAME_UNIFORMS =
        DYNAMIC_TRANSFORMS +
        """
        layout(std140) uniform Projection {
            mat4 mc2_ProjMat;
        };
        layout(std140) uniform Fog {
            vec4 mc2_FogColor;
            float mc2_FogEnvironmentalStart;
            float mc2_FogEnvironmentalEnd;
            float mc2_FogRenderDistanceStart;
            float mc2_FogRenderDistanceEnd;
            float mc2_FogSkyEnd;
            float mc2_FogCloudsEnd;
        };
        """ +
        GLOBALS_BLOCK;

    /** Each Sodium uniform the game holds the value of, and where. Any other is left alone, and reads as zero. */
    private static final Map<String, String> UNIFORMS = new LinkedHashMap<>();
    /** Those Sodium keeps in its block of globals, from 0.9. */
    private static final Set<String> GLOBAL_UNIFORMS = Set.of(
        "u_ProjectionMatrix",
        "u_ModelViewMatrix",
        "u_FogColor",
        "u_EnvironmentFog",
        "u_RenderFog",
        "u_TexelSize",
        "u_TexCoordShrink",
        "u_FadePeriodInv",
        "u_UseRGSS"
    );

    static {
        UNIFORMS.put("u_ProjectionMatrix", "mc2_ProjMat");
        UNIFORMS.put("u_ModelViewMatrix", "mc2_ModelViewMat");
        // Positions are written relative to the region, and the mod's include moves each one by nothing more.
        UNIFORMS.put("u_RegionOffset", "mc2_ModelOffset");
        UNIFORMS.put("u_RegionID", "0u");
        // Only scales how far a texture coordinate is nudged inside its sprite, which the mod's vertices never are.
        UNIFORMS.put("u_TexCoordShrink", "vec2(0.0)");
        UNIFORMS.put(
            "u_TexelSize",
            "(1.0 / vec2(textureSize(" + BLOCK_TEXTURE + ", 0)))"
        );
        UNIFORMS.put("u_UseRGSS", "(mc2_UseRgss == 1)");
        UNIFORMS.put("u_FogColor", "mc2_FogColor");
        UNIFORMS.put(
            "u_EnvironmentFog",
            "vec2(mc2_FogEnvironmentalStart, mc2_FogEnvironmentalEnd)"
        );
        UNIFORMS.put(
            "u_RenderFog",
            "vec2(mc2_FogRenderDistanceStart, mc2_FogRenderDistanceEnd)"
        );
        UNIFORMS.put("u_CurrentTime", "0");
        UNIFORMS.put("u_FadePeriodInv", "0.0");
        // Sodium Core Shader Support's: the fraction of the day, as the game's own shaders read it.
        UNIFORMS.put("u_GameTime", "mc2_GameTime");
    }

    /** Where the mod's own inputs and sway.glsl go in {@link #VERTEX_INPUTS}. */
    private static final String ADDITIONS = "MC2_ADDITIONS";

    /**
     * What goes there on 26.3: the mod's two per-vertex values, after the four it shares with vanilla, the blocks the sway
     * reads -- the same file the mod's own shader includes -- and the sway.
     */
    private static final String ADDITIONS_SOURCE = """
    layout(location = 4) in float SwayCell;
    layout(location = 5) in float SwayWeights;
    #include <allthingsfoliage:foliage_uniforms.glsl>
    #include <allthingsfoliage:sway.glsl>
    """;

    /** The mod's vertices as the shader reads them; from 26.3 every input names its location. */

    private static final String VERTEX_ATTRIBUTES = """
    layout(location = 0) in vec3 Position;
    layout(location = 1) in vec4 Color;
    layout(location = 2) in vec2 UV0;
    layout(location = 3) in ivec2 UV2;
    """;

    /**
     * Fills in what Sodium's vertex include does, from the mod's vertices: the position swayed, relative to the region,
     * the colour, the texture and light coordinates as Sodium encodes them, and the material of cutout foliage.
     */
    private static final String VERTEX_INPUTS =
        VERTEX_ATTRIBUTES +
        """

        vec3 _vert_position;
        vec2 _vert_tex_diffuse_coord;
        vec2 _vert_tex_diffuse_coord_bias;
        vec2 _vert_tex_light_coord;
        vec4 _vert_color;
        uint _draw_id;
        uint _material_params;

        MC2_ADDITIONS

        void _vert_init() {
            _vert_position = mc2_sway(Position + mc2_ModelOffset, mc2_ModelOffset, mc2_CameraBlockPos, mc2_CameraOffset,
                    mc2_GameTime, SwayCell, SwayWeights) - mc2_ModelOffset;
            _vert_color = Color;
            _vert_tex_diffuse_coord = UV0;
            _vert_tex_diffuse_coord_bias = vec2(0.0);
            // Sodium keeps each light level half a texel inside the light map. The light is its own eight bits: Polytone's
            // coloured lights write the face above it, which is handed on below where Sodium's mesh carries it.
            _vert_tex_light_coord = vec2(clamp(ivec2(UV2.x & 255, UV2.y & 255) + 8, 8, 248)) / 256.0;
            // The only draw, so the first chunk's slot in Sodium's per-chunk data.
            _draw_id = 0u;
            // Sodium's cutout material: mipmapped, and cut out below half alpha.
            _material_params = 5u;
            _material_params |= uint((UV2.x >> 8) & 7) << 5u;
        }
        """;

    /**
     * Each copy as last compiled -- cut out, and solid for Fast leaves -- or null where it could not be; asked for again
     * once the shaders reload.
     */
    private static final Map<RenderPipeline, CompiledRenderPipeline> COMPILED =
        new java.util.IdentityHashMap<>();
    /** Vanilla's terrain pipeline as compiled when the copies were last built, which a shader reload replaces. */
    private static CompiledRenderPipeline compiledFor;

    /**
     * The two shaders of the copy, already written out whole. Nothing is left for the compiler to include, so it never
     * asks for an include.
     */
    private record BuiltSource(
        String vertex,
        String fragment
    ) implements ShaderSource {
        @Override
        public String getShader(Identifier id, ShaderType type) {
            return id.equals(SHADER)
                ? type == ShaderType.VERTEX
                    ? vertex
                    : fragment
                : null;
        }

        @Override
        public ShaderSource.CachedIncludeSource getInclude(Identifier id) {
            return null;
        }

        @Override
        public void close() {}
    }

    private SodiumFoliageShader() {}

    /** Whether Sodium draws the chunks. */
    static boolean available() {
        return SODIUM;
    }

    /** A shader file as the game reads it -- the topmost resource pack's -- by its full name, or null. */
    private static String read(String namespace, String path) {
        Optional<Resource> resource = Minecraft.getInstance()
            .getResourceManager()
            .getResource(Identifier.fromNamespaceAndPath(namespace, path));
        if (resource.isEmpty()) {
            return null;
        }
        try (Reader reader = resource.get().openAsReader()) {
            return IOUtils.toString(reader);
        } catch (IOException e) {
            return null;
        }
    }

    /** An import's full name: under its namespace's shader includes, or next to the file importing it. */
    private static String imported(Matcher match, String from) {
        if (match.group(1) != null) {
            return match.group(1) + ":shaders/include/" + match.group(2);
        }
        int directory = from.lastIndexOf('/');
        return from.substring(0, directory + 1) + match.group(3);
    }

    /**
     * The pipeline compiled with the copy of Sodium's shaders, or null where Sodium does not draw the chunks or the copy
     * could not be built or compiled.
     * <p>
     * The game compiles only the shaders it reads from resources, so the copy is compiled here, into a pipeline the mod
     * holds itself. Each pipeline asked for is built once after each shader reload -- which shows as vanilla's own terrain
     * pipeline coming back as a different compiled one -- and the ones before are closed then. Called while the frame is
     * prepared, with no render pass open.
     * <p>
     * Cut out, the copy is Sodium's cutout pass; not, its solid pass, which keeps every pixel: for leaves drawn solid, as
     * the game's Fast leaves are.
     */
    public static CompiledRenderPipeline compiled(
        RenderPipeline pipeline,
        boolean cutout
    ) {
        if (!available()) {
            return null;
        }
        CompiledRenderPipeline terrain =
            RenderSystem.getCompiledPipelineNullable(
                RenderPipelines.SOLID_TERRAIN
            );
        if (terrain != compiledFor) {
            compiledFor = terrain;
            for (CompiledRenderPipeline compiled : COMPILED.values()) {
                if (compiled != null) {
                    compiled.close();
                }
            }
            COMPILED.clear();
        }
        if (COMPILED.containsKey(pipeline)) {
            return COMPILED.get(pipeline);
        }
        CompiledRenderPipeline compiled = null;
        String vertex = vertex(cutout);
        String fragment = fragment(cutout);
        if (vertex == null || fragment == null) {
            AllThingsFoliage.logger.warn(
                "Sodium's chunk shaders in use can't be read by the GPU foliage renderer; foliage near " +
                    "the player is drawn with the mod's own shaders, which may not match a resource pack's look"
            );
        } else {
            try {
                compiled = RenderSystem.getDevice()
                    .compilePipeline(
                        pipeline,
                        new BuiltSource(vertex, fragment),
                        Util.backgroundExecutor()
                    )
                    .join()
                    .finishCompile();
            } catch (RuntimeException e) {
                // A shader that does not compile comes back as an exception rather than as nothing.
                AllThingsFoliage.logger.warn(
                    "Sodium's chunk shaders did not compile with the GPU foliage renderer's vertices; " +
                        "foliage near the player is drawn with the mod's own shaders",
                    e
                );
            }
        }
        COMPILED.put(pipeline, compiled);
        return compiled;
    }

    private static String read(String name) {
        int colon = name.indexOf(':');
        return read(name.substring(0, colon), name.substring(colon + 1));
    }

    /** A shader's full name, as the imports inside it are resolved against. */
    private static String shaderName(String path) {

        return "sodium:shaders/" + path;

    }

    /** Sodium's solid material, which the copy for solid leaves hands its shaders in place of the cutout one. */
    private static final String CUTOUT_MATERIAL = "_material_params = 5u;";
    private static final String SOLID_MATERIAL = "_material_params = 1u;";

    private static String vertex(boolean cutout) {
        String name = shaderName(OPAQUE_VERTEX);
        String raw = read(name);

        String additions = ADDITIONS_SOURCE;

        if (raw == null || additions == null || !raw.contains("_vert_init")) {
            return null;
        }
        String inputs = VERTEX_INPUTS.replace(
            ADDITIONS,
            VERSION.matcher(additions).replaceFirst("")
        );
        if (!cutout) {
            inputs = inputs.replace(CUTOUT_MATERIAL, SOLID_MATERIAL);
        }
        Set<String> included = new HashSet<>();
        String expanded = expand(raw, name, inputs, included);
        return expanded == null || !expanded.contains("void _vert_init()")
            ? null
            : adapt(expanded, included.contains(GLOBALS), cutout);
    }

    private static String fragment(boolean cutout) {
        String name = shaderName(OPAQUE_FRAGMENT);
        String raw = read(name);
        Set<String> included = new HashSet<>();
        String expanded =
            raw == null ? null : expand(raw, name, null, included);
        return expanded == null
            ? null
            : adapt(expanded, included.contains(GLOBALS), cutout);
    }

    /**
     * Sodium's imports written out, each once, as the parser reading them does; the vertex include replaced by the mod's,
     * the block of globals left out, or null if an import can't be found.
     */
    private static String expand(
        String source,
        String from,
        String vertexInputs,
        Set<String> included
    ) {
        Matcher matcher = IMPORT.matcher(source);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String name = imported(matcher, from);
            String replacement = "";
            if (included.add(name) && !name.equals(GLOBALS)) {
                if (name.equals(CHUNK_VERTEX) && vertexInputs != null) {

                    // Written out as well: on 26.3 the copy is handed to the compiler whole, with nothing left to include.
                    replacement = expand(vertexInputs, name, null, included);
                    if (replacement == null) {
                        return null;
                    }

                } else {
                    String imported = read(name);
                    replacement =
                        imported == null
                            ? null
                            : expand(imported, name, vertexInputs, included);
                    if (replacement == null) {
                        return null;
                    }
                }
            }
            matcher.appendReplacement(
                out,
                Matcher.quoteReplacement(replacement)
            );
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /**
     * Sodium's defines and the game's uniform blocks after the version line, and Sodium's uniforms pointed at them: each
     * one declared loose, and every one its block of globals held where that block was imported and left out.
     */
    private static String adapt(
        String source,
        boolean globalsLeftOut,
        boolean cutout
    ) {
        Matcher version = VERSION.matcher(source);
        if (!version.find()) {
            return null;
        }
        String body = source.substring(version.end());
        StringBuilder header = new StringBuilder(version.group());
        for (String define : DEFINES) {
            // The solid copy goes without the alpha test, as Sodium's solid pass does: a define of its own from Sodium 0.9,
            // the fragment discard before, which Sodium 0.8 compiles only its cutout and translucent passes with. Sodium
            // Core Shader Support names the pass for the packs that ask.
            if (
                !cutout &&
                (define.startsWith("ALPHA_CUTOUT") ||
                    define.equals("USE_FRAGMENT_DISCARD"))
            ) {
                continue;
            }
            if (!cutout && define.equals("RENDER_PASS_CUTOUT")) {
                define = "RENDER_PASS_SOLID";
            }
            header.append("#define ").append(define).append('\n');
        }
        header.append(GAME_UNIFORMS);
        for (Map.Entry<String, String> uniform : UNIFORMS.entrySet()) {
            Matcher declaration = Pattern.compile(
                "\\buniform\\s+\\w+\\s+" + uniform.getKey() + "\\s*;"
            ).matcher(body);
            boolean declared = declaration.find();
            if (declared) {
                body = declaration.replaceAll("");
            }
            if (
                declared ||
                (globalsLeftOut && GLOBAL_UNIFORMS.contains(uniform.getKey()))
            ) {
                header
                    .append("#define ")
                    .append(uniform.getKey())
                    .append(' ')
                    .append(uniform.getValue())
                    .append('\n');
            }
        }

        body = withoutSodiumDrawState(body);

        return body == null ? null : header.append(body).toString();
    }

    /**
     * Every read of a section's fade-in time answers -1, never faded: the reads replaced whole, arguments and all, and
     * the buffer they read from no longer declared, so nothing needs binding to it. The same goes for Sodium 0.8's block
     * of fade-in times, which a pack on 26.1.2 may have been written against.
     * <p>
     * Sodium's push constants go too. On Vulkan the shader compiler defines {@code VULKAN}, and Sodium then declares its
     * region offset and time in a push constant block instead of as loose uniforms -- names already pointed at the game's
     * values, which would leave the block declaring {@code int 0;}.
     */
    private static String withoutSodiumDrawState(String body) {
        body = PUSH_CONSTANTS.matcher(body).replaceAll("");
        body = CHUNK_FADES_BLOCK.matcher(body).replaceAll("");
        body = CHUNK_FADE.matcher(body).replaceAll("-1");
        body = Pattern.compile("\\buniform\\s+\\w+\\s+u_SectionTimeInfo\\s*;")
            .matcher(body)
            .replaceAll("");
        Matcher read = SECTION_TIME.matcher(body);
        StringBuilder out = new StringBuilder();
        int copied = 0;
        while (read.find(copied)) {
            int open = body.indexOf('(', read.start());
            int depth = 0;
            int close = -1;
            for (int i = open; i < body.length(); i++) {
                char c = body.charAt(i);
                if (c == '(') {
                    depth++;
                } else if (c == ')' && --depth == 0) {
                    close = i;
                    break;
                }
            }
            if (close < 0) {
                return null;
            }
            out.append(body, copied, read.start()).append("ivec4(-1)");
            copied = close + 1;
        }
        return out.append(body.substring(copied)).toString();
    }

}
