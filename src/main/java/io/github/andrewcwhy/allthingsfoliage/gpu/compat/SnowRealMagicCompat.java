package io.github.andrewcwhy.allthingsfoliage.gpu.compat;

import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Snow! Real Magic lets snow settle on plants: the plant's block becomes its snow layer block, and the plant lives on
 * inside that block's block entity, drawn through its own model at the same position before the snow. The renderer
 * meshes the plant inside instead of the block, so plants in snow sway near the player as they do further out; the
 * snow stays with the chunk mesh, which draws it on its own. Sway pushes the plant through the snow block, which the
 * mod registers for it (see ModCompatRegistry).
 * <p>
 * Read by name, as the mod is not compiled against: without it, or with a version that does not match, nothing here
 * applies and the plants in its snow stay with the chunk mesh.
 */
public final class SnowRealMagicCompat {

    private static final String MOD_ID = "snowrealmagic";
    /**
     * The classes of the mod's snow blocks that hold a plant; its fences, stairs and walls hold what they were made
     * from. Up to 10.x (1.20.1) one block, snowrealmagic:snow; from 12.x (1.21.1) several of one class: snow,
     * snow_extra_collision, snowy_plant, and a tall plant's two halves, snowy_double_plant_lower and _upper.
     */
    private static final String[] SNOW_BLOCKS = {
        "snownee.snow.block.EntitySnowLayerBlock",
        "snownee.snow.block.SRMSnowLayerBlock",
    };
    private static final String SNOW_BLOCK_ENTITY =
        "snownee.snow.block.entity.SnowBlockEntity";
    private static final String CORE_MODULE = "snownee.snow.CoreModule";
    /** How far the mod raises the plants it names in its offset_y tag, above the snow. */
    private static final float OFFSET_Y = 0.101F;

    private static final boolean LOADED = AllThingsFoliage.xplat().isModLoaded(
        MOD_ID
    );
    /** The plant a snow block holds, or null while the mod is not installed. */
    private static final MethodHandle CONTAINED = LOADED
        ? findContained()
        : null;
    private static final TagKey<?> RAISED = LOADED ? findRaisedTag() : null;
    /**
     * The snow blocks themselves, found once by their class and looked up by identity from then on: the renderer asks
     * about every block of every section it scans. Empty until they are looked for, and when there are none.
     */
    private static final Set<Block> SNOW_BLOCK_SET = Collections.newSetFromMap(
        new IdentityHashMap<>()
    );
    private static boolean snowBlocksSought;
    private static boolean warned;

    private SnowRealMagicCompat() {}

    private static MethodHandle findContained() {
        try {
            Class<?> entity = Class.forName(
                SNOW_BLOCK_ENTITY,
                false,
                SnowRealMagicCompat.class.getClassLoader()
            );
            return MethodHandles.publicLookup()
                .findVirtual(
                    entity,
                    "getContainedState",
                    MethodType.methodType(BlockState.class)
                )
                .asType(MethodType.methodType(BlockState.class, Object.class));
        } catch (ReflectiveOperationException | LinkageError e) {
            AllThingsFoliage.logger.warn(
                "Snow! Real Magic is installed, but not a version the GPU renderer knows: plants in its " +
                    "snow are left to the chunk mesh",
                e
            );
            return null;
        }
    }

    private static TagKey<?> findRaisedTag() {
        try {
            return (TagKey<?>) Class.forName(
                CORE_MODULE,
                false,
                SnowRealMagicCompat.class.getClassLoader()
            )
                .getField("OFFSET_Y")
                .get(null);
        } catch (
            ReflectiveOperationException
            | LinkageError
            | ClassCastException e
        ) {
            return null;
        }
    }

    /** Whether Snow! Real Magic is installed, and a version the renderer can read. */
    public static boolean isAvailable() {
        return CONTAINED != null;
    }

