package io.github.andrewcwhy.allthingsfoliage.platform

import java.io.IOException
import java.nio.file.Path

/**
 * Provides loader-independent access to platform-specific functionality.
 *
 * Implementations supply mod detection, configuration paths, and loader
 * identification for Fabric and NeoForge.
 */
interface Platform {
    /** Returns the directory containing the loader's mod configuration files. */
    val configDir: Path

    /** Returns the active Minecraft mod loader. */
    val loader: ModLoader

    /** Returns whether the specified mod is loaded. */
    fun isModLoaded(modId: String): Boolean

    /**
     * Reads every resource matching the specified path across available JARs.
     *
     * Preserves successfully read resources if an I/O error occurs.
     *
     * @param path Classpath-relative resource path.
     * @return Resource contents in classloader enumeration order.
     */
    fun readAll(path: String): List<ByteArray> {
        val resources = mutableListOf<ByteArray>()

        try {
            val urls = Platform::class.java.classLoader.getResources(path)

            while (urls.hasMoreElements()) {
                urls.nextElement().openStream().use { stream ->
                    resources.add(stream.readAllBytes())
                }
            }
        } catch (_: IOException) {
            // Retain resources successfully read before the failure.
        }

        return resources
    }

    /** Supported Minecraft mod loaders. */
    enum class ModLoader {
        FABRIC,
        NEOFORGE
    }
}
