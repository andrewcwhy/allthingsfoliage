package io.github.andrewcwhy.allthingsfoliage.platform.neoforge;
//? neoforge {
/*
import com.github.razorplay01.sway.config.SwayConfig;
import com.mojang.blaze3d.platform.InputConstants;
import io.github.andrewcwhy.allthingsfoliage.FoliageConfigScreen;
import io.github.andrewcwhy.allthingsfoliage.FoliageSettings;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources. Identifier ;

import java.util.function.Consumer;

public class NeoforgeKeyBindings {

	public static KeyMapping openConfig;
	public static KeyMapping toggleMod;

	public static  Identifier  resourceLocation =
			 Identifier 
			.fromNamespaceAndPath("allthingsfoliage", "general");

	public static void register(Consumer<KeyMapping> registrar) {

		KeyMapping.Category category = KeyMapping.Category.register(resourceLocation);

		openConfig = new KeyMapping(
				"key.allthingsfoliage.open_config",
				InputConstants.UNKNOWN.getValue(),

				category
				
		);

		toggleMod = new KeyMapping(
				"key.allthingsfoliage.toggle",
				InputConstants.UNKNOWN.getValue(),

				category
				
		);

		registrar.accept(openConfig);
		registrar.accept(toggleMod);
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
*/
//?}
