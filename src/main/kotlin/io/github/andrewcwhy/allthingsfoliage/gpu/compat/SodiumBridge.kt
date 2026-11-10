package io.github.andrewcwhy.allthingsfoliage.gpu.compat

import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage
import io.github.andrewcwhy.allthingsfoliage.gpu.GpuFoliageSplit
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import net.minecraft.client.renderer.texture.TextureAtlasSprite

/**
 * Integrates GPU foliage with Sodium's rendering APIs for Minecraft 26.3 and newer.
 *
 * Missing renderer operations retain frustum visibility and vanilla rebuild fallbacks.
 * Internal Sodium API changes may require updates to the reflective lookups.
 */
object SodiumBridge {
    private const val MOD_ID = "sodium"
    private const val RENDERER_CLASS =
        "net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer"

    private const val SODIUM_CLIENT_PACKAGE = "net.caffeinemc.mods.sodium.client."
    private const val SPRITE_UTIL_CLASS =
        "net.caffeinemc.mods.sodium.api.texture.SpriteUtil"

    private val boxVisibilityQuery: MethodHandle?
    private val instanceGetter: MethodHandle?
    private val markSpriteActive: MethodHandle?
    private val rebuildRequest: MethodHandle?
    private val spriteAnimationQuery: MethodHandle?

    init {
        var instance: MethodHandle? = null
        var visibility: MethodHandle? = null
        var rebuild: MethodHandle? = null

        if (AllThingsFoliage.platform.isModLoaded(MOD_ID)) {
            try {
                val rendererClass = Class.forName(RENDERER_CLASS)
                val lookup = MethodHandles.publicLookup()

                instance = lookup.findStatic(
                    rendererClass,
                    "instanceNullable",
                    MethodType.methodType(rendererClass),
                ).asType(
                    MethodType.methodType(Any::class.java)
                )

                visibility = lookup.findVirtual(
                    rendererClass,
                    "isBoxVisible",
                    MethodType.methodType(
                        Boolean::class.java,
                        Double::class.java,
                        Double::class.java,
                        Double::class.java,
                        Double::class.java,
                        Double::class.java,
                        Double::class.java,
                    ),
                ).asType(
                    MethodType.methodType(
                        Boolean::class.java,
                        Any::class.java,
                        Double::class.java,
                        Double::class.java,
                        Double::class.java,
                        Double::class.java,
                        Double::class.java,
                        Double::class.java,
                    )
                )

                rebuild = lookup.findVirtual(
                    rendererClass,
                    "scheduleRebuildForChunk",
                    MethodType.methodType(
                        Void.TYPE,
                        Int::class.java,
                        Int::class.java,
                        Int::class.java,
                        Boolean::class.java,
                    ),
                ).asType(
                    MethodType.methodType(
                        Void.TYPE,
                        Any::class.java,
                        Int::class.java,
                        Int::class.java,
                        Int::class.java,
                        Boolean::class.java,
                    )
                )
            } catch (exception: ReflectiveOperationException) {
                AllThingsFoliage.logger.warn(
                    "Sodium is installed but could not be reached; foliage will be culled by " +
                            "the camera frustum only, and near foliage may keep being drawn by " +
                            "the chunk mesh until something else rebuilds it",
                    exception,
                )

                instance = null
                visibility = null
                rebuild = null
            }
        }

        boxVisibilityQuery = visibility
        instanceGetter = instance
        rebuildRequest = rebuild
    }

    init {
        var mark: MethodHandle? = null
        var animation: MethodHandle? = null

        if (AllThingsFoliage.platform.isModLoaded(MOD_ID)) {
            try {
                val lookup = MethodHandles.publicLookup()
                val utilityClass = Class.forName(SPRITE_UTIL_CLASS)
                val utility = utilityClass.getField("INSTANCE").get(null)

                mark = lookup.findVirtual(
                    utilityClass,
                    "markSpriteActive",
                    MethodType.methodType(
                        Void.TYPE,
                        TextureAtlasSprite::class.java,
                    ),
                ).bindTo(utility)

                animation = lookup.findVirtual(
                    utilityClass,
                    "hasAnimation",
                    MethodType.methodType(
                        Boolean::class.java,
                        TextureAtlasSprite::class.java,
                    ),
                ).bindTo(utility)
            } catch (exception: Throwable) {
                if (
                    exception !is ReflectiveOperationException &&
                    exception !is LinkageError
                ) {
                    throw exception
                }

                mark = null
                animation = null

                AllThingsFoliage.logger.warn(
                    "Sodium's sprite API could not be reached; animated foliage textures " +
                            "may stand still near the player",
                    exception,
                )
            }
        }

        markSpriteActive = mark
        spriteAnimationQuery = animation
    }

    /** Resolves mesh-result members only when Sodium first reports an upload. */
    private object MeshSwaps {
        private const val SECTION_FIELD = "section"
        private const val SECTION_OWNER_CLASS =
            SODIUM_CLIENT_PACKAGE + "render.chunk.compile.BuilderTaskOutput"

        val buildOutputClass: Class<*>?
        val chunkXGetter: MethodHandle?
        val chunkYGetter: MethodHandle?
        val chunkZGetter: MethodHandle?
        val sectionGetter: MethodHandle?

