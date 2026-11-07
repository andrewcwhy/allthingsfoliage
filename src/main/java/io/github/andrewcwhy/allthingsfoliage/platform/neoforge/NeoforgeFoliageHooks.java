package io.github.andrewcwhy.allthingsfoliage.platform.neoforge;
//? neoforge {
/*
import com.github.razorplay01.sway.api.SwayAPI;
import com.github.razorplay01.sway.platform.neoforge.util.SwayModel;
import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage;
import io.github.andrewcwhy.allthingsfoliage.gpu.GpuFoliageRenderer;
import io.github.andrewcwhy.allthingsfoliage.gpu.GpuFoliageSplit;
import net.minecraft.client.multiplayer.ClientLevel;

import net.minecraft.client.renderer.block.dispatch.BlockStateModel;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;

import net.neoforged.neoforge.event.level.ChunkEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;

/^*
 * Where the GPU foliage renderer meets NeoForge: the model wrapper, the draw, and chunks coming and going.
 ^/
@EventBusSubscriber(modid = AllThingsFoliage.MOD_ID, value = Dist.CLIENT)
public final class NeoforgeFoliageHooks {

	private NeoforgeFoliageHooks() {
	}

	// From 26.3 the opaque world is drawn inside one pass vanilla opens itself, and AfterOpaqueBlocks fires inside it,
	// where no buffer may be written. The frame is prepared as the level's extraction ends instead -- as Fabric's
	// END_EXTRACTION does -- and drawn into vanilla's pass by LevelPassMixin.
	@SubscribeEvent
	public static void onExtractLevelRenderState(ExtractLevelRenderStateEvent event) {
		GpuFoliageRenderer.draw(event.getRenderState());
	}

	@SubscribeEvent
	public static void onChunkLoad(ChunkEvent.Load event) {
		// Loaded client chunks are always full ones, though before 1.21.11 the event only promises a ChunkAccess.
		if (event.getLevel() instanceof ClientLevel level && event.getChunk() instanceof LevelChunk chunk) {
			GpuFoliageRenderer.discoverChunk(level, chunk);
		}
	}

	@SubscribeEvent
	public static void onChunkUnload(ChunkEvent.Unload event) {
		if (event.getLevel() instanceof ClientLevel level && event.getChunk() instanceof LevelChunk chunk) {
			GpuFoliageRenderer.forgetChunk(level, chunk);
		}
	}

	/^*
	 * Wraps the model of every block Sway animates, first of all: Sway wraps afterwards, so ours ends up
	 * inside its wrapper. That is what lets the renderer mesh plain geometry while the chunk mesh still gets
	 * Sway's deformation on everything our wrapper hands over.
	 ^/
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
		Map<BlockState, BlockStateModel> models = event.getBakingResult().blockStateModels();
		for (Map.Entry<BlockState, BlockStateModel> entry : new ArrayList<>(models.entrySet())) {
			if (!GpuFoliageSplit.mayWave(entry.getKey().getBlock())) {
				continue;
			}
			NeoforgeFoliageModel wrapped = new NeoforgeFoliageModel(entry.getValue());
			GpuFoliageSplit.registerGpuModel(entry.getKey(), wrapped);
			models.put(entry.getKey(), wrapped);
		}
	}

	/^*
	 * Last of all, once Sway has wrapped them: Sway wraps the model of every block it knows, the ones it only pushes
	 * and never bends as well -- Snow! Real Magic's snow, whose plant inside bends on its own. Its wrapper answers
	 * through its parts, so the snow's own drawing -- the snow and the plant in it, through Fabric's rendering API --
	 * is skipped and only the snow is drawn. Those models are handed back as they were.
	 ^/
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void unwrapPushedOnly(ModelEvent.ModifyBakingResult event) {
		Map<BlockState, BlockStateModel> models = event.getBakingResult().blockStateModels();
		for (Map.Entry<BlockState, BlockStateModel> entry : models.entrySet()) {
			Block block = entry.getKey().getBlock();
			if (entry.getValue() instanceof SwayModel sway && SwayAPI.isInteractive(block) && !GpuFoliageSplit.bends(block)) {
				BlockStateModel inside = insideSway(sway);
				if (inside != null) {
					entry.setValue(inside);
				}
			}
		}
	}

	private static Field swayParent;
	private static boolean swayParentMissing;

	/^* The model Sway wrapped, or null if its wrapper is not one this knows. ^/
	private static BlockStateModel insideSway(SwayModel sway) {
		if (swayParentMissing) {
			return null;
		}
		try {
			if (swayParent == null) {
				swayParent = SwayModel.class.getDeclaredField("parent");
				swayParent.setAccessible(true);
			}
			return (BlockStateModel) swayParent.get(sway);
		} catch (ReflectiveOperationException | RuntimeException e) {
			swayParentMissing = true;
			AllThingsFoliage.logger.warn("Could not unwrap Sway's model of a block it only pushes; Snow! Real Magic's snow "
					+ "may not be drawn", e);
			return null;
		}
	}

}
*/
//?}
