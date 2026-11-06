package io.github.andrewcwhy.allthingsfoliage.gpu;

import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Supplier;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

/**
 * Draws the foliage through VulkanMod itself, for the versions where it stands in for OpenGL call by call (before
 * 1.21.11). There the renderer draws with OpenGL calls of its own -- a block of uniforms bound by hand, a draw from a
 * place in a buffer -- which VulkanMod does not stand in for, so the foliage could not be drawn at all.
 * <p>
 * VulkanMod draws everything of its own through one small interface, and that is what is used here, as its own vertex
 * buffers use it: a pipeline is bound, its textures and uniforms with it, and the buffer is drawn. The pipeline is
 * built as VulkanMod builds its own, from the shaders in {@link VulkanFoliageShader}. Its uniforms are named fields,
 * which VulkanMod fills each time the pipeline is bound from a supplier it looks up by the field's name; the mod puts
 * suppliers of its own beside VulkanMod's, each handing over a few floats the mod writes before it draws.
 * <p>
 * A region is still uploaded as a vertex buffer of the game's, which VulkanMod stands in for with a buffer of its own;
 * that buffer is what is drawn from. VulkanMod's own draw call always starts at a buffer's first vertex, and a region
 * is drawn in runs of visible sections, so the two Vulkan commands that draw a run are recorded directly, into the
 * command buffer VulkanMod is recording the frame in.
 * <p>
 * VulkanMod is not compiled against: everything is reached by name, on 0.6.7. Whatever is not found, or does not
 * compile, leaves the GPU renderer out, and the foliage in VulkanMod's chunk meshes.
 */
final class VulkanLegacyDraw {

    private static final String VULKAN = "net.vulkanmod.vulkan.";
    /** Vertex and fragment shader stages, as Vulkan numbers them. */
    private static final int VERTEX_STAGE = 1;
    private static final int FRAGMENT_STAGE = 16;
    private static final int TRIANGLES = 4;
    /** The camera's block is handed over as floats, which hold this much of it exactly; the sway wraps it lower still. */
    private static final int BLOCK_BITS = 0xFFFFF;

    /** The five vectors of the vertex block, in the order VulkanFoliageShader names them after the matrix. */
    private static final ByteBuffer VECTORS = ByteBuffer.allocateDirect(
        5 * 16
    ).order(ByteOrder.nativeOrder());
    private static final int REGION_OFFSET = 0;
    private static final int CAMERA_BLOCK = 16;
    private static final int CAMERA_OFFSET = 32;
    private static final int SWAY_EDGE = 48;
    private static final int WEATHER = 64;
    private static float swayIntensity;
    private static float calmSway;
    private static float gameTime;
    private static float alphaCutout;
    private static ByteBuffer cells;

    /** The mod's own pipeline, and the one the runs are drawn with: that, Beryl's lighting, or its shadow map's. */
    private static Object pipeline;
    private static Object drawn;
    private static Object format;
    private static int normalAt;
    private static int swayAt;
    private static boolean failed;
    private static final LongBuffer BUFFER = MemoryUtil.memAllocLong(1);
    private static final LongBuffer OFFSET = MemoryUtil.memAllocLong(1);

    private VulkanLegacyDraw() {}

    /**
     * Whether the foliage can be drawn through VulkanMod, building the pipeline the first time. Render thread only.
     *
     * @param vertexFormat the format of the foliage's vertices
     * @param normalLocation the place of the normal among its elements, or -1 where it has none
     * @param swayLocation the place of the first of the sway's two values among them
     * @param cellData the pushes, laid out as the block of them is: a count, then vectors from byte 16 on
     */
    static boolean ready(
        Object vertexFormat,
        int normalLocation,
        int swayLocation,
        ByteBuffer cellData
    ) {
        if (failed) {
            return false;
        }
        if (pipeline != null) {
            return true;
        }
        try {
            cells = cellData;
            format = vertexFormat;
            normalAt = normalLocation;
            swayAt = swayLocation;
            Names.registerSuppliers();
            pipeline = Names.build(vertexFormat, swayLocation);
            return true;
        } catch (Throwable e) {
            failed = true;
            AllThingsFoliage.LOGGER.warn(
                "The GPU foliage renderer can't draw through VulkanMod; foliage is left to its chunk " +
                    "meshes",
                e
            );
            return false;
        }
    }

