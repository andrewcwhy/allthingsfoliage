package io.github.andrewcwhy.allthingsfoliage.platform.fabric

//? fabric {
import dev.kikugie.fletching_table.annotation.fabric.Entrypoint
import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage
import net.fabricmc.api.ModInitializer

/**
 * Initializes All Things Foliage through Fabric's main entrypoint.
 */
@Entrypoint("main")
class FabricEntrypoint : ModInitializer {

    override fun onInitialize() {
        AllThingsFoliage.onInitialize()
    }
}
//?}
