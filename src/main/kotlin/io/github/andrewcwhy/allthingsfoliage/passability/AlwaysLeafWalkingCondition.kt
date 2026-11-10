package io.github.andrewcwhy.allthingsfoliage.passability

import com.mojang.serialization.MapCodec
import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage
import io.github.andrewcwhy.allthingsfoliage.Config
//? fabric {
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType
import net.minecraft.resources.RegistryOps

class AlwaysLeafWalkingCondition : ResourceCondition {
    override fun getType(): ResourceConditionType<*> {
        return Companion.type
    }

    override fun test(registryInfo: RegistryOps.RegistryInfoLookup?): Boolean {
        return Config.passability.alwaysLeafWalking
    }

    companion object {
        val type: ResourceConditionType<AlwaysLeafWalkingCondition> =
            ResourceConditionType.create(
                AllThingsFoliage.id("always_leaf_walking"),
                MapCodec.unit(AlwaysLeafWalkingCondition()),
            )
    }
}
//?} else {
/*
import net.neoforged.neoforge.common.conditions.ICondition

class AlwaysLeafWalkingCondition : ICondition {
    override fun codec(): MapCodec<out ICondition> {
        return Companion.codec
    }

    override fun test(context: ICondition.IContext): Boolean {
        return Config.passability.alwaysLeafWalking
    }

    companion object {
        val codec: MapCodec<AlwaysLeafWalkingCondition> = MapCodec.unit(AlwaysLeafWalkingCondition())
    }
}
*/
//?}
