package io.github.andrewcwhy.allthingsfoliage.platform.fabric;
//? fabric {

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.kikugie.fletching_table.annotation.fabric.Entrypoint;
import io.github.andrewcwhy.allthingsfoliage.FoliageConfigScreen;

/**
 * Hooks the config screen into Mod Menu's per-mod settings button.
 * <p>
 * NeoForge and Forge get the same screen through their own extension points, so without this the
 * button only worked outside Fabric and the key binding was the sole way in.
 * <p>
 * Mod Menu is an optional dependency: the entrypoint is only queried when it is installed, so this
 * class never loads otherwise.
 */
@Entrypoint("modmenu")
public class FabricModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return FoliageConfigScreen::new;
    }
}
//?}
