package io.github.andrewcwhy.allthingsfoliage.gpu;

//?} else {
/*import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.shaders.ShaderType;
*/ //?}
import com.mojang.blaze3d.systems.RenderSystem;
//? >=1.21.11 {

//? >=26.3 {
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.IdentityHashMap;
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
 * Draws foliage with the terrain's own shaders -- vanilla's, or the ones a resource pack puts in their place -- so it is
 * lit and coloured exactly as the chunk mesh would draw it.
 * <p>
 * The fragment shader is the terrain's, used as it is. The vertex shader cannot be: it has to move each vertex first. The
 * mod builds it from the terrain's when the pipeline is compiled: every read of {@code Position} is pointed at a swayed
 * copy, the mod's inputs and {@code sway.glsl} go in next to {@code Position}'s declaration, and a new {@code main} sways
 * the vertex and then runs the shader's own. Whatever else the shader does to a vertex -- its light, its colour, its own
 * waving -- it keeps doing.
 * <p>
 * A terrain shader that cannot be read that way, or that does not compile once it is, leaves the renderer on its own
 * shaders, which draw foliage as vanilla does.
 * <p>
 * On 26.3 both shaders are read from resources as the game reads them -- the topmost pack's -- and handed to the
 * compiler with their includes written out, since the game compiles only what it reads itself. The mod holds the
 * pipeline, and closes it when the shaders reload. The terrain's section comes from its ChunkSection block, whose
 * shader variant the mod draws; the other, for multidraw, reads it from vertex inputs instead.
 */
final class TerrainFoliageShader {

    /** The pipeline's vertex shader: the mod's additions, which only compile once spliced into the terrain's. */
    static final Identifier VERTEX = Identifier.fromNamespaceAndPath(
        AllThingsFoliage.MOD_ID,
        "core/foliage_terrain"
    );
    static final Identifier TERRAIN = Identifier.withDefaultNamespace(
        "core/terrain"
    );

    private static final Pattern MAIN = Pattern.compile(
        "\\bvoid\\s+main\\s*\\(\\s*(?:void\\s*)?\\)"
    );
    private static final Pattern POSITION = Pattern.compile("\\bPosition\\b");
    private static final Pattern VERSION = Pattern.compile(
        "^\\s*#version[^\\n]*\\n",
        Pattern.MULTILINE
    );
    /** Position's declaration once renamed, with the layout it may have been given. */
    private static final Pattern RENAMED_INPUT = Pattern.compile(
        "((?:layout\\s*\\([^)]*\\)\\s*)?)in\\s+vec3\\s+mc2_Position\\s*;"
    );
    /** What the new main needs from the terrain shader's own declarations. */
    private static final String[] REQUIRED = {
        "ChunkPosition",
        "CameraBlockPos",
        "CameraOffset",
        "GameTime",
    };

    // The section's corner relative to the camera, as the terrain shader works it out, which SwayCell is measured from.
    private static final String SWAYED_MAIN = """

    void main() {
        vec3 mc2_sectionOffset = vec3(ChunkPosition - CameraBlockPos) + CameraOffset;
        mc2_Position = mc2_sway(Position + mc2_sectionOffset, mc2_sectionOffset, CameraBlockPos, CameraOffset,
                GameTime, SwayCell, SwayWeights) - mc2_sectionOffset;
        mc2_terrainMain();
    }
    """;

    //? >=26.3 {
    /**
     * What goes in next to Position's declaration: the mod's two per-vertex values, at locations well clear of the ones a
     * terrain shader gives its own inputs, the blocks the sway reads, and the sway.
     */
    private static final String ADDITIONS = """
    layout(location = 14) in float SwayCell;
    layout(location = 15) in float SwayWeights;
    #include <mc2_interactivefoliage:foliage_uniforms.glsl>
    #include <mc2_interactivefoliage:sway.glsl>
    """;
    /** An include, by namespace and path under that namespace's shader includes, or relative to the file. */
    private static final Pattern INCLUDE = Pattern.compile(
        "^\\s*#include\\s*(?:<([\\w.-]+):([\\w/.-]+)>|\"([\\w/.-]+)\")\\s*$",
        Pattern.MULTILINE
    );
    private static final String VERTEX_FILE =
        "minecraft:shaders/core/terrain.vsh";
    private static final String FRAGMENT_FILE =
        "minecraft:shaders/core/terrain.fsh";

