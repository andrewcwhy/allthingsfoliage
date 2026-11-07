package io.github.andrewcwhy.allthingsfoliage.gpu.shader;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.mojang.renderpearl.api.vertex.VertexFormatElement;
import com.mojang.renderpearl.backend.api.BackendRenderPipeline;
import com.mojang.renderpearl.backend.opengl.GlBuffer;
import com.mojang.renderpearl.backend.opengl.GlProgram;
import com.mojang.renderpearl.backend.opengl.GlRenderPipeline;
import com.mojang.renderpearl.backend.opengl.GlStateManager;
import com.mojang.renderpearl.backend.opengl.VertexArray;
import com.mojang.renderpearl.frontend.FrontendRenderPipeline;
import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage;
import io.github.andrewcwhy.allthingsfoliage.gpu.compat.IrisCompat;
import io.github.andrewcwhy.allthingsfoliage.mixin.compat.iris.IrisRenderingPipelineAccessor;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.api.v0.IrisProgram;
import net.irisshaders.iris.api.v0.IrisShadowProgram;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.mixinterface.GlRenderPipelineAccess;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPhase;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.pipeline.programs.ShaderKey;
import net.irisshaders.iris.pipeline.programs.ShaderSupplier;
import net.irisshaders.iris.pipeline.transform.Patch;
import net.irisshaders.iris.shaderpack.loading.ProgramId;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import net.minecraft.world.level.block.state.BlockState;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GL;

/**
 * Draws the GPU renderer's foliage through a loaded shader pack, so plants sway and get pushed exactly as without one
 * while the pack lights, shades and shadows them.
 * <p>
 * Iris replaces the program of every pipeline it knows with one of the pack's. The renderer's own pipeline is not one
 * of them, so for each pack the mod builds two programs of its own out of the pack's sources, through the very route
 * Iris builds the pack's programs: its terrain cutout program and its shadow cutout program. Both are copies; the pack's
 * own programs are never touched. After Iris has translated the vertex shader, the position the pack reads --
 * {@code iris_Position}, whether the pack wrote {@code gl_Vertex} or {@code vaPosition} -- is swapped for the same
 * position moved by {@code sway.glsl}, the file the mod's own vertex shader moves plants with. The fragment shader is
 * left as the pack wrote it.
 * <p>
 * Everything here reaches into Iris itself, so any step that goes wrong is caught, logged once, and leaves the
 * foliage to the chunk mesh until the next pack loads; the pack itself keeps working either way.
 */
public final class IrisFoliageShaders {

    /** Iris's terrain format, which a shader pack's terrain program reads, with the renderer's two values after it. */
    public static final VertexFormat FORMAT = format();

    /** Set while a program of the mod's is being built, so the hooks inside Iris change that one and no other. */
    private static final ThreadLocal<Boolean> BUILDING =
        ThreadLocal.withInitial(() -> false);

    private static final Pattern POSITION_INPUT = Pattern.compile(
        "\\bin\\s+vec3\\s+iris_Position\\s*;"
    );
    private static final Pattern POSITION = Pattern.compile(
        "\\biris_Position\\b"
    );
    private static final Pattern MAIN = Pattern.compile(
        "\\bvoid\\s+main\\s*\\(\\s*(?:void\\s*)?\\)"
    );
    /** What the pack reads in place of its position: the same position, moved. */
    private static final String SWAYED_POSITION = "mc2_swayedPosition";

    /** Whether the renderer has handed over what drawing through a pack takes; nothing is built before. */
    private static boolean configured;

    private static RenderPipeline pipeline;
    private static RenderPipeline shadowPipeline;
    /** Bind group layouts from 26.2, uniform descriptions before: whatever the version declares a program's uniforms with. */
    private static List<?> layouts = List.of();

    private static IrisRenderingPipeline owner;

    private static GlProgram main;
    private static GlProgram shadow;

    /** Bumped for every pack loaded, so sections meshed for the last one are meshed again with this one's block ids. */
    private static int generation;
    private static IrisCompat.ShadowDraw foliageShadowDraw;

