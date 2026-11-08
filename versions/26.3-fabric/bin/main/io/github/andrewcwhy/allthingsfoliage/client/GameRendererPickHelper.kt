package io.github.andrewcwhy.allthingsfoliage.client

import net.minecraft.world.entity.Entity
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult

/**
 * Performs block raycasting against collision shapes instead of outlines.
 */
object GameRendererPickHelper {

    @JvmStatic
    fun pick(entity: Entity, pickRange: Double, partialTick: Float): HitResult {
        val eyePosition = entity.getEyePosition(partialTick)
        val viewVector = entity.getViewVector(partialTick)
        val endPosition = eyePosition.add(viewVector.scale(pickRange))

        return entity.level().clip(
            ClipContext(
                eyePosition,
                endPosition,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                entity
            )
        )
    }
}
