package io.github.andrewcwhy.allthingsfoliage.gpu.compat;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage;
import io.github.andrewcwhy.allthingsfoliage.gpu.shader.IrisFoliageShaders;
import java.util.List;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What the renderer asks about shader packs, answered without Iris's classes ever being loaded when Iris is not
 * installed: every call that reaches {@link IrisFoliageShaders} checks first. Where the shader pack support has not
 * been written yet, a shader pack never counts as loaded.
 */
public final class IrisCompat {

    private static final boolean IRIS = AllThingsFoliage.xplat().isModLoaded(
        "iris"
    );

    private IrisCompat() {}

    /** Whether a shader pack is loaded, which the renderer then draws through. */
    public static boolean shaderPackInUse() {

        return IRIS && IrisFoliageShaders.shaderPackInUse();

    }

    /** Whether the pack's shadow map is being drawn right now, rather than the screen. */
    public static boolean renderingShadows() {

        return IRIS && IrisFoliageShaders.renderingShadows();

    }

    /** Whether the programs for the loaded pack are built; builds them the first time a pack is asked about. */
    public static boolean programsReady() {

        return IRIS && IrisFoliageShaders.ready();

    }

    public static boolean hasShadowProgram() {

        return IRIS && IrisFoliageShaders.hasShadowProgram();

    }

    /** Changes each time a pack loads, so what was meshed for another pack is meshed again. */
    public static int programGeneration() {

        return IRIS ? IrisFoliageShaders.generation() : 0;

    }

    /** The vertex format a pack's programs read, which the renderer's region buffers hold while one is loaded. */
    public static VertexFormat shaderPackFormat() {
        return IrisFoliageShaders.FORMAT;
    }

    /** The format a section comes out of meshing in while a pack is loaded: Iris's terrain format. */
    public static VertexFormat shaderPackMeshFormat() {
        return net.irisshaders.iris.vertices.IrisVertexFormats.TERRAIN;
    }

    /** Draws the foliage into a shader pack's shadow map, from the camera Iris rendered the shadow pass for. */
    @FunctionalInterface
    public interface ShadowDraw {
        void draw(
            org.joml.Matrix4f modelView,
            org.joml.Matrix4f projection,
            double cameraX,
            double cameraY,
            double cameraZ
        );
    }

    /**
     * Hands the renderer's shader pack pipelines -- for the pack's own pass and for its shadow map -- their extra uniform
     * blocks (bind group layouts from 26.2, uniform descriptions before) and its shadow draw to the Iris side.
     */

    public static void setUp(
        RenderPipeline pipeline,
        RenderPipeline shadowPipeline,
        List<?> layouts,
        ShadowDraw drawShadow
    ) {
        if (IRIS) {
            IrisFoliageShaders.setUp(
                pipeline,
                shadowPipeline,
                layouts,
                drawShadow
            );
        }
    }

    public static void beginBlock(
        BufferBuilder builder,
        BlockState state,
        int x,
        int y,
        int z
    ) {
        if (IRIS) {
            IrisFoliageShaders.beginBlock(builder, state, x, y, z);
        }
    }

    public static void endBlock(BufferBuilder builder) {
        if (IRIS) {
            IrisFoliageShaders.endBlock(builder);
        }
    }

    /**
     * The pipeline to draw the foliage with through the pack: Iris's, with the mod's swaying program in it; its solid one
     * for solid leaves.
     */
    public static com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline withFoliageProgram(
        com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline iris,
        boolean solid
    ) {
        return IRIS ? IrisFoliageShaders.withFoliageProgram(iris, solid) : iris;
    }

    /** Hands Iris the renderer's pipeline for leaves drawn solid; see IrisFoliageShaders.setUpOpaque. */
    public static void setUpOpaque(
        com.mojang.renderpearl.api.pipeline.RenderPipeline pipeline
    ) {
        if (IRIS) {
            IrisFoliageShaders.setUpOpaque(pipeline);
        }
    }

    /** Whether leaves can be drawn solid through the loaded pack: its solid programs are built. */
    public static boolean hasOpaquePrograms() {
        return IRIS && IrisFoliageShaders.hasOpaquePrograms();
    }

    /** Inside inTerrainPhase: what is drawn next is solid terrain, or cutout terrain, for the pack. */
    public static void setTerrainPhase(boolean solid) {
        if (IRIS) {
            IrisFoliageShaders.setTerrainPhase(solid);
        }
    }

    /** Binds the sway settings and the plant pushes where the mod's programs for the pack read them. */
    public static void bindSwayBlocks(
        com.mojang.renderpearl.api.buffers.GpuBuffer settings,
        com.mojang.renderpearl.api.buffers.GpuBuffer interaction
    ) {
        if (IRIS) {
            IrisFoliageShaders.bindSwayBlocks(settings, interaction);
        }
    }

    /** Runs a draw as terrain cutout for the pack. */
    public static void inTerrainPhase(Runnable draw) {
        if (!IRIS) {
            draw.run();
            return;
        }
        Object previous = IrisFoliageShaders.beginTerrainPhase();
        try {
            draw.run();
        } finally {
            IrisFoliageShaders.endTerrainPhase(previous);
        }
    }

}
