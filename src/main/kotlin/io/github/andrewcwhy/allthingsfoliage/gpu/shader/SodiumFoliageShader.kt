package io.github.andrewcwhy.allthingsfoliage.gpu.shader

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import com.mojang.renderpearl.api.pipeline.ShaderSource
import com.mojang.renderpearl.api.pipeline.ShaderType
import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
import java.io.IOException
import kotlin.jvm.optionals.getOrNull
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import net.minecraft.util.Util

/**
 * Adapts Sodium's active terrain shaders to the foliage renderer's vertices and uniforms.
 *
 * Resource-pack shader replacements are preserved. Unsupported sources or compilation failures
 * leave foliage rendering on the mod's fallback shaders.
 */
object SodiumFoliageShader {
    const val BLOCK_TEXTURE = "u_BlockTex"
    const val LIGHT_TEXTURE = "u_LightTex"

    @JvmField
    val isAvailable: Boolean = AllThingsFoliage.platform.isModLoaded("sodium")

    @JvmField
    val shaderId: Identifier = Identifier.fromNamespaceAndPath(
        AllThingsFoliage.MOD_ID,
        "core/foliage_sodium",
    )

    private const val ADDITIONS_MARKER = "ATF_ADDITIONS"
    private const val CHUNK_VERTEX_INCLUDE = "sodium:shaders/include/chunk_vertex.glsl"
    private const val CUTOUT_MATERIAL = "_material_params = 5u;"
    private const val GLOBALS_INCLUDE = "sodium:shaders/include/globals.glsl"
    private const val OPAQUE_FRAGMENT = "blocks/block_layer_opaque.fsh"
    private const val OPAQUE_VERTEX = "blocks/block_layer_opaque.vsh"
    private const val SOLID_MATERIAL = "_material_params = 1u;"

    private val additionsSource = """
        layout(location = 4) in float SwayCell;
        layout(location = 5) in float SwayWeights;
        #include <allthingsfoliage:foliage_uniforms.glsl>
        #include <allthingsfoliage:sway.glsl>
    """.trimIndent() + "\n"

    private val chunkFadePattern =
        Regex("""\bu_chunkFades\s*\[[^\]]*\]\s*\[[^\]]*\]""")

    private val chunkFadesBlockPattern = Regex(
        """\blayout\s*\(\s*std140\s*\)\s*uniform\s+ChunkData\s*\{[^}]*\}\s*\w*\s*;"""
    )

    private val compiledPipelines =
        Reference2ObjectOpenHashMap<RenderPipeline, CompiledRenderPipeline?>()

    private val defines = listOf(
        "ALPHA_CUTOUT 0.5",
        "USE_FOG",
        "USE_VERTEX_COMPRESSION",
    )

    // Member order must match Minecraft's buffer layout.
    private val dynamicTransforms = """
        layout(std140) uniform DynamicTransforms {
            mat4 atf_ModelViewMat;
            mat4 atf_TextureMat;
            vec4 atf_ColorModulator;
            vec3 atf_ModelOffset;
        };
    """.trimIndent() + "\n"

    private val globalUniforms = setOf(
        "u_EnvironmentFog",
        "u_FadePeriodInv",
        "u_FogColor",
        "u_ModelViewMatrix",
        "u_ProjectionMatrix",
        "u_RenderFog",
        "u_TexCoordShrink",
        "u_TexelSize",
        "u_UseRGSS",
    )

    // Member order must match Minecraft's buffer layout.
    private val globalsBlock = """
        layout(std140) uniform Globals {
            ivec3 atf_CameraBlockPos;
            float atf_GlintAlpha;
            vec3 atf_CameraOffset;
            float atf_GameTime;
            vec2 atf_ScreenSize;
            int atf_MenuBlurRadius;
            int atf_UseRgss;
        };
    """.trimIndent() + "\n"

