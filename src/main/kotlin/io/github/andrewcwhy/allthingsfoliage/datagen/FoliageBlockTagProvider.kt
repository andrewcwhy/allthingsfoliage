package io.github.andrewcwhy.allthingsfoliage.datagen

//? fabric {

import io.github.andrewcwhy.allthingsfoliage.passability.FoliageTags
import java.util.concurrent.CompletableFuture
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider
import net.minecraft.core.HolderLookup
import net.minecraft.tags.BlockTags

class FoliageBlockTagProvider(
    output: FabricPackOutput,
    registries: CompletableFuture<HolderLookup.Provider>,
) : FabricTagsProvider.BlockTagsProvider(output, registries) {
    override fun addTags(registries: HolderLookup.Provider) {
        // Required tag references can come from Minecraft rather than this generation run.
        builder(FoliageTags.passables).forceAddTag(BlockTags.LEAVES)
        builder(BlockTags.PREVENT_MOB_SPAWNING_INSIDE).forceAddTag(FoliageTags.passables)
        builder(BlockTags.SUPPORT_OVERRIDE_SNOW_LAYER).forceAddTag(BlockTags.LEAVES)
    }
}
//?}
