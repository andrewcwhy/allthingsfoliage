package io.github.andrewcwhy.allthingsfoliage.datagen

//? fabric {

/*import com.google.common.hash.HashCode
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage
import io.github.andrewcwhy.allthingsfoliage.passability.AlwaysLeafWalkingCondition
import io.github.andrewcwhy.allthingsfoliage.passability.FoliageEnchantments
import java.security.MessageDigest
import java.util.concurrent.CompletableFuture
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions
import net.minecraft.advancements.Advancement
import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.data.CachedOutput
import net.minecraft.data.PackOutput
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.data.recipes.RecipeProvider
import net.minecraft.data.worldgen.BootstrapContext
import net.minecraft.resources.ResourceKey
import net.minecraft.tags.ItemTags
import net.minecraft.world.item.ItemStackTemplate
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.enchantment.ItemEnchantments

/** Generates the upstream recipe and load conditions understood by both loaders. */
class FoliageRecipeProvider(
    output: FabricPackOutput,
    registries: CompletableFuture<HolderLookup.Provider>,
) : FabricRecipeProvider(output, registries) {
    override fun createRecipeProvider(
        registries: HolderLookup.Provider,
        recipes: BootstrapContext<Recipe<*>>,
        advancements: BootstrapContext<Advancement>,
    ): RecipeProvider {
        return object : RecipeProvider(recipes, advancements) {
            override fun buildRecipes() {
                val enchantment =
                    registries
                        .lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(FoliageEnchantments.leafWalker)
                val enchantments = ItemEnchantments.Mutable(ItemEnchantments.EMPTY)
                enchantments.set(enchantment, 1)
                val components =
                    DataComponentPatch.builder()
                        .set(DataComponents.STORED_ENCHANTMENTS, enchantments.toImmutable())
                        .build()
                val conditionalOutput =
                    withConditions(output, ResourceConditions.not(AlwaysLeafWalkingCondition()))
                shapeless(RecipeCategory.MISC, ItemStackTemplate(Items.ENCHANTED_BOOK, components))
                    .requires(Items.ENCHANTED_BOOK)
                    .requires(ItemTags.LEAVES)
                    .unlockedBy(getHasName(Items.ENCHANTED_BOOK), has(Items.ENCHANTED_BOOK))
                    .save(
                        conditionalOutput,
                        ResourceKey.create(Registries.RECIPE, AllThingsFoliage.id("leaf_walker")),
                    )
            }
        }
    }

    override fun getName(): String {
        return "All Things Foliage recipes"
    }

    override fun run(writer: CachedOutput): CompletableFuture<*> {
        val dataRoot =
            output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve(AllThingsFoliage.MOD_ID)
        val conditionalPaths =
            setOf(
                dataRoot.resolve("advancement/recipes/misc/leaf_walker.json"),
                dataRoot.resolve("recipe/leaf_walker.json"),
            )
        val gson = GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
        // Add both loaders' conditions before hashing, so cache hits retain the complete output.
        val conditionalWriter = CachedOutput { path, bytes, hash ->
            if (path in conditionalPaths) {
                val json = JsonParser.parseString(bytes.toString(Charsets.UTF_8)).asJsonObject
                val condition =
                    JsonObject().apply {
                        addProperty("type", "neoforge:not")
                        add(
                            "value",
                            JsonObject().apply {
                                addProperty(
                                    "type",
                                    AllThingsFoliage.id("always_leaf_walking").toString(),
                                )
                            },
                        )
                    }
                json.add("neoforge:conditions", JsonArray().apply { add(condition) })
                val conditionedBytes = gson.toJson(json).toByteArray(Charsets.UTF_8)
                writer.writeIfNeeded(
                    path,
                    conditionedBytes,
                    HashCode.fromBytes(MessageDigest.getInstance("SHA-1").digest(conditionedBytes)),
                )
            } else {
                writer.writeIfNeeded(path, bytes, hash)
            }
        }
        return super.run(conditionalWriter)
    }
}
*///?}
