package io.github.andrewcwhy.allthingsfoliage.gpu;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds the vertex shader the foliage is drawn with while Beryl's shaders are on, from the one Beryl draws its terrain
 * with, so a plant is lit, shadowed and fogged exactly as the chunk mesh around it: Beryl works all of that out in its
 * own shaders, from values the mod's shader holds none of.
 * <p>
 * Beryl's terrain shader reads VulkanMod's compressed vertices, a block id beside them, and where each section is from a
 * push constant and a block of its own. The copy reads the mod's vertices instead -- Beryl's source already carries the
 * declarations for plain ones -- takes the region's corner from the transform the mod writes for it, and sways the
 * position before anything is worked out from it. Everything the shader does from there on is left as it is, so it
 * hands Beryl's own fragment shader, which is used untouched, what the terrain's hands it. The shader Beryl draws the
 * terrain into its shadow map with is written the same way, and copied the same way.
 * <p>
 * Nothing here is Beryl's text: the copy is made as the game runs, from the file in Beryl's jar, and every change has
 * to find its place exactly once. Where one does not -- another Beryl -- there is no copy, and the foliage is drawn
 * with the mod's own shader, unlit by Beryl, as it is without this.
 */
final class BerylFoliageShader {

    /**
     * The binding of the region's transform -- the game's block of that name, read for the region's corner alone: the
     * one Beryl's block of section offsets leaves free. VulkanMod lays a pipeline's bindings out by their number, so
     * they have to follow one another with none left out.
     */
    static final int REGION_BINDING = 2;
    /** The size of the game's transform block: two matrices, a colour and an offset. */
    static final int REGION_SIZE = 160;
    /** The size of the game's block of globals, as laid out below. */
    static final int GLOBALS_SIZE = 56;

    /** The material Beryl gives grass, ferns and most flowers: lit by the sky rather than by which way a face looks. */
    private static final int FOLIAGE_MATERIAL = 13;
    /**
     * A vertex's light takes the low eight bits of each of its two numbers, and the first has room above them: under
     * Beryl the mesher writes the id Beryl gives the block there, which is what Beryl picks a block's material by --
     * foliage for some plants, lit by its faces for every other and for leaves. The mod's own shader takes the light
     * from under it too.
     */
    static final int LIGHT_BITS = 8;
    private static final int LIGHT_MASK = (1 << LIGHT_BITS) - 1;

    private static final String INPUTS = """
    layout(location = %d) in float SwayCell;
    layout(location = %d) in float SwayWeights;
    """;

    /**
     * What stands in for the normal where the foliage's vertices carry none, as from 26.1.2: none at all. Beryl lights
     * its foliage material by the sky rather than by the face, so the light is the same without one; what a normal adds
     * is a highlight, and for a face looking up a shadow looked up as far as 0.6 blocks toward the sun, which is meant
     * for the ground and makes a plant miss the shadow of the plant in front of it. A plant's own faces look sideways,
     * and have neither.
     */
    private static final String NO_NORMAL = "const vec3 Normal = vec3(0.0);\n";

    private static final String UNIFORMS = """

    // The mod's: where the region is, what the sway goes by, and sway.glsl.
    layout(binding = %d) uniform MC2Region {
        mat4 mc2_RegionView;
        vec4 mc2_RegionColor;
        vec3 mc2_RegionOffset;
        mat4 mc2_RegionTexture;
    };

    layout(binding = %d) uniform MC2Globals {
        ivec3 mc2_CameraBlockPos;
        vec3 mc2_CameraOffset;
        vec2 mc2_ScreenSize;
        float mc2_GlintAlpha;
        float mc2_GameTime;
        int mc2_MenuBlurRadius;
        int mc2_UseRgss;
    };

    layout(binding = %d) uniform FoliageSway {
        float mc2_SwayIntensity;
        float mc2_CalmSway;
        vec4 mc2_SwayEdge;
        vec4 mc2_Weather;
    };

    #define MC2_MAX_CELLS 128
    layout(binding = %d) uniform FoliageInteraction {
        int mc2_CellCount;
        vec4 mc2_CellBoundsMin;
        vec4 mc2_CellBoundsMax;
        vec4 mc2_CellPosition[MC2_MAX_CELLS];
        vec4 mc2_CellForce[MC2_MAX_CELLS];
    };

    """;

    private static final String POSITION =
        "vec4 position = vec4(mc2_sway(Position + mc2_RegionOffset, mc2_RegionOffset, " +
        "mc2_CameraBlockPos, mc2_CameraOffset, mc2_GameTime, SwayCell, SwayWeights), 1.0);";

    private BerylFoliageShader() {}

    /**
     * The copy of Beryl's terrain vertex shader the foliage is drawn with, or null where the shader is not the one
     * this was written against. The sway's other three blocks take the three bindings from firstFreeBinding on.
     */
    static String vertex(
        String terrain,
        String sway,
        int firstFreeBinding,
        int normalLocation,
        int swayLocation
    ) {
        return copy(
            terrain,
            declarations(sway, firstFreeBinding),
            POSITION,
            normalLocation,
            swayLocation,
            true
        );
    }

    /**
     * The copy of the vertex shader Beryl draws its terrain into its shadow map with, for the foliage to be drawn into
     * it by: the same shader read the same way, which only places the vertex as the light sees it, so a plant's shadow
     * sways with the plant. Null where the shader is not the one this was written against.
     */
    static String shadowVertex(
        String shadow,
        String sway,
        int firstFreeBinding,
        int normalLocation,
        int swayLocation
    ) {
        return copy(
            shadow,
            declarations(sway, firstFreeBinding),
            POSITION,
            normalLocation,
            swayLocation,
            false
        );
    }

