package io.github.andrewcwhy.allthingsfoliage

import com.github.razorplay01.sway.api.SwayAPI
import com.github.razorplay01.sway.client.behavior.BuiltinBehaviors
import com.github.razorplay01.sway.client.behavior.multiblock.HangingVineMultiblockBehavior
import com.github.razorplay01.sway.client.behavior.multiblock.SugarCaneMultiblockBehavior
import kotlin.jvm.optionals.getOrNull
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks

/**
 * Registers optional blocks from other mods with Sway.
 *
 * Missing or invalid block identifiers are skipped. Successfully resolved blocks
 * are recorded in lookup order, including blocks already supported by Sway.
 */
object ModCompatRegistry {
    private val resolvedBlocks = mutableListOf<Block>()

    private var initialized = false
    private var missingBlockCount = 0
    private var resolvedBlockCount = 0
    private var snowRealMagicRegistered = false

    /**
     * Registers compatible foliage in the established order, only once.
     */
    fun initialize() {
        if (initialized) {
            return
        }

        initialized = true

        // Registration order is intentional; do not alphabetize these calls.
        registerBiomesOPlenty()
        registerFarmersDelight()
        registerSereneShrubbery()
        registerNoMansLand()
        registerAdditionalVanilla()
        registerSnowRealMagic()

        AllThingsFoliage.logger.info(
            "Foliage compatibility: {} blocks resolved, {} entries unavailable",
            resolvedBlockCount,
            missingBlockCount
        )
    }

    /**
     * Retries Snow! Real Magic registration during model loading, before model baking.
     *
     * The retry runs only after initialization and only while the integration
     * remains unregistered.
     */
    @Synchronized
    fun registerLate() {
        if (
            !initialized ||
            snowRealMagicRegistered ||
            !AllThingsFoliage.platform.isModLoaded("snowrealmagic")
        ) {
            return
        }

        registerSnowRealMagic()

        if (snowRealMagicRegistered) {
            AllThingsFoliage.logger.info(
                "Foliage compatibility: registered Snow! Real Magic blocks during model loading"
            )
        }
    }

    private fun registerAdditionalVanilla() {
        registerPlants(
            "minecraft:red_shrub",
            "minecraft:poplar_sapling"
        )

        // Preserve Sway's behavior when it already supports these blocks.
        registerUnregisteredPlants(
            "minecraft:bush",
            "minecraft:firefly_bush",
            "minecraft:short_dry_grass",
            "minecraft:tall_dry_grass",
            "minecraft:closed_eyeblossom",
            "minecraft:open_eyeblossom",
            "minecraft:cactus_flower",
            "minecraft:pale_oak_sapling"
        )

        registerHangingPlants(
            "minecraft:pale_hanging_moss",
            "minecraft:hanging_roots"
        )
    }

    private fun registerBiomesOPlenty() {
        registerPlants(
            "biomesoplenty:dune_grass",
            "biomesoplenty:desert_grass",
            "biomesoplenty:dead_grass",
            "biomesoplenty:tundra_shrub",
            "biomesoplenty:bush",
            "biomesoplenty:barley",
            "biomesoplenty:enderphyte",
            "biomesoplenty:tiny_cactus",
            "biomesoplenty:yellow_maple_sapling",
            "biomesoplenty:orange_maple_sapling",
            "biomesoplenty:red_maple_sapling",
            "biomesoplenty:pine_sapling",
            "biomesoplenty:fir_sapling",
            "biomesoplenty:origin_oak_sapling",
            "biomesoplenty:origin_sapling",
            "biomesoplenty:snowblossom_sapling",
            "biomesoplenty:cypress_sapling",
            "biomesoplenty:flowering_oak_sapling",
            "biomesoplenty:empyreal_sapling",
            "biomesoplenty:hellbark_sapling",
            "biomesoplenty:magic_sapling",
            "biomesoplenty:violet",
            "biomesoplenty:lavender",
            "biomesoplenty:umbran_sapling",
            "biomesoplenty:palm_sapling",
            "biomesoplenty:jacaranda_sapling",
            "biomesoplenty:mahogany_sapling",
            "biomesoplenty:redwood_sapling",
            "biomesoplenty:flower_bud",
            "biomesoplenty:toadstool",
            "biomesoplenty:white_lavender",
            "biomesoplenty:orange_cosmos",
            "biomesoplenty:pink_daffodil",
            "biomesoplenty:pink_hibiscus",
            "biomesoplenty:origin_dandelion",
            "biomesoplenty:willow_sapling",
            "biomesoplenty:dead_sapling",
            "biomesoplenty:marigold",
            "biomesoplenty:origin_rose",
            "biomesoplenty:glowshroom",
            "biomesoplenty:brimstone_bud",
            "biomesoplenty:endbloom",
            "biomesoplenty:blackstone_bulb",
            "biomesoplenty:blackstone_spines",
            "biomesoplenty:glowflower",
            "biomesoplenty:wilted_lily",
            "biomesoplenty:burning_blossom",
            "biomesoplenty:sprout",
            "biomesoplenty:hair"
        )

        registerPlants(
            "biomesoplenty:sea_oats",
            "biomesoplenty:cattail",
            "biomesoplenty:reed",
            "biomesoplenty:watergrass",
            "biomesoplenty:goldenrod",
            "biomesoplenty:tall_white_lavender",
            "biomesoplenty:blue_hydrangea",
            "biomesoplenty:tall_lavender",
            "biomesoplenty:icy_iris",
            "biomesoplenty:brimstone_cluster"
        )

        registerPlants("biomesoplenty:eyebulb")

        registerStackablePlants(
            "biomesoplenty:lumaloop",
            "biomesoplenty:lumaloop_plant",
            "biomesoplenty:high_grass",
            "biomesoplenty:high_grass_plant"
        )

        registerHangingPlants(
            "biomesoplenty:spanish_moss",
            "biomesoplenty:spanish_moss_plant",
            "biomesoplenty:willow_vine",
            "biomesoplenty:glowworm_silk",
            "biomesoplenty:glowworm_silk_strand"
        )

        registerHangingPlants(
            "biomesoplenty:flesh_tendons",
            "biomesoplenty:flesh_tendons_strand",
            "biomesoplenty:hanging_cobweb",
            "biomesoplenty:hanging_cobweb_strand"
        )
    }

