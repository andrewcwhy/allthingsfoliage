package io.github.andrewcwhy.allthingsfoliage.passability

import io.github.andrewcwhy.allthingsfoliage.Config
import io.github.andrewcwhy.allthingsfoliage.duck.FoliageBlock
import io.github.andrewcwhy.allthingsfoliage.duck.FoliageEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.tags.BlockTags
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState

/** Applies movement, sound, and fall effects while entities intersect passable foliage. */
object FoliagePassability {
    private val suppressionDepth = ThreadLocal.withInitial { 0 }

    fun hasLeafWalker(entity: LivingEntity): Boolean {
        return Config.passability().alwaysLeafWalking() ||
            EnchantmentHelper.has(
                entity.getItemBySlot(EquipmentSlot.FEET),
                FoliageEnchantments.leafWalkerEffect,
            )
    }

    fun isPartiallyInFoliage(entity: LivingEntity): Boolean {
        return (entity as FoliageEntity).partiallyInFoliage
    }

    fun isPassable(state: BlockState): Boolean {
        return suppressionDepth.get() == 0 && (state.block as FoliageBlock).foliagePassable
    }

    fun onEntityInside(level: Level, pos: BlockPos, state: BlockState, entity: Entity) {
        val livingEntity = entity as? LivingEntity ?: return
        val settings = Config.passability()
        if (entity is Player && entity.isCreative && entity.abilities.flying) {
            return
        }
        if (!entity.isPassenger) {
            val colliding = withOriginalCollision { entity.isColliding(pos, state) }
            if (!colliding) {
                return
            }
        }

        if ((!settings.soundsPlayerOnly() || entity is Player) && state.`is`(BlockTags.LEAVES)) {
            val sound = state.soundType
            if (livingEntity.fallDistance > 3.0) {
                entity.playSound(
                    sound.breakSound,
                    sound.volume * 0.6f * settings.soundVolume(),
                    sound.pitch * 0.65f,
                )
            } else if (level.gameTime % 6L == 0L && entity.knownSpeed.lengthSqr() > 5e-7) {
                entity.playSound(
                    sound.hitSound,
                    sound.volume * 0.5f * settings.soundVolume(),
                    sound.pitch * 0.45f,
                )
            }
        }

        var horizontalSpeed = 1.0f
        var verticalSpeed = 1.0f
        val foliageEntity = livingEntity as FoliageEntity
        if (settings.alwaysLeafWalking() || !hasLeafWalker(livingEntity)) {
            val jumping = livingEntity.isJumping || livingEntity.knownSpeed.y > 0.0
            if (!jumping && !foliageEntity.foliageSlownessHandled) {
                horizontalSpeed = settings.speedMultiplierHorizontal()
                verticalSpeed = settings.speedMultiplierVertical()
            }
        }
        foliageEntity.foliageSlownessHandled = true
        if (horizontalSpeed < 1.0f || verticalSpeed < 1.0f) {
            entity.deltaMovement =
                entity.deltaMovement.multiply(
                    horizontalSpeed.toDouble(),
                    verticalSpeed.toDouble(),
                    horizontalSpeed.toDouble(),
                )
        }

        val threshold = settings.fallDamageThreshold().toDouble()
        if (livingEntity.fallDistance > threshold) {
            livingEntity.fallDistance -= threshold
            livingEntity.causeFallDamage(
                threshold,
                1.0f - settings.fallDamageMultiplier(),
                level.damageSources().fall(),
            )
        }
        if (livingEntity.fallDistance > 1.0) {
            livingEntity.fallDistance = 1.0
        }

        for (passenger in entity.passengers) {
            onEntityInside(level, pos, state, passenger)
        }
    }

    fun setCollisionSuppressed(suppressed: Boolean) {
        val depth = suppressionDepth.get()
        if (suppressed) {
            suppressionDepth.set(depth + 1)
        } else {
            check(depth > 0) { "Unbalanced foliage collision suppression" }
            if (depth == 1) {
                suppressionDepth.remove()
            } else {
                suppressionDepth.set(depth - 1)
            }
        }
    }

    /** Rebuilds changed state caches after tags are bound or reloaded. */
    fun tagsLoaded() {
        check(BuiltInRegistries.BLOCK.all { it is FoliageBlock }) {
            "The All Things Foliage passability mixins were not applied; check loader metadata"
        }
        for (block in BuiltInRegistries.BLOCK) {
            val foliageBlock = block as FoliageBlock
            val passable = block.defaultBlockState().`is`(FoliageTags.passables)
            if (foliageBlock.foliagePassable != passable) {
                foliageBlock.foliagePassable = passable
                for (state in block.stateDefinition.possibleStates) {
                    state.initCache()
                }
            }
        }
    }

    /** Inlines the scope into mixins without generating callbacks that reference mixin classes. */
    inline fun <T> withOriginalCollision(action: () -> T): T {
        setCollisionSuppressed(true)
        try {
            return action()
        } finally {
            setCollisionSuppressed(false)
        }
    }
}