    /** Whether the frame has been drawn through VulkanMod before, so there is everything to draw a shadow map with. */
    static boolean built() {
        return pipeline != null && !failed;
    }

    /** Whether drawing through VulkanMod was tried and cannot be done, so the renderer stays out. */
    static boolean failed() {
        return failed;
    }

    /**
     * What every run of the frame shares: the matrices, the camera, the time and the sway's settings. The pipeline is
     * picked here too: Beryl's lighting while Beryl draws the terrain with its shaders, the mod's own otherwise.
     * <p>
     * Into Beryl's shadow map -- no matrices handed over -- the pipeline is Beryl's for that or there is nothing to
     * draw with, which is what is returned. It places a vertex by the light's matrix, which Beryl feeds it, and
     * VulkanMod's own are left as Beryl set them, as is the render state its terrain was just drawn with.
     */
    static boolean begin(
        Matrix4f modelView,
        Matrix4f projection,
        int cameraBlockX,
        int cameraBlockY,
        int cameraBlockZ,
        float cameraOffsetX,
        float cameraOffsetY,
        float cameraOffsetZ,
        float time,
        float intensity,
        float calm,
        float edgeX,
        float edgeY,
        float edgeZ,
        float edgeW,
        float weatherX,
        float weatherY,
        float weatherZ,
        float weatherW
    ) {
        put(
            CAMERA_BLOCK,
            cameraBlockX & BLOCK_BITS,
            cameraBlockY & BLOCK_BITS,
            cameraBlockZ & BLOCK_BITS,
            0.0F
        );
        put(CAMERA_OFFSET, cameraOffsetX, cameraOffsetY, cameraOffsetZ, 0.0F);
        put(SWAY_EDGE, edgeX, edgeY, edgeZ, edgeW);
        put(WEATHER, weatherX, weatherY, weatherZ, weatherW);
        swayIntensity = intensity;
        calmSway = calm;
        gameTime = time;
        if (modelView == null) {
            drawn = BerylBridge.legacyShadow(format, normalAt, swayAt);
            return drawn != null;
        }
        Object beryls = BerylBridge.legacyLit(format, normalAt, swayAt);
        drawn = beryls != null ? beryls : pipeline;
        try {
            matrices(modelView, projection);
            Names.SET_TOPOLOGY.invoke(null, TRIANGLES);
        } catch (Throwable e) {
            fail(e);
        }
        return !failed;
    }

    /** What the fragment shader discards below, for the runs that follow: the cutout layer's, or nothing. */
    static void cutout(float below) {
        alphaCutout = below;
    }

    /**
     * Binds the pipeline for a region: where its corner is from the camera, and the buffer its vertices are in, which
     * is the game's vertex buffer the region was uploaded into.
     */
    static void region(
        Object vertexBuffer,
        float x,
        float y,
        float z,
        int longestRunVertices
    ) {
        if (failed) {
            return;
        }
        put(REGION_OFFSET, x, y, z, 0.0F);
        try {
            Object renderer = Names.RENDERER.invoke(null);
            Names.BIND_PIPELINE.invoke(renderer, drawn);
            Names.BIND_TEXTURES.invoke(null, drawn);
            Names.BIND_UNIFORMS.invoke(renderer, drawn);

            Object buffer = Names.vulkanBuffer(vertexBuffer);
            Object commands = Names.COMMANDS.invoke(null);
            BUFFER.put(0, (long) Names.BUFFER_ID.invoke(buffer));
            OFFSET.put(0, (long) Names.BUFFER_OFFSET.invoke(buffer));
            Names.BIND_VERTICES.invoke(null, commands, 0, BUFFER, OFFSET);

            // VulkanMod's shared indices for quads, grown to reach the longest run: a run counts from its own start.
            Object drawer = Names.DRAWER.invoke(null);
            Object quads = Names.QUAD_INDICES.invoke(drawer);
            Names.CHECK_CAPACITY.invoke(quads, longestRunVertices);
            Object indices = Names.INDEX_BUFFER.invoke(quads);
            int indexType = Names.INDEX_TYPE_VALUE.getInt(
                Names.INDEX_TYPE.get(indices)
            );
            Names.BIND_INDICES.invoke(drawer, commands, indices, indexType);
        } catch (Throwable e) {
            fail(e);
        }
    }

