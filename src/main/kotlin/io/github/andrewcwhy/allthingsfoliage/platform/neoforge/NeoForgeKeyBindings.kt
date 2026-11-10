package io.github.andrewcwhy.allthingsfoliage.platform.neoforge

//? neoforge {
/*
import com.github.razorplay01.sway.config.SwayConfig
import com.mojang.blaze3d.platform.InputConstants
import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage
import io.github.andrewcwhy.allthingsfoliage.client.gui.screens.ConfigScreen
import io.github.andrewcwhy.allthingsfoliage.config.FoliageConfig
import java.util.function.Consumer
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object NeoForgeKeyBindings {

    private val categoryId = AllThingsFoliage.id("general")

    lateinit var openConfig: KeyMapping
        private set

    lateinit var toggleMod: KeyMapping
        private set

    @JvmStatic
    fun register(registrar: Consumer<KeyMapping>) {
        val category = KeyMapping.Category.register(categoryId)

        openConfig = KeyMapping(
            "key.allthingsfoliage.open_config",
            InputConstants.UNKNOWN.value,
            category,
        )

        toggleMod = KeyMapping(
            "key.allthingsfoliage.toggle",
            InputConstants.UNKNOWN.value,
            category,
        )

        registrar.accept(openConfig)
        registrar.accept(toggleMod)
    }

    @JvmStatic
    fun tick(minecraft: Minecraft) {
        FoliageConfig.applyDefaultRadiusOnce()

        while (openConfig.consumeClick()) {
            minecraft.setScreenAndShow(ConfigScreen(null))
        }

        while (toggleMod.consumeClick()) {
            val swayConfig = SwayConfig.INSTANCE
            swayConfig.enabled = !swayConfig.enabled
            SwayConfig.save()

            val translationKey = if (swayConfig.enabled) {
                "key.allthingsfoliage.toggle.on"
            } else {
                "key.allthingsfoliage.toggle.off"
            }

            minecraft.player?.sendSystemMessage(
                Component.translatable(translationKey)
            )
        }
    }
}
*/
//?}
