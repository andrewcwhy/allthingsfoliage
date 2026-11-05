package io.github.andrewcwhy.allthingsfoliage.platform.fabric;

import dev.kikugie.fletching_table.annotation.fabric.Entrypoint;
//? fabric {

import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage;
import net.fabricmc.api.ModInitializer;

@Entrypoint("main")
public class FabricEntrypoint implements ModInitializer {

    @Override
    public void onInitialize() {
        AllThingsFoliage.onInitialize();
    }
}
//?}
