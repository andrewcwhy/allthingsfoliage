package io.github.andrewcwhy.allthingsfoliage.gpu;

//? >=1.20.1 {

//? iris {
import com.mojang.blaze3d.vertex.BufferBuilder;
//? >=1.21.11 {
//? >=26.3 {
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
//? >=26.3 {
import com.mojang.renderpearl.api.vertex.VertexFormat;
//?} else {
/*import com.mojang.blaze3d.vertex.VertexFormat;
 */ //?}
import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage;
//?} else {
/*import com.mojang.blaze3d.pipeline.RenderPipeline;
 */ //?}

import java.util.List;
import net.minecraft.world.level.block.state.BlockState;

//?} else {
/*import net.minecraft.client.renderer.ShaderInstance;
 */ //?}
//?}

/**
 * What the renderer asks about shader packs, answered without Iris's classes ever being loaded when Iris is not
 * installed: every call that reaches {@link IrisFoliageShaders} checks first. Where the shader pack support has not
 * been written yet, a shader pack never counts as loaded.
 */
final class IrisCompat {

    //? iris {
    //? forge {
    /*// Forge has Oculus, Iris ported to it, under its own id: Forge ignores the Iris id it says it provides.
	private static final boolean IRIS = AllThingsFoliage.xplat().isModLoaded("oculus");
	*/ //?} else {
    private static final boolean IRIS = AllThingsFoliage.xplat().isModLoaded(
        "iris"
    );

    //?}
    //?}

    private IrisCompat() {}

    /** Whether a shader pack is loaded, which the renderer then draws through. */
    static boolean shaderPackInUse() {
        //? iris {
        return IRIS && IrisFoliageShaders.shaderPackInUse();
        //?} else {
        /*// An Iris installed where the support has not been written counts as a pack always loaded, with no programs to
		// draw through it: the chunk mesh keeps the foliage, drawn by the pack, rather than the renderer drawing it over
		// the pack unlit.
		return io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage.xplat().isModLoaded("iris");
		*/ //?}
    }

    /** Whether the pack's shadow map is being drawn right now, rather than the screen. */
    static boolean renderingShadows() {
        //? iris {
        return IRIS && IrisFoliageShaders.renderingShadows();
        //?} else {
        /*return false;
         */ //?}
    }

    /** Whether the programs for the loaded pack are built; builds them the first time a pack is asked about. */
    static boolean programsReady() {
        //? iris {
        return IRIS && IrisFoliageShaders.ready();
        //?} else {
        /*return false;
         */ //?}
    }

    static boolean hasShadowProgram() {
        //? iris {
        return IRIS && IrisFoliageShaders.hasShadowProgram();
        //?} else {
        /*return false;
         */ //?}
    }

    /** Changes each time a pack loads, so what was meshed for another pack is meshed again. */
    static int programGeneration() {
        //? iris {
        return IRIS ? IrisFoliageShaders.generation() : 0;
        //?} else {
        /*return 0;
         */ //?}
    }

    //? iris {
    /** The vertex format a pack's programs read, which the renderer's region buffers hold while one is loaded. */
    static VertexFormat shaderPackFormat() {
        return IrisFoliageShaders.FORMAT;
    }

    /** The format a section comes out of meshing in while a pack is loaded: Iris's terrain format. */
    static VertexFormat shaderPackMeshFormat() {
        return net.irisshaders.iris.vertices.IrisVertexFormats.TERRAIN;
    }

