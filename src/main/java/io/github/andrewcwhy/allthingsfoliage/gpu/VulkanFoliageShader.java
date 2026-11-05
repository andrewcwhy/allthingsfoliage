package io.github.andrewcwhy.allthingsfoliage.gpu;

/**
 * The foliage's shaders written for VulkanMod itself, for the versions where it stands in for OpenGL call by call
 * rather than for the game's graphics device (before 1.21.11): there the mod's own shader, which reads a block of
 * uniforms OpenGL binds, is one VulkanMod cannot take, and the foliage is drawn through VulkanMod's own pipelines
 * instead ({@link VulkanLegacyDraw}).
 * <p>
 * They do what VulkanMod's terrain shaders do -- the vertex's colour times the lightmap, the texture, a cutout, its
 * linear fog -- with the sway before the vertex is placed, so a plant looks as it does in VulkanMod's chunk mesh.
 * Every value is a named field of a uniform block, which is how VulkanMod feeds a pipeline: it looks each field up by
 * its name. It takes no arrays there, so the pushes are laid out as matrices, sixteen floats being four of the
 * vectors, as in the mod's own shader under VulkanMod on later versions.
 * <p>
 * What the sway reads is declared apart, since the copy of Beryl's terrain shader the foliage is drawn with under
 * Beryl on these versions reads the same, the same way ({@link BerylBridge}).
 */
final class VulkanFoliageShader {

    /** How many matrices hold the pushes' positions, and as many their forces: four cells each. */
    static final int CELL_MATRICES = 32;
    /** What the sway reads beside the pushes, type then name, as a pipeline is told them. */
    static final String[][] SWAY_FIELDS = {
        { "vec4", "mc2_RegionOffset" },
        { "vec4", "mc2_CameraBlock" },
        { "vec4", "mc2_CameraOffset" },
        { "vec4", "mc2_SwayEdge" },
        { "vec4", "mc2_Weather" },
        { "float", "mc2_GameTime" },
        { "float", "mc2_SwayIntensity" },
        { "float", "mc2_CalmSway" },
    };
    /** The fields of the mod's own blocks in order: the matrix and the sway's, and the fragment shader's. */
    static final String[][] VERTEX_FIELDS = withMatrix();
    static final String[][] FRAGMENT_FIELDS = {
        { "vec4", "FogColor" },
        { "float", "FogStart" },
        { "float", "FogEnd" },
        { "float", "mc2_AlphaCutout" },
    };
    static final int VERTEX_BINDING = 0;
    static final int CELLS_BINDING = 1;
    static final int FRAGMENT_BINDING = 2;
    static final int TEXTURE_BINDING = 3;
    static final int LIGHTMAP_BINDING = 4;
    /**
     * A vertex moved by the sway: its position relative to the camera, the region's corner being relative to it, with
     * the camera's block handed over as floats.
     */
    static final String SWAYED =
        "mc2_sway(Position + mc2_RegionOffset.xyz, mc2_RegionOffset.xyz, " +
        "ivec3(mc2_CameraBlock.xyz), mc2_CameraOffset.xyz, mc2_GameTime, SwayCell, SwayWeights)";

    private VulkanFoliageShader() {}

    private static String[][] withMatrix() {
        String[][] fields = new String[SWAY_FIELDS.length + 1][];
        fields[0] = new String[] { "mat4", "MVP" };
        System.arraycopy(SWAY_FIELDS, 0, fields, 1, SWAY_FIELDS.length);
        return fields;
    }

    /** The fields of the block of pushes, in order: a count, the two bounds, the positions and the forces. */
    static String[][] cellFields() {
        String[][] fields = new String[3 + CELL_MATRICES * 2][];
        fields[0] = new String[] { "int", "mc2_CellCount" };
        fields[1] = new String[] { "vec4", "mc2_CellBoundsMin" };
        fields[2] = new String[] { "vec4", "mc2_CellBoundsMax" };
        for (int i = 0; i < CELL_MATRICES; i++) {
            fields[3 + i] = new String[] { "mat4", "mc2_CellPosition" + i };
            fields[3 + CELL_MATRICES + i] = new String[] {
                "mat4",
                "mc2_CellForce" + i,
            };
        }
        return fields;
    }