    private fun registerFarmersDelight() {
        registerPlants(
            "farmersdelight:sandy_shrub",
            "farmersdelight:wild_cabbages",
            "farmersdelight:wild_onions",
            "farmersdelight:wild_tomatoes",
            "farmersdelight:wild_carrots",
            "farmersdelight:wild_potatoes",
            "farmersdelight:wild_beetroots",
            "farmersdelight:onions",
            "farmersdelight:budding_tomatoes",
            "farmersdelight:cabbages",
            "farmersdelight:brown_mushroom_colony",
            "farmersdelight:red_mushroom_colony"
        )

        registerPlants("farmersdelight:wild_rice")

        registerStackablePlants(
            "farmersdelight:rice",
            "farmersdelight:rice_panicles"
        )
    }

    private fun registerHangingPlants(vararg blockIds: String) {
        for (blockId in blockIds) {
            val block = resolveBlock(blockId) ?: continue
            HangingVineMultiblockBehavior.addBlock(block)

            SwayAPI.setBlockPipeline(
                block,
                listOf(
                    BuiltinBehaviors.ENTITY_COLLISION_KEY,
                    BuiltinBehaviors.PROXIMITY_FORCE_KEY,
                    BuiltinBehaviors.HANGING_VINE_MULTIBLOCK_KEY,
                    BuiltinBehaviors.HANGING_VINE_DEFORMATION_KEY,
                    BuiltinBehaviors.VINE_CLIMB_TENSION_KEY,
                    BuiltinBehaviors.multiplierKey(1.0f)
                )
            )
        }
    }

    private fun registerNoMansLand() {
        registerPlants(
            "nomansland:field_mushroom",
            "nomansland:frosted_grass",
            "nomansland:grass_sprouts",
            "nomansland:oat_grass",
            "nomansland:short_beachgrass",
            "nomansland:tall_beachgrass",
            "nomansland:dried_grass",
            "nomansland:fiddlehead",
            "nomansland:mycelium_sprouts",
            "nomansland:cave_weeds",
            "nomansland:mycelium_growths",
            "nomansland:yellow_birch_sapling",
            "nomansland:automnal_oak_sapling",
            "nomansland:pale_cherry_sapling",
            "nomansland:aconite",
            "nomansland:starflower",
            "nomansland:thistle",
            "nomansland:blue_lupine",
            "nomansland:red_lupine",
            "nomansland:yellow_lupine",
            "nomansland:pink_lupine",
            "nomansland:autumn_crocus",
            "nomansland:wild_mint",
            "nomansland:pickleweed",
            "nomansland:barrel_cactus",
            "nomansland:lavender_bush",
            "nomansland:succulent",
            "nomansland:pine_sapling",
            "nomansland:maple_sapling",
            "nomansland:red_maple_sapling",
            "nomansland:walnut_sapling",
            "nomansland:willow_sapling",
            "nomansland:field_mushroom_colony"
        )

        registerPlants(
            "nomansland:cattail",
            "nomansland:reeds"
        )

        registerHangingPlants("nomansland:beard_moss")
    }

    private fun registerPlants(vararg blockIds: String) {
        for (blockId in blockIds) {
            resolveBlock(blockId)?.let { SwayAPI.register(it, 1.0f) }
        }
    }

