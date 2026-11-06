package io.github.andrewcwhy.allthingsfoliage.platform.fabric;
//? fabric {

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;

import io.github.andrewcwhy.allthingsfoliage.ModCompatRegistry;
import io.github.andrewcwhy.allthingsfoliage.gpu.GpuFoliageModel;
import io.github.andrewcwhy.allthingsfoliage.gpu.GpuFoliageRenderer;
import io.github.andrewcwhy.allthingsfoliage.gpu.GpuFoliageSplit;

/** Where the GPU foliage renderer meets Fabric: the model wrapper, the draw, and chunks coming and going. */
public final class FabricFoliageHooks {

    private FabricFoliageHooks() {}

    public static void register() {
        // Mods start in no promised order here, so a block registered after this mod started is looked for again as the
        // models load, once every mod has; see ModCompatRegistry.registerLate.
        ModelLoadingPlugin.register(context ->
            ModCompatRegistry.registerLate()
        );
        // Wrapped outside Sway's own wrapper, so everything our wrapper does not withhold still goes
        // through Sway. Here that costs nothing extra: Fabric's meshing asks models through the rendering
        // API, which Sway hooks, while the renderer meshes through vanilla's path, which it does not.
        
        ModelLoadingPlugin.register(context ->
            context
                .modifyBlockModelAfterBake()
                .register(ModelModifier.WRAP_LAST_PHASE, (model, bake) ->
                    GpuFoliageSplit.mayWave(bake.state().getBlock())
                        ? new GpuFoliageModel(model)
                        : model
                )
        );

        // On 26.3 the opaque world is drawn inside one render pass vanilla opens, in which nothing may be written, so the
        // foliage is prepared as the level's extraction ends, before anything is drawn, and LevelPassMixin draws it into
        // that pass.
        
        LevelExtractionEvents.END_EXTRACTION.register(context ->
            GpuFoliageRenderer.draw(context.levelState())
        );

        // Sodium overwrites every vanilla route that marks a section dirty when a chunk arrives, so chunk
        // loads are followed through Fabric's own event, which fires whichever renderer is used.
        ClientChunkEvents.CHUNK_LOAD.register(
            GpuFoliageRenderer::discoverChunk
        );
        ClientChunkEvents.CHUNK_UNLOAD.register(
            GpuFoliageRenderer::forgetChunk
        );
    }

}
//?}
