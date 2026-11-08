package io.github.andrewcwhy.allthingsfoliage.mixin.render

import io.github.andrewcwhy.allthingsfoliage.passability.FoliagePassability
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.world.entity.Entity
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(EntityRenderer::class)
abstract class EntityRendererMixin<T : Entity, S : EntityRenderState> {
    @Inject(method = ["finalizeRenderState"], at = [At("RETURN")])
    private fun filterNameTag(entity: T, state: S, callback: CallbackInfo) {
        if (
            !entity
                .level()
                .getBlockStatesIfLoaded(entity.boundingBox)
                .allMatch(FoliagePassability::isPassable)
        ) {
            state.nameTag = null
        }
    }
}