    /**
     * Each pipeline as last compiled -- cut out, and solid for Fast leaves -- or null where it could not be; built again
     * once the shaders reload.
     */
    private static final Map<RenderPipeline, CompiledRenderPipeline> COMPILED =
        new IdentityHashMap<>();
    /** Vanilla's terrain pipeline as compiled when these were last built, which a shader reload replaces. */
    private static CompiledRenderPipeline compiledFor;
    /** Both shaders, spliced and written out whole, as last read; null where they could not be. */
    private static BuiltSource source;
    private static boolean sourceRead;

    /** Both shaders, already written out whole; nothing is left for the compiler to include. */
    private record BuiltSource(
        String vertex,
        String fragment
    ) implements ShaderSource {
        @Override
        public String getShader(Identifier id, ShaderType type) {
            if (type == ShaderType.VERTEX) {
                return id.equals(VERTEX) ? vertex : null;
            }
            return id.equals(TERRAIN) ? fragment : null;
        }

        @Override
        public ShaderSource.CachedIncludeSource getInclude(Identifier id) {
            return null;
        }

        @Override
        public void close() {}
    }

    //?} else {
    /*/^* Hands the compiler the spliced vertex shader, and every other shader as the game holds it. ^/
	private static final ShaderSource SOURCE = (id, type) -> {
		String source = Minecraft.getInstance().getShaderManager().getShader(id, type);
		if (!id.equals(VERTEX) || type != ShaderType.VERTEX) {
			return source;
		}
		String terrain = Minecraft.getInstance().getShaderManager().getShader(TERRAIN, ShaderType.VERTEX);
		String spliced = terrain == null || source == null ? null : splice(terrain, source);
		if (spliced == null) {
			AllThingsFoliage.LOGGER.warn("The terrain vertex shader in use can't be read by the GPU foliage renderer; foliage "
					+ "near the player is drawn with the renderer's own shaders, which may not match a resource pack's look");
		}
		return spliced;
	};
	*/ //?}

    private TerrainFoliageShader() {}

