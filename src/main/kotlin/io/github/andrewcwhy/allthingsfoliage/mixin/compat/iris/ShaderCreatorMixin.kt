@file:JvmName("ShaderCreatorMixin")
@file:Mixin(value = [ShaderCreator::class], remap = false)

package io.github.andrewcwhy.allthingsfoliage.mixin.compat.iris

import io.github.andrewcwhy.allthingsfoliage.gpu.shader.IrisFoliageShaders
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import java.util.EnumMap
import net.irisshaders.iris.gl.blending.AlphaTest
import net.irisshaders.iris.gl.state.ShaderAttributeInputs
import net.irisshaders.iris.gl.texture.TextureType
import net.irisshaders.iris.helpers.Tri
import net.irisshaders.iris.pipeline.programs.ShaderCreator
import net.irisshaders.iris.pipeline.transform.PatchShaderType
import net.irisshaders.iris.pipeline.transform.TransformPatcher
import net.irisshaders.iris.shaderpack.texture.TextureStage
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Redirect

/** Patches foliage vertex shaders after Iris translation and before compilation. */
@Redirect(
    method = ["create", "createShadow"],
    at = [At(
        value = "INVOKE",
        target =
            "Lnet/irisshaders/iris/pipeline/transform/TransformPatcher;" +
                    "patchVanilla(" +
                    "Ljava/lang/String;" +
                    "Ljava/lang/String;" +
                    "Ljava/lang/String;" +
                    "Ljava/lang/String;" +
                    "Ljava/lang/String;" +
                    "Ljava/lang/String;" +
                    "Lnet/irisshaders/iris/gl/blending/AlphaTest;" +
                    "ZZZ" +
                    "Lnet/irisshaders/iris/gl/state/ShaderAttributeInputs;" +
                    "Lit/unimi/dsi/fastutil/objects/Object2ObjectMap;" +
                    "Ljava/util/Set;)" +
                    "Ljava/util/Map;",
    )],
)
private fun patchFoliageShaders(
    name: String,
    vertex: String?,
    geometry: String?,
    tessellationControl: String?,
    tessellationEvaluation: String?,
    fragment: String?,
    alphaTest: AlphaTest,
    isLines: Boolean,
    isClouds: Boolean,
    hasChunkOffset: Boolean,
    inputs: ShaderAttributeInputs,
    textureMap: Object2ObjectMap<Tri<String, TextureType, TextureStage>, String>,
    textureOverrides: Set<String>,
): Map<PatchShaderType, String> {
    val translated = TransformPatcher.patchVanilla(
        name,
        vertex,
        geometry,
        tessellationControl,
        tessellationEvaluation,
        fragment,
        alphaTest,
        isLines,
        isClouds,
        hasChunkOffset,
        inputs,
        textureMap,
        textureOverrides,
    )

    if (!IrisFoliageShaders.isBuilding) {
        return translated
    }

    // Iris may reuse cached translations; modify a copy to preserve other programs.
    val patched = EnumMap<PatchShaderType, String>(PatchShaderType::class.java)
    patched.putAll(translated)
    patched[PatchShaderType.VERTEX] = IrisFoliageShaders.patchVertex(
        checkNotNull(translated[PatchShaderType.VERTEX]) { "Missing translated vertex shader" }
    )

    return patched
}
