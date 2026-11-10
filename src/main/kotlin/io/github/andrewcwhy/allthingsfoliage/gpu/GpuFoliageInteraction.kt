package io.github.andrewcwhy.allthingsfoliage.gpu

import com.github.razorplay01.sway.client.SwayData
import com.github.razorplay01.sway.client.SwayEngine
import com.github.razorplay01.sway.config.SwayConfig
import net.minecraft.util.Util
import com.mojang.blaze3d.buffers.Std140Builder
import com.mojang.blaze3d.buffers.Std140SizeCalculator
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.renderpearl.api.buffers.GpuBuffer
import com.mojang.renderpearl.api.pipeline.BindGroupLayout
import com.mojang.renderpearl.api.pipeline.UniformType
import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage
import kotlin.math.abs
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.lwjgl.system.MemoryStack

/**
 * Transfers Sway's plant interactions to foliage shaders without remeshing sections.
 *
 * Each plant follows Sway's force through a damped spring. Positions and forces are uploaded
 * relative to the camera, with the nearest plants taking priority.
 *
 * Mutable rendering state is accessed on the render thread.
 */
object GpuFoliageInteraction {
    /** Maximum number of plant interactions uploaded per frame. */
    const val MAX_CELLS = 128

    /** Horizontal hitbox multiplier used only when calculating plant interactions. */
    const val PLANT_HITBOX_SCALE = 1.5

    const val UNIFORM = "FoliageInteraction"

    val LAYOUT: BindGroupLayout =
        BindGroupLayout.builder()
            .withUniform(UNIFORM, UniformType.UNIFORM_BUFFER)
            .build()

    private const val CELL_DAMPING = 7.0f
    private const val CELL_REACH_SQUARED = 256.0 * 256.0
    private const val CELL_SETTLED = 0.005f
    private const val CELL_STIFFNESS = 90.0f
    private const val MAX_SLIDER_INTENSITY = 2.0f
    private const val PUSH_BOOST = 1.5f
    private const val SWAY_PUSH_CAP = 2.0f

    private val bufferSize = calculateBufferSize()
    private val byDistance = compareBy<Cell> { it.distanceSquared }

    /** Minimum and maximum interaction positions relative to the camera. */
    private val cellBounds = FloatArray(6)
    private val cellForces = FloatArray(MAX_CELLS * 4)
    private val cellPositions = FloatArray(MAX_CELLS * 4)
    private val cells = hashMapOf<BlockPos, Cell>()
    private val nearest = mutableListOf<Cell>()

    private val swayForceMap: Map<BlockPos, SwayData> = findSwayForces()

    private var buffer: GpuBuffer? = null
    private var lastNanos = 0L

    /** Returns a live, read-only Kotlin view of Sway's interaction forces. */
    val swayForces: Map<BlockPos, SwayData>
        get() {
            return swayForceMap
        }

    /** Returns the last uploaded buffer without advancing the springs. */
    val uploadedBuffer: GpuBuffer?
        get() {
            return buffer
        }

    /** Tracks a plant until both its force and velocity have settled. */
    private class Cell(val pos: BlockPos) {
        var distanceSquared = 0.0
        var pushX = 0.0f
        var pushZ = 0.0f
        var targetX = 0.0f
        var targetZ = 0.0f
        var touched = false
        var velocityX = 0.0f
        var velocityZ = 0.0f
    }

    /** Matches the shader block: count, two bounds, positions, then forces. */
    private fun calculateBufferSize(): Int {
        val size =
            Std140SizeCalculator()
                .putInt()
                .putVec4()
                .putVec4()

        repeat(MAX_CELLS * 2) {
            size.putVec4()
        }

        return size.get()
    }

    /** Collects nearby plants and bounds their displacement to the renderer's culling allowance. */
    private fun collectCells(camera: Vec3): Int {
        val limit = SWAY_PUSH_CAP * minOf(SwayConfig.INSTANCE.intensity, MAX_SLIDER_INTENSITY)

        nearest.clear()

        for (cell in cells.values) {
            cell.distanceSquared = cell.pos.distToCenterSqr(camera)

            if (cell.distanceSquared < CELL_REACH_SQUARED) {
                nearest.add(cell)
            }
        }

        nearest.sortWith(byDistance)

        val count = minOf(nearest.size, MAX_CELLS)

        for (index in 0 until count) {
            val cell = nearest[index]
            val x = (cell.pos.x - camera.x).toFloat()
            val y = (cell.pos.y - camera.y).toFloat()
            val z = (cell.pos.z - camera.z).toFloat()
            val offset = index * 4

            cellPositions[offset] = x
            cellPositions[offset + 1] = y
            cellPositions[offset + 2] = z

            cellForces[offset] = cell.pushX
            cellForces[offset + 1] = cell.pushZ
            cellForces[offset + 2] = limit

            if (index == 0) {
                cellBounds[0] = x
                cellBounds[1] = y
                cellBounds[2] = z
                cellBounds[3] = x
                cellBounds[4] = y
                cellBounds[5] = z
            } else {
                cellBounds[0] = minOf(cellBounds[0], x)
                cellBounds[1] = minOf(cellBounds[1], y)
                cellBounds[2] = minOf(cellBounds[2], z)
                cellBounds[3] = maxOf(cellBounds[3], x)
                cellBounds[4] = maxOf(cellBounds[4], y)
                cellBounds[5] = maxOf(cellBounds[5], z)
            }
        }

        return count
    }

