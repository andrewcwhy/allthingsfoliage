package io.github.andrewcwhy.allthingsfoliage.mixin.compat.iris

import com.mojang.renderpearl.api.vertex.VertexFormat
import java.io.IOException
import net.irisshaders.iris.gl.blending.AlphaTest
import net.irisshaders.iris.gl.state.FogMode
import net.irisshaders.iris.pipeline.IrisRenderingPipeline
import net.irisshaders.iris.pipeline.programs.ShaderKey
import net.irisshaders.iris.pipeline.programs.ShaderSupplier
import net.irisshaders.iris.pipeline.transform.Patch
import net.irisshaders.iris.shaderpack.loading.ProgramId
import net.irisshaders.iris.shaderpack.programs.ProgramFallbackResolver
import net.irisshaders.iris.shaderpack.programs.ProgramSource
import net.irisshaders.iris.shadows.ShadowRenderTargets
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor
import org.spongepowered.asm.mixin.gen.Invoker

/**
 * Exposes Iris shader resources and creation methods so foliage shaders
 * use the same framebuffers, samplers, and uniforms as the shader pack.
 */
@Mixin(value = [IrisRenderingPipeline::class], remap = false)
interface IrisRenderingPipelineAccessor {

    @Accessor("resolver")
    fun `allthingsfoliage$resolver`(): ProgramFallbackResolver

    @Accessor("shadowRenderTargets")
    fun `allthingsfoliage$shadowRenderTargets`(): ShadowRenderTargets

    @Throws(IOException::class)
    @Invoker("createShader")
    fun `allthingsfoliage$createShader`(
        name: String,
        key: ShaderKey,
        source: ProgramSource,
        programId: ProgramId,
        fallbackAlpha: AlphaTest,
        vertexFormat: VertexFormat,
        fogMode: FogMode,
        isIntensity: Boolean,
        isFullbright: Boolean,
        isGlint: Boolean,
        isText: Boolean,
        isIE: Boolean,
        patch: Patch
    ): ShaderSupplier

    @Throws(IOException::class)
    @Invoker("createShadowShader")
    fun `allthingsfoliage$createShadowShader`(
        name: String,
        key: ShaderKey,
        source: ProgramSource,
        programId: ProgramId,
        fallbackAlpha: AlphaTest,
        vertexFormat: VertexFormat,
        isIntensity: Boolean,
        isFullbright: Boolean,
        isText: Boolean,
        isIE: Boolean,
        patch: Patch
    ): ShaderSupplier
}
