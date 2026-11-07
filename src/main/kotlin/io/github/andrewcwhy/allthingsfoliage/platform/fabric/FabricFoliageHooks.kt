package io.github.andrewcwhy.allthingsfoliage.platform.fabric

//? fabric {
import io.github.andrewcwhy.allthingsfoliage.ModCompatRegistry
import io.github.andrewcwhy.allthingsfoliage.gpu.GpuFoliageModel
import io.github.andrewcwhy.allthingsfoliage.gpu.GpuFoliageRenderer
import io.github.andrewcwhy.allthingsfoliage.gpu.GpuFoliageSplit
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents

/**
 * Integrates GPU foliage rendering with Fabric's model, chunk, and level events.
 */
object FabricFoliageHooks {

    /** Registers model, rendering, and chunk lifecycle callbacks. */
    @JvmStatic
    fun register() {
        // Resolve late-registered blocks before models are baked.
        ModelLoadingPlugin.register { _ ->
            ModCompatRegistry.registerLate()
        }

        // Wrap models after other modifiers, preserving Sway's rendering pipeline.
        ModelLoadingPlugin.register { context ->
            context.modifyBlockModelAfterBake().register(
                ModelModifier.WRAP_LAST_PHASE
            ) { model, bake ->
                if (GpuFoliageSplit.mayWave(bake.state().block)) {
                    GpuFoliageModel(model)
                } else {
                    model
                }
            }
        }

        // Prepare foliage before Minecraft's opaque rendering pass.
        LevelExtractionEvents.END_EXTRACTION.register { context ->
            GpuFoliageRenderer.draw(context.levelState())
        }

        // Track chunk lifecycle events independently of Sodium's section updates.
        ClientChunkEvents.CHUNK_LOAD.register { level, chunk ->
            GpuFoliageRenderer.discoverChunk(level, chunk)
        }

        ClientChunkEvents.CHUNK_UNLOAD.register { level, chunk ->
            GpuFoliageRenderer.forgetChunk(level, chunk)
        }
    }
}
//?}
