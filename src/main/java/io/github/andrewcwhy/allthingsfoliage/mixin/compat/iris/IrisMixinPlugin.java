package io.github.andrewcwhy.allthingsfoliage.mixin.compat.iris;

//? fabric {
import net.fabricmc.loader.api.FabricLoader;
//?} else {
/*import net.neoforged.fml.loading.FMLLoader;
*/
//?}

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Applies the shader pack support only when Iris is installed: every other mixin here targets Iris or needs it. */
public final class IrisMixinPlugin implements IMixinConfigPlugin {

//? fabric {
	private static final boolean IRIS = FabricLoader.getInstance().isModLoaded("iris");
//?} else {
/*	// Mixin configs are read once the mod list is built, but before ModList exists.
	private static final boolean IRIS =
			FMLLoader.getCurrent().getLoadingModList().getModFileById("iris") != null;
*/
//?}

	@Override
	public void onLoad(String mixinPackage) {
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return IRIS;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	/**
	 * The mixins only some versions have, each compiled only where its target exists: the configuration lists what every
	 * version shares.
	 */
	@Override
	public List<String> getMixins() {
		// Mixin never asks shouldApplyMixin about the mixins a plugin adds here, so without Iris none are added at all.
		if (!IRIS) {
			return List.of();
		}
		List<String> mixins = new ArrayList<>();

		return mixins;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}
