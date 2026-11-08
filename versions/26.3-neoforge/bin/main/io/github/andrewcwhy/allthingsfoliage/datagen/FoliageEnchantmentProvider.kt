package io.github.andrewcwhy.allthingsfoliage.datagen

//? fabric {

/*import io.github.andrewcwhy.allthingsfoliage.passability.FoliageEnchantments
import java.util.concurrent.CompletableFuture
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.data.worldgen.BootstrapContext
import net.minecraft.tags.ItemTags
import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.item.enchantment.Enchantment

class FoliageEnchantmentProvider(
    output: FabricPackOutput,
    registries: CompletableFuture<HolderLookup.Provider>,
) : FabricDynamicRegistryProvider(output, registries) {
    override fun configure(registries: HolderLookup.Provider, entries: Entries) {
        entries.add(
            registries
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(FoliageEnchantments.leafWalker)
        )
    }

    override fun getName(): String {
        return "All Things Foliage enchantments"
    }

    companion object {
        fun bootstrap(context: BootstrapContext<Enchantment>) {
            val items = context.lookup(Registries.ITEM)
            val definition =
                Enchantment.definition(
                    items.getOrThrow(ItemTags.FOOT_ARMOR_ENCHANTABLE),
                    1,
                    1,
                    Enchantment.constantCost(25),
                    Enchantment.constantCost(50),
                    8,
                    EquipmentSlotGroup.FEET,
                )
            context.register(
                FoliageEnchantments.leafWalker,
                Enchantment.enchantment(definition)
                    .withEffect(FoliageEnchantments.leafWalkerEffect)
                    .build(FoliageEnchantments.leafWalker.identifier()),
            )
        }
    }
}
*///?}