    /** Draws the foliage into a shader pack's shadow map, from the camera Iris rendered the shadow pass for. */
    @FunctionalInterface
    interface ShadowDraw {
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
    //? >=1.21.11 {
    static void setUp(
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

    //?} else {
    /*static void setUp(ShadowDraw drawShadow) {
		if (IRIS) {
			IrisFoliageShaders.setUp(drawShadow);
		}
	}

	/^*
	 * The pack's program the foliage is drawn with, in its own pass or its shadow pass -- the solid one for leaves drawn
	 * solid -- or null if there is none.
	 ^/
	static ShaderInstance program(boolean shadowPass, boolean solid) {
		return IRIS ? IrisFoliageShaders.program(shadowPass, solid) : null;
	}

	/^* Sets the sway's own uniforms on a pack's program, once it is applied. ^/
	static void setSwayUniforms(ShaderInstance program, float intensity, float calmSway, org.joml.Vector4f edge,
			int cameraX, int cameraY,
			int cameraZ, float offsetX, float offsetY, float offsetZ, float gameTime, org.joml.Vector4f weather,
			org.joml.Vector3f subLevelDown, float subLevelWindX, float subLevelWindZ, org.joml.Vector3f subLevelHang) {
		if (IRIS) {
			IrisFoliageShaders.setSwayUniforms(program, intensity, calmSway, edge.x, edge.y, edge.z, edge.w, cameraX, cameraY,
					cameraZ, offsetX, offsetY, offsetZ, gameTime, weather, subLevelDown, subLevelWindX, subLevelWindZ,
					subLevelHang);
		}
	}
	*/ //?}

    static void beginBlock(
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

    static void endBlock(BufferBuilder builder) {
        if (IRIS) {
            IrisFoliageShaders.endBlock(builder);
        }
    }

    //? >=26.3 {
    /**
     * The pipeline to draw the foliage with through the pack: Iris's, with the mod's swaying program in it; its solid one
     * for solid leaves.
     */
    static com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline withFoliageProgram(
        com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline iris,
        boolean solid
    ) {
        return IRIS ? IrisFoliageShaders.withFoliageProgram(iris, solid) : iris;
    }

    /** Hands Iris the renderer's pipeline for leaves drawn solid; see IrisFoliageShaders.setUpOpaque. */
    static void setUpOpaque(
        com.mojang.renderpearl.api.pipeline.RenderPipeline pipeline
    ) {
        if (IRIS) {
            IrisFoliageShaders.setUpOpaque(pipeline);
        }
    }

    /** Whether leaves can be drawn solid through the loaded pack: its solid programs are built. */
    static boolean hasOpaquePrograms() {
        return IRIS && IrisFoliageShaders.hasOpaquePrograms();
    }

    /** Inside inTerrainPhase: what is drawn next is solid terrain, or cutout terrain, for the pack. */
    static void setTerrainPhase(boolean solid) {
        if (IRIS) {
            IrisFoliageShaders.setTerrainPhase(solid);
        }
    }

    /** Binds the sway settings and the plant pushes where the mod's programs for the pack read them. */
    static void bindSwayBlocks(
        com.mojang.renderpearl.api.buffers.GpuBuffer settings,
        com.mojang.renderpearl.api.buffers.GpuBuffer interaction
    ) {
        if (IRIS) {
            IrisFoliageShaders.bindSwayBlocks(settings, interaction);
        }
    }

    //?} elif >=1.21.11 {
    /*/^* Hands over the renderer's pipelines for leaves drawn solid, in the pack's own pass and in its shadow pass. ^/
	static void setUpOpaque(RenderPipeline pipeline, RenderPipeline shadowPipeline) {
		if (IRIS) {
			IrisFoliageShaders.setUpOpaque(pipeline, shadowPipeline);
		}
	}

	/^* Whether leaves can be drawn solid through the loaded pack: its solid programs are built. ^/
	static boolean hasOpaquePrograms() {
		return IRIS && IrisFoliageShaders.hasOpaquePrograms();
	}

	/^* Inside inTerrainPhase: what is drawn next is solid terrain, or cutout terrain, for the pack. ^/
	static void setTerrainPhase(boolean solid) {
		if (IRIS) {
			IrisFoliageShaders.setTerrainPhase(solid);
		}
	}
	*/ //?} else {
    /*/^* Whether leaves can be drawn solid through the loaded pack: its solid programs are built. ^/
	static boolean hasOpaquePrograms() {
		return IRIS && IrisFoliageShaders.hasOpaquePrograms();
	}

	/^* Inside inTerrainPhase: what is drawn next is solid terrain, or cutout terrain, for the pack. ^/
	static void setTerrainPhase(boolean solid) {
		if (IRIS) {
			IrisFoliageShaders.setTerrainPhase(solid);
		}
	}
	*/ //?}

    /** Runs a draw as terrain cutout for the pack. */
    static void inTerrainPhase(Runnable draw) {
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
    //?}

    //? !iris && >=26.3 {
    /*// Where the support has not been written no frame is drawn through a pack, and these are never reached.
	static com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline withFoliageProgram(
			com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline iris, boolean solid) {
		return iris;
	}

	static boolean hasOpaquePrograms() {
		return false;
	}

	static void setTerrainPhase(boolean solid) {
	}

	static void bindSwayBlocks(com.mojang.renderpearl.api.buffers.GpuBuffer settings,
			com.mojang.renderpearl.api.buffers.GpuBuffer interaction) {
	}

	static void inTerrainPhase(Runnable draw) {
		draw.run();
	}
	*/ //?}
}
//?}
