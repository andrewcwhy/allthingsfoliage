package net.karto.mc2.mc2_interactivefoliage.mixin.iris;

//? iris {

//? >=26.3 {
import com.mojang.renderpearl.api.vertex.VertexFormat;
//?} else {
/*import com.mojang.blaze3d.vertex.VertexFormat;
*///?}
import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.gl.state.FogMode;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
//? >=1.21.11 {
import net.irisshaders.iris.pipeline.programs.ShaderKey;
import net.irisshaders.iris.pipeline.programs.ShaderSupplier;
//?} else {
/*import net.minecraft.client.renderer.ShaderInstance;
*///?}
//? >=26.2 {
import net.irisshaders.iris.pipeline.transform.Patch;
//?}
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

	//? >=26.2 {
	@Invoker("createShader")
	ShaderSupplier mc2$createShader(String name, ShaderKey key, ProgramSource source, ProgramId programId,
			AlphaTest fallbackAlpha, VertexFormat vertexFormat, FogMode fogMode, boolean isIntensity,
			boolean isFullbright, boolean isGlint, boolean isText, boolean isIE, Patch patch) throws IOException;

	@Invoker("createShadowShader")
	ShaderSupplier mc2$createShadowShader(String name, ShaderKey key, ProgramSource source, ProgramId programId,
			AlphaTest fallbackAlpha, VertexFormat vertexFormat, boolean isIntensity, boolean isFullbright,
			boolean isText, boolean isIE, Patch patch) throws IOException;
	//?} elif >=1.21.11 {
	/*// Before 26.2 Iris translates every program one way, so neither method takes how to patch it.
	@Invoker("createShader")
	ShaderSupplier mc2$createShader(String name, ShaderKey key, ProgramSource source, ProgramId programId,
			AlphaTest fallbackAlpha, VertexFormat vertexFormat, FogMode fogMode, boolean isIntensity,
			boolean isFullbright, boolean isGlint, boolean isText, boolean isIE) throws IOException;

	@Invoker("createShadowShader")
	ShaderSupplier mc2$createShadowShader(String name, ShaderKey key, ProgramSource source, ProgramId programId,
			AlphaTest fallbackAlpha, VertexFormat vertexFormat, boolean isIntensity, boolean isFullbright,
			boolean isText, boolean isIE) throws IOException;
	*///?} elif >=1.21.1 {
	/*// Before 1.21.11 a program is a shader instance, built straight away rather than supplied, and not keyed.
	@Invoker("createShader")
	ShaderInstance mc2$createShader(String name, ProgramSource source, ProgramId programId, AlphaTest fallbackAlpha,
			VertexFormat vertexFormat, FogMode fogMode, boolean isIntensity, boolean isFullbright, boolean isGlint,
			boolean isText, boolean isIE) throws IOException;

	@Invoker("createShadowShader")
	ShaderInstance mc2$createShadowShader(String name, ProgramSource source, ProgramId programId,
			AlphaTest fallbackAlpha, VertexFormat vertexFormat, boolean isIntensity, boolean isFullbright,
			boolean isText, boolean isIE) throws IOException;
	*///?} else {
	/*// Before 1.21.1 Iris has no Immersive Engineering programs to tell apart.
	@Invoker("createShader")
	ShaderInstance mc2$createShader(String name, ProgramSource source, ProgramId programId, AlphaTest fallbackAlpha,
			VertexFormat vertexFormat, FogMode fogMode, boolean isIntensity, boolean isFullbright, boolean isGlint,
			boolean isText) throws IOException;

	@Invoker("createShadowShader")
	ShaderInstance mc2$createShadowShader(String name, ProgramSource source, ProgramId programId,
			AlphaTest fallbackAlpha, VertexFormat vertexFormat, boolean isIntensity, boolean isFullbright,
			boolean isText) throws IOException;
	*///?}
}
//?}
