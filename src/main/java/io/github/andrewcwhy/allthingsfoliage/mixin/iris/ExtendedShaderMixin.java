package net.karto.mc2.mc2_interactivefoliage.mixin.iris;

//? >=1.21.11 && iris && <26.3 {

/*//? >=26.2 {
import com.mojang.blaze3d.pipeline.BindGroupLayout;
//?} else {
/^import com.mojang.blaze3d.pipeline.RenderPipeline;
^///?}
import net.irisshaders.iris.pipeline.programs.ExtendedShader;
import net.karto.mc2.mc2_interactivefoliage.gpu.IrisFoliageShaders;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.List;

/^*
 * Gives a foliage program the uniform blocks it reads beyond Iris's own -- the sway settings and the plant pushes -- in
 * the one call a program learns its uniforms from, so the renderer binds them the way it binds any other.
 ^/
@Mixin(value = ExtendedShader.class, remap = false)
public abstract class ExtendedShaderMixin {

	//? >=26.2 {
	@ModifyArg(method = "<init>", at = @At(value = "INVOKE",
			target = "Lcom/mojang/blaze3d/opengl/GlProgram;setupBindGroupLayouts(Ljava/util/List;)V"))
	private List<BindGroupLayout> mc2$addFoliageLayouts(List<BindGroupLayout> layouts) {
		return IrisFoliageShaders.isBuilding() ? IrisFoliageShaders.withFoliageLayouts(layouts) : layouts;
	}
	//?} else {
	/^// Before 26.2 a program is handed its uniforms one by one, with its samplers after them. The call is Minecraft's, so
	// its name is remapped where the game is obfuscated, unlike the Iris class it is called from.
	@ModifyArg(method = "<init>", at = @At(value = "INVOKE",
			target = "Lcom/mojang/blaze3d/opengl/GlProgram;setupUniforms(Ljava/util/List;Ljava/util/List;)V", remap = true),
			index = 0)
	private List<RenderPipeline.UniformDescription> mc2$addFoliageLayouts(List<RenderPipeline.UniformDescription> uniforms) {
		return IrisFoliageShaders.isBuilding() ? IrisFoliageShaders.withFoliageLayouts(uniforms) : uniforms;
	}
	^///?}
}
*///?}
