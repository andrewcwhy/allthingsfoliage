package io.github.andrewcwhy.allthingsfoliage.platform.fabric

//? fabric {
/*import io.github.andrewcwhy.allthingsfoliage.platform.Platform
import java.nio.file.Path
import net.fabricmc.loader.api.FabricLoader

/** Provides Fabric-specific implementations of platform services. */
class FabricPlatform : Platform {

    override fun configDir(): Path {
        return FabricLoader.getInstance().configDir
    }

    override fun isModLoaded(modId: String): Boolean {
        return FabricLoader.getInstance().isModLoaded(modId)
    }

    override fun loader(): Platform.ModLoader {
        return Platform.ModLoader.FABRIC
    }
}
*///?}
