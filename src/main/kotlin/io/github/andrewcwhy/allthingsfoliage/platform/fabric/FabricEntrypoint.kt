package io.github.andrewcwhy.allthingsfoliage.platform.fabric

//? fabric {
import dev.kikugie.fletching_table.annotation.fabric.Entrypoint
import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage
import io.github.andrewcwhy.allthingsfoliage.passability.AlwaysLeafWalkingCondition
import io.github.andrewcwhy.allthingsfoliage.passability.FoliageEnchantments
import io.github.andrewcwhy.allthingsfoliage.passability.FoliagePassability
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries

/** Initializes All Things Foliage through Fabric's main entrypoint. */
@Entrypoint("main")
class FabricEntrypoint : ModInitializer {
    override fun onInitialize() {
        AllThingsFoliage.onInitialize()
        Registry.register(
            BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE,
            AllThingsFoliage.id("leaf_walker"),
            FoliageEnchantments.leafWalkerEffect,
        )
        ResourceConditions.register(AlwaysLeafWalkingCondition.type)
        CommonLifecycleEvents.TAGS_LOADED.register { _, _ -> FoliagePassability.tagsLoaded() }
    }
}
//?}