    private val gameUniforms = dynamicTransforms + """
        layout(std140) uniform Projection {
            mat4 atf_ProjMat;
        };
        layout(std140) uniform Fog {
            vec4 atf_FogColor;
            float atf_FogEnvironmentalStart;
            float atf_FogEnvironmentalEnd;
            float atf_FogRenderDistanceStart;
            float atf_FogRenderDistanceEnd;
            float atf_FogSkyEnd;
            float atf_FogCloudsEnd;
        };
    """.trimIndent() + "\n" + globalsBlock

    private val includePattern = Regex(
        """^\s*#include\s*(?:<([\w.-]+):([\w/.-]+)>|"([\w/.-]+)")\s*$""",
        RegexOption.MULTILINE,
    )

    private val pushConstantsPattern = Regex(
        """\blayout\s*\(\s*push_constant\s*\)\s*uniform\s+\w+\s*\{[^}]*\}\s*\w*\s*;"""
    )

    private val sectionTimePattern =
        Regex("""\btexelFetch\s*\(\s*u_SectionTimeInfo\b""")

    private val sectionTimeUniformPattern =
        Regex("""\buniform\s+\w+\s+u_SectionTimeInfo\s*;""")

    private val uniformReplacements = linkedMapOf(
        "u_CurrentTime" to "0",
        "u_EnvironmentFog" to
                "vec2(atf_FogEnvironmentalStart, atf_FogEnvironmentalEnd)",
        "u_FadePeriodInv" to "0.0",
        "u_FogColor" to "atf_FogColor",
        "u_GameTime" to "atf_GameTime",
        "u_ModelViewMatrix" to "atf_ModelViewMat",
        "u_ProjectionMatrix" to "atf_ProjMat",
        "u_RegionID" to "0u",
        "u_RegionOffset" to "atf_ModelOffset",
        "u_RenderFog" to
                "vec2(atf_FogRenderDistanceStart, atf_FogRenderDistanceEnd)",
        "u_TexCoordShrink" to "vec2(0.0)",
        "u_TexelSize" to "(1.0 / vec2(textureSize($BLOCK_TEXTURE, 0)))",
        "u_UseRGSS" to "(atf_UseRgss == 1)",
    )

    private val versionPattern = Regex(
        """^\s*#version[^\n]*\n""",
        RegexOption.MULTILINE,
    )

    private val vertexAttributes = """
        layout(location = 0) in vec3 Position;
        layout(location = 1) in vec4 Color;
        layout(location = 2) in vec2 UV0;
        layout(location = 3) in ivec2 UV2;
    """.trimIndent() + "\n"

    private val vertexInputs = vertexAttributes + "\n" + """
        vec3 _vert_position;
        vec2 _vert_tex_diffuse_coord;
        vec2 _vert_tex_diffuse_coord_bias;
        vec2 _vert_tex_light_coord;
        vec4 _vert_color;
        uint _draw_id;
        uint _material_params;

        $ADDITIONS_MARKER

        void _vert_init() {
            _vert_position = atf_sway(Position + atf_ModelOffset, atf_ModelOffset, atf_CameraBlockPos, atf_CameraOffset,
                    atf_GameTime, SwayCell, SwayWeights) - atf_ModelOffset;
            _vert_color = Color;
            _vert_tex_diffuse_coord = UV0;
            _vert_tex_diffuse_coord_bias = vec2(0.0);

            // Keep light levels in their lower eight bits; Polytone stores the face above them.
            _vert_tex_light_coord = vec2(clamp(ivec2(UV2.x & 255, UV2.y & 255) + 8, 8, 248)) / 256.0;

            _draw_id = 0u;

            // Mipmapped cutout material, with Polytone's face information preserved.
            _material_params = 5u;
            _material_params |= uint((UV2.x >> 8) & 7) << 5u;
        }
    """.trimIndent() + "\n"

    private var compiledFor: CompiledRenderPipeline? = null

