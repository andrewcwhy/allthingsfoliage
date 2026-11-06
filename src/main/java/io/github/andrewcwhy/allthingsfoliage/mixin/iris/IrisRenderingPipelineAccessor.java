package io.github.andrewcwhy.allthingsfoliage.mixin.iris;

import com.mojang.renderpearl.api.vertex.VertexFormat;

import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.gl.state.FogMode;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;

import net.irisshaders.iris.pipeline.programs.ShaderKey;
import net.irisshaders.iris.pipeline.programs.ShaderSupplier;

import net.irisshaders.iris.pipeline.transform.Patch;

import net.irisshaders.iris.shaderpack.loading.ProgramId;
import net.irisshaders.iris.shaderpack.programs.ProgramFallbackResolver;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shadows.ShadowRenderTargets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.io.IOException;

/**
 * What the foliage programs are built with: the pack's sources, and the two private methods Iris builds the pack's own
 * terrain and shadow programs through, so the mod's copies get the same framebuffers, samplers and uniforms.
 */
@Mixin(value = IrisRenderingPipeline.class, remap = false)
public interface IrisRenderingPipelineAccessor {

	@Accessor("resolver")
	ProgramFallbackResolver mc2$resolver();

	@Accessor("shadowRenderTargets")
	ShadowRenderTargets mc2$shadowRenderTargets();

	@Invoker("createShader")
	ShaderSupplier mc2$createShader(String name, ShaderKey key, ProgramSource source, ProgramId programId,
			AlphaTest fallbackAlpha, VertexFormat vertexFormat, FogMode fogMode, boolean isIntensity,
			boolean isFullbright, boolean isGlint, boolean isText, boolean isIE, Patch patch) throws IOException;

	@Invoker("createShadowShader")
	ShaderSupplier mc2$createShadowShader(String name, ShaderKey key, ProgramSource source, ProgramId programId,
			AlphaTest fallbackAlpha, VertexFormat vertexFormat, boolean isIntensity, boolean isFullbright,
			boolean isText, boolean isIE, Patch patch) throws IOException;
	
}
