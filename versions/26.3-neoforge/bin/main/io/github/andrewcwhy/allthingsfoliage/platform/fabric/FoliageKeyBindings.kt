package io.github.andrewcwhy.allthingsfoliage.platform.fabric

//? fabric {
/*import com.github.razorplay01.sway.config.SwayConfig
import com.mojang.blaze3d.platform.InputConstants
import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage
import io.github.andrewcwhy.allthingsfoliage.Config
import io.github.andrewcwhy.allthingsfoliage.client.gui.screens.ConfigScreen
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

/** Registers and handles Fabric's configuration and mod-toggle keybindings. */
object FoliageKeyBindings {

    @JvmField
    var openConfig: KeyMapping? = null

    @JvmField
    var resourceLocation: Identifier =
        Identifier.fromNamespaceAndPath(AllThingsFoliage.MOD_ID, "general")

    @JvmField
    var toggleMod: KeyMapping? = null

    /** Registers keybindings with Fabric's keymapping API. */
    @JvmStatic
    fun register() {
        val category = KeyMapping.Category.register(resourceLocation)

        openConfig =
            KeyMappingHelper.registerKeyMapping(
                KeyMapping(
                    "key.allthingsfoliage.open_config",
                    InputConstants.UNKNOWN.value,
                    category
                )
            )

        toggleMod =
            KeyMappingHelper.registerKeyMapping(
                KeyMapping(
                    "key.allthingsfoliage.toggle",
                    InputConstants.UNKNOWN.value,
                    category
                )
            )
    }

    /** Handles keybinding actions at the end of each client tick. */
    @JvmStatic
    fun tick(client: Minecraft) {
        Config.applyDefaultRadiusOnce()

        val configKey =
            checkNotNull(openConfig) { "The configuration keybinding has not been registered" }

        val toggleKey =
            checkNotNull(toggleMod) { "The mod-toggle keybinding has not been registered" }

        while (configKey.consumeClick()) {
            client.setScreenAndShow(ConfigScreen(client.gui.screen()))
        }

        while (toggleKey.consumeClick()) {
            val swayConfig = SwayConfig.INSTANCE

            swayConfig.enabled = !swayConfig.enabled
            SwayConfig.save()

            client.player?.sendSystemMessage(
                Component.translatable(
                    if (swayConfig.enabled) {
                        "key.allthingsfoliage.toggle.on"
                    } else {
                        "key.allthingsfoliage.toggle.off"
                    }
                )
            )
        }
    }
}
*///?}