    /**
     * The same two copies for the versions where VulkanMod feeds a pipeline by named fields alone (before 1.21.11):
     * what the sway reads is declared the way {@link VulkanFoliageShader} declares it, which is handed over whole with
     * sway.glsl after it, and so is the expression that sways a vertex.
     */
    static String legacyVertex(
        String beryls,
        String swayDeclarations,
        String swayed,
        int normalLocation,
        int swayLocation,
        boolean lit
    ) {
        return copy(
            beryls,
            "\n" + swayDeclarations,
            "vec4 position = vec4(" + swayed + ", 1.0);",
            normalLocation,
            swayLocation,
            lit
        );
    }

    /** The sway's blocks as the game's own graphics device binds them, and sway.glsl. */
    private static String declarations(String sway, int firstFreeBinding) {
        // The game's block of globals, read for the camera and the time the sway goes by, the sway's settings and the
        // pushes, after the last of Beryl's own bindings.
        return (
            UNIFORMS.formatted(
                REGION_BINDING,
                firstFreeBinding,
                firstFreeBinding + 1,
                firstFreeBinding + 2
            ) + sway.replace("\r\n", "\n")
        );
    }

    /**
     * VulkanMod numbers a vertex's inputs by their place in its format, and the foliage's format is the game's block
     * format with the sway's two values after it -- which has a normal before 26.1.2 and none from then on. So the
     * normal's place, or -1 where there is none, and the place of the first of the sway's two are handed over.
     */
    private static String copy(
        String terrain,
        String declarations,
        String position,
        int normalLocation,
        int swayLocation,
        boolean lit
    ) {
        String inputs = INPUTS.formatted(swayLocation, swayLocation + 1);
        String normal =
            normalLocation < 0
                ? NO_NORMAL
                : "layout(location = " + normalLocation + ") in vec3 Normal;\n";
        // Each block's own material where the faces say which way they look, as Beryl lights most blocks by that. Where
        // they do not, the one material that needs no face: Beryl's foliage, lit by the sky.
        String material =
            normalLocation < 0
                ? "getMaterial(" + FOLIAGE_MATERIAL + ")"
                : "getMaterial(mc2_UV2.x >> " + LIGHT_BITS + ")";
        String source = terrain.replace("\r\n", "\n");
        String[][] changes = {
            // Plain vertices rather than VulkanMod's compressed ones: the declarations are Beryl's own.
            { "(?m)^#define COMPRESSED_VERTEX[ \\t]*\\n", "" },
            // Where a section is comes from the mod's transform, not from Beryl's push constant and section block.
            // Beryl 0.2.0 has no section block, or has it commented out.
            {
                "(?s)(?://)?layout\\s*\\(binding = 2\\) uniform UBO2 \\{.*?\\};\\n",
                "",
                "optional",
            },
            {
                "(?s)layout\\s*\\(push_constant\\) uniform pushConstant \\{.*?\\};\\n",
                "",
            },
            { "(?s)vec3 getVertexPosition\\(\\) \\{.*?\\n\\}\\n", "" },
            // The block id's place goes to the mod's two values.
            {
                "(?m)^layout\\s*\\(location = 5\\) in int BlockId;\\n",
                Matcher.quoteReplacement(inputs),
            },
            {
                "(?m)^layout\\s*\\(location = 4\\) in vec3 Normal;\\n",
                Matcher.quoteReplacement(normal),
            },
            // The light arrives with Beryl's block id above it (see LIGHT_BITS), under a name of its own.
            {
                "(?m)^layout\\s*\\(location = 3\\) in ivec2 UV2;\\n",
                "layout(location = 3) in ivec2 mc2_UV2;\n",
            },
            // The shadow's shader reads no material.
            lit
                ? new String[] {
                      "getMaterial\\(BlockId\\)",
                      Matcher.quoteReplacement(material),
                  }
                : null,
            // What the compressed vertex was taken apart into is there as it is, but for the light, which is under
            // the block id.
            {
                "(?m)^\\s*const vec4 Color = unpackUnorm4x8\\(PackedColor\\);\\n",
                "",
            },
            { "(?m)^\\s*const uint uv = Position\\.a;\\n", "" },
            {
                "(?m)^\\s*const ivec2 UV2 = ivec2\\(bitfieldExtract\\(uv, 0, 8\\), bitfieldExtract\\(uv, 8, 8\\)\\);\\n",
                "    const ivec2 UV2 = mc2_UV2 & " + LIGHT_MASK + ";\n",
            },
            { "texCoord0 = UV0 \\* UV_INV;", "texCoord0 = UV0;" },
            // The position, swayed before anything is worked out from it.
            {
                "vec4 position = vec4\\(getVertexPosition\\(\\), 1\\.0\\);",
                Matcher.quoteReplacement(position),
            },
            {
                "(?m)^void main\\(\\) \\{",
                Matcher.quoteReplacement(declarations + "\nvoid main() {"),
            },
        };
        for (String[] change : changes) {
            if (change == null) {
                continue;
            }
            Matcher matcher = Pattern.compile(change[0]).matcher(source);
            int found = 0;
            while (matcher.find()) {
                found++;
            }
            if (found == 0 && change.length > 2) {
                // Not in every Beryl: nothing to change.
                continue;
            }
            if (found != 1) {
                // Not there, or there twice: not the shader this was written against.
                return null;
            }
            source = matcher.replaceFirst(change[1]);
        }
        return source;
    }
}