    private static boolean shadowCallbackRegistered;
    /**
     * Set when the Iris installed lacks what the renderer hands its foliage over to it by -- Iris 1.11.2 on 26.2 has no
     * shadow callback yet -- and the foliage is then left to the chunk mesh while a pack is loaded, as where the pack's
     * programs could not be built.
     */
    private static boolean irisTooOld;

    /**
     * The renderer's pipeline for leaves drawn solid, as the game's Fast leaves are, and the mod's copies of the pack's
     * solid terrain program and solid shadow program, which keep every pixel; null where the pack has none or they could
     * not be built, and the leaves are then left to the chunk mesh in Fast.
     */
    private static RenderPipeline opaquePipeline;
    private static GlProgram mainSolid;
    private static GlProgram shadowSolid;

    /** Whether Iris has been told to draw the renderer's pipeline with the pack's program; see setUp. */
    private static boolean assigned;
    /**
     * The pipelines the renderer draws through the pack with: each of Iris's for the renderer's pipeline, with the
     * mod's program in place of the pack's, and the vertex arrays made for them. A pipeline deletes its program when
     * closed, and every one of these shares the one program, so they are never closed themselves: their vertex arrays
     * are, and the program once, in closePrograms.
     */
    private static final Map<
        CompiledRenderPipeline,
        FrontendRenderPipeline
    > WITH_FOLIAGE_PROGRAM = new IdentityHashMap<>();
    private static final List<VertexArray> VERTEX_ARRAYS = new ArrayList<>();
    /**
     * Where the programs' sway blocks are bound: the last two points the driver has, left to the mod. Nothing registers
     * binding points, so none can be asked for free; vanilla's pipelines and Iris's programs count from the first.
     */
    private static int swayBinding = -1;
    private static int interactionBinding = -1;

    private IrisFoliageShaders() {}

    private static VertexFormat format() {

        VertexFormat.Builder builder = VertexFormat.builder(0);
        for (VertexFormatElement element : IrisVertexFormats.TERRAIN.getElements()) {
            builder.addAttribute(element.name(), element.format());
        }
        return builder
            .addAttribute("SwayCell", GpuFormat.R32_FLOAT)
            .addAttribute("SwayWeights", GpuFormat.R32_FLOAT)
            .build();

    }

    /**
     * The renderer's pipelines for drawing through a shader pack -- in the pack's own pass and into its shadow map -- and
     * the uniform blocks they bind beyond Iris's own: the sway settings and the plant pushes.
     */

    public static void setUp(
        RenderPipeline foliagePipeline,
        RenderPipeline foliageShadowPipeline,
        List<?> foliageLayouts,
        IrisCompat.ShadowDraw shadowDraw
    ) {
        pipeline = foliagePipeline;
        shadowPipeline = foliageShadowPipeline;
        layouts = List.copyOf(foliageLayouts);

        configured = true;
        foliageShadowDraw = shadowDraw;

        // On 26.3 Iris swaps the program of any pipeline it has been told about for the pack's own, as it does for the
        // terrain's, and builds the pipeline that draws it; the renderer then puts its own program in the pack's place in
        // that pipeline (see withFoliageProgram). Iris keeps the shadow pass's programs apart, so the same pipeline is
        // told about for both, and turns the depth test round itself there for every pipeline. Iris refuses a pipeline
        // told about twice.
        if (!assigned) {
            assigned = true;
            IrisApi.getInstance().assignPipeline(
                foliagePipeline,
                IrisProgram.TERRAIN_CUTOUT
            );
            IrisApi.getInstance().assignPipelineShadow(
                foliagePipeline,
                IrisShadowProgram.SHADOW_TERRAIN_CUTOUT
            );
        }

        if (!shadowCallbackRegistered) {
            shadowCallbackRegistered = true;
            try {
                // Called by Iris in its shadow pass once the terrain is in the shadow map, with the sun's matrices and the
                // camera it rendered from.
                IrisApi.getInstance().registerShadowRenderCallback(
                    (
                        modelView,
                        projection,
                        cameraX,
                        cameraY,
                        cameraZ,
                        tickDelta
                    ) ->
                        drawShadow(
                            modelView,
                            projection,
                            cameraX,
                            cameraY,
                            cameraZ
                        )
                );
            } catch (LinkageError e) {
                irisTooOld = true;
                AllThingsFoliage.logger.warn(
                    "This Iris is older than the GPU renderer needs to draw foliage through shader packs; " +
                        "while one is loaded, foliage near the player is left to the chunk mesh and does not sway. Update Iris " +
                        "for it to sway: {}",
                    e.toString()
                );
            }
        }

    }

