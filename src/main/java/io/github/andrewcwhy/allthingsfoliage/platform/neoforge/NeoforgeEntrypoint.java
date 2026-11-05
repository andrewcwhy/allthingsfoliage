package io.github.andrewcwhy.allthingsfoliage.platform.neoforge;

//? neoforge {

/*import io.github.andrewcwhy.allthingsfoliage.FoliageConfigScreen;
import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage;
import io.github.andrewcwhy.allthingsfoliage.gpu.GpuFoliageSplit;
//? <= 1.21.1 {
/^import io.github.andrewcwhy.allthingsfoliage.ModCompatRegistry;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
^///?}
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import java.util.function.Supplier;

@Mod(AllThingsFoliage.MOD_ID)
public class NeoforgeEntrypoint {

	public NeoforgeEntrypoint(IEventBus modEventBus, ModContainer modContainer) {
		AllThingsFoliage.onInitialize();
		modEventBus.addListener(this::onClientSetup);
		modContainer.registerExtensionPoint(
				IConfigScreenFactory.class,
				(Supplier<IConfigScreenFactory>) () -> (client, parent) -> new FoliageConfigScreen(parent)
		);
	}

	private void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			AllThingsFoliage.onInitializeClient();

			// The constructor runs during mod construction, before any mod has registered its
			// blocks, so third-party lookups have to wait. Client setup is the earliest point that
			// is both past the registry events and guaranteed client-only: the compat registry
			// touches Sway's client behaviour classes, which must not load on a dedicated server.
			AllThingsFoliage.onRegistriesReady();

			//? <= 1.21.1 {
			/^// Sway wraps every registered block's BakedModel in its own SwayModel, which does not
			// delegate NeoForge's getRenderTypes(). Wrapped models therefore fall back to the
			// ItemBlockRenderTypes lookup, losing any "render_type" declared in the model JSON and
			// rendering foliage in the solid layer (transparent texels turn black). Populating the
			// lookup for the blocks we register makes that fallback resolve correctly.
			//
			// 1.21.11+ replaced BakedModel with BlockStateModel, where the render layer travels
			// inside each BlockStateModelPart instead of a model-level method, so Sway's wrapper
			// cannot drop it there and no workaround is needed.
			// Only the plants Sway bends: a block it only pushes keeps its own layers -- Snow! Real Magic's snow draws
			// its snow in the solid layer and the plant inside in the plant's own, and on cutout alone it draws nothing.
			for (Block block : ModCompatRegistry.registeredBlocks()) {
				if (GpuFoliageSplit.bends(block)) {
					ItemBlockRenderTypes.setRenderLayer(block, RenderType.cutoutMipped());
				}
			}
			^///?}
		});
	}
}
*/ //?}