    /** Draws a run of the region bound last: so many indices, counted from the vertex it starts at. */
    static void run(int firstVertex, int indexCount) {
        if (failed) {
            return;
        }
        try {
            Names.DRAW_INDEXED.invoke(
                null,
                Names.COMMANDS.invoke(null),
                indexCount,
                1,
                0,
                firstVertex,
                0
            );
        } catch (Throwable e) {
            fail(e);
        }
    }

    /** Hands VulkanMod back the matrices the game holds, as its own vertex buffers do once they have drawn. */
    static void end(Matrix4f modelView, Matrix4f projection) {
        try {
            matrices(modelView, projection);
        } catch (Throwable e) {
            fail(e);
        }
    }

    /**
     * The pipeline the foliage is lit by Beryl with on these versions, built as the mod's own is: Beryl's two blocks,
     * which VulkanMod fills by their fields' names, the two the sway reads at the two bindings after Beryl's textures,
     * and those textures. The sway's values are the ones the mod's own pipeline is handed.
     */
    static Object berylPipeline(
        Object vertexFormat,
        Object[] berylBlocks,
        int firstFreeBinding,
        Object[][] textures,
        String vertex,
        String fragment
    ) throws ReflectiveOperationException {
        Object[] blocks = new Object[berylBlocks.length + 2];
        System.arraycopy(berylBlocks, 0, blocks, 0, berylBlocks.length);
        blocks[berylBlocks.length] = Names.block(
            firstFreeBinding,
            VERTEX_STAGE,
            VulkanFoliageShader.SWAY_FIELDS
        );
        blocks[berylBlocks.length + 1] = Names.block(
            firstFreeBinding + 1,
            VERTEX_STAGE,
            VulkanFoliageShader.cellFields()
        );
        return Names.pipeline(
            vertexFormat,
            "mc2_foliage_beryl",
            blocks,
            textures,
            vertex,
            fragment
        );
    }

    /** A block of named fields for a pipeline of Beryl's that Beryl itself is told in a file. */
    static Object block(int binding, int stage, String[][] fields)
        throws ReflectiveOperationException {
        return Names.block(binding, stage, fields);
    }

    /** sway.glsl, and what it reads declared before it, for the copy of one of Beryl's vertex shaders. */
    static String swayDeclarations(int firstFreeBinding) {
        return VulkanFoliageShader.swayDeclarations(
            Names.readSway(),
            "MC2FoliageSway",
            VulkanFoliageShader.SWAY_FIELDS,
            firstFreeBinding,
            firstFreeBinding + 1
        );
    }

    /** The three VulkanMod keeps for its pipelines: Beryl's shaders read the view on its own beside the two together. */
    private static void matrices(Matrix4f modelView, Matrix4f projection)
        throws ReflectiveOperationException {
        Names.APPLY_MODEL_VIEW.invoke(null, modelView);
        Names.APPLY_PROJECTION.invoke(null, projection);
        Names.APPLY_MVP.invoke(null, modelView, projection);
    }

    private static void put(int at, float x, float y, float z, float w) {
        VECTORS.putFloat(at, x)
            .putFloat(at + 4, y)
            .putFloat(at + 8, z)
            .putFloat(at + 12, w);
    }

    private static void fail(Throwable cause) {
        if (!failed) {
            failed = true;
            AllThingsFoliage.LOGGER.warn(
                "The GPU foliage renderer can't draw through VulkanMod; foliage is left to its chunk " +
                    "meshes",
                cause
            );
        }
    }

    /** Found the first time the foliage is drawn with VulkanMod installed, when its classes are certainly loaded. */
    private static final class Names {