    /** Reads Sway's live force map, which its public API does not expose. */
    @Suppress("UNCHECKED_CAST")
    private fun findSwayForces(): Map<BlockPos, SwayData> {
        try {
            val currentField = SwayEngine::class.java.getDeclaredField("CURRENT")
            currentField.isAccessible = true

            return currentField.get(null) as Map<BlockPos, SwayData>
        } catch (exception: Exception) {
            if (
                exception !is ReflectiveOperationException &&
                exception !is RuntimeException
            ) {
                throw exception
            }

            AllThingsFoliage.logger.warn(
                "Could not read Sway's forces; foliage drawn on the GPU will not react to entities",
                exception,
            )

            return emptyMap()
        }
    }

    private fun followForces(forces: Map<BlockPos, SwayData>) {
        for ((pos, force) in forces) {
            if (force.intensity < 0.01f) {
                continue
            }

            val cell = cells[pos] ?: Cell(pos.immutable()).also {
                cells[it.pos] = it
            }

            cell.touched = true

            // Sway's force already includes the boost used by chunk-rendered foliage.
            cell.targetX = force.nx * force.intensity
            cell.targetZ = force.nz * force.intensity
        }
    }

    /** Follows active interactions and returns untouched plants toward their resting positions. */
    private fun followSway(delta: Float) {
        for (cell in cells.values) {
            cell.touched = false
            cell.targetX = 0.0f
            cell.targetZ = 0.0f
        }

        followForces(swayForceMap)
        stepSprings(delta)
    }

    /** Returns a horizontally expanded interaction box without changing the entity's hitbox. */
    fun plantHitbox(hitbox: AABB): AABB {
        val growX = hitbox.xsize * (PLANT_HITBOX_SCALE - 1.0) * 0.5
        val growZ = hitbox.zsize * (PLANT_HITBOX_SCALE - 1.0) * 0.5

        return hitbox.inflate(growX, 0.0, growZ)
    }

    /** Boosts the interaction force without changing Sway's configured displacement cap. */
    fun plantPushIntensity(intensity: Float): Float {
        return intensity * PUSH_BOOST
    }

    private fun putAll(data: Std140Builder, vectors: FloatArray) {
        for (index in vectors.indices step 4) {
            data.putVec4(
                vectors[index],
                vectors[index + 1],
                vectors[index + 2],
                vectors[index + 3],
            )
        }
    }

    /** Forgets plant springs and restarts frame timing when the world changes. */
    fun reset() {
        cells.clear()
        lastNanos = 0L
    }

    private fun stepSprings(delta: Float) {
        val iterator = cells.values.iterator()

        while (iterator.hasNext()) {
            val cell = iterator.next()

            // Update velocity before position, preserving the original integration order.
            // Update velocity before position, preserving the original integration order.
            cell.velocityX +=
                ((cell.targetX - cell.pushX) * CELL_STIFFNESS -
                        cell.velocityX * CELL_DAMPING) * delta

            cell.velocityZ +=
                ((cell.targetZ - cell.pushZ) * CELL_STIFFNESS -
                        cell.velocityZ * CELL_DAMPING) * delta

            cell.pushX += cell.velocityX * delta
            cell.pushZ += cell.velocityZ * delta

            if (
                !cell.touched &&
                abs(cell.pushX) < CELL_SETTLED &&
                abs(cell.pushZ) < CELL_SETTLED &&
                abs(cell.velocityX) < CELL_SETTLED &&
                abs(cell.velocityZ) < CELL_SETTLED
            ) {
                iterator.remove()
            }
        }
    }

    /** Returns a live, read-only Kotlin view of Sway's interaction forces. */
    fun swayForces(): Map<BlockPos, SwayData> {
        return swayForceMap
    }

    private fun update(camera: Vec3): Int {
        val now = Util.getNanos()

        // Use elapsed real time, capped to limit integration after a delayed frame.
        val delta = if (lastNanos == 0L) {
            0.0f
        } else {
            minOf((now - lastNanos) / 1.0e9f, 0.1f)
        }

        lastNanos = now

        cellPositions.fill(0.0f)
        cellForces.fill(0.0f)
        cellBounds.fill(0.0f)

        if (!SwayConfig.INSTANCE.enabled) {
            cells.clear()
            return 0
        }

        followSway(delta)

        return collectCells(camera)
    }

    /** Uploads this frame's interactions on the render thread before opening a render pass. */
    fun upload(camera: Vec3): GpuBuffer {
        val cellCount = update(camera)
        val device = RenderSystem.getDevice()

        val interactionBuffer = buffer ?: device.createBuffer(
            { "All Things Foliage interaction" },
            GpuBuffer.USAGE_UNIFORM or GpuBuffer.USAGE_COPY_DST,
            bufferSize.toLong(),
        ).also {
            buffer = it
        }

        MemoryStack.stackPush().use { stack ->
            val data = Std140Builder.onStack(stack, bufferSize)
                .putInt(cellCount)
                .putVec4(cellBounds[0], cellBounds[1], cellBounds[2], 0.0f)
                .putVec4(cellBounds[3], cellBounds[4], cellBounds[5], 0.0f)

            putAll(data, cellPositions)
            putAll(data, cellForces)

            device.createCommandEncoder()
                .writeToBuffer(interactionBuffer.slice(), data.get())
        }

        return interactionBuffer
    }

    /** Reuses the last upload for additional passes without advancing the springs again. */
    fun uploaded(): GpuBuffer? {
        return buffer
    }
}