    private fun registerSereneShrubbery() {
        registerPlants(
            "serene_shrubbery:red_pansies",
            "serene_shrubbery:white_pansies",
            "serene_shrubbery:yellow_pansies",
            "serene_shrubbery:orange_pansies",
            "serene_shrubbery:pink_pansies",
            "serene_shrubbery:purple_pansies",
            "serene_shrubbery:blue_frost_pansies",
            "serene_shrubbery:panola_pink_pansies",
            "serene_shrubbery:sunrise_pansies",
            "serene_shrubbery:halloween_pansies",
            "serene_shrubbery:hydrangea",
            "serene_shrubbery:pink_hydrangea",
            "serene_shrubbery:purple_hydrangea",
            "serene_shrubbery:red_hydrangea",
            "serene_shrubbery:white_hydrangea",
            "serene_shrubbery:halloween_hydrangea",
            "serene_shrubbery:green_hydrangea",
            "serene_shrubbery:butterfly_bush",
            "serene_shrubbery:white_butterfly_bush",
            "serene_shrubbery:pink_butterfly_bush",
            "serene_shrubbery:indigo_butterfly_bush",
            "serene_shrubbery:twinflower",
            "serene_shrubbery:blanketflower"
        )

        registerPlants(
            "serene_shrubbery:manhattan_lights_lupine",
            "serene_shrubbery:sky_blue_lupine",
            "serene_shrubbery:golden_lupine",
            "serene_shrubbery:purple_lupine",
            "serene_shrubbery:lupine_white",
            "serene_shrubbery:purple_foxglove",
            "serene_shrubbery:sunset_foxglove",
            "serene_shrubbery:peach_foxglove",
            "serene_shrubbery:halloween_foxglove",
            "serene_shrubbery:candy_mountain_foxglove",
            "serene_shrubbery:lavender_foxglove",
            "serene_shrubbery:lupine_pink",
            "serene_shrubbery:white_foxglove",
            "serene_shrubbery:fireweed"
        )
    }

    /**
     * Gives snow-covered foliage proximity and collision forces without applying
     * additional deformation to the snow or its contained plants.
     */
    private fun registerSnowRealMagic() {
        val blockIds = arrayOf(
            "snowrealmagic:snow",
            "snowrealmagic:snow_extra_collision",
            "snowrealmagic:snowy_plant",
            "snowrealmagic:snowy_double_plant_lower",
            "snowrealmagic:snowy_double_plant_upper"
        )

        for (blockId in blockIds) {
            val block = resolveBlock(blockId) ?: continue
            SwayAPI.setBlockPipeline(
                block,
                listOf(
                    BuiltinBehaviors.ENTITY_COLLISION_KEY,
                    BuiltinBehaviors.PROXIMITY_FORCE_KEY,
                    BuiltinBehaviors.multiplierKey(1.0f)
                )
            )
            snowRealMagicRegistered = true
        }
    }

    private fun registerStackablePlants(vararg blockIds: String) {
        for (blockId in blockIds) {
            val block = resolveBlock(blockId) ?: continue
            SugarCaneMultiblockBehavior.addBlock(block)

            SwayAPI.setBlockPipeline(
                block,
                listOf(
                    BuiltinBehaviors.ENTITY_COLLISION_KEY,
                    BuiltinBehaviors.PROXIMITY_FORCE_KEY,
                    BuiltinBehaviors.SUGAR_CANE_MULTIBLOCK_KEY,
                    BuiltinBehaviors.SUGAR_CANE_DEFORMATION_KEY,
                    BuiltinBehaviors.multiplierKey(1.0f)
                )
            )
        }
    }

    private fun registerUnregisteredPlants(vararg blockIds: String) {
        for (blockId in blockIds) {
            val block = resolveBlock(blockId) ?: continue
            if (!SwayAPI.isInteractive(block)) {
                SwayAPI.register(block, 1.0f)
            }
        }
    }

    /**
     * Resolves an optional block and records each lookup independently.
     *
     * Failed lookups do not prevent other compatibility registrations.
     */
    private fun resolveBlock(blockId: String): Block? {
        return try {
            val identifier = Identifier.parse(blockId)
            val block =
                BuiltInRegistries.BLOCK.getOptional(identifier)
                    .getOrNull()
                    ?.takeUnless { it === Blocks.AIR }

            if (block != null) {
                resolvedBlockCount++
                resolvedBlocks.add(block)
            } else {
                missingBlockCount++

                AllThingsFoliage.logger.debug(
                    "Foliage compatibility: {} not present, skipping",
                    blockId,
                )
            }

            block
        } catch (_: RuntimeException) {
            // Optional integrations tolerate invalid identifiers and registry errors.
            missingBlockCount++
            null
        }
    }
}
