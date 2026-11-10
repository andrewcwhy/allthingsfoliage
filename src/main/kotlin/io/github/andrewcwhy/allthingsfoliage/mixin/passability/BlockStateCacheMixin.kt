package io.github.andrewcwhy.allthingsfoliage.mixin.passability

import io.github.andrewcwhy.allthingsfoliage.passability.FoliagePassability
import net.minecraft.world.level.block.state.BlockState
import org.spongepowered.asm.mixin.Final
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Mutable
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(targets = ["net.minecraft.world.level.block.state.BlockBehaviour\$BlockStateBase\$Cache"])
abstract class BlockStateCacheMixin {
    @field:Final
    @field:Shadow
    private lateinit var faceSturdy: BooleanArray

    @field:Final
    @field:Mutable
    @field:Shadow
    private var isCollisionShapeFullBlock: Boolean = false

    @field:Final
    @field:Mutable
    @field:Shadow
    private var largeCollisionShape: Boolean = false

    @Inject(method = ["<init>"], at = [At("RETURN")])
    private fun updatePassableCache(state: BlockState, callback: CallbackInfo) {
        if (FoliagePassability.isPassable(state)) {
            faceSturdy.fill(false)
            isCollisionShapeFullBlock = false
            largeCollisionShape = false
        }
    }
}
