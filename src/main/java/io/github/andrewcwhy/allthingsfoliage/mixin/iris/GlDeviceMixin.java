package net.karto.mc2.mc2_interactivefoliage.mixin.iris;

//? >=1.21.11 && iris && <26.3 {

/*import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.opengl.GlRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.karto.mc2.mc2_interactivefoliage.gpu.IrisFoliageShaders;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/^*
 * Draws the renderer's shader pack pipelines with the foliage programs built for the pack: the terrain copy for its
 * own pass, the shadow copy for its shadow map. Iris answers the same question the same way for the pipelines it
 * knows, and does not know these, so the two never compete. Ahead of Iris, which otherwise logs them as pipelines it
 * has no program for.
 ^/
// Named rather than referenced: the class is package-private.
@Mixin(targets = "com.mojang.blaze3d.opengl.GlDevice", priority = 900)
public abstract class GlDeviceMixin {

	@Inject(method = "getOrCompilePipeline(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)Lcom/mojang/blaze3d/opengl/GlRenderPipeline;",
			at = @At("HEAD"), cancellable = true)
	private void mc2$foliageProgram(RenderPipeline pipeline, CallbackInfoReturnable<GlRenderPipeline> cir) {
		GlProgram program = IrisFoliageShaders.programFor(pipeline);
		if (program != null) {
			cir.setReturnValue(new GlRenderPipeline(pipeline, program));
		}
	}
}
*///?}
