package io.github.andrewcwhy.allthingsfoliage.gpu.compat

import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Field
import java.util.Collections
import java.util.IdentityHashMap
//? neoforge {
/*
import net.minecraft.client.renderer.block.BlockAndTintGetter
*/
//?}
import net.minecraft.client.renderer.block.dispatch.BlockStateModel
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart
import net.minecraft.client.resources.model.sprite.Material
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.tags.TagKey
import net.minecraft.util.RandomSource
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DoublePlantBlock
import net.minecraft.world.level.block.SnowLayerBlock
import net.minecraft.world.level.block.state.BlockState

/**
 * Extracts plants held by Snow! Real Magic for independent foliage rendering.
 *
 * Snow remains in the chunk mesh. Reflective access avoids a required dependency and leaves
 * unsupported implementations with their original rendering.
 */
object SnowRealMagicCompat {
    private const val CORE_MODULE_CLASS = "snownee.snow.CoreModule"
    private const val MOD_ID = "snowrealmagic"
    private const val PLANT_LIFT = 0.101f
    private const val SNOW_BLOCK_ENTITY_CLASS = "snownee.snow.block.entity.SnowBlockEntity"
    private const val SNOW_CLIENT_CONFIG_CLASS = "snownee.snow.client.SnowClientConfig"
    private const val SNOWY_PART_CLASS = "snownee.snow.client.model.SnowyBlockStateModelPart"

    private val snowBlockClassNames = listOf(
        "snownee.snow.block.EntitySnowLayerBlock",
        "snownee.snow.block.SRMSnowLayerBlock",
    )

    private val loaded = AllThingsFoliage.platform.isModLoaded(MOD_ID)

    private val containedStateGetter: MethodHandle? =
        if (loaded) findContainedStateGetter() else null

    private val raisedPlantTag: TagKey<*>? =
        if (loaded) findRaisedPlantTag() else null

    private val snowBlocks: MutableSet<Block> =
        Collections.newSetFromMap(IdentityHashMap<Block, Boolean>())

    private var plantReadWarningIssued = false
    private var snowBlocksSought = false
    private var snowVariantsField: Field? = null
    private var snowyPartField: Field? = null
    private var snowyPartsUnknown = false

    /** Replaces a plant model's collected parts with their snowy variants. */
    private class SnowyModel(
        private val parent: BlockStateModel,
    ) : BlockStateModel {
        override fun collectParts(
            random: RandomSource,
            parts: MutableList<BlockStateModelPart>,
        ) {
            val collectedParts = mutableListOf<BlockStateModelPart>()
            parent.collectParts(random, collectedParts)

            for (part in collectedParts) {
                parts.add(snowyPart(part))
            }
        }

        //? neoforge {
        /*
        override fun collectParts(
            level: BlockAndTintGetter,
            pos: BlockPos,
            state: BlockState,
            random: RandomSource,
            parts: MutableList<BlockStateModelPart>,
        ) {
            val collectedParts = mutableListOf<BlockStateModelPart>()
            parent.collectParts(level, pos, state, random, collectedParts)

            for (part in collectedParts) {
                parts.add(snowyPart(part))
            }
        }
        */
        //?}

        override fun materialFlags(): Int {
            return parent.materialFlags()
        }

        override fun particleMaterial(): Material.Baked {
            return parent.particleMaterial()
        }
    }

    /** Checks whether a plant is drawn inside snow or directly above a snow container. */
    @JvmStatic
    fun drawnSnowy(
        level: BlockGetter,
        pos: BlockPos,
        plant: BlockState,
    ): Boolean {
        return containedStateGetter != null &&
                (
                        (isSnowBlock(level.getBlockState(pos)) && !isSnowBlock(plant)) ||
                                standsOnSnow(level, pos, plant)
                        )
    }

    private fun findContainedStateGetter(): MethodHandle? {
        try {
            val entityClass = Class.forName(
                SNOW_BLOCK_ENTITY_CLASS,
                false,
                SnowRealMagicCompat::class.java.classLoader,
            )

            return MethodHandles.publicLookup()
                .findVirtual(
                    entityClass,
                    "getContainedState",
                    MethodType.methodType(BlockState::class.java),
                )
                .asType(
                    MethodType.methodType(BlockState::class.java, Any::class.java)
                )
        } catch (exception: Throwable) {
            if (
                exception !is ReflectiveOperationException &&
                exception !is LinkageError
            ) {
                throw exception
            }

            AllThingsFoliage.logger.warn(
                "Snow! Real Magic is installed, but not a version the GPU renderer knows: " +
                        "plants in its snow are left to the chunk mesh",
                exception,
            )

            return null
        }
    }

    private fun findRaisedPlantTag(): TagKey<*>? {
        try {
            val coreModuleClass = Class.forName(
                CORE_MODULE_CLASS,
                false,
                SnowRealMagicCompat::class.java.classLoader,
            )

            return coreModuleClass.getField("OFFSET_Y").get(null) as TagKey<*>?
        } catch (exception: Throwable) {
            if (
                exception !is ReflectiveOperationException &&
                exception !is LinkageError &&
                exception !is ClassCastException
            ) {
                throw exception
            }

            return null
        }
    }

    @JvmStatic
    fun isAvailable(): Boolean {
        return containedStateGetter != null
    }