    /**
     * The renderer's pipeline for leaves drawn solid: Iris is told to draw it with the pack's solid terrain program, in
     * whose place the mod's solid copy then goes, as for the cutout one. In the shadow pass Iris's state is the cutout
     * shadow's, which is all it names for terrain there; the program put in its place is the mod's solid shadow copy.
     */
    public static void setUpOpaque(RenderPipeline foliageOpaquePipeline) {
        if (opaquePipeline == null) {
            opaquePipeline = foliageOpaquePipeline;
            IrisApi.getInstance().assignPipeline(
                foliageOpaquePipeline,
                IrisProgram.TERRAIN_SOLID
            );
            IrisApi.getInstance().assignPipelineShadow(
                foliageOpaquePipeline,
                IrisShadowProgram.SHADOW_TERRAIN_CUTOUT
            );
        }
    }

    /** Whether the loaded pack's solid programs are built, so leaves can be drawn solid through it. */
    public static boolean hasOpaquePrograms() {
        return (
            owner != null &&
            mainSolid != null &&
            (shadow == null || shadowSolid != null)
        );
    }

    /**
     * Draws the foliage into the shadow map, in the middle of Iris's shadow pass once the terrain is in it. From 26.1.2
     * Iris calls this through its shadow render callback; before, it has none, and ShadowRendererMixin calls it at the same
     * point. The shadow pass comes before the terrain, where the programs are built: in the first frame of a pack loaded
     * again -- its settings changed -- the programs are still the last pack's, whose images Iris has destroyed, and
     * nothing is drawn.
     */
    public static void drawShadow(
        org.joml.Matrix4f modelView,
        org.joml.Matrix4f projection,
        double cameraX,
        double cameraY,
        double cameraZ
    ) {
        if (
            owner != null &&
            shadow != null &&
            foliageShadowDraw != null &&
            Iris.getPipelineManager().getPipelineNullable() == owner
        ) {
            foliageShadowDraw.draw(
                modelView,
                projection,
                cameraX,
                cameraY,
                cameraZ
            );
        }
    }

    /** Whether Iris is drawing the pack's shadow map right now. */
    public static boolean renderingShadows() {
        return IrisApi.getInstance().isRenderingShadowPass();
    }

    public static boolean shaderPackInUse() {
        return IrisApi.getInstance().isShaderPackInUse();
    }

    /**
     * Builds the programs for the pack loaded now, once per pack, and returns whether there are programs to draw with.
     * Called on the render thread each frame the renderer draws through a pack: a pack can load before the renderer
     * is ever used, and building here rather than as it loads never depends on which comes first.
     */
    public static boolean ready() {

        if (irisTooOld) {
            return false;
        }

        if (
            !configured ||
            !(Iris.getPipelineManager().getPipelineNullable() instanceof
                IrisRenderingPipeline current)
        ) {
            // The pack was turned off: its programs go with it.
            if (owner != null) {
                closePrograms();
                owner = null;
            }
            return false;
        }
        if (current != owner) {
            closePrograms();
            owner = current;
            generation++;
            build(current);
        }
        return main != null;
    }

    public static int generation() {
        return generation;
    }

    /** The program one of the renderer's pipelines draws with, or null for any other pipeline, left to Iris. */
    public static GlProgram programFor(RenderPipeline requested) {
        if (
            requested == null ||
            Iris.getPipelineManager().getPipelineNullable() != owner
        ) {
            return null;
        }
        if (requested == pipeline) {
            return main;
        }
        if (requested == opaquePipeline) {
            return mainSolid;
        }

        return requested == shadowPipeline ? shadow : null;
    }

    public static boolean hasShadowProgram() {
        return shadow != null;
    }