    /** Whether this is one of the snow blocks that can hold a plant. */
    public static boolean isSnowBlock(BlockState state) {
        if (CONTAINED == null) {
            return false;
        }
        if (!snowBlocksSought) {
            // Asked only once the world is drawn, when every block is registered.
            snowBlocksSought = true;
            for (String name : SNOW_BLOCKS) {
                Class<?> type;
                try {
                    type = Class.forName(
                        name,
                        false,
                        SnowRealMagicCompat.class.getClassLoader()
                    );
                } catch (ClassNotFoundException | LinkageError e) {
                    continue;
                }
                for (Block block : BuiltInRegistries.BLOCK) {
                    if (type.isInstance(block)) {
                        SNOW_BLOCK_SET.add(block);
                    }
                }
            }
        }
        return SNOW_BLOCK_SET.contains(state.getBlock());
    }

    /**
     * The plant the snow block at this position holds and draws, or null for any other block, an empty one, or snow
     * piled up to a full block, which the mod draws without the plant.
     */
    public static BlockState plantIn(
        BlockGetter level,
        BlockPos pos,
        BlockState state
    ) {
        if (
            !isSnowBlock(state) ||
            (state.hasProperty(SnowLayerBlock.LAYERS) &&
                state.getValue(SnowLayerBlock.LAYERS) >=
                    SnowLayerBlock.MAX_HEIGHT)
        ) {
            return null;
        }
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity == null) {
            return null;
        }
        try {
            BlockState plant = (BlockState) CONTAINED.invokeExact(
                (Object) entity
            );
            return plant == null || plant.isAir() ? null : plant;
        } catch (Throwable e) {
            if (!warned) {
                warned = true;
                AllThingsFoliage.logger.warn(
                    "Could not read the plant inside Snow! Real Magic's snow",
                    e
                );
            }
            return null;
        }
    }

    /** What stands at this position as far as the foliage goes: the plant inside the snow there, or the block. */
    public static BlockState plantAt(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockState plant = plantIn(level, pos, state);
        return plant != null ? plant : state;
    }

    /**
     * Whether Snow! Real Magic draws the plant at this position snowy, as the chunk mesh shows it: a plant held in its
     * snow (the level has the snow there, the plant being drawn from inside it), or the top half of a tall plant
     * standing on it.
     */
    public static boolean drawnSnowy(
        BlockGetter level,
        BlockPos pos,
        BlockState plant
    ) {
        return (
            CONTAINED != null &&
            ((isSnowBlock(level.getBlockState(pos)) && !isSnowBlock(plant)) ||
                standsOnSnow(level, pos, plant))
        );
    }

    private static final String SNOW_CLIENT_CONFIG_26 =
        "snownee.snow.client.SnowClientConfig";
    /** The part Snow! Real Magic bakes for a model with a snowy variant: its normal and its snowy part together. */
    private static final String SNOWY_PART =
        "snownee.snow.client.model.SnowyBlockStateModelPart";
    private static Field snowyPartField;
    private static Field snowVariantsField;
    private static boolean snowyPartsUnknown;

    /**
     * The model Snow! Real Magic draws a plant with where snow touches it, as the chunk mesh shows it: one whose parts
     * are the snowy ones, for the plant held in the snow, and for the top half of a tall plant standing on it while the
     * player has the snowy variants on. The plant's own model anywhere else.
     * <p>
     * From 26.1.2 the mod keeps the snowy variant in each part of the plant's model, and hands the snowy one only to
     * what it draws the plant through, which vanilla's block renderer is not.
     */
    public static BlockStateModel snowyVariant(
        BlockStateModel model,
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        boolean inSnow
    ) {
        if (CONTAINED == null || snowyPartsUnknown) {
            return model;
        }
        if (!inSnow && !(standsOnSnow(level, pos, state) && snowVariantsOn())) {
            return model;
        }
        return new SnowyModel(model);
    }

    private static boolean snowVariantsOn() {
        try {
            if (snowVariantsField == null) {
                snowVariantsField = Class.forName(
                    SNOW_CLIENT_CONFIG_26,
                    false,
                    SnowRealMagicCompat.class.getClassLoader()
                ).getField("snowVariants");
            }
            return snowVariantsField.getBoolean(null);
        } catch (
            ReflectiveOperationException
            | LinkageError
            | RuntimeException e
        ) {
            return true;
        }
    }

    /**
     * Swaps the mod's parts of a plant's model for their snowy ones where it draws the plant snowy: held in its snow
     * (the level has the snow there, the plant being drawn from inside it), or the top half of a tall plant standing on
     * it while the player has the snowy variants on. For a model that hands its parts on to a wrapper that only asks
     * for their quads, which would otherwise always get the plain ones: Sway's on NeoForge.
     */
    public static void snowyParts(
        BlockGetter level,
        BlockPos pos,
        BlockState plant,
        List<BlockStateModelPart> parts
    ) {
        if (CONTAINED == null || snowyPartsUnknown) {
            return;
        }
        boolean inSnow =
            isSnowBlock(level.getBlockState(pos)) && !isSnowBlock(plant);
        if (!inSnow && !(standsOnSnow(level, pos, plant) && snowVariantsOn())) {
            return;
        }
        parts.replaceAll(SnowRealMagicCompat::snowyPart);
    }

    /** The snowy part of one of the mod's parts, or the part itself for any other. */
    private static BlockStateModelPart snowyPart(BlockStateModelPart part) {
        if (
            snowyPartsUnknown || !part.getClass().getName().equals(SNOWY_PART)
        ) {
            return part;
        }
        try {
            if (snowyPartField == null) {
                Field field = part.getClass().getDeclaredField("snowy");
                field.setAccessible(true);
                snowyPartField = field;
            }
            Object snowy = snowyPartField.get(part);
            return snowy instanceof BlockStateModelPart snowyPart
                ? snowyPart
                : part;
        } catch (ReflectiveOperationException | RuntimeException e) {
            snowyPartsUnknown = true;
            AllThingsFoliage.logger.warn(
                "Could not find Snow! Real Magic's snowy plant models: plants in its snow are drawn " +
                    "without snow near the player",
                e
            );
            return part;
        }
    }

    /** A plant's model with the mod's parts swapped for their snowy ones. */
    private static final class SnowyModel implements BlockStateModel {

        private final BlockStateModel parent;

        SnowyModel(BlockStateModel parent) {
            this.parent = parent;
        }

        @Override
        public void collectParts(
            RandomSource random,
            List<BlockStateModelPart> parts
        ) {
            List<BlockStateModelPart> own = new ArrayList<>();
            parent.collectParts(random, own);
            for (BlockStateModelPart part : own) {
                parts.add(snowyPart(part));
            }
        }

        @Override
//? neoforge {
/*		public void collectParts(net.minecraft.client.renderer.block.BlockAndTintGetter level, BlockPos pos,
				BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
			List<BlockStateModelPart> own = new ArrayList<>();
			parent.collectParts(level, pos, state, random, own);
			for (BlockStateModelPart part : own) {
				parts.add(snowyPart(part));
			}
		}

        @Override
*/
//?}
        public Material.Baked particleMaterial() {
            return parent.particleMaterial();
        }

        @Override
        public int materialFlags() {
            return parent.materialFlags();
        }
    }

    /** Whether this is the top half of a tall plant whose lower half is held in Snow! Real Magic's snow. */
    public static boolean standsOnSnow(
        BlockGetter level,
        BlockPos pos,
        BlockState state
    ) {
        return (
            CONTAINED != null &&
            state.hasProperty(DoublePlantBlock.HALF) &&
            isSnowBlock(level.getBlockState(pos.below()))
        );
    }

    /** How far the mod raises a plant it holds, which the chunk mesh shows: some small plants sit on the snow. */
    @SuppressWarnings("unchecked")
    public static float liftOf(BlockState plant) {
        return RAISED != null && plant.is((TagKey<Block>) RAISED)
            ? OFFSET_Y
            : 0.0F;
    }
}
