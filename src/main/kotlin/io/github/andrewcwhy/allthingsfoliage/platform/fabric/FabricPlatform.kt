package io.github.andrewcwhy.allthingsfoliage.platform.fabric

//? fabric {
import io.github.andrewcwhy.allthingsfoliage.platform.Platform
import java.nio.file.Path
import net.fabricmc.loader.api.FabricLoader

/** Provides Fabric-specific implementations of platform services. */
class FabricPlatform : Platform {
    override val configDir: Path
        get() {
            return FabricLoader.getInstance().configDir
        }

    override val loader: Platform.ModLoader
        get() {
            return Platform.ModLoader.FABRIC
        }

    override fun isModLoaded(modId: String): Boolean {
        return FabricLoader.getInstance().isModLoaded(modId)
    }
}
//?}
