package io.github.andrewcwhy.allthingsfoliage.gpu.shader

import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage
import java.lang.reflect.Field

/**
 * Controls Iris's vertex-format expansion while foliage geometry is meshed.
 *
 * Standard foliage pipelines suppress expansion; shader-pack pipelines require it.
 * Missing integration hooks leave Iris's behavior unchanged.
 */
object IrisVertexExtension {
    private const val IMMEDIATE_STATE_CLASS =
        "net.irisshaders.iris.vertices.ImmediateState"

    private val renderingLevelField: Field?
    private val skipExtension: ThreadLocal<Boolean?>?

    private var renderingLevelBefore: Boolean? = null

    init {
        var renderingLevel: Field? = null
        var extensionOverride: ThreadLocal<Boolean?>? = null

        if (AllThingsFoliage.platform.isModLoaded("iris")) {
            try {
                val field = Class.forName(IMMEDIATE_STATE_CLASS)
                    .getField("skipExtension")

                @Suppress("UNCHECKED_CAST")
                val override = field.get(null) as ThreadLocal<Boolean?>?

                extensionOverride = override
            } catch (exception: Exception) {
                if (
                    exception !is ReflectiveOperationException &&
                    exception !is ClassCastException
                ) {
                    throw exception
                }

                AllThingsFoliage.logger.warn(
                    "Iris is installed but its vertex format extension could not be turned " +
                            "off for the foliage renderer; drawing foliage on the GPU may fail " +
                            "while a shader pack is loaded",
                    exception,
                )
            }

            try {
                val field = Class.forName(IMMEDIATE_STATE_CLASS)
                    .getField("isRenderingLevel")

                if (field.type == Boolean::class.java) {
                    renderingLevel = field
                }
            } catch (_: ReflectiveOperationException) {
                // Retain Iris's existing rendering-level state if its hook is unavailable.
            }
        }

        renderingLevelField = renderingLevel
        skipExtension = extensionOverride
    }

    /**
     * Enables Iris's expanded vertex format for shader-pack meshing.
     *
     * Temporarily marks the level as rendering because meshing occurs before the draw phase.
     * Render thread only; restore the returned state with [end].
     */
    @JvmStatic
    fun allow(): Boolean {
        val extensionOverride = skipExtension ?: return false
        val previous = extensionOverride.get() == true

        extensionOverride.set(false)

        val field = renderingLevelField

        if (field != null && renderingLevelBefore == null) {
            try {
                renderingLevelBefore = field.getBoolean(null)
                field.setBoolean(null, true)
            } catch (_: IllegalAccessException) {
                renderingLevelBefore = null
            }
        }

        return previous
    }

    /**
     * Suppresses vertex-format expansion on the current thread.
     *
     * Restore the returned state with [end], preferably in a `finally` block.
     */
    @JvmStatic
    fun begin(): Boolean {
        val extensionOverride = skipExtension ?: return false
        val previous = extensionOverride.get() == true

        extensionOverride.set(true)

        return previous
    }

    /** Restores the extension override and any temporarily changed rendering-level state. */
    @JvmStatic
    fun end(previous: Boolean) {
        skipExtension?.set(previous)

        val previousRenderingLevel = renderingLevelBefore ?: return

        try {
            renderingLevelField?.setBoolean(null, previousRenderingLevel)
        } catch (_: IllegalAccessException) {
            // Restoration is best-effort if Iris's field becomes inaccessible.
        }

        renderingLevelBefore = null
    }
}