    /**
     * Tells Iris which block the next vertices belong to, as it is told for the chunk mesh, so the pack sees the same
     * block ids, light emission and block centres on foliage the renderer draws.
     */
    public static void beginBlock(
        BufferBuilder builder,
        BlockState state,
        int x,
        int y,
        int z
    ) {
        Object2IntMap<BlockState> ids =
            WorldRenderingSettings.INSTANCE.getBlockStateIds();
        if (
            ids != null &&
            (Object) builder instanceof BlockSensitiveBufferBuilder blocks
        ) {

            blocks.beginBlock(
                ids.getOrDefault(state, -1),
                (byte) 0,
                (byte) state.getLightEmission(),
                x,
                y,
                z
            );

        }
    }

    public static void endBlock(BufferBuilder builder) {
        if ((Object) builder instanceof BlockSensitiveBufferBuilder blocks) {
            blocks.endBlock();
        }
    }

    /** Marks what the renderer draws as terrain cutout for the pack, which some packs read to tell passes apart. */
    public static Object beginTerrainPhase() {
        WorldRenderingPipeline current =
            Iris.getPipelineManager().getPipelineNullable();
        if (current == null) {
            return null;
        }
        WorldRenderingPhase previous = current.getPhase();
        current.setPhase(WorldRenderingPhase.TERRAIN_CUTOUT);
        return previous;
    }

    /**
     * Inside {@link #beginTerrainPhase}: marks what is drawn next as solid terrain or cutout terrain for the pack, as the
     * chunk mesh's own layers are marked.
     */
    public static void setTerrainPhase(boolean solid) {
        WorldRenderingPipeline current =
            Iris.getPipelineManager().getPipelineNullable();
        if (current != null) {
            current.setPhase(
                solid
                    ? WorldRenderingPhase.TERRAIN_SOLID
                    : WorldRenderingPhase.TERRAIN_CUTOUT
            );
        }
    }

    /** Takes what {@link #beginTerrainPhase} returned, typed loosely so callers never name Iris's classes. */
    public static void endTerrainPhase(Object previous) {
        WorldRenderingPipeline current =
            Iris.getPipelineManager().getPipelineNullable();
        if (current != null && previous instanceof WorldRenderingPhase phase) {
            current.setPhase(phase);
        }
    }

    /** Builds the mod's two programs for a pack; on any failure leaves none, and says why once. */
    private static void build(IrisRenderingPipeline created) {
        IrisRenderingPipelineAccessor access =
            (IrisRenderingPipelineAccessor) created;
        BUILDING.set(true);
        try {
            Optional<ProgramSource> terrain = access
                .allthingsfoliage$resolver()
                .resolve(ProgramId.TerrainCutout);
            if (terrain.isEmpty()) {
                AllThingsFoliage.logger.warn(
                    "The shader pack has no terrain program; foliage near the player is left to " +
                        "the chunk mesh while it is loaded"
                );
                return;
            }

            main = finish(
                access.allthingsfoliage$createShader(
                    "mc2_foliage",
                    ShaderKey.TERRAIN_CUTOUT,
                    terrain.get(),
                    ProgramId.TerrainCutout,
                    ShaderKey.TERRAIN_CUTOUT.getAlphaTest(),
                    FORMAT,
                    ShaderKey.TERRAIN_CUTOUT.getFogMode(),
                    false,
                    false,
                    false,
                    false,
                    false,

                    Patch.VANILLA

                )
            );

            if (access.allthingsfoliage$shadowRenderTargets() != null) {
                Optional<ProgramSource> shadowSource = access
                    .allthingsfoliage$resolver()
                    .resolve(ProgramId.ShadowCutout);
                if (shadowSource.isPresent()) {

                    shadow = finish(
                        access.allthingsfoliage$createShadowShader(
                            "mc2_foliage_shadow",
                            ShaderKey.SHADOW_TERRAIN_CUTOUT,
                            shadowSource.get(),
                            ProgramId.ShadowCutout,
                            ShaderKey.SHADOW_TERRAIN_CUTOUT.getAlphaTest(),
                            FORMAT,
                            false,
                            false,
                            false,
                            false,

                            Patch.VANILLA

                        )
                    );

                }
            }
            buildSolid(access);
        } catch (Throwable e) {
            closePrograms();
            AllThingsFoliage.logger.error(
                "Could not build the foliage programs for this shader pack; foliage near the " +
                    "player is left to the chunk mesh while it is loaded",
                e
            );
        } finally {
            BUILDING.set(false);
        }
    }

