package io.github.andrewcwhy.allthingsfoliage;

import com.github.razorplay01.sway.registry.SwayRegistry;
import io.github.andrewcwhy.allthingsfoliage.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//? fabric {
import io.github.andrewcwhy.allthingsfoliage.platform.fabric.FabricPlatform;
//?} else {
/*import io.github.andrewcwhy.allthingsfoliage.platform.neoforge.NeoforgePlatform;
*/
//?}

@SuppressWarnings("LoggingSimilarMessage")
public class AllThingsFoliage {

    public static final String MOD_ID = /*$ mod_id*/ "allthingsfoliage";
    public static final String MOD_VERSION = /*$ mod_version*/ "0.1.0";
    public static final String MOD_FRIENDLY_NAME =
        /*$ mod_name*/ "All Things Foliage";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final Platform PLATFORM = createPlatformInstance();

    public static void onInitialize() {
        LOGGER.info(
            "Initializing {} on {}",
            MOD_ID,
            AllThingsFoliage.xplat().loader()
        );
        LOGGER.debug(
            "{}: { version: {}; friendly_name: {} }",
            MOD_ID,
            MOD_VERSION,
            MOD_FRIENDLY_NAME
        );
        SwayRegistry.initialize();
    }

    /**
     * Registers third-party block compatibility once all block registries
     * are populated.
     *
     * <p>Fabric calls this from the client entrypoint. NeoForge calls it
     * from the client setup event because its mod constructor runs before
     * block registration is complete.
     */
    public static void onRegistriesReady() {
        ModCompatRegistry.initialize();
        SwayRemovals.initialize();
    }

    public static void onInitializeClient() {
        LOGGER.info(
            "Initializing {} Client on {}",
            MOD_ID,
            AllThingsFoliage.xplat().loader()
        );
        LOGGER.debug(
            "{}: { version: {}; friendly_name: {} }",
            MOD_ID,
            MOD_VERSION,
            MOD_FRIENDLY_NAME
        );
    }

    public static Platform xplat() {
        return PLATFORM;
    }

    private static Platform createPlatformInstance() {
        
//? fabric {
        return new FabricPlatform();
//?} else {
/*        return new NeoforgePlatform();
*/
//?}
        
    }
}
