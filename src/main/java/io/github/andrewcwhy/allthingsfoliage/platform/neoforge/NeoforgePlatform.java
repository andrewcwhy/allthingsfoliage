package io.github.andrewcwhy.allthingsfoliage.platform.neoforge;
//? neoforge {
/*
import io.github.andrewcwhy.allthingsfoliage.platform.Platform;
import net.neoforged.fml.ModList;

public class NeoforgePlatform implements Platform {

	@Override
	public boolean isModLoaded(String modId) {
		return ModList.get().isLoaded(modId);
	}

	@Override
	public java.nio.file.Path configDir() {
		return net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get();
	}

	@Override
	public ModLoader loader() {
		return ModLoader.NEOFORGE;
	}

	/^*
	 * Every mod file's own copy, Minecraft's among them: the mods are modules here, and a file in a module is not found
	 * through the class loader unless the module opens its folder.
	 ^/
	@Override
	public java.util.List<byte[]> readAll(String path) {
		java.util.List<byte[]> found = new java.util.ArrayList<>();
		for (net.neoforged.neoforgespi.language.IModFileInfo file : ModList.get().getModFiles()) {
			try {
				
				if (file.getFile().getContents().containsFile(path)) {
					found.add(file.getFile().getContents().readFile(path));
				}
				
			} catch (java.io.IOException e) {
				// A file that cannot be read adds nothing.
			}
		}
		return found;
	}
	
}
*/
//?}