    /**
     * Builds the mod's copies of the pack's solid terrain and solid shadow programs, as Iris builds the pack's own for
     * its solid terrain: the same sources with the alpha test off. A failure here leaves the cutout programs as they
     * are and only the solid ones out, so Fast leaves stay with the chunk mesh under this pack.
     */
    private static void buildSolid(IrisRenderingPipelineAccessor access) {
        try {
            Optional<ProgramSource> terrain = access
                .allthingsfoliage$resolver()
                .resolve(ProgramId.TerrainSolid);
            if (terrain.isEmpty()) {
                return;
            }
            GlProgram solid = finish(
                access.allthingsfoliage$createShader(
                    "mc2_foliage_solid",
                    ShaderKey.TERRAIN_SOLID,
                    terrain.get(),
                    ProgramId.TerrainSolid,
                    ShaderKey.TERRAIN_SOLID.getAlphaTest(),
                    FORMAT,
                    ShaderKey.TERRAIN_SOLID.getFogMode(),
                    false,
                    false,
                    false,
                    false,
                    false,

                    Patch.VANILLA

                )
            );
            GlProgram solidShadow = null;
            if (shadow != null) {
                Optional<ProgramSource> shadowSource = access
                    .allthingsfoliage$resolver()
                    .resolve(ProgramId.ShadowSolid);
                if (shadowSource.isEmpty()) {
                    solid.close();
                    return;
                }
                // Keyed as the cutout shadow, the one terrain shadow Iris names for vanilla's format; the source and the
                // alpha test are the solid shadow's.
                solidShadow = finish(
                    access.allthingsfoliage$createShadowShader(
                        "mc2_foliage_shadow_solid",
                        ShaderKey.SHADOW_TERRAIN_CUTOUT,
                        shadowSource.get(),
                        ProgramId.ShadowSolid,
                        ShaderKey.TERRAIN_SOLID.getAlphaTest(),
                        FORMAT,
                        false,
                        false,
                        false,
                        false,

                        Patch.VANILLA

                    )
                );
            }
            mainSolid = solid;
            shadowSolid = solidShadow;
        } catch (Throwable e) {
            closeSolidPrograms();
            AllThingsFoliage.logger.warn(
                "Could not build the solid foliage programs for this shader pack; Fast leaves near " +
                    "the player are left to the chunk mesh while it is loaded",
                e
            );
        }
    }

    private static void closeSolidPrograms() {
        if (mainSolid != null) {
            mainSolid.close();
            mainSolid = null;
        }
        if (shadowSolid != null) {
            shadowSolid.close();
            shadowSolid = null;
        }
    }

    /** Links the program Iris compiled, or fails the same way Iris would for one of the pack's own. */
    private static GlProgram finish(ShaderSupplier supplier) {
        int program = supplier.id().program();
        // 35714 is GL_LINK_STATUS.
        if (GlStateManager.glGetProgrami(program, 35714) == 0) {
            throw new IllegalStateException(
                "Linking failed: " +
                    GlStateManager.glGetProgramInfoLog(program, 32768)
            );
        }

        // An Iris program binds only the blocks Iris knows by name, so the sway's are tied to points of the mod's own and
        // bound there by the renderer as it draws; see bindSwayBlocks.
        if (swayBinding < 0) {
            int points = GL31.glGetInteger(GL31.GL_MAX_UNIFORM_BUFFER_BINDINGS);
            swayBinding = points - 1;
            interactionBinding = points - 2;
        }
        bindBlock(program, "iris_FoliageSway", swayBinding);
        bindBlock(program, "iris_FoliageInteraction", interactionBinding);

        return supplier.shader().get();
    }

    private static void bindBlock(int program, String name, int binding) {
        int index = GL31.glGetUniformBlockIndex(program, name);
        if (index != GL31.GL_INVALID_INDEX) {
            GL31.glUniformBlockBinding(program, index, binding);
        }
    }

