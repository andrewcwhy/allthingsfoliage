package net.karto.mc2.mc2_interactivefoliage.mixin.blockgetter;

//? neoforge && >=26.1.2 {

/*import net.neoforged.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/^*
 * Gives the views of the level a chunk is built from the block getter interface of Forgified Fabric API, which Iris
 * keeps from them on NeoForge.
 * <p>
 * The API adds its FabricBlockGetter to Minecraft's BlockGetter, and every view of the level has it through that. Iris
 * 1.11.4 loads BlockGetter while Mixin is still preparing its configurations -- its Distant Horizons plugin asks
 * IrisPlatformHelpers whether that mod is installed, and loading IrisForgeHelpers loads BlockGetter -- so the interface
 * is never added, with or without a shader pack. Snow! Real Magic reads its snow's plant through that interface as it
 * draws a chunk, and the game crashed as soon as it drew one.
 * <p>
 * Here the interface is named on the views themselves, which load long after: Sodium's level slice, vanilla's section
 * region, and the level. Its methods all have defaults, which read what the API gives block entities. Where
 * BlockGetter has it already, naming it again changes nothing; without the API, nothing is applied at all.
 ^/
public final class BlockGetterMixinPlugin implements IMixinConfigPlugin {

	private static final String FABRIC_BLOCK_GETTER = "net/fabricmc/fabric/api/blockgetter/v2/FabricBlockGetter";

	// Mixin configs are read once the mod list is built, but before ModList exists.
	private static final boolean FABRIC_API =
			FMLLoader.getCurrent().getLoadingModList().getModFileById("fabric_block_getter_api_v2") != null;

	@Override
	public void onLoad(String mixinPackage) {
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return FABRIC_API;
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
		if (!targetClass.interfaces.contains(FABRIC_BLOCK_GETTER)) {
			targetClass.interfaces.add(FABRIC_BLOCK_GETTER);
		}
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}
*///?}
