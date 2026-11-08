package io.github.andrewcwhy.allthingsfoliage.platform.fabric.datagen

//? fabric {

import io.github.andrewcwhy.allthingsfoliage.datagen.FoliageBlockTagProvider
import io.github.andrewcwhy.allthingsfoliage.datagen.FoliageEnchantmentProvider
import io.github.andrewcwhy.allthingsfoliage.datagen.FoliageEnchantmentTagProvider
import io.github.andrewcwhy.allthingsfoliage.datagen.FoliageRecipeProvider
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator
import net.minecraft.core.RegistrySetBuilder
import net.minecraft.core.registries.Registries

class FabricDataGeneratorEntrypoint : DataGeneratorEntrypoint {
    override fun buildRegistry(registries: RegistrySetBuilder) {
        registries.add(Registries.ENCHANTMENT) { context ->
            FoliageEnchantmentProvider.bootstrap(context)
        }
    }

    override fun onInitializeDataGenerator(generator: FabricDataGenerator) {
        val pack = generator.createPack()
        pack.addProvider(::FoliageBlockTagProvider)
        pack.addProvider(::FoliageEnchantmentProvider)
        pack.addProvider(::FoliageEnchantmentTagProvider)
        pack.addProvider(::FoliageRecipeProvider)
    }
}
//?}
