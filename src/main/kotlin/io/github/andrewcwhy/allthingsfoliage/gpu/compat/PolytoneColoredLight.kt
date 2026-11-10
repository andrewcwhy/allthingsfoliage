package io.github.andrewcwhy.allthingsfoliage.gpu.compat

import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

/**
 * Applies Polytone's colored lighting to GPU foliage shaders without a compile-time dependency.
 *
 * Missing or incompatible implementations retain vanilla lighting.
 */
internal object PolytoneColoredLight {

    private const val PATCHER_CLASS =
        "net.mehvahdjukaar.polytone.content.shaders.light.LightShaderPatcher"

    /** Identifies terrain shaders that use Polytone's colored lighting. */
    const val TINTED_READ = "_polyColoredLightmap"

    private val globalsPattern =
        Regex("""layout\s*\(\s*std140\s*\)\s*uniform\s+Globals\s*\{([^}]*)\}\s*;""")

    private val memberPattern = Regex("""\b\w+\s+(\w+)\s*;""")

    private val versionPattern =
        Regex("""^\s*#version[^\n]*\n""", RegexOption.MULTILINE)

    private val patchSodiumShader: MethodHandle? =
        findPatchMethod("patchSodiumChunkShader")

    private var failed = false

    private fun findPatchMethod(name: String): MethodHandle? {
        if (!AllThingsFoliage.platform.isModLoaded("polytone")) {
            return null
        }

        try {
            val patcherClass = Class.forName(
                PATCHER_CLASS,
                false,
                PolytoneColoredLight::class.java.classLoader,
            )

            return MethodHandles.publicLookup().findStatic(
                patcherClass,
                name,
                MethodType.methodType(String::class.java, String::class.java),
            )
        } catch (exception: Throwable) {
            if (
                exception !is ReflectiveOperationException &&
                exception !is LinkageError
            ) {
                throw exception
            }

            // Older or incompatible Polytone versions may not expose colored lighting.
            return null
        }
    }

    /**
     * Replaces Polytone's globals with the renderer's block and aliases its member names.
     *
     * Unrecognized shader layouts remain unchanged so shader compilation can use its fallback.
     */
    private fun shareGlobals(
        original: String,
        tinted: String,
        globalsBlock: String,
    ): String {
        val version = versionPattern.find(original) ?: return tinted
        val afterVersion = version.range.last + 1
        val following = original.substring(
            afterVersion,
            minOf(original.length, afterVersion + 400),
        )

        val copyStarts = tinted.indexOf(following, afterVersion)

        if (copyStarts < 0) {
            return tinted
        }

        val ownBlock = tinted.indexOf(globalsBlock, copyStarts)

        if (ownBlock < 0) {
            return tinted
        }

        val polytone = tinted.substring(afterVersion, copyStarts)
        val block = globalsPattern.find(polytone) ?: return tinted
        val renames = StringBuilder()

        for (member in memberPattern.findAll(block.groupValues[1])) {
            val name = member.groupValues[1]

            if (!globalsBlock.contains(" atf_$name;")) {
                return tinted
            }

            renames.append("#define ")
                .append(name)
                .append(" atf_")
                .append(name)
                .append('\n')
        }

        return buildString {
            append(tinted, 0, afterVersion)
            append(globalsBlock)
            append(renames)
            append(polytone, 0, block.range.first)
            append(polytone, block.range.last + 1, polytone.length)
            append('\n')
            append(tinted, copyStarts, ownBlock)
            append(tinted, ownBlock + globalsBlock.length, tinted.length)
        }
    }

    /** Adds colored lighting only when the terrain shader confirms that Polytone uses it. */
    @JvmStatic
    fun tintSodiumVertex(
        vertex: String?,
        terrainVertex: String?,
        globalsBlock: String,
    ): String? {
        val patcher = patchSodiumShader ?: return vertex

        if (
            failed ||
            vertex == null ||
            terrainVertex == null ||
            !terrainVertex.contains(TINTED_READ)
        ) {
            return vertex
        }

        try {
            val tinted = patcher.invoke(vertex) as String
            return shareGlobals(vertex, tinted, globalsBlock)
        } catch (exception: Throwable) {
            failed = true

            AllThingsFoliage.logger.warn(
                "Polytone's coloured lights could not be added to the GPU foliage's shaders; " +
                        "the foliage near the player keeps the game's light",
                exception,
            )

            return vertex
        }
    }
}
