package io.github.andrewcwhy.allthingsfoliage.mixin;

import com.github.razorplay01.sway.client.behavior.force.ProximityForceBehavior;
import com.github.razorplay01.sway.config.SwayConfig;
import io.github.andrewcwhy.allthingsfoliage.gpu.GpuFoliageInteraction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Makes entities push plants from a little further away, and a little harder, than Sway does by itself.
 * <p>
 * Only what Sway reads while working out a plant's push is changed. The box is a new one, so the entity keeps
 * its own hitbox and collisions, damage and movement are untouched; and the intensity is raised only as that
 * one read sees it, so the setting stays as the player chose it. Nothing leaves this client.
 */
@Mixin(ProximityForceBehavior.class)
public abstract class SwayProximityForceMixin {

    // Sway's own method is never renamed, only Minecraft's: the call it makes is the one part looked up in the mappings.
    @Redirect(
        method = "contributeForce",
        remap = false,
        at = @At(
            value = "INVOKE",
            remap = true,
            target = "Lnet/minecraft/world/entity/Entity;getBoundingBox()Lnet/minecraft/world/phys/AABB;"
        )
    )
    private AABB mc2$plantHitbox(Entity entity) {
        return GpuFoliageInteraction.plantHitbox(entity.getBoundingBox());
    }

    /** The intensity Sway pushes with. Raising it here moves plants alike in the chunk mesh and on the GPU. */
    @Redirect(
        method = "contributeForce",
        remap = false,
        at = @At(
            value = "FIELD",
            target = "Lcom/github/razorplay01/sway/config/SwayConfig;intensity:F",
            opcode = Opcodes.GETFIELD
        )
    )
    private float mc2$plantPushIntensity(SwayConfig config) {
        return GpuFoliageInteraction.plantPushIntensity(config.intensity);
    }
}
