package io.github.andrewcwhy.allthingsfoliage.datagen

//? fabric {

import io.github.andrewcwhy.allthingsfoliage.passability.FoliageEnchantments
import java.util.concurrent.CompletableFuture
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput
import net.minecraft.core.HolderLookup
import net.minecraft.data.tags.EnchantmentTagsProvider
import net.minecraft.tags.EnchantmentTags

class FoliageEnchantmentTagProvider(
    output: FabricPackOutput,
    registries: CompletableFuture<HolderLookup.Provider>,
) : EnchantmentTagsProvider(output, registries) {
    override fun addTags(registries: HolderLookup.Provider) {
        tag(EnchantmentTags.CURSE).addOptional(FoliageEnchantments.leafWalker)
        tag(EnchantmentTags.NON_TREASURE).addOptional(FoliageEnchantments.leafWalker)
    }
}
//?}