    /** Provides fully expanded shader text without owning external resources. */
    private data class BuiltSource(
        val vertex: String,
        val fragment: String,
    ) : ShaderSource {
        override fun close() {
            // Shader text has no resources to release.
        }

        override fun getInclude(id: Identifier): ShaderSource.CachedIncludeSource? {
            return null
        }

        override fun getShader(id: Identifier, type: ShaderType): String? {
            if (id != shaderId) {
                return null
            }

            return if (type == ShaderType.VERTEX) vertex else fragment
        }
    }

    /** Replaces Sodium uniforms with values supplied by Minecraft's uniform blocks. */
    private fun adapt(
        source: String,
        globalsLeftOut: Boolean,
        cutout: Boolean,
    ): String? {
        val version = versionPattern.find(source) ?: return null
        var body = source.substring(version.range.last + 1)
        val header = StringBuilder(version.value)

        for (define in defines) {
            if (!cutout && define.startsWith("ALPHA_CUTOUT")) {
                continue
            }

            header.append("#define ")
                .append(define)
                .append('\n')
        }

        header.append(gameUniforms)

        for ((name, replacement) in uniformReplacements) {
            val declaration = Regex(
                """\buniform\s+\w+\s+${Regex.escape(name)}\s*;"""
            )

            val declared = declaration.containsMatchIn(body)

            if (declared) {
                body = declaration.replace(body, "")
            }

            if (declared || (globalsLeftOut && name in globalUniforms)) {
                header.append("#define ")
                    .append(name)
                    .append(' ')
                    .append(replacement)
                    .append('\n')
            }
        }

        val adaptedBody = withoutSodiumDrawState(body) ?: return null

        return header.append(adaptedBody).toString()
    }

    /**
     * Returns a cached adapted pipeline, compiling it once after each terrain-shader reload.
     *
     * Call during frame preparation, before opening a render pass. Failed compilations are
     * cached until the next reload.
     */
    @JvmStatic
    fun compiled(
        pipeline: RenderPipeline,
        cutout: Boolean,
    ): CompiledRenderPipeline? {
        if (!isAvailable) {
            return null
        }

        val terrain = RenderSystem.getCompiledPipelineNullable(
            RenderPipelines.SOLID_TERRAIN
        )

        if (terrain !== compiledFor) {
            compiledFor = terrain

            for (compiledPipeline in compiledPipelines.values) {
                compiledPipeline?.close()
            }

            compiledPipelines.clear()
        }

        if (compiledPipelines.containsKey(pipeline)) {
            return compiledPipelines.get(pipeline)
        }

        var compiledPipeline: CompiledRenderPipeline? = null
        val vertex = vertex(cutout)
        val fragment = fragment(cutout)

        if (vertex == null || fragment == null) {
            AllThingsFoliage.logger.warn(
                "Sodium's chunk shaders in use can't be read by the GPU foliage renderer; " +
                        "foliage near the player is drawn with the mod's own shaders, " +
                        "which may not match a resource pack's look"
            )
        } else {
            try {
                compiledPipeline = RenderSystem.getDevice()
                    .compilePipeline(
                        pipeline,
                        BuiltSource(vertex, fragment),
                        Util.backgroundExecutor(),
                    )
                    .join()
                    .finishCompile()
            } catch (exception: RuntimeException) {
                AllThingsFoliage.logger.warn(
                    "Sodium's chunk shaders did not compile with the GPU foliage renderer's " +
                            "vertices; foliage near the player is drawn with the mod's own shaders",
                    exception,
                )
            }
        }

        compiledPipelines.put(pipeline, compiledPipeline)

        return compiledPipeline
    }

