package io.github.andrewcwhy.allthingsfoliage.client.particle;

import com.github.razorplay01.sway.client.SwayData;
import com.github.razorplay01.sway.config.SwayConfig;
import io.github.andrewcwhy.allthingsfoliage.gpu.GpuFoliageInteraction;
import io.github.andrewcwhy.allthingsfoliage.gpu.GpuFoliageSplit;
import io.github.andrewcwhy.allthingsfoliage.gpu.compat.SnowRealMagicCompat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Shakes snow off a plant standing in Snow! Real Magic's snow the moment something pushes it, as if the push knocked
 * it loose: a few snowflakes, and now and then a crumb of the snow itself, drifting the way the plant was pushed.
 * <p>
 * A push is Sway's, read from its engine, so it is seen the same whoever draws the plant, and for every entity that
 * pushes one. Only the start of a push shakes a plant -- standing in it does not keep shaking it -- and a plant
 * shaken a moment ago waits a second before it shakes again. The game's particle setting decides how many are shown.
 * Client only: nothing is sent anywhere.
 */
public final class SnowShakeParticles {

    /** A push below this is Sway letting go, not a touch. */
    private static final float PUSHED = 0.05F;
    /** The most particles one shake gives, for a push as hard as Sway's intensity setting; a light brush gives fewer. */
    private static final int MOST_PARTICLES = 5;
    private static final int FEWEST_PARTICLES = 2;
    /** How much of a shake is crumbs of snow rather than flakes. */
    private static final float CRUMB_SHARE = 0.25F;
    /** Ticks a plant waits before it shakes again. */
    private static final int COOLDOWN_TICKS = 20;
    /** How fast the particles leave the plant the way it was pushed, and upwards, in blocks per tick. */
    private static final double PUSH_SPEED = 0.035;
    private static final double LIFT_SPEED = 0.02;
    private static final double SPREAD_SPEED = 0.012;
    /** How far into a tall plant's top half the snow is shaken from, above where that half begins. */
    private static final double TALL_PLANT_REACH = 0.3;

    /** The plants being pushed as of the last tick: a push that goes on is not a new one. */
    private static Set<BlockPos> pushed = new HashSet<>();
    /** When each plant last shook, in game ticks. */
    private static final Map<BlockPos, Long> LAST_SHAKE = new HashMap<>();

    private SnowShakeParticles() {}

    /** Called once per client tick. */
    public static void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        Map<BlockPos, SwayData> forces = GpuFoliageInteraction.swayForces();
        if (
            level == null ||
            !SnowRealMagicCompat.isAvailable() ||
            forces.isEmpty()
        ) {
            pushed.clear();
            return;
        }
        long now = level.getGameTime();
        Set<BlockPos> pushedNow = new HashSet<>();
        for (Map.Entry<BlockPos, SwayData> entry : forces.entrySet()) {
            SwayData force = entry.getValue();
            if (force == null || force.intensity < PUSHED) {
                continue;
            }
            BlockPos pos = entry.getKey();
            pushedNow.add(pos);
            if (pushed.contains(pos)) {
                continue;
            }
            // Sway also pushes the top half of a tall plant where it stands; the plant shakes once, from where it is held,
            // the block in the snow.
            BlockState plant = SnowRealMagicCompat.plantIn(
                level,
                pos,
                level.getBlockState(pos)
            );
            if (plant == null || !GpuFoliageSplit.isFoliage(plant)) {
                continue;
            }
            Long last = LAST_SHAKE.get(pos);
            if (last != null && now - last < COOLDOWN_TICKS) {
                continue;
            }
            LAST_SHAKE.put(pos, now);
            shake(level, pos, plant, force);
        }
        pushed = pushedNow;
        if ((now & 63) == 0) {
            LAST_SHAKE.values().removeIf(time -> now - time >= COOLDOWN_TICKS);
        }
    }

    private static void shake(
        ClientLevel level,
        BlockPos pos,
        BlockState plant,
        SwayData force
    ) {
        RandomSource random = level.getRandom();
        float strength = Math.min(
            1.0F,
            force.intensity / Math.max(0.01F, SwayConfig.INSTANCE.intensity)
        );
        int count = Math.max(
            FEWEST_PARTICLES,
            Math.round(MOST_PARTICLES * strength)
        );
        double top = pos.getY() + topOf(level, pos, plant);
        ParticleOptions crumb = new BlockParticleOption(
            ParticleTypes.BLOCK,
            Blocks.SNOW.defaultBlockState()
        );
        for (int i = 0; i < count; i++) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
            double y = top - random.nextDouble() * 0.35;
            double vx =
                force.nx * PUSH_SPEED +
                (random.nextDouble() - 0.5) * SPREAD_SPEED;
            double vz =
                force.nz * PUSH_SPEED +
                (random.nextDouble() - 0.5) * SPREAD_SPEED;
            double vy = LIFT_SPEED * random.nextDouble();
            level.addParticle(
                random.nextFloat() < CRUMB_SHARE
                    ? crumb
                    : ParticleTypes.SNOWFLAKE,
                x,
                y,
                z,
                vx,
                vy,
                vz
            );
        }
    }

    /**
     * How high above its block the snow is shaken from: the plant's top, or for a tall plant whose top half stands
     * above, just where that top half begins -- where the snow settles on it, not the tip.
     */
    private static double topOf(
        ClientLevel level,
        BlockPos pos,
        BlockState plant
    ) {
        if (plant.hasProperty(DoublePlantBlock.HALF)) {
            BlockPos above = pos.above();
            BlockState upper = level.getBlockState(above);
            if (upper.getBlock() == plant.getBlock()) {
                return (
                    1.0 +
                    Math.min(TALL_PLANT_REACH, heightOf(level, above, upper))
                );
            }
        }
        return heightOf(level, pos, plant);
    }

    private static double heightOf(
        ClientLevel level,
        BlockPos pos,
        BlockState state
    ) {
        VoxelShape shape = state.getShape(level, pos);
        return shape.isEmpty() ? 0.8 : shape.max(Direction.Axis.Y);
    }
}
