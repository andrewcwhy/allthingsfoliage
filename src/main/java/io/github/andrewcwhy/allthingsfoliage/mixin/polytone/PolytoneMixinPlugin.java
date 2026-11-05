package net.karto.mc2.mc2_interactivefoliage.mixin.polytone;

//? >=1.21.1 {

//? fabric {
import net.fabricmc.loader.api.FabricLoader;
//?} else {
/*import net.neoforged.fml.loading.FMLLoader;
*///?}
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/** Applies the Polytone support only when Polytone is installed: every mixin here targets it. */
public final class PolytoneMixinPlugin implements IMixinConfigPlugin {

	//? fabric {
	private static final boolean POLYTONE = FabricLoader.getInstance().isModLoaded("polytone");
	//?} else {
	/*// Mixin configs are read once the mod list is built, but before ModList exists.
	private static final boolean POLYTONE =
			FMLLoader/^? if >1.21.7 {^/.getCurrent()/^?}^/.getLoadingModList().getModFileById("polytone") != null;
	*///?}

	@Override
	public void onLoad(String mixinPackage) {
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return POLYTONE;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}
//?}
