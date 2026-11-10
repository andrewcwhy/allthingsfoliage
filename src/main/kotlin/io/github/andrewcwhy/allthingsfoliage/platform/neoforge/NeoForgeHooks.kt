package io.github.andrewcwhy.allthingsfoliage.platform.neoforge

//? neoforge {
/*
import com.github.razorplay01.sway.api.SwayAPI
import com.github.razorplay01.sway.platform.neoforge.util.SwayModel
import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage
import io.github.andrewcwhy.allthingsfoliage.gpu.GpuFoliageRenderer
import io.github.andrewcwhy.allthingsfoliage.gpu.GpuFoliageSplit
import java.lang.reflect.Field
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.block.dispatch.BlockStateModel
import net.minecraft.world.level.chunk.LevelChunk
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent
import net.neoforged.neoforge.client.event.ModelEvent
import net.neoforged.neoforge.event.level.ChunkEvent

/^* Connects GPU foliage rendering to NeoForge's model, chunk, and extraction events. ^/
@EventBusSubscriber(
    modid = AllThingsFoliage.MOD_ID,
    value = [Dist.CLIENT],
)
object NeoForgeFoliageHooks {

    private var swayParentField: Field? = null
    private var swayParentUnavailable = false

    /^* Returns the wrapped model, disabling further attempts if Sway's implementation differs. ^/
    private fun getWrappedSwayModel(model: SwayModel): BlockStateModel? {
        if (swayParentUnavailable) {
            return null
        }

        try {
            val parentField = swayParentField
                ?: SwayModel::class.java.getDeclaredField("parent").apply {
                    isAccessible = true
                    swayParentField = this
                }

            return parentField.get(model) as BlockStateModel?
        } catch (exception: Exception) {
            if (
                exception !is ReflectiveOperationException &&
                exception !is RuntimeException
            ) {
                throw exception
            }

            swayParentUnavailable = true

            AllThingsFoliage.logger.warn(
                "Could not unwrap Sway's model of a block it only pushes; " +
                    "Snow! Real Magic's snow may not be drawn",
                exception,
            )

            return null
        }
    }

    @JvmStatic
    @SubscribeEvent
    fun onChunkLoad(event: ChunkEvent.Load) {
        val level = event.level as? ClientLevel ?: return
        val chunk = event.chunk as? LevelChunk ?: return

        GpuFoliageRenderer.discoverChunk(level, chunk)
    }

    @JvmStatic
    @SubscribeEvent
    fun onChunkUnload(event: ChunkEvent.Unload) {
        val level = event.level as? ClientLevel ?: return
        val chunk = event.chunk as? LevelChunk ?: return

        GpuFoliageRenderer.forgetChunk(level, chunk)
    }

    /^* Prepares foliage before LevelPassMixin draws it inside vanilla's opaque pass. ^/
    @JvmStatic
    @SubscribeEvent
    fun onExtractLevelRenderState(event: ExtractLevelRenderStateEvent) {
        GpuFoliageRenderer.draw(event.renderState)
    }

    /^*
     * Wraps models before Sway so GPU meshing can access geometry without Sway's deformation.
     ^/
    @JvmStatic
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onModifyBakingResult(event: ModelEvent.ModifyBakingResult) {
        val models = event.bakingResult.blockStateModels()

        for ((state, model) in models.toList()) {
            if (!GpuFoliageSplit.mayWave(state.block)) {
                continue
            }

            val wrapped = NeoForgeFoliageModel(model)
            GpuFoliageSplit.registerGpuModel(state, wrapped)
            models[state] = wrapped
        }
    }

    /^*
     * Restores models Sway only pushes so their custom snow and plant rendering remains intact.
     ^/
    @JvmStatic
    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun unwrapPushedOnly(event: ModelEvent.ModifyBakingResult) {
        val models = event.bakingResult.blockStateModels()

        for (entry in models.entries) {
            val block = entry.key.block
            val model = entry.value as? SwayModel ?: continue

            if (!SwayAPI.isInteractive(block) || GpuFoliageSplit.bends(block)) {
                continue
            }

            val wrapped = getWrappedSwayModel(model) ?: continue
            entry.setValue(wrapped)
        }
    }
}
*/
//?}
