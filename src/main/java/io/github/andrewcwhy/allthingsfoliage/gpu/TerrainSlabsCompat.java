package io.github.andrewcwhy.allthingsfoliage.gpu;

import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * Terrain Slabs lets plants grow on bottom slabs, and lowers them half a block onto the slab where their model is asked
 * for its geometry through the rendering API. The renderer meshes from the plain model underneath, so it never sees that
 * and the plant floats; it lowers the plant the same way instead, by the same rule.
 */
final class TerrainSlabsCompat {

    private static final String MOD_ID = "terrain_slabs";
    private static final String HELPER =
        "net.countered.terrainslabs.util.OnTopHelper";
    private static final String VALID_ON_TOP =
        "terrain_slabs$isStateValidOnTop";
    /** How far Terrain Slabs lowers a plant standing on a bottom slab. */
    private static final float SLAB_DROP = -0.5F;

    /** Which plants Terrain Slabs lowers onto a slab, or null while it is not installed. */
    private static final MethodHandle LOWERS = findRule();

    private TerrainSlabsCompat() {}

    private static MethodHandle findRule() {
        if (!AllThingsFoliage.xplat().isModLoaded(MOD_ID)) {
            return null;
        }
        try {
            Class<?> helper = Class.forName(
                HELPER,
                false,
                TerrainSlabsCompat.class.getClassLoader()
            );
            return MethodHandles.publicLookup().findStatic(
                helper,
                VALID_ON_TOP,
                MethodType.methodType(boolean.class, BlockState.class)
            );
        } catch (ReflectiveOperationException | LinkageError e) {
            AllThingsFoliage.LOGGER.warn(
                "Terrain Slabs is installed, but not a version the GPU renderer knows: plants on its " +
                    "slabs may float while the renderer draws them",
                e
            );
            return null;
        }
    }

    /**
     * How far to move this plant up or down from its block: half a block down onto the bottom slab it stands on, as
     * Terrain Slabs draws it, or not at all. The upper half of a tall plant follows the slab under its lower half.
     */
    static float offsetY(BlockGetter level, BlockPos pos, BlockState state) {
        if (LOWERS == null) {
            return 0.0F;
        }
        boolean upperHalf =
            state.getBlock() instanceof DoublePlantBlock &&
            state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER;
        BlockState ground = level.getBlockState(pos.below(upperHalf ? 2 : 1));
        if (
            !(ground.getBlock() instanceof SlabBlock) ||
            ground.getValue(SlabBlock.TYPE) != SlabType.BOTTOM
        ) {
            return 0.0F;
        }
        try {
            return (boolean) LOWERS.invokeExact(state) ? SLAB_DROP : 0.0F;
        } catch (Throwable e) {
            return 0.0F;
        }
    }
}
