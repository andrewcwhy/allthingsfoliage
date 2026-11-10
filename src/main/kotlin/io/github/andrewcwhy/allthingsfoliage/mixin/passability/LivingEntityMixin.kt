package io.github.andrewcwhy.allthingsfoliage.mixin.passability

import io.github.andrewcwhy.allthingsfoliage.Config
import io.github.andrewcwhy.allthingsfoliage.duck.FoliageEntity
import io.github.andrewcwhy.allthingsfoliage.passability.FoliagePassability
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.Level
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(value = [LivingEntity::class], priority = 0)
abstract class LivingEntityMixin(type: EntityType<*>, level: Level) :
    Entity(type, level), FoliageEntity {
    @field:Unique
    override var foliageSlownessHandled: Boolean = false

    @field:Unique
    override var partiallyInFoliage: Boolean = false

    @Inject(method = ["baseTick"], at = [At("HEAD")])
    private fun resetFoliageState(callback: CallbackInfo) {
        foliageSlownessHandled = false

        partiallyInFoliage =
            Config.passability.headHitter &&
                    level().getBlockStatesIfLoaded(boundingBox)
                        .anyMatch(FoliagePassability::isPassable)
    }
}
