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
 * Adds foliage movement to the active terrain shaders while preserving resource-pack rendering.
 *
 * Includes are expanded before compilation. Unsupported shader layouts or compilation failures
 * leave the renderer using its own fallback shaders.
 */
object TerrainFoliageShader {
    @JvmField
    val TERRAIN: Identifier = Identifier.withDefaultNamespace("core/terrain")

    @JvmField
    val VERTEX: Identifier = Identifier.fromNamespaceAndPath(
        AllThingsFoliage.MOD_ID,
        "core/foliage_terrain",
    )

    private const val FRAGMENT_FILE = "minecraft:shaders/core/terrain.fsh"
    private const val VERTEX_FILE = "minecraft:shaders/core/terrain.vsh"

    private val additions = """
        layout(location = 14) in float SwayCell;
        layout(location = 15) in float SwayWeights;
        #include <allthingsfoliage:foliage_uniforms.glsl>
        #include <allthingsfoliage:sway.glsl>
    """.trimIndent() + "\n"

    private val compiledPipelines =
        Reference2ObjectOpenHashMap<RenderPipeline, CompiledRenderPipeline?>()

    private val includePattern = Regex(
        """^\s*#include\s*(?:<([\w.-]+):([\w/.-]+)>|"([\w/.-]+)")\s*$""",
        RegexOption.MULTILINE,
    )

    private val mainPattern = Regex(
        """\bvoid\s+main\s*\(\s*(?:void\s*)?\)"""
    )

    private val positionPattern = Regex("""\bPosition\b""")

    private val renamedInputPattern = Regex(
        """((?:layout\s*\([^)]*\)\s*)?)in\s+vec3\s+atf_Position\s*;"""
    )

    private val requiredDeclarations = listOf(
        "CameraBlockPos",
        "CameraOffset",
        "ChunkPosition",
        "GameTime",
    ).map { name ->
        Regex("""\b${Regex.escape(name)}\b""")
    }

    private val swayedMain = "\n" + """
        void main() {
            vec3 atf_sectionOffset = vec3(ChunkPosition - CameraBlockPos) + CameraOffset;
            atf_Position = atf_sway(Position + atf_sectionOffset, atf_sectionOffset, CameraBlockPos, CameraOffset,
                    GameTime, SwayCell, SwayWeights) - atf_sectionOffset;
            atf_terrainMain();
        }
    """.trimIndent() + "\n"

    private val versionPattern = Regex(
        """^\s*#version[^\n]*\n""",
        RegexOption.MULTILINE,
    )

    private var compiledFor: CompiledRenderPipeline? = null
    private var source: BuiltSource? = null
    private var sourceRead = false

    /** Supplies fully expanded shaders without owning any external resources. */
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
            if (type == ShaderType.VERTEX) {
                return vertex.takeIf { id == VERTEX }
            }

            return fragment.takeIf { id == TERRAIN }
        }
    }

    /**
     * Compiles each pipeline once per terrain-shader reload, caching failures as well as successes.
     *
     * Call during frame preparation, before opening a render pass.
     */
    @JvmStatic
    fun compiled(pipeline: RenderPipeline): CompiledRenderPipeline? {
        val terrain = RenderSystem.getCompiledPipelineNullable(
            RenderPipelines.SOLID_TERRAIN
        )

        if (terrain !== compiledFor) {
            compiledFor = terrain

            for (compiledPipeline in compiledPipelines.values) {
                compiledPipeline?.close()
            }

            compiledPipelines.clear()
            sourceRead = false
        }

        if (compiledPipelines.containsKey(pipeline)) {
            return compiledPipelines.get(pipeline)
        }

        if (!sourceRead) {
            sourceRead = true
            source = readSource()
        }

        var compiledPipeline: CompiledRenderPipeline? = null
        val shaderSource = source

        if (shaderSource != null) {
            try {
                compiledPipeline = RenderSystem.getDevice()
                    .compilePipeline(
                        pipeline,
                        shaderSource,
                        Util.backgroundExecutor(),
                    )
                    .join()
                    .finishCompile()
            } catch (exception: RuntimeException) {
                AllThingsFoliage.logger.warn(
                    "The terrain shaders in use did not compile with the GPU foliage renderer's " +
                            "sway; foliage near the player is drawn with the renderer's own shaders",
                    exception,
                )
            }
        }

        compiledPipelines.put(pipeline, compiledPipeline)

        return compiledPipeline
    }

    /** Expands namespaced and relative includes once per shader compilation unit. */
    private fun expand(
        source: String,
        from: String,
        included: MutableSet<String>,
    ): String? {
        val result = StringBuilder()
        var copiedUntil = 0

        for (match in includePattern.findAll(source)) {
            val namespace = match.groups[1]?.value

            val name = if (namespace != null) {
                "$namespace:shaders/include/${match.groupValues[2]}"
            } else {
                from.substring(0, from.lastIndexOf('/') + 1) + match.groupValues[3]
            }

            var replacement = ""

            if (included.add(name)) {
                val includeSource = read(name) ?: return null
                replacement = expand(includeSource, name, included) ?: return null
            }

            result.append(source, copiedUntil, match.range.first)
            result.append(replacement)
            copiedUntil = match.range.last + 1
        }

        result.append(source, copiedUntil, source.length)

        return result.toString()
    }

    /** Reads the highest-priority resource pack's copy of a shader file. */
    private fun read(name: String): String? {
        val separator = name.indexOf(':')
        val identifier = Identifier.fromNamespaceAndPath(
            name.substring(0, separator),
            name.substring(separator + 1),
        )

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

    /** Expands terrain declarations before splicing, then expands the added foliage includes. */
    private fun readSource(): BuiltSource? {
        val included = mutableSetOf<String>()

        val vertexSource = read(VERTEX_FILE)
        val terrainVertex = vertexSource?.let {
            expand(it, VERTEX_FILE, included)
        }

        val spliced = terrainVertex?.let {
            splice(it, additions)
        }

        val vertex = spliced?.let {
            expand(it, VERTEX_FILE, included)
        }

        val fragmentSource = read(FRAGMENT_FILE)
        val fragment = fragmentSource?.let {
            expand(it, FRAGMENT_FILE, mutableSetOf())
        }

        if (vertex == null || fragment == null) {
            AllThingsFoliage.logger.warn(
                "The terrain shaders in use can't be read by the GPU foliage renderer; " +
                        "foliage near the player is drawn with the renderer's own shaders, " +
                        "which may not match a resource pack's look"
            )

            return null
        }

        return BuiltSource(vertex, fragment)
    }

    /** Adds foliage inputs and a wrapper entry point to a compatible terrain vertex shader. */
    @JvmStatic
    fun splice(terrain: String, additions: String): String? {
        if (!mainPattern.containsMatchIn(terrain)) {
            return null
        }

        for (declaration in requiredDeclarations) {
            if (!declaration.containsMatchIn(terrain)) {
                return null
            }
        }

        val renamedMain = mainPattern.replaceFirst(
            terrain,
            "void atf_terrainMain()",
        )

        val renamed = positionPattern.replace(renamedMain, "atf_Position")
        val input = renamedInputPattern.find(renamed) ?: return null

        return buildString {
            append(renamed, 0, input.range.first)
            append(input.groupValues[1])
            append("in vec3 Position;\n")
            append("vec3 atf_Position;\n")
            append(versionPattern.replaceFirst(additions, ""))
            append('\n')
            append(renamed, input.range.last + 1, renamed.length)
            append(swayedMain)
        }
    }
}
