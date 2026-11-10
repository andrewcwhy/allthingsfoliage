package io.github.andrewcwhy.allthingsfoliage.client.particle

import com.github.razorplay01.sway.client.SwayData
import com.github.razorplay01.sway.config.SwayConfig
import io.github.andrewcwhy.allthingsfoliage.gpu.GpuFoliageInteraction
import io.github.andrewcwhy.allthingsfoliage.gpu.GpuFoliageSplit
import io.github.andrewcwhy.allthingsfoliage.gpu.compat.SnowRealMagicCompat
import kotlin.math.floor
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.BlockParticleOption
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.DoublePlantBlock
import net.minecraft.world.level.block.state.BlockState

/**
 * Emits client-side snow particles when foliage is first pushed by Sway.
 *
 * Continuous pushes do not trigger additional particles, and recently shaken
 * plants must complete their cooldown before shaking again.
 */
object SnowShakeParticles {
    private const val PUSH_THRESHOLD = 0.05f
    private const val MIN_PARTICLES = 2
    private const val MAX_PARTICLES = 5
    private const val CRUMB_SHARE = 0.25f
    private const val COOLDOWN_TICKS = 20L

    private const val PUSH_SPEED = 0.035
    private const val LIFT_SPEED = 0.02
    private const val SPREAD_SPEED = 0.012
    private const val TALL_PLANT_REACH = 0.3

    private var pushedLastTick = hashSetOf<BlockPos>()
    private val lastShakeTick = hashMapOf<BlockPos, Long>()

    /** Processes foliage pushes once per client tick. */
    fun tick() {
        val level = Minecraft.getInstance().level
        val forces = GpuFoliageInteraction.swayForces

        if (level == null || !SnowRealMagicCompat.isAvailable() || forces.isEmpty()) {
            pushedLastTick.clear()
            return
        }

        val now = level.gameTime
        val pushedNow = hashSetOf<BlockPos>()

        for ((pos, force) in forces) {
            if (force == null || force.intensity < PUSH_THRESHOLD) {
                continue
            }

            pushedNow.add(pos)

            if (pos in pushedLastTick) {
                continue
            }

            // Resolve the plant inside the snow rather than its exposed upper half.
            val plant = SnowRealMagicCompat.plantIn(
                level,
                pos,
                level.getBlockState(pos)
            )

            if (plant == null || !GpuFoliageSplit.isFoliage(plant)) {
                continue
            }

            val previousShake = lastShakeTick[pos]

            if (previousShake != null && now - previousShake < COOLDOWN_TICKS) {
                continue
            }

            lastShakeTick[pos] = now
            shake(level, pos, plant, force)
        }

        pushedLastTick = pushedNow

        if ((now and 63L) == 0L) {
            lastShakeTick.values.removeAll { lastTick ->
                now - lastTick >= COOLDOWN_TICKS
            }
        }
    }

    /** Spawns snowflakes and occasional snow crumbs along the push direction. */
    private fun shake(
        level: ClientLevel,
        pos: BlockPos,
        plant: BlockState,
        force: SwayData
    ) {
        val random = level.random

        val strength = minOf(
            1.0f,
            force.intensity / maxOf(0.01f, SwayConfig.INSTANCE.intensity)
        )

        val count = maxOf(
            MIN_PARTICLES,
            floor(MAX_PARTICLES * strength + 0.5f).toInt()
        )

        val top = pos.y + topOf(level, pos, plant)

        val crumb = BlockParticleOption(
            ParticleTypes.BLOCK,
            Blocks.SNOW.defaultBlockState()
        )

        repeat(count) {
            val x = pos.x + 0.5 + (random.nextDouble() - 0.5) * 0.6
            val z = pos.z + 0.5 + (random.nextDouble() - 0.5) * 0.6
            val y = top - random.nextDouble() * 0.35

            val vx = force.nx * PUSH_SPEED +
                    (random.nextDouble() - 0.5) * SPREAD_SPEED

            val vz = force.nz * PUSH_SPEED +
                    (random.nextDouble() - 0.5) * SPREAD_SPEED

            val vy = LIFT_SPEED * random.nextDouble()

            val particle: ParticleOptions = if (random.nextFloat() < CRUMB_SHARE) {
                crumb
            } else {
                ParticleTypes.SNOWFLAKE
            }

            level.addParticle(
                particle,
                x,
                y,
                z,
                vx,
                vy,
                vz
            )
        }
    }

    /**
     * Returns the snow-release height, accounting for the upper half
     * of a double-height plant.
     */
    private fun topOf(
        level: ClientLevel,
        pos: BlockPos,
        plant: BlockState
    ): Double {
        if (plant.hasProperty(DoublePlantBlock.HALF)) {
            val above = pos.above()
            val upper = level.getBlockState(above)

            if (upper.block == plant.block) {
                return 1.0 + minOf(
                    TALL_PLANT_REACH,
                    heightOf(level, above, upper)
                )
            }
        }

        return heightOf(level, pos, plant)
    }

    /** Returns the foliage shape height or its fallback for empty shapes. */
    private fun heightOf(
        level: ClientLevel,
        pos: BlockPos,
        state: BlockState
    ): Double {
        val shape = state.getShape(level, pos)

        return if (shape.isEmpty) {
            0.8
        } else {
            shape.max(Direction.Axis.Y)
        }
    }
}
