package io.github.andrewcwhy.allthingsfoliage.platform.neoforge

//? neoforge {
/*
import io.github.andrewcwhy.allthingsfoliage.platform.Platform
import java.io.IOException
import java.nio.file.Path
import net.neoforged.fml.ModList
import net.neoforged.fml.loading.FMLPaths

class NeoForgePlatform : Platform {

    override fun configDir(): Path {
        return FMLPaths.CONFIGDIR.get()
    }

    override fun isModLoaded(modId: String): Boolean {
        return ModList.get().isLoaded(modId)
    }

    override fun loader(): Platform.ModLoader {
        return Platform.ModLoader.NEOFORGE
    }

    /^*
     * Reads each mod's copy directly because module resources may be hidden from class loaders.
     ^/
    override fun readAll(path: String): List<ByteArray> {
        val resources = mutableListOf<ByteArray>()

        for (modFile in ModList.get().modFiles) {
            try {
                val contents = modFile.file.contents

                if (contents.containsFile(path)) {
                    resources.add(contents.readFile(path))
                }
            } catch (_: IOException) {
                // Unreadable resources do not prevent other mods from being checked.
            }
        }

        return resources
    }
}
*/
//?}