    /** Resolves container blocks lazily, after block registration has completed. */
    @JvmStatic
    fun isSnowBlock(state: BlockState): Boolean {
        if (containedStateGetter == null) {
            return false
        }

        if (!snowBlocksSought) {
            snowBlocksSought = true

            for (className in snowBlockClassNames) {
                val blockClass = try {
                    Class.forName(
                        className,
                        false,
                        SnowRealMagicCompat::class.java.classLoader,
                    )
                } catch (exception: Throwable) {
                    if (
                        exception !is ClassNotFoundException &&
                        exception !is LinkageError
                    ) {
                        throw exception
                    }

                    continue
                }

                for (block in BuiltInRegistries.BLOCK) {
                    if (blockClass.isInstance(block)) {
                        snowBlocks.add(block)
                    }
                }
            }
        }

        return state.block in snowBlocks
    }

    /** Returns the vertical offset applied to plants in the mod's raised-plant tag. */
    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun liftOf(plant: BlockState): Float {
        val tag = raisedPlantTag as TagKey<Block>? ?: return 0.0f

        if (plant.`is`(tag)) {
            return PLANT_LIFT
        }

        return 0.0f
    }

    /** Returns the contained plant when present, otherwise the world's block state. */
    @JvmStatic
    fun plantAt(level: BlockGetter, pos: BlockPos): BlockState {
        val state = level.getBlockState(pos)
        return plantIn(level, pos, state) ?: state
    }

    /** Returns a visible contained plant, excluding empty containers and full-height snow. */
    @JvmStatic
    fun plantIn(
        level: BlockGetter,
        pos: BlockPos,
        state: BlockState,
    ): BlockState? {
        if (
            !isSnowBlock(state) ||
            (
                    state.hasProperty(SnowLayerBlock.LAYERS) &&
                            state.getValue(SnowLayerBlock.LAYERS) >= SnowLayerBlock.MAX_HEIGHT
                    )
        ) {
            return null
        }

        val entity = level.getBlockEntity(pos) ?: return null
        val getter = containedStateGetter ?: return null

        try {
            val plant = getter.invoke(entity) as BlockState? ?: return null

            if (plant.isAir) {
                return null
            }

            return plant
        } catch (exception: Throwable) {
            if (!plantReadWarningIssued) {
                plantReadWarningIssued = true

                AllThingsFoliage.logger.warn(
                    "Could not read the plant inside Snow! Real Magic's snow",
                    exception,
                )
            }

            return null
        }
    }

    private fun snowVariantsOn(): Boolean {
        try {
            val field = snowVariantsField
                ?: Class.forName(
                    SNOW_CLIENT_CONFIG_CLASS,
                    false,
                    SnowRealMagicCompat::class.java.classLoader,
                ).getField("snowVariants").also {
                    snowVariantsField = it
                }

            return field.getBoolean(null)
        } catch (exception: Throwable) {
            if (
                exception !is ReflectiveOperationException &&
                exception !is LinkageError &&
                exception !is RuntimeException
            ) {
                throw exception
            }

            return true
        }
    }

    /** Returns a recognized wrapper's snowy part, retaining other parts unchanged. */
    private fun snowyPart(part: BlockStateModelPart): BlockStateModelPart {
        if (snowyPartsUnknown || part.javaClass.name != SNOWY_PART_CLASS) {
            return part
        }

        try {
            val field = snowyPartField
                ?: part.javaClass.getDeclaredField("snowy").apply {
                    isAccessible = true
                    snowyPartField = this
                }

            return field.get(part) as? BlockStateModelPart ?: part
        } catch (exception: Exception) {
            if (
                exception !is ReflectiveOperationException &&
                exception !is RuntimeException
            ) {
                throw exception
            }

            snowyPartsUnknown = true

            AllThingsFoliage.logger.warn(
                "Could not find Snow! Real Magic's snowy plant models: " +
                        "plants in its snow are drawn without snow near the player",
                exception,
            )

            return part
        }
    }

    /**
     * Selects snowy parts before wrappers such as Sway's NeoForge model extract their quads.
     */
    @JvmStatic
    fun snowyParts(
        level: BlockGetter,
        pos: BlockPos,
        plant: BlockState,
        parts: MutableList<BlockStateModelPart>,
    ) {
        if (containedStateGetter == null || snowyPartsUnknown) {
            return
        }

        val inSnow = isSnowBlock(level.getBlockState(pos)) && !isSnowBlock(plant)

        if (!inSnow && !(standsOnSnow(level, pos, plant) && snowVariantsOn())) {
            return
        }

        val iterator = parts.listIterator()

        while (iterator.hasNext()) {
            iterator.set(snowyPart(iterator.next()))
        }
    }

    /** Wraps eligible models so their collected parts use the snowy variants. */
    @JvmStatic
    fun snowyVariant(
        model: BlockStateModel,
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        inSnow: Boolean,
    ): BlockStateModel {
        if (containedStateGetter == null || snowyPartsUnknown) {
            return model
        }

        if (!inSnow && !(standsOnSnow(level, pos, state) && snowVariantsOn())) {
            return model
        }

        return SnowyModel(model)
    }

    /** Checks whether a double-plant state stands directly above a snow container. */
    @JvmStatic
    fun standsOnSnow(
        level: BlockGetter,
        pos: BlockPos,
        state: BlockState,
    ): Boolean {
        return containedStateGetter != null &&
                state.hasProperty(DoublePlantBlock.HALF) &&
                isSnowBlock(level.getBlockState(pos.below()))
    }
}
