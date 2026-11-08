@file:JvmName("LocalPlayerPickMixin")
@file:Mixin(LocalPlayer::class)

package io.github.andrewcwhy.allthingsfoliage.mixin.targeting

import com.llamalad7.mixinextras.injector.ModifyReturnValue
import com.llamalad7.mixinextras.sugar.Share
import com.llamalad7.mixinextras.sugar.ref.LocalRef
import io.github.andrewcwhy.allthingsfoliage.Config
import io.github.andrewcwhy.allthingsfoliage.client.GameRendererPickHelper
import java.util.function.Predicate
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.HitResult
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.ModifyArg
import org.spongepowered.asm.mixin.injection.ModifyVariable

@ModifyArg(
    method = [
        "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;"
    ],
    at = [At(
        value = "INVOKE",
        target =
            "Lnet/minecraft/world/entity/projectile/ProjectileUtil;" +
                    "getEntityHitResult(" +
                    "Lnet/minecraft/world/entity/Entity;" +
                    "Lnet/minecraft/world/phys/Vec3;" +
                    "Lnet/minecraft/world/phys/Vec3;" +
                    "Lnet/minecraft/world/phys/AABB;" +
                    "Ljava/util/function/Predicate;D)" +
                    "Lnet/minecraft/world/phys/EntityHitResult;",
    )],
)
private fun filterPickEntities(entitySelector: Predicate<Entity>): Predicate<Entity> {
    if (Config.targetAliveOnly()) {
        return entitySelector.and { entity -> entity.isAlive }
    }

    return entitySelector
}

@ModifyArg(
    method = ["raycastHitResult"],
    at = [At(
        value = "INVOKE",
        target =
            "Lnet/minecraft/world/item/component/AttackRange;" +
                    "getClosesetHit(" +
                    "Lnet/minecraft/world/entity/Entity;F" +
                    "Ljava/util/function/Predicate;)" +
                    "Lnet/minecraft/world/phys/HitResult;",
    )],
)
private fun filterRaycastEntities(entitySelector: Predicate<Entity>): Predicate<Entity> {
    if (Config.targetAliveOnly()) {
        return entitySelector.and { entity -> entity.isAlive }
    }

    return entitySelector
}

/** Extends the block hit past foliage while retaining the original hit for fallback. */
@ModifyVariable(
    method = [
        "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;"
    ],
    at = [At("STORE")],
)
private fun preserveFoliageHit(
    hitResult: HitResult,
    entity: Entity,
    blockInteractionRange: Double,
    entityInteractionRange: Double,
    partialTick: Float,
    @Share("originalHitResult") originalHitResult: LocalRef<HitResult?>,
): HitResult {
    val foliageHit = GameRendererPickHelper.pick(entity, entityInteractionRange, partialTick)
    val eyePosition = entity.getEyePosition(partialTick)

    if (
        foliageHit.location.distanceToSqr(eyePosition) >
        hitResult.location.distanceToSqr(eyePosition)
    ) {
        originalHitResult.set(hitResult)
        return foliageHit
    }

    return hitResult
}

/** Restores the nearer block hit when targeting did not find an entity. */
@ModifyReturnValue(
    method = [
        "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;"
    ],
    at = [At("TAIL")],
)
private fun restoreOriginalBlockHit(
    hitResult: HitResult,
    entity: Entity,
    blockInteractionRange: Double,
    entityInteractionRange: Double,
    partialTick: Float,
    @Share("originalHitResult") originalHitResult: LocalRef<HitResult?>,
): HitResult {
    val originalHit = originalHitResult.get() ?: return hitResult

    if (hitResult.type == HitResult.Type.ENTITY) {
        return hitResult
    }

    val eyePosition = entity.getEyePosition(partialTick)

    if (
        originalHit.location.distanceToSqr(eyePosition) <
        hitResult.location.distanceToSqr(eyePosition)
    ) {
        return originalHit
    }

    return hitResult
}
