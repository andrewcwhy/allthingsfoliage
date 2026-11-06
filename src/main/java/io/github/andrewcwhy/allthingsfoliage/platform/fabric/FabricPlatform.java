package io.github.andrewcwhy.allthingsfoliage.platform.fabric;
//? fabric {

import io.github.andrewcwhy.allthingsfoliage.platform.Platform;
import net.fabricmc.loader.api.FabricLoader;

public class FabricPlatform implements Platform {

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public java.nio.file.Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public ModLoader loader() {
        return ModLoader.FABRIC;
    }
}
//?}