        init {
            var buildOutput: Class<*>? = null
            var section: MethodHandle? = null
            var chunkX: MethodHandle? = null
            var chunkY: MethodHandle? = null
            var chunkZ: MethodHandle? = null

            try {
                val taskOutputClass = Class.forName(SECTION_OWNER_CLASS)
                val renderSectionClass = Class.forName(
                    SODIUM_CLIENT_PACKAGE + "render.chunk.RenderSection"
                )

                val lookup = MethodHandles.publicLookup()

                section = lookup.findGetter(
                    taskOutputClass,
                    SECTION_FIELD,
                    renderSectionClass,
                ).asType(
                    MethodType.methodType(Any::class.java, Any::class.java)
                )

                val coordinateType = MethodType.methodType(Int::class.java)
                val erasedCoordinateType = MethodType.methodType(
                    Int::class.java,
                    Any::class.java,
                )

                chunkX = lookup.findVirtual(
                    renderSectionClass,
                    "getChunkX",
                    coordinateType,
                ).asType(erasedCoordinateType)

                chunkY = lookup.findVirtual(
                    renderSectionClass,
                    "getChunkY",
                    coordinateType,
                ).asType(erasedCoordinateType)

                chunkZ = lookup.findVirtual(
                    renderSectionClass,
                    "getChunkZ",
                    coordinateType,
                ).asType(erasedCoordinateType)

                buildOutput = Class.forName(
                    SODIUM_CLIENT_PACKAGE + "render.chunk.compile.ChunkBuildOutput"
                )
            } catch (exception: ReflectiveOperationException) {
                AllThingsFoliage.logger.warn(
                    "Sodium's chunk build results could not be read; foliage handed " +
                            "back to the chunk mesh may blink out for a moment",
                    exception,
                )

                buildOutput = null
            }

            buildOutputClass = buildOutput
            chunkXGetter = chunkX
            chunkYGetter = chunkY
            chunkZGetter = chunkZ
            sectionGetter = section
        }
    }

    @JvmStatic
    fun animates(sprite: TextureAtlasSprite): Boolean {
        val query = spriteAnimationQuery ?: return false

        try {
            return query.invoke(sprite) as Boolean
        } catch (_: Throwable) {
            return false
        }
    }

    @JvmStatic
    fun canSplitChunkMeshes(): Boolean {
        return true
    }

    /** Returns whether Sodium owns terrain rendering. */
    @JvmStatic
    fun drawsChunks(): Boolean {
        return AllThingsFoliage.platform.isModLoaded(MOD_ID)
    }

    /** Treats query failures as visible to avoid incorrectly hiding foliage. */
    @JvmStatic
    fun isVisible(
        renderer: Any?,
        minX: Double,
        minY: Double,
        minZ: Double,
        maxX: Double,
        maxY: Double,
        maxZ: Double,
    ): Boolean {
        val query = boxVisibilityQuery ?: return true

        try {
            return query.invoke(
                renderer,
                minX,
                minY,
                minZ,
                maxX,
                maxY,
                maxZ,
            ) as Boolean
        } catch (_: Throwable) {
            return true
        }
    }

    /** Keeps animated sprites moving when their foliage is absent from Sodium's chunk mesh. */
    @JvmStatic
    fun markActive(sprites: Array<TextureAtlasSprite>) {
        val mark = markSpriteActive ?: return

        try {
            for (sprite in sprites) {
                mark.invoke(sprite)
            }
        } catch (_: Throwable) {
            // Failed sprite tracking leaves the existing animation state unchanged.
        }
    }

    /** Reports geometry replacements, excluding uploads that only reorder translucent geometry. */
    @JvmStatic
    fun onResultsUploaded(results: Collection<*>) {
        val buildOutputClass = MeshSwaps.buildOutputClass ?: return
        val sectionGetter = MeshSwaps.sectionGetter ?: return
        val chunkXGetter = MeshSwaps.chunkXGetter ?: return
        val chunkYGetter = MeshSwaps.chunkYGetter ?: return
        val chunkZGetter = MeshSwaps.chunkZGetter ?: return

        try {
            for (output in results) {
                if (!buildOutputClass.isInstance(output)) {
                    continue
                }

                val section = sectionGetter.invoke(output)

                GpuFoliageSplit.onChunkMeshSwapped(
                    chunkXGetter.invoke(section) as Int,
                    chunkYGetter.invoke(section) as Int,
                    chunkZGetter.invoke(section) as Int,
                )
            }
        } catch (_: Throwable) {
            // GpuFoliageSplit's safety net handles replacements that were not reported.
        }
    }

    /** Returns the active Sodium world renderer, if available. */
    @JvmStatic
    fun renderer(): Any? {
        val getter = instanceGetter ?: return null

        try {
            return getter.invoke()
        } catch (_: Throwable) {
            return null
        }
    }

    /** Requests a Sodium section rebuild alongside the renderer's vanilla rebuild route. */
    @JvmStatic
    fun scheduleRebuild(sectionX: Int, sectionY: Int, sectionZ: Int) {
        val renderer = renderer() ?: return
        val request = rebuildRequest ?: return

        try {
            request.invoke(
                renderer,
                sectionX,
                sectionY,
                sectionZ,
                false,
            )
        } catch (_: Throwable) {
            // The caller also requests a rebuild through vanilla.
        }
    }

    @JvmStatic
    fun tracksSprites(): Boolean {
        return markSpriteActive != null && spriteAnimationQuery != null
    }
}