    /**
     * The pipeline to draw the foliage with through the pack: the one Iris built around the pack's program for the
     * renderer's pipeline, with the mod's program in its place and the attributes tied to it, as Iris ties them -- by
     * name, Iris's prefixed first. Everything else is Iris's: the uniforms, their slots, the state. Built inside the pass
     * the first time, as Iris builds its own, and kept until the program goes. In Iris's shadow pass the program is the
     * mod's shadow one, as Iris's is the pack's shadow one there. Where the mod has no program, the pack's own is drawn
     * with, unswayed. For leaves drawn solid, the mod's solid copies, in the pipeline Iris built for the renderer's solid
     * one.
     */
    public static CompiledRenderPipeline withFoliageProgram(
        CompiledRenderPipeline iris,
        boolean solid
    ) {
        GlProgram program = IrisApi.getInstance().isRenderingShadowPass()
            ? solid
                ? shadowSolid
                : shadow
            : solid
              ? mainSolid
              : main;
        if (
            program == null ||
            !(iris instanceof FrontendRenderPipeline frontend) ||
            !(frontend.backendRenderPipeline() instanceof
                GlRenderPipeline irisPipeline)
        ) {
            return iris;
        }
        FrontendRenderPipeline built = WITH_FOLIAGE_PROGRAM.get(iris);
        if (built != null) {
            return built;
        }
        BackendRenderPipeline.CreateInfo original = (
            (GlRenderPipelineAccess) (Object) irisPipeline
        ).getCreateInfo();
        List<BackendRenderPipeline.CreateInfo.VertexBuffer> vertexBuffers =
            new ArrayList<>();
        List<BackendRenderPipeline.CreateInfo.AttribBinding> attributes =
            new ArrayList<>();
        List<VertexFormat> formats = frontend.vertexFormats();
        for (int slot = 0; slot < formats.size(); slot++) {
            VertexFormat format = formats.get(slot);
            if (format == null) {
                continue;
            }
            vertexBuffers.add(
                new BackendRenderPipeline.CreateInfo.VertexBuffer(
                    slot,
                    format.getVertexSize(),
                    format.getStepRate()
                )
            );
            for (VertexFormatElement element : format.getElements()) {
                int location = GL33C.glGetAttribLocation(
                    program.getProgramId(),
                    "iris_" + element.name()
                );
                if (location == -1) {
                    location = GL33C.glGetAttribLocation(
                        program.getProgramId(),
                        element.name()
                    );
                }
                if (location != -1) {
                    attributes.add(
                        new BackendRenderPipeline.CreateInfo.AttribBinding(
                            slot,
                            location,
                            element.offset(),
                            element.format()
                        )
                    );
                }
            }
        }
        BackendRenderPipeline.CreateInfo createInfo =
            new BackendRenderPipeline.CreateInfo(
                original.name(),
                original.shaders(),
                vertexBuffers,
                attributes,
                original.uniforms(),
                original.pushConstantsSize(),
                original.depthStencilState(),
                original.polygonMode(),
                original.cull(),
                original.colorTargetStates(),
                original.primitiveTopology()
            );
        VertexArray vertexArray = VertexArray.createSource(
            GL.getCapabilities(),
            new HashSet<>()
        ).apply(program, createInfo);
        VERTEX_ARRAYS.add(vertexArray);
        built = new FrontendRenderPipeline(
            frontend.name(),
            new GlRenderPipeline(
                IrisRenderSystem.getGlDevice(),
                createInfo,
                program,
                vertexArray
            ),
            formats,
            frontend.uniformIndices(),
            frontend.uniforms(),
            frontend.colorTargetStates(),
            frontend.wantsDepthTexture(),
            frontend.pushConstantSize()
        );
        WITH_FOLIAGE_PROGRAM.put(iris, built);
        return built;
    }

