package io.github.andrewcwhy.allthingsfoliage.platform.neoforge;
//? neoforge {
/*
import io.github.andrewcwhy.allthingsfoliage.FoliageConfigScreen;
import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage;
// Update these imports to match wherever you placed the CutThrough files
import io.github.andrewcwhy.allthingsfoliage.cutthrough.CutThrough;
import io.github.andrewcwhy.allthingsfoliage.cutthrough.CutThroughClient;

import fuzs.puzzleslib.common.api.core.v1.ModConstructor;
import fuzs.puzzleslib.common.api.client.core.v1.ClientModConstructor;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import java.util.function.Supplier;

@Mod(AllThingsFoliage.MOD_ID)
public class NeoforgeEntrypoint {

    public NeoforgeEntrypoint(IEventBus modEventBus, ModContainer modContainer) {
        AllThingsFoliage.onInitialize();

        // --- Cut Through Initialization ---
        // Bind PuzzlesLib logic to your own mod ID
        ModConstructor.construct(AllThingsFoliage.MOD_ID, CutThrough::new);

        // Safely run the client constructor only on the physical client
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientModConstructor.construct(AllThingsFoliage.MOD_ID, CutThroughClient::new);
        }
        // ----------------------------------

        modEventBus.addListener(this::onClientSetup);
        modContainer.registerExtensionPoint(
                IConfigScreenFactory.class,
                (Supplier<IConfigScreenFactory>) () -> (client, parent) -> new FoliageConfigScreen(parent)
        );
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            AllThingsFoliage.onInitializeClient();

            // The constructor runs during mod construction, before any mod has registered its
            // blocks, so third-party lookups have to wait. Client setup is the earliest point that
            // is both past the registry events and guaranteed client-only: the compat registry
            // touches Sway's client behaviour classes, which must not load on a dedicated server.
            AllThingsFoliage.onRegistriesReady();

        });
    }
}
*/
//?}