        static final Method RENDERER;
        static final Method DRAWER;
        static final Method COMMANDS;
        static final Method BIND_PIPELINE;
        static final Method BIND_UNIFORMS;
        static final Method BIND_TEXTURES;
        static final Method APPLY_MVP;
        static final Method APPLY_MODEL_VIEW;
        static final Method APPLY_PROJECTION;
        static final Method SET_TOPOLOGY;
        static final Method QUAD_INDICES;
        static final Method CHECK_CAPACITY;
        static final Method INDEX_BUFFER;
        static final Field INDEX_TYPE;
        static final Field INDEX_TYPE_VALUE;
        static final Method BIND_INDICES;
        static final Method BUFFER_ID;
        static final Method BUFFER_OFFSET;
        static final Method BIND_VERTICES;
        static final Method DRAW_INDEXED;
        static final Class<?> VBO;
        static final Class<?> VERTEX_BUFFER;
        private static Field vboOfVertexBuffer;
        private static Field bufferOfVbo;

        static {
            try {
                Class<?> renderer = Class.forName(VULKAN + "Renderer");
                Class<?> drawer = Class.forName(VULKAN + "Drawer");
                Class<?> graphics = Class.forName(
                    VULKAN + "shader.GraphicsPipeline"
                );
                Class<?> anyPipeline = Class.forName(
                    VULKAN + "shader.Pipeline"
                );
                Class<?> system = Class.forName(VULKAN + "VRenderSystem");
                Class<?> buffer = Class.forName(
                    VULKAN + "memory.buffer.Buffer"
                );
                Class<?> indexBuffer = Class.forName(
                    VULKAN + "memory.buffer.IndexBuffer"
                );
                Class<?> quads = Class.forName(
                    VULKAN + "memory.buffer.index.AutoIndexBuffer"
                );
                Class<?> commands = Class.forName(
                    "org.lwjgl.vulkan.VkCommandBuffer"
                );
                Class<?> vulkan = Class.forName("org.lwjgl.vulkan.VK10");
                RENDERER = renderer.getMethod("getInstance");
                DRAWER = renderer.getMethod("getDrawer");
                COMMANDS = renderer.getMethod("getCommandBuffer");
                BIND_PIPELINE = renderer.getMethod(
                    "bindGraphicsPipeline",
                    graphics
                );
                BIND_UNIFORMS = renderer.getMethod(
                    "uploadAndBindUBOs",
                    anyPipeline
                );
                BIND_TEXTURES = Class.forName(
                    VULKAN + "texture.VTextureSelector"
                ).getMethod("bindShaderTextures", anyPipeline);
                APPLY_MVP = system.getMethod(
                    "applyMVP",
                    Matrix4f.class,
                    Matrix4f.class
                );
                APPLY_MODEL_VIEW = system.getMethod(
                    "applyModelViewMatrix",
                    Matrix4f.class
                );
                APPLY_PROJECTION = system.getMethod(
                    "applyProjectionMatrix",
                    Matrix4f.class
                );
                SET_TOPOLOGY = system.getMethod(
                    "setPrimitiveTopologyGL",
                    int.class
                );
                QUAD_INDICES = drawer.getMethod("getQuadsIndexBuffer");
                CHECK_CAPACITY = quads.getMethod("checkCapacity", int.class);
                INDEX_BUFFER = quads.getMethod("getIndexBuffer");
                INDEX_TYPE = indexBuffer.getField("indexType");
                INDEX_TYPE_VALUE = INDEX_TYPE.getType().getDeclaredField(
                    "value"
                );
                INDEX_TYPE_VALUE.setAccessible(true);
                BIND_INDICES = drawer.getMethod(
                    "bindIndexBuffer",
                    commands,
                    buffer,
                    int.class
                );
                BUFFER_ID = buffer.getMethod("getId");
                BUFFER_OFFSET = buffer.getMethod("getOffset");
                BIND_VERTICES = vulkan.getMethod(
                    "vkCmdBindVertexBuffers",
                    commands,
                    int.class,
                    LongBuffer.class,
                    LongBuffer.class
                );
                DRAW_INDEXED = vulkan.getMethod(
                    "vkCmdDrawIndexed",
                    commands,
                    int.class,
                    int.class,
                    int.class,
                    int.class,
                    int.class
                );
                VBO = Class.forName("net.vulkanmod.render.VBO");
                VERTEX_BUFFER = Class.forName(
                    VULKAN + "memory.buffer.VertexBuffer"
                );
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        /**
         * VulkanMod's buffer behind one of the game's vertex buffers: it keeps an object of its own in each, which
         * holds the buffer the vertices were uploaded into. Both are found by what they are rather than by a name.
         */
        static Object vulkanBuffer(Object vertexBuffer)
            throws ReflectiveOperationException {
            if (vboOfVertexBuffer == null) {
                vboOfVertexBuffer = fieldOf(vertexBuffer.getClass(), VBO);
                bufferOfVbo = fieldOf(VBO, VERTEX_BUFFER);
            }
            return bufferOfVbo.get(vboOfVertexBuffer.get(vertexBuffer));
        }

        private static Field fieldOf(Class<?> owner, Class<?> type)
            throws NoSuchFieldException {
            for (Field field : owner.getDeclaredFields()) {
                if (field.getType() == type) {
                    field.setAccessible(true);
                    return field;
                }
            }
            throw new NoSuchFieldException(
                type.getName() + " in " + owner.getName()
            );
        }

        /** Puts the mod's values beside VulkanMod's own, each under the name of the field that reads it. */
        @SuppressWarnings({ "unchecked", "rawtypes" })
        static void registerSuppliers() throws ReflectiveOperationException {
            Class<?> uniforms = Class.forName(VULKAN + "shader.Uniforms");
            Method mapped = Class.forName(
                VULKAN + "util.MappedBuffer"
            ).getMethod("createFromBuffer", ByteBuffer.class);
            Map vectors = (Map) uniforms.getField("vec4f_uniformMap").get(null);
            Map matrices = (Map) uniforms
                .getField("mat4f_uniformMap")
                .get(null);
            Map floats = (Map) uniforms.getField("vec1f_uniformMap").get(null);
            Map integers = (Map) uniforms
                .getField("vec1i_uniformMap")
                .get(null);

            vectors.put(
                "mc2_RegionOffset",
                fixed(mapped, VECTORS, REGION_OFFSET, 16)
            );
            vectors.put(
                "mc2_CameraBlock",
                fixed(mapped, VECTORS, CAMERA_BLOCK, 16)
            );
            vectors.put(
                "mc2_CameraOffset",
                fixed(mapped, VECTORS, CAMERA_OFFSET, 16)
            );
            vectors.put("mc2_SwayEdge", fixed(mapped, VECTORS, SWAY_EDGE, 16));
            vectors.put("mc2_Weather", fixed(mapped, VECTORS, WEATHER, 16));
            floats.put("mc2_GameTime", (Supplier<Float>) () -> gameTime);
            floats.put(
                "mc2_SwayIntensity",
                (Supplier<Float>) () -> swayIntensity
            );
            floats.put("mc2_CalmSway", (Supplier<Float>) () -> calmSway);
            floats.put("mc2_AlphaCutout", (Supplier<Float>) () -> alphaCutout);

            // The pushes: a count, two bounds from byte 16 on, then the positions and the forces, four to a matrix.
            ByteBuffer data = cells;
            integers.put(
                "mc2_CellCount",
                (Supplier<Integer>) () -> data.getInt(0)
            );
            vectors.put("mc2_CellBoundsMin", fixed(mapped, data, 16, 16));
            vectors.put("mc2_CellBoundsMax", fixed(mapped, data, 32, 16));
            int positions = 48;
            int forces = positions + VulkanFoliageShader.CELL_MATRICES * 64;
            for (int i = 0; i < VulkanFoliageShader.CELL_MATRICES; i++) {
                matrices.put(
                    "mc2_CellPosition" + i,
                    fixed(mapped, data, positions + i * 64, 64)
                );
                matrices.put(
                    "mc2_CellForce" + i,
                    fixed(mapped, data, forces + i * 64, 64)
                );
            }
        }

        /** A supplier of VulkanMod's view of a stretch of a buffer the mod writes into. */
        private static Supplier<Object> fixed(
            Method mapped,
            ByteBuffer data,
            int at,
            int bytes
        ) throws ReflectiveOperationException {
            Object view = mapped.invoke(
                null,
                data.slice(at, bytes).order(ByteOrder.nativeOrder())
            );
            return () -> view;
        }

        static Object build(Object vertexFormat, int swayLocation)
            throws ReflectiveOperationException {
            Object[] blocks = {
                block(
                    VulkanFoliageShader.VERTEX_BINDING,
                    VERTEX_STAGE,
                    VulkanFoliageShader.VERTEX_FIELDS
                ),
                block(
                    VulkanFoliageShader.CELLS_BINDING,
                    VERTEX_STAGE,
                    VulkanFoliageShader.cellFields()
                ),
                block(
                    VulkanFoliageShader.FRAGMENT_BINDING,
                    FRAGMENT_STAGE,
                    VulkanFoliageShader.FRAGMENT_FIELDS
                ),
            };
            Object[][] textures = {
                { VulkanFoliageShader.TEXTURE_BINDING, "Sampler0", 0 },
                { VulkanFoliageShader.LIGHTMAP_BINDING, "Sampler2", 2 },
            };
            return pipeline(
                vertexFormat,
                "mc2_foliage",
                blocks,
                textures,
                VulkanFoliageShader.vertex(readSway(), swayLocation),
                VulkanFoliageShader.fragment()
            );
        }

        /**
         * A pipeline as VulkanMod builds its own: its blocks of named fields in the order of their bindings, its
         * textures as a binding, the name VulkanMod knows the texture by and the slot it is bound at, and its shaders.
         */
        static Object pipeline(
            Object vertexFormat,
            String name,
            Object[] blocks,
            Object[][] textures,
            String vertex,
            String fragment
        ) throws ReflectiveOperationException {
            String shader = VULKAN + "shader.";
            Object config = Class.forName(shader + "PipelineConfig")
                .getMethod("builder")
                .invoke(null);
            Method addBlock = config
                .getClass()
                .getMethod(
                    "addUB",
                    Class.forName(shader + "PipelineConfig$UB")
                );
            for (Object block : blocks) {
                addBlock.invoke(config, block);
            }
            Method addTexture = config
                .getClass()
                .getMethod(
                    "addImageDescriptor",
                    int.class,
                    String.class,
                    String.class,
                    int.class
                );
            for (Object[] texture : textures) {
                addTexture.invoke(
                    config,
                    texture[0],
                    "sampler2D",
                    texture[1],
                    texture[2]
                );
            }
            Object built = config.getClass().getMethod("build").invoke(config);

            Class<?> builderClass = Class.forName(shader + "Pipeline$Builder");
            Object builder = null;
            for (var constructor : builderClass.getConstructors()) {
                Class<?>[] parameters = constructor.getParameterTypes();
                if (parameters.length == 2 && parameters[1] == String.class) {
                    builder = constructor.newInstance(vertexFormat, name);
                }
            }
            if (builder == null) {
                throw new NoSuchMethodException(
                    "Pipeline.Builder(VertexFormat, String)"
                );
            }
            builderClass
                .getMethod("applyConfig", built.getClass())
                .invoke(builder, built);
            Class<?> kind = Class.forName(shader + "SPIRVUtils$ShaderKind");
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

        static Object block(int binding, int stage, String[][] fields)
            throws ReflectiveOperationException {
            Class<?> block = Class.forName(VULKAN + "shader.PipelineConfig$UB");
            Object builder = block
                .getMethod("builder", int.class, int.class)
                .invoke(null, binding, stage);
            Method add = builder
                .getClass()
                .getMethod("addUniform", String.class, String.class);
            for (String[] field : fields) {
                add.invoke(builder, field[0], field[1]);
            }
            return builder.getClass().getMethod("build").invoke(builder);
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        private static Object enumValue(Class<?> type, String name) {
            return Enum.valueOf((Class) type, name);
        }

        static String readSway() {
            String path =
                "/assets/" +
                AllThingsFoliage.MOD_ID +
                "/shaders/include/sway.glsl";
            try (
                InputStream in = VulkanLegacyDraw.class.getResourceAsStream(
                    path
                )
            ) {
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
