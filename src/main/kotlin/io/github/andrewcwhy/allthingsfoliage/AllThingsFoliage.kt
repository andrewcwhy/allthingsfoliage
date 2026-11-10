package io.github.andrewcwhy.allthingsfoliage

import com.github.razorplay01.sway.registry.SwayRegistry
import io.github.andrewcwhy.allthingsfoliage.platform.Platform
//? fabric {
import io.github.andrewcwhy.allthingsfoliage.platform.fabric.FabricPlatform
//?} else {
/*
import io.github.andrewcwhy.allthingsfoliage.platform.neoforge.NeoforgePlatform
*/
//?}
import net.minecraft.resources.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Main entry point for All Things Foliage.
 *
 * Handles mod initialization, platform selection, and compatibility registration for Fabric and
 * NeoForge.
 */
@Suppress("LoggingSimilarMessage")
object AllThingsFoliage {
    const val MOD_ID = /*$ mod_id*/ "allthingsfoliage"
    const val MOD_NAME = /*$ mod_name*/ "All Things Foliage"
    const val MOD_VERSION = /*$ mod_version*/ "0.1.0"

    val logger: Logger = LoggerFactory.getLogger(MOD_ID)

    val platform: Platform = createPlatformInstance()

    fun id(path: String): Identifier {
        return Identifier.fromNamespaceAndPath(MOD_ID, path)
    }

    /** Initializes common mod functionality. */
    fun onInitialize() {
        logger.info(
            "Initializing {} on {}",
            MOD_ID,
            platform.loader,
        )

        logger.debug(
            "{}: { version: {}; friendly_name: {} }",
            MOD_ID,
            MOD_VERSION,
            MOD_NAME,
        )
    }

    /** Initializes client-side functionality. */
    fun onInitializeClient() {
        SwayRegistry.initialize()
        logger.info(
            "Initializing {} Client on {}",
            MOD_ID,
            platform.loader,
        )

        logger.debug(
            "{}: { version: {}; friendly_name: {} }",
            MOD_ID,
            MOD_VERSION,
            MOD_NAME,
        )
    }

    /**
     * Registers block compatibility after registries are populated.
     *
     * Fabric invokes this from its client entry point, while NeoForge invokes it during client
     * setup.
     */
    fun onRegistriesReady() {
        ModCompatRegistry.initialize()
        SwayRemovals.initialize()
    }

    /** Creates the platform implementation for the active mod loader. */
    private fun createPlatformInstance(): Platform {
        //? fabric {
        return FabricPlatform()
        //?} else {
        /*return NeoforgePlatform()
         */
        //?}
    }
}
