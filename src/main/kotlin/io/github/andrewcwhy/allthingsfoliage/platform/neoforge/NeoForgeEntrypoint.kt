package io.github.andrewcwhy.allthingsfoliage.platform.neoforge

//? neoforge {
/*
import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage
import io.github.andrewcwhy.allthingsfoliage.client.gui.screens.ConfigScreen
import java.util.function.Supplier
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.fml.loading.FMLEnvironment
import net.neoforged.neoforge.client.gui.IConfigScreenFactory

@Mod(AllThingsFoliage.MOD_ID)
class NeoForgeEntrypoint(
    modEventBus: IEventBus,
    modContainer: ModContainer,
) {

    init {
        AllThingsFoliage.onInitialize()

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.register(modEventBus, modContainer)
        }
    }

    /^* Isolates client setup and configuration-screen registration from server initialization. ^/
    private object ClientSetup {

        private fun onClientSetup(event: FMLClientSetupEvent) {
            event.enqueueWork {
                AllThingsFoliage.onInitializeClient()

                // Compatibility lookups require populated block registries.
                AllThingsFoliage.onRegistriesReady()
            }
        }

        fun register(
            modEventBus: IEventBus,
            modContainer: ModContainer,
        ) {
            modEventBus.addListener(::onClientSetup)

            modContainer.registerExtensionPoint(
                IConfigScreenFactory::class.java,
                Supplier<IConfigScreenFactory> {
                    IConfigScreenFactory { _, parent ->
                        ConfigScreen(parent)
                    }
                },
            )
        }
    }
}
*/
//?}
