
package io.github.andrewcwhy.allthingsfoliage.mixin

import com.github.razorplay01.sway.client.behavior.force.ProximityForceBehavior
import com.github.razorplay01.sway.config.SwayConfig
import io.github.andrewcwhy.allthingsfoliage.gpu.GpuFoliageInteraction
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.AABB
import org.objectweb.asm.Opcodes
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Redirect

/**
 * Extends the range and strength of entity interactions with foliage
 * beyond Sway's default proximity behavior.
 *
 * Modifies only the values read during Sway's force calculations:
 * - Uses an expanded bounding box without affecting the entity's actual
 *   collision geometry, movement, or damage detection.
 * - Increases the effective push intensity without modifying the
 *   player's configured value.
 *
 * These adjustments remain client-side and apply consistently to
 * chunk-mesh and GPU-rendered foliage.
 */
@Mixin(ProximityForceBehavior::class)
abstract class SwayProximityForceMixin {

    /**
     * Substitutes an expanded bounding box during proximity force calculations.
     *
     * Remaps the Minecraft method invocation while preserving Sway's
     * unmapped method name.
     */
    @Redirect(
        method = ["contributeForce"],
        at = [
            At(
                value = "INVOKE",
                target = "Lnet/minecraft/world/entity/Entity;getBoundingBox()Lnet/minecraft/world/phys/AABB;",
                remap = true
            )
        ],
        remap = false
    )
    private fun `allthingsfoliage$plantHitbox`(entity: Entity): AABB =
        GpuFoliageInteraction.plantHitbox(entity.boundingBox)

    /**
     * Adjusts the push intensity used during foliage force calculations.
     *
     * Intercepts the field read without changing the underlying
     * configuration value.
     */
    @Redirect(
        method = ["contributeForce"],
        at = [
            At(
                value = "FIELD",
                target = "Lcom/github/razorplay01/sway/config/SwayConfig;intensity:F",
                opcode = Opcodes.GETFIELD
            )
        ],
        remap = false
    )
    private fun `allthingsfoliage$plantPushIntensity`(config: SwayConfig): Float =
        GpuFoliageInteraction.plantPushIntensity(config.intensity)
}