    /** Binds the sway settings and the plant pushes where the mod's programs read them; see finish. */
    public static void bindSwayBlocks(GpuBuffer settings, GpuBuffer interaction) {
        if (swayBinding < 0) {
            return;
        }
        GL31.glBindBufferBase(
            GL31.GL_UNIFORM_BUFFER,
            swayBinding,
            ((GlBuffer) settings).handle()
        );
        GL31.glBindBufferBase(
            GL31.GL_UNIFORM_BUFFER,
            interactionBinding,
            ((GlBuffer) interaction).handle()
        );
    }

    private static void closePrograms() {

        // The pipelines built around the program are let go of without closing them, which would delete the program once
        // for each; their vertex arrays are closed here, and the program below.
        WITH_FOLIAGE_PROGRAM.clear();
        VERTEX_ARRAYS.forEach(VertexArray::close);
        VERTEX_ARRAYS.clear();

        closeSolidPrograms();
        if (main != null) {
            main.close();
            main = null;
        }
        if (shadow != null) {
            shadow.close();
            shadow = null;
        }
    }

    /** Whether a program of the mod's is being built on this thread. */
    public static boolean isBuilding() {
        return BUILDING.get();
    }

    /** The uniform blocks the mod's programs bind on top of Iris's. */
    @SuppressWarnings("unchecked")
    public static <T> List<T> withFoliageLayouts(List<T> irisLayouts) {
        List<T> all = new ArrayList<>(irisLayouts);
        all.addAll((List<T>) layouts);
        return all;
    }

    /**
     * The pack's vertex shader, as Iris translated it, reading a moved position in place of its own: the position
     * input is kept under its name, since Iris binds it by that name, and every use of it reads the moved one instead,
     * which a {@code main} of the mod's sets before running the pack's.
     */
    public static String patchVertex(String vertex) {
        if (count(POSITION_INPUT, vertex) != 1) {
            throw new IllegalStateException(
                "Expected one position input in the translated vertex shader"
            );
        }
        if (count(MAIN, vertex) != 1) {
            throw new IllegalStateException(
                "Expected one main function in the translated vertex shader"
            );
        }
        String patched = POSITION.matcher(vertex).replaceAll(SWAYED_POSITION);
        patched = patched.replaceFirst(
            "\\bin\\s+vec3\\s+" + SWAYED_POSITION + "\\s*;",
            "in vec3 iris_Position;\nvec3 " + SWAYED_POSITION + ";"
        );
        patched = MAIN.matcher(patched).replaceFirst("void mc2_packMain()");
        return patched + "\n" + SWAY_SHADER;
    }

    private static int count(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        int found = 0;
        while (matcher.find()) {
            found++;
        }
        return found;
    }

    /**
     * What is appended to the pack's vertex shader. The blocks are named with Iris's prefix, which it adds when it
     * looks a program's uniform blocks up, and they hold what the mod's own vertex shader reads under the same names.
     */

    private static final String SWAY_SHADER =
        """
        // Added by MC2 - Interactive Foliage: the foliage it draws sways as it does without a shader pack.
        in float SwayCell;
        in float SwayWeights;
        layout(std140) uniform iris_FoliageSway {
            float mc2_SwayIntensity;
            float mc2_CalmSway;
            vec4 mc2_SwayEdge;
            vec4 mc2_Weather;
        };
        #define MC2_MAX_CELLS 128
        layout(std140) uniform iris_FoliageInteraction {
            int mc2_CellCount;
            vec4 mc2_CellBoundsMin;
            vec4 mc2_CellBoundsMax;
            vec4 mc2_CellPosition[MC2_MAX_CELLS];
            vec4 mc2_CellForce[MC2_MAX_CELLS];
        };
        """ +
        readSway() +
        """

        void main() {
            vec3 mc2_offset = iris_transforms.ModelOffset;
            mc2_swayedPosition = mc2_sway(iris_Position + mc2_offset, mc2_offset, iris_globalInfo.CameraBlockPos,
                    iris_globalInfo.CameraOffset, iris_globalInfo.GameTime, SwayCell, SwayWeights) - mc2_offset;
            mc2_packMain();
        }
        """;

    private static String readSway() {
        String path =
            "/assets/" + AllThingsFoliage.MOD_ID + "/shaders/include/sway.glsl";
        try (
            InputStream in = IrisFoliageShaders.class.getResourceAsStream(path)
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
