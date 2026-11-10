package io.github.andrewcwhy.allthingsfoliage.mixin.passability

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import io.github.andrewcwhy.allthingsfoliage.Config
import io.github.andrewcwhy.allthingsfoliage.passability.FoliagePassability
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.InsideBlockEffectApplier
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.EntityCollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(BlockStateBase::class)
abstract class BlockStateMixin {
    @Shadow
    protected abstract fun asState(): BlockState

    @Inject(
        method = [
            "getCollisionShape(" +
                    "Lnet/minecraft/world/level/BlockGetter;" +
                    "Lnet/minecraft/core/BlockPos;" +
                    ")Lnet/minecraft/world/phys/shapes/VoxelShape;",
        ],
        at = [At("HEAD")],
        cancellable = true,
    )
    private fun overrideCollision(
        level: BlockGetter,
        pos: BlockPos,
        callback: CallbackInfoReturnable<VoxelShape>,
    ) {
        if (FoliagePassability.isPassable(asState())) {
            callback.returnValue = Shapes.empty()
        }
    }

    @Inject(
        method = [
            "getCollisionShape(" +
                    "Lnet/minecraft/world/level/BlockGetter;" +
                    "Lnet/minecraft/core/BlockPos;" +
                    "Lnet/minecraft/world/phys/shapes/CollisionContext;" +
                    ")Lnet/minecraft/world/phys/shapes/VoxelShape;",
        ],
        at = [At("HEAD")],
        cancellable = true,
    )
    private fun overrideEntityCollision(
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext,
        callback: CallbackInfoReturnable<VoxelShape>,
    ) {
        if (!FoliagePassability.isPassable(asState())) {
            return
        }

        val entity = (context as? EntityCollisionContext)?.entity
        val settings = Config.passability

        if (settings.playerOnly && entity !is Player) {
            return
        }

        if (entity is LivingEntity) {
            if (
                settings.headHitter &&
                entity.eyeY < pos.y &&
                !entity.isFallFlying &&
                !(entity is Player && entity.abilities.flying) &&
                !FoliagePassability.isPartiallyInFoliage(entity)
            ) {
                return
            }

            if (FoliagePassability.hasLeafWalker(entity)) {
                if (context.isDescending || entity.blockPosition().y <= pos.y) {
                    callback.returnValue = Shapes.empty()
                }

                return
            }
        }

        callback.returnValue = Shapes.empty()
    }

    @Inject(method = ["isCollisionShapeFullBlock"], at = [At("HEAD")], cancellable = true)
    private fun overrideFullCollision(
        level: BlockGetter,
        pos: BlockPos,
        callback: CallbackInfoReturnable<Boolean>,
    ) {
        if (FoliagePassability.isPassable(asState())) {
            callback.returnValue = false
        }
    }

    @Inject(method = ["canOcclude"], at = [At("HEAD")], cancellable = true)
    private fun overrideOcclusion(callback: CallbackInfoReturnable<Boolean>) {
        if (FoliagePassability.isPassable(asState())) {
            callback.returnValue = false
        }
    }

    @Inject(method = ["getShadeBrightness"], at = [At("HEAD")], cancellable = true)
    private fun overrideShade(
        level: BlockGetter,
        pos: BlockPos,
        callback: CallbackInfoReturnable<Float>,
    ) {
        if (FoliagePassability.isPassable(asState())) {
            val full = FoliagePassability.withOriginalCollision {
                Block.isShapeFullBlock(asState().getCollisionShape(level, pos))
            }

            callback.returnValue = if (full) 0.2f else 1.0f
        }
    }

    @Inject(method = ["isSuffocating"], at = [At("HEAD")], cancellable = true)
    private fun overrideSuffocation(
        level: BlockGetter,
        pos: BlockPos,
        callback: CallbackInfoReturnable<Boolean>,
    ) {
        if (FoliagePassability.isPassable(asState())) {
            callback.returnValue = false
        }
    }

    @Inject(method = ["isViewBlocking"], at = [At("HEAD")], cancellable = true)
    private fun overrideViewBlocking(
        level: BlockGetter,
        pos: BlockPos,
        bounds: AABB,
        callback: CallbackInfoReturnable<Boolean>,
    ) {
        if (
            Config.passability.alwaysNotViewBlocking &&
            FoliagePassability.isPassable(asState())
        ) {
            callback.returnValue = false
        }
    }

    @Inject(method = ["getVisualShape"], at = [At("HEAD")], cancellable = true)
    private fun overrideVisualShape(
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext,
        callback: CallbackInfoReturnable<VoxelShape>,
    ) {
        if (FoliagePassability.isPassable(asState())) {
            callback.returnValue = Shapes.empty()
        }
    }

    @WrapMethod(method = ["getBlockSupportShape"])
    private fun preserveSupport(
        level: BlockGetter,
        pos: BlockPos,
        original: Operation<VoxelShape>,
    ): VoxelShape {
        if (!FoliagePassability.isPassable(asState())) {
            return original.call(level, pos)
        }

        return FoliagePassability.withOriginalCollision { original.call(level, pos) }
    }

    @Inject(method = ["entityInside"], at = [At("HEAD")])
    private fun processFoliageCollision(
        level: Level,
        pos: BlockPos,
        entity: Entity,
        effects: InsideBlockEffectApplier,
        precise: Boolean,
        callback: CallbackInfo,
    ) {
        if (FoliagePassability.isPassable(asState())) {
            FoliagePassability.onEntityInside(level, pos, asState(), entity)
        }
    }
}
