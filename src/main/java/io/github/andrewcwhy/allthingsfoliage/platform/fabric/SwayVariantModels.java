package io.github.andrewcwhy.allthingsfoliage.platform.fabric;

//? fabric && <1.21.11 {

/*import com.github.razorplay01.sway.api.SwayAPI;
import com.github.razorplay01.sway.platform.fabric.util.SwayModel;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage;
import io.github.andrewcwhy.allthingsfoliage.gpu.GpuFoliageSplit;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/^*
 * Lets Sway bend the models of its plants it cannot find by their name, before 1.21.11.
 * <p>
 * Sway wraps a model file on these versions, not a block: it takes {@code block/<name>} to be the model of the block
 * {@code <name>}. A model a plant's block state uses under any other name is never wrapped, so it never bends -- the
 * extra variants a resource pack gives a plant, and the snowy variants Snow! Real Magic draws plants inside its snow
 * with. From 1.21.11 Sway wraps each block state's whole model, and all of these bend there already.
 * <p>
 * Two sets of models are wrapped here, with the very wrapper Sway uses, so they bend exactly as the plant's own:
 * <ul>
 *   <li>every model a Sway plant's block states name that Sway's naming rule does not reach;</li>
 *   <li>the snowy variants Snow! Real Magic puts in place of such a plant's models, which no block state names.</li>
 * </ul>
 * A model Sway wraps itself is left alone, or the plant would bend twice. Sway bends a model by the block drawn and
 * the push at its position, so a wrapped model drawn for anything Sway does not push stays still.
 ^/
public final class SwayVariantModels {

	/^*
	 * Where Snow! Real Magic keeps its map from a model to its snowy variant, read by name: the mod is not compiled
	 * against. SnowClient up to 10.x (1.20.1), ClientHooks from 12.x (1.21.1).
	 ^/
	private static final String[] SNOW_CLIENTS = {"snownee.snow.client.SnowClient", "snownee.snow.client.ClientHooks"};
	private static final String SNOW_VARIANTS = "snowVariantMapping";
	private static final String VARIANT_MODEL = "model";

	/^* The models Sway's plants' block states name, gathered as each block state's model loads. ^/
	private static final Set<ResourceLocation> PLANT_MODELS = new HashSet<>();
	/^* The snowy variants of those models, worked out once every model has loaded; null until then. ^/
	private static Set<ResourceLocation> snowyVariants;
	private static boolean warned;

	private SwayVariantModels() {
	}

	public static void register() {
		ModelLoadingPlugin.register(context -> {
			// Once per resource reload: the models, and the variants resource packs give them, may have changed.
			PLANT_MODELS.clear();
			snowyVariants = null;
			context.modifyModelOnLoad().register((model, load) -> {
				if (model != null && plantOf(topLevel(load)) != null) {
					PLANT_MODELS.addAll(model.getDependencies());
				}
				return model;
			});
			context.modifyModelAfterBake().register(ModelModifier.WRAP_LAST_PHASE, (model, bake) -> {
				ResourceLocation id = resourceId(bake);
				if (model == null || id == null || model instanceof SwayModel || swayWrapsByName(id)) {
					return model;
				}
				return PLANT_MODELS.contains(id) || snowyVariants().contains(id) ? new SwayModel(model) : model;
			});
		});
	}

	//? >=1.21.1 {
	private static ModelResourceLocation topLevel(ModelModifier.OnLoad.Context load) {
		return load.topLevelId();
	}

	private static ResourceLocation resourceId(ModelModifier.AfterBake.Context bake) {
		return bake.resourceId();
	}

	/^* The Sway plant a block state's model belongs to, or null for anything else, items included. ^/
	private static Block plantOf(ModelResourceLocation id) {
		if (id == null || ModelResourceLocation.INVENTORY_VARIANT.equals(id.variant())) {
			return null;
		}
		return bending(BuiltInRegistries.BLOCK.getOptional(id.id()));
	}
	//?} else {
	/^// Before 1.21.1 one id stands for every model, and a block state's is a model location.
	private static ResourceLocation topLevel(ModelModifier.OnLoad.Context load) {
		return load.id();
	}

	private static ResourceLocation resourceId(ModelModifier.AfterBake.Context bake) {
		return bake.id() instanceof ModelResourceLocation ? null : bake.id();
	}

	private static Block plantOf(ResourceLocation id) {
		if (!(id instanceof ModelResourceLocation location) || "inventory".equals(location.getVariant())) {
			return null;
		}
		return bending(BuiltInRegistries.BLOCK.getOptional(new ResourceLocation(location.getNamespace(), location.getPath())));
	}
	^///?}

	/^* The block, if Sway bends it: a block Sway only pushes, like Snow! Real Magic's snow, is not a plant here. ^/
	private static Block bending(Optional<Block> block) {
		return block.filter(GpuFoliageSplit::bends).orElse(null);
	}

	/^* Sway's own rule on these versions: whether it wraps this model file itself, taking its name for a block's. ^/
	private static boolean swayWrapsByName(ResourceLocation id) {
		String path = id.getPath();
		if (!path.startsWith("block/")) {
			return false;
		}
		String name = path.substring(6).replaceAll("_(top|bottom|upper|lower)$", "").split("#")[0];
		ResourceLocation block = ResourceLocation.tryBuild(id.getNamespace(), name);
		return block != null && BuiltInRegistries.BLOCK.getOptional(block).filter(SwayAPI::isInteractive).isPresent();
	}

	/^*
	 * The snowy variants Snow! Real Magic draws in place of a plant model, read the first time they are asked for,
	 * once it has read its own for this reload. Empty without it, or if it is not a version this knows.
	 ^/
	private static Set<ResourceLocation> snowyVariants() {
		if (snowyVariants != null) {
			return snowyVariants;
		}
		Set<ResourceLocation> found = new HashSet<>();
		if (AllThingsFoliage.xplat().isModLoaded("snowrealmagic")) {
			try {
				Field mapping = snowVariantMapping();
				for (Map.Entry<?, ?> entry : ((Map<?, ?>) mapping.get(null)).entrySet()) {
					if (!(entry.getKey() instanceof ResourceLocation plant)
							|| !(PLANT_MODELS.contains(plant) || swayWrapsByName(plant))) {
						continue;
					}
					Object variant = entry.getValue().getClass().getField(VARIANT_MODEL).get(entry.getValue());
					if (variant instanceof ResourceLocation id) {
						found.add(id);
					}
				}
			} catch (ReflectiveOperationException | LinkageError | ClassCastException e) {
				if (!warned) {
					warned = true;
					AllThingsFoliage.LOGGER.warn("Snow! Real Magic is installed, but not a version Sway's plants can be bent "
							+ "inside: plants in its snow stay still", e);
				}
			}
		}
		snowyVariants = found;
		return found;
	}

	/^* The field Snow! Real Magic keeps its snowy variants in, in whichever class this version has it. ^/
	private static Field snowVariantMapping() throws ReflectiveOperationException {
		ReflectiveOperationException missing = null;
		for (String name : SNOW_CLIENTS) {
			try {
				return Class.forName(name).getField(SNOW_VARIANTS);
			} catch (ReflectiveOperationException e) {
				missing = e;
			}
		}
		throw missing;
	}
}
*/ //?}
