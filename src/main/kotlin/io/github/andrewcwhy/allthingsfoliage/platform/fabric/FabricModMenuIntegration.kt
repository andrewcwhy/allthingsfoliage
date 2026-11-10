package io.github.andrewcwhy.allthingsfoliage.platform.fabric

//? fabric {
import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import dev.kikugie.fletching_table.annotation.fabric.Entrypoint
import io.github.andrewcwhy.allthingsfoliage.client.gui.screens.ConfigScreen

/**
 * Provides All Things Foliage's configuration screen to Mod Menu.
 *
 * This entrypoint is loaded only when the optional Mod Menu mod is present.
 */
@Entrypoint("modmenu")
class FabricModMenuIntegration : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return ConfigScreenFactory { parent ->
            ConfigScreen(parent)
        }
    }
}
//?}
