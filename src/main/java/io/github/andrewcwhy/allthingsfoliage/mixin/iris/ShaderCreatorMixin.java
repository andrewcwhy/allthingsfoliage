package net.karto.mc2.mc2_interactivefoliage.mixin.iris;

//? iris {

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.gl.state.ShaderAttributeInputs;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.helpers.Tri;
import net.irisshaders.iris.pipeline.programs.ShaderCreator;
import net.irisshaders.iris.pipeline.transform.PatchShaderType;
import net.irisshaders.iris.pipeline.transform.TransformPatcher;
import net.irisshaders.iris.shaderpack.texture.TextureStage;
import net.karto.mc2.mc2_interactivefoliage.gpu.IrisFoliageShaders;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.EnumMap;
import java.util.Map;
//? >=26.3 {
import java.util.Set;
//?}

/**
 * Where a foliage program's vertex shader is changed: right after Iris has translated the pack's source, and before it
 * is compiled. Only while the mod builds one of its own; the pack's programs pass through untouched.
 */
@Mixin(value = ShaderCreator.class, remap = false)
public abstract class ShaderCreatorMixin {

	//? >=26.3 {
	// Iris 1.11.6 hands the translation the textures the pack overrides as well.
	@Redirect(method = {"create", "createShadow"}, at = @At(value = "INVOKE",
			target = "Lnet/irisshaders/iris/pipeline/transform/TransformPatcher;patchVanilla(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lnet/irisshaders/iris/gl/blending/AlphaTest;ZZZLnet/irisshaders/iris/gl/state/ShaderAttributeInputs;Lit/unimi/dsi/fastutil/objects/Object2ObjectMap;Ljava/util/Set;)Ljava/util/Map;"))
	private static Map<PatchShaderType, String> mc2$swayFoliage(String name, String vertex, String geometry,
			String tessControl, String tessEval, String fragment, AlphaTest alpha, boolean isLines, boolean isClouds,
			boolean hasChunkOffset, ShaderAttributeInputs inputs,
			Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap, Set<String> textureOverrides) {
		Map<PatchShaderType, String> translated = TransformPatcher.patchVanilla(name, vertex, geometry, tessControl,
				tessEval, fragment, alpha, isLines, isClouds, hasChunkOffset, inputs, textureMap, textureOverrides);
	//?} elif >=1.21.11 {
	/*@Redirect(method = {"create", "createShadow"}, at = @At(value = "INVOKE",
			target = "Lnet/irisshaders/iris/pipeline/transform/TransformPatcher;patchVanilla(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lnet/irisshaders/iris/gl/blending/AlphaTest;ZZZLnet/irisshaders/iris/gl/state/ShaderAttributeInputs;Lit/unimi/dsi/fastutil/objects/Object2ObjectMap;)Ljava/util/Map;"))
	private static Map<PatchShaderType, String> mc2$swayFoliage(String name, String vertex, String geometry,
			String tessControl, String tessEval, String fragment, AlphaTest alpha, boolean isLines, boolean isClouds,
			boolean hasChunkOffset, ShaderAttributeInputs inputs,
			Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap) {
		Map<PatchShaderType, String> translated = TransformPatcher.patchVanilla(name, vertex, geometry, tessControl,
				tessEval, fragment, alpha, isLines, isClouds, hasChunkOffset, inputs, textureMap);
	*///?} else {
	/*// Before 1.21.11 clouds are not told apart, and shadow programs are built by the same method as the rest.
	@Redirect(method = "create", at = @At(value = "INVOKE",
			target = "Lnet/irisshaders/iris/pipeline/transform/TransformPatcher;patchVanilla(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lnet/irisshaders/iris/gl/blending/AlphaTest;ZZLnet/irisshaders/iris/gl/state/ShaderAttributeInputs;Lit/unimi/dsi/fastutil/objects/Object2ObjectMap;)Ljava/util/Map;"))
	private static Map<PatchShaderType, String> mc2$swayFoliage(String name, String vertex, String geometry,
			String tessControl, String tessEval, String fragment, AlphaTest alpha, boolean isLines,
			boolean hasChunkOffset, ShaderAttributeInputs inputs,
			Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap) {
		Map<PatchShaderType, String> translated = TransformPatcher.patchVanilla(name, vertex, geometry, tessControl,
				tessEval, fragment, alpha, isLines, hasChunkOffset, inputs, textureMap);
	*///?}
		if (!IrisFoliageShaders.isBuilding()) {
			return translated;
		}
		// Iris may cache what it translated and hand the same map out again, so the change goes into a copy.
		Map<PatchShaderType, String> patched = new EnumMap<>(PatchShaderType.class);
		patched.putAll(translated);
		patched.put(PatchShaderType.VERTEX, IrisFoliageShaders.patchVertex(translated.get(PatchShaderType.VERTEX)));
		return patched;
	}
}
//?}