    /** Expands includes once, substituting foliage inputs and omitting Sodium's globals. */
    private fun expand(
        source: String,
        from: String,
        vertexInputs: String?,
        included: MutableSet<String>,
    ): String? {
        val result = StringBuilder()
        var copiedUntil = 0

        for (match in includePattern.findAll(source)) {
            val name = includedResourceName(match, from)
            var replacement = ""

            if (included.add(name) && name != GLOBALS_INCLUDE) {
                replacement = if (name == CHUNK_VERTEX_INCLUDE && vertexInputs != null) {
                    expand(vertexInputs, name, null, included) ?: return null
                } else {
                    val includedSource = read(name) ?: return null

                    expand(
                        includedSource,
                        name,
                        vertexInputs,
                        included,
                    ) ?: return null
                }
            }

            result.append(source, copiedUntil, match.range.first)
            result.append(replacement)
            copiedUntil = match.range.last + 1
        }

        result.append(source, copiedUntil, source.length)

        return result.toString()
    }

    private fun fragment(cutout: Boolean): String? {
        val name = shaderName(OPAQUE_FRAGMENT)
        val raw = read(name) ?: return null
        val included = mutableSetOf<String>()
        val expanded = expand(raw, name, null, included) ?: return null

        return adapt(expanded, GLOBALS_INCLUDE in included, cutout)
    }

    /** Resolves namespaced includes or paths relative to their containing shader. */
    private fun includedResourceName(match: MatchResult, from: String): String {
        val namespace = match.groups[1]?.value

        if (namespace != null) {
            return "$namespace:shaders/include/${match.groupValues[2]}"
        }

        return from.substring(0, from.lastIndexOf('/') + 1) + match.groupValues[3]
    }

    /** Reads the highest-priority resource pack's copy of a shader. */
    private fun read(name: String): String? {
        val identifier = Identifier.tryParse(name) ?: return null

        val resource = Minecraft.getInstance()
            .resourceManager
            .getResource(identifier)
            .getOrNull()
            ?: return null

        try {
            return resource.openAsReader().use { reader ->
                reader.readText()
            }
        } catch (_: IOException) {
            return null
        }
    }

    private fun shaderName(path: String): String {
        return "sodium:shaders/$path"
    }

    private fun vertex(cutout: Boolean): String? {
        val name = shaderName(OPAQUE_VERTEX)
        val raw = read(name) ?: return null

        if (!raw.contains("_vert_init")) {
            return null
        }

        var inputs = vertexInputs.replace(
            ADDITIONS_MARKER,
            versionPattern.replaceFirst(additionsSource, ""),
        )

        if (!cutout) {
            inputs = inputs.replace(CUTOUT_MATERIAL, SOLID_MATERIAL)
        }

        val included = mutableSetOf<String>()
        val expanded = expand(raw, name, inputs, included) ?: return null

        if (!expanded.contains("void _vert_init()")) {
            return null
        }

        return adapt(expanded, GLOBALS_INCLUDE in included, cutout)
    }

    /**
     * Removes Sodium-owned draw state and treats every section as fully faded in.
     *
     * Balanced parentheses preserve complete replacement of nested section-time expressions.
     */
    private fun withoutSodiumDrawState(source: String): String? {
        var body = pushConstantsPattern.replace(source, "")
        body = chunkFadesBlockPattern.replace(body, "")
        body = chunkFadePattern.replace(body, "-1")
        body = sectionTimeUniformPattern.replace(body, "")

        val result = StringBuilder()
        var copiedUntil = 0

        while (true) {
            val read = sectionTimePattern.find(body, copiedUntil) ?: break
            val open = body.indexOf('(', read.range.first)
            var depth = 0
            var close = -1

            for (index in open until body.length) {
                when (body[index]) {
                    '(' -> {
                        depth++
                    }

                    ')' -> {
                        depth--

                        if (depth == 0) {
                            close = index
                            break
                        }
                    }
                }
            }

            if (close < 0) {
                return null
            }

            result.append(body, copiedUntil, read.range.first)
            result.append("ivec4(-1)")
            copiedUntil = close + 1
        }

        result.append(body, copiedUntil, body.length)

        return result.toString()
    }
}
