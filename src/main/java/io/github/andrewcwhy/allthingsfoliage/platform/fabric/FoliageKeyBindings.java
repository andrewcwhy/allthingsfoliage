package io.github.andrewcwhy.allthingsfoliage.platform.fabric;
//? fabric {

import com.github.razorplay01.sway.config.SwayConfig;
import com.mojang.blaze3d.platform.InputConstants;
import io.github.andrewcwhy.allthingsfoliage.FoliageConfigScreen;
import io.github.andrewcwhy.allthingsfoliage.FoliageSettings;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources. Identifier ;

public class FoliageKeyBindings {

    public static KeyMapping openConfig;
    public static KeyMapping toggleMod;
	public static  Identifier  resourceLocation =   Identifier 
			.fromNamespaceAndPath("allthingsfoliage", "general");

    public static void register() {
        
        KeyMapping.Category category = KeyMapping.Category.register(
            resourceLocation
        );

        openConfig = KeyMappingHelper
             .registerKeyMapping (
                new KeyMapping(
                    "key.allthingsfoliage.open_config",
                    InputConstants.UNKNOWN.getValue(),

                    category
                    
                )
            );

        toggleMod = KeyMappingHelper
             .registerKeyMapping (
                new KeyMapping(
                    "key.allthingsfoliage.toggle",
                    InputConstants.UNKNOWN.getValue(),

                    category
                    
                )
            );
    }

    public static void tick(Minecraft mc) {
        FoliageSettings.applyDefaultRadiusOnce();
        while (openConfig.consumeClick()) {
            
            mc.setScreenAndShow(new FoliageConfigScreen(null));
            
        }

        while (toggleMod.consumeClick()) {
            SwayConfig.INSTANCE.enabled = !SwayConfig.INSTANCE.enabled;
            SwayConfig.save();
            if (mc.player != null) {

                mc.player.sendSystemMessage(
                    
                    Component.translatable(
                        SwayConfig.INSTANCE.enabled
                            ? "key.allthingsfoliage.toggle.on"
                            : "key.allthingsfoliage.toggle.off"
                    )
                    
                );
            }
        }
    }
}
//?}
