package io.github.andrewcwhy.allthingsfoliage.platform.fabric

//? fabric {
import dev.kikugie.fletching_table.annotation.fabric.Entrypoint
import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage
import io.github.andrewcwhy.allthingsfoliage.client.particle.SnowShakeParticles
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents

/**
 * Registers client-side foliage functionality after Fabric initializes mods.
 */
@Entrypoint("client")
class FabricClientEntrypoint : ClientModInitializer {

    override fun onInitializeClient() {
        AllThingsFoliage.onInitializeClient()

        // Main entrypoints finish before client entrypoints, so block
        // registries are populated before compatibility registration.
        AllThingsFoliage.onRegistriesReady()

        FabricFoliageHooks.register()
        FoliageKeyBindings.register()

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            FoliageKeyBindings.tick(client)
        }

        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            SnowShakeParticles.tick()
        }
    }
}
//?}