    //? >=26.3 {
    /**
     * The pipeline compiled from the terrain shaders in use, or null where they could not be read that way or did not
     * compile. Each pipeline asked for is built once after each shader reload -- which shows as vanilla's own terrain
     * pipeline coming back as a different compiled one -- when the ones before are closed. Called while the frame is
     * prepared, with no pass open.
     */
    static CompiledRenderPipeline compiled(RenderPipeline pipeline) {
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
            sourceRead = false;
        }
        if (COMPILED.containsKey(pipeline)) {
            return COMPILED.get(pipeline);
        }
        if (!sourceRead) {
            sourceRead = true;
            source = readSource();
        }
        CompiledRenderPipeline compiled = null;
        if (source != null) {
            try {
                compiled = RenderSystem.getDevice()
                    .compilePipeline(
                        pipeline,
                        source,
                        Util.backgroundExecutor()
                    )
                    .join()
                    .finishCompile();
            } catch (RuntimeException e) {
                // A shader that does not compile comes back as an exception rather than as nothing.
                AllThingsFoliage.LOGGER.warn(
                    "The terrain shaders in use did not compile with the GPU foliage renderer's sway; " +
                        "foliage near the player is drawn with the renderer's own shaders",
                    e
                );
            }
        }
        COMPILED.put(pipeline, compiled);
        return compiled;
    }

    /** Both terrain shaders, the vertex one spliced, written out whole; null where they could not be read that way. */
    private static BuiltSource readSource() {
        // Written out before it is spliced, as the game hands shaders over up to 26.2: what the new main reads from the
        // terrain's includes -- the camera and the time -- has to be there to be found. The mod's own includes, which the
        // splicing adds, are written out after.
        Set<String> included = new HashSet<>();
        String vertexSource = read(VERTEX_FILE);
        String terrainVertex =
            vertexSource == null
                ? null
                : expand(vertexSource, VERTEX_FILE, included);
        String spliced =
            terrainVertex == null ? null : splice(terrainVertex, ADDITIONS);
        String vertex =
            spliced == null ? null : expand(spliced, VERTEX_FILE, included);
        String fragmentSource = read(FRAGMENT_FILE);
        String fragment =
            fragmentSource == null
                ? null
                : expand(fragmentSource, FRAGMENT_FILE, new HashSet<>());
        if (vertex == null || fragment == null) {
            AllThingsFoliage.LOGGER.warn(
                "The terrain shaders in use can't be read by the GPU foliage renderer; foliage near " +
                    "the player is drawn with the renderer's own shaders, which may not match a resource pack's look"
            );
            return null;
        }
        return new BuiltSource(vertex, fragment);
    }

    /** A shader file as the game reads it -- the topmost resource pack's -- by its full name, or null. */
    private static String read(String name) {
        int colon = name.indexOf(':');
        Optional<Resource> resource = Minecraft.getInstance()
            .getResourceManager()
            .getResource(
                Identifier.fromNamespaceAndPath(
                    name.substring(0, colon),
                    name.substring(colon + 1)
                )
            );
        if (resource.isEmpty()) {
            return null;
        }
        try (Reader reader = resource.get().openAsReader()) {
            return IOUtils.toString(reader);
        } catch (IOException e) {
            return null;
        }
    }

    /** Every include written out, each once, or null if one can't be found. */
    private static String expand(
        String source,
        String from,
        Set<String> included
    ) {
        Matcher matcher = INCLUDE.matcher(source);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String name =
                matcher.group(1) != null
                    ? matcher.group(1) + ":shaders/include/" + matcher.group(2)
                    : from.substring(0, from.lastIndexOf('/') + 1) +
                      matcher.group(3);
            String replacement = "";
            if (included.add(name)) {
                String include = read(name);
                replacement =
                    include == null ? null : expand(include, name, included);
                if (replacement == null) {
                    return null;
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

    //?} else {
    /*/^*
	 * Whether the pipeline compiles from the terrain shaders in use. It is compiled here the first time it is asked about
	 * after each resource reload -- the game forgets every compiled pipeline then -- and the answer is kept alongside it,
	 * so asking every frame costs a lookup.
	 ^/
	static boolean usable(RenderPipeline pipeline) {
		return RenderSystem.getDevice().precompilePipeline(pipeline, SOURCE).isValid();
	}
	*/ //?}

    /** The terrain vertex shader with the mod's additions spliced in, or null if it isn't shaped the way that needs. */
    static String splice(String terrain, String additions) {
        if (!MAIN.matcher(terrain).find()) {
            return null;
        }
        for (String name : REQUIRED) {
            if (
                !Pattern.compile("\\b" + name + "\\b")
                    .matcher(terrain)
                    .find()
            ) {
                return null;
            }
        }
        String renamed = POSITION.matcher(
            MAIN.matcher(terrain).replaceFirst("void mc2_terrainMain()")
        ).replaceAll("mc2_Position");
        Matcher input = RENAMED_INPUT.matcher(renamed);
        if (!input.find()) {
            return null;
        }
        String declarations =
            input.group(1) +
            "in vec3 Position;\nvec3 mc2_Position;\n" +
            VERSION.matcher(additions).replaceFirst("") +
            "\n";
        return (
            renamed.substring(0, input.start()) +
            declarations +
            renamed.substring(input.end()) +
            SWAYED_MAIN
        );
    }
}
//?}