    /**
     * Everything sway.glsl reads, declared for a vertex shader, and sway.glsl after it: a block of the fields given at
     * one binding, the pushes at another, and the two functions the pushes are read through.
     */
    static String swayDeclarations(
        String sway,
        String blockName,
        String[][] fields,
        int binding,
        int cellsBinding
    ) {
        StringBuilder source = new StringBuilder();
        block(source, binding, blockName, fields);
        block(source, cellsBinding, "FoliageInteraction", cellFields());
        source
            .append("#define MC2_MAX_CELLS ")
            .append(CELL_MATRICES * 4)
            .append('\n');
        source.append("#define MC2_NO_UNIFORM_ARRAYS\n\n");
        cells(source, "mc2_cellPosition", "mc2_CellPosition");
        cells(source, "mc2_cellForce", "mc2_CellForce");
        source.append(sway.replace("\r\n", "\n")).append('\n');
        return source.toString();
    }

    /**
     * The vertex shader. VulkanMod numbers a vertex's inputs by their place in its format, so the place of the first of
     * the sway's two values is handed over; the block format's own come first on every version.
     */
    static String vertex(String sway, int swayLocation) {
        StringBuilder source = new StringBuilder(
            """
            #version 450

            #include "light.glsl"
            #include "fog.glsl"

            """
        );
        source
            .append("layout(binding = ")
            .append(LIGHTMAP_BINDING)
            .append(") uniform sampler2D Sampler2;\n\n");
        source.append(
            """
            layout(location = 0) in vec3 Position;
            layout(location = 1) in vec4 Color;
            layout(location = 2) in vec2 UV0;
            layout(location = 3) in ivec2 UV2;
            """
        );
        source
            .append("layout(location = ")
            .append(swayLocation)
            .append(") in float SwayCell;\n");
        source
            .append("layout(location = ")
            .append(swayLocation + 1)
            .append(") in float SwayWeights;\n\n");
        source.append(
            swayDeclarations(
                sway,
                "MC2Foliage",
                VERTEX_FIELDS,
                VERTEX_BINDING,
                CELLS_BINDING
            )
        );
        source.append(
            """

            layout(location = 0) out vec4 vertexColor;
            layout(location = 1) out vec2 texCoord0;
            layout(location = 2) out float vertexDistance;

            void main() {
            """
        );
        source.append("    vec3 pos = ").append(SWAYED).append(";\n");
        source.append(
            """
                gl_Position = MVP * vec4(pos, 1.0);

                vertexDistance = fog_distance(pos, 0);
                // The light is the low eight bits of each number: under Beryl a block's id is written above it.
                vertexColor = Color * sample_lightmap(Sampler2, UV2 & 255);
                texCoord0 = UV0;
            }
            """
        );
        return source.toString();
    }

    static String fragment() {
        StringBuilder source = new StringBuilder(
            """
            #version 450

            #include "fog.glsl"

            """
        );
        source
            .append("layout(binding = ")
            .append(TEXTURE_BINDING)
            .append(") uniform sampler2D Sampler0;\n\n");
        block(source, FRAGMENT_BINDING, "MC2FoliageFog", FRAGMENT_FIELDS);
        source.append(
            """
            layout(location = 0) in vec4 vertexColor;
            layout(location = 1) in vec2 texCoord0;
            layout(location = 2) in float vertexDistance;

            layout(location = 0) out vec4 fragColor;

            void main() {
                vec4 color = texture(Sampler0, texCoord0) * vertexColor;
                if (color.a < mc2_AlphaCutout) {
                    discard;
                }
                fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
            }
            """
        );
        return source.toString();
    }

    private static void block(
        StringBuilder source,
        int binding,
        String name,
        String[][] fields
    ) {
        source
            .append("layout(binding = ")
            .append(binding)
            .append(") uniform ")
            .append(name)
            .append(" {\n");
        for (String[] field : fields) {
            source
                .append("    ")
                .append(field[0])
                .append(' ')
                .append(field[1])
                .append(";\n");
        }
        source.append("};\n\n");
    }

    /** One of the two functions sway.glsl reads the cells through: the matrix a cell is in, and its column there. */
    private static void cells(
        StringBuilder source,
        String function,
        String field
    ) {
        source
            .append("vec4 ")
            .append(function)
            .append(
                "(int i) {\n    int column = i & 3;\n    switch (i >> 2) {\n"
            );
        for (int i = 0; i < CELL_MATRICES; i++) {
            source
                .append("        case ")
                .append(i)
                .append(": return ")
                .append(field)
                .append(i)
                .append("[column];\n");
        }
        source.append("    }\n    return vec4(0.0);\n}\n\n");
    }
}
