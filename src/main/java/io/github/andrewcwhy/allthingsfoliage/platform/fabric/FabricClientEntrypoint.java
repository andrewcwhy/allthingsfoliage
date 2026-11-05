package io.github.andrewcwhy.allthingsfoliage.platform.fabric;

import dev.kikugie.fletching_table.annotation.fabric.Entrypoint;
import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage;
import io.github.andrewcwhy.allthingsfoliage.gpu.SnowShakeParticles;
import net.fabricmc.api.ClientModInitializer;
//? fabric {

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

@Entrypoint("client")
public class FabricClientEntrypoint implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        AllThingsFoliage.onInitializeClient();
        // Loader runs every mod's "main" entrypoint before any "client" one, so by now all blocks
        // are in the registry. Registering from the main entrypoint instead would depend on mod
        // load order and silently miss mods that initialize after this one.
        AllThingsFoliage.onRegistriesReady();
        FabricFoliageHooks.register();
        //? <1.21.11 {
        /*SwayVariantModels.register();
         */ //?}
        FoliageKeyBindings.register();
        ClientTickEvents.END_CLIENT_TICK.register(FoliageKeyBindings::tick);
        ClientTickEvents.END_CLIENT_TICK.register(client ->
            SnowShakeParticles.tick()
        );
    }
}
//?}
