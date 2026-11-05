package io.github.andrewcwhy.allthingsfoliage.platform.fabric;

//? fabric {

import com.github.razorplay01.sway.config.SwayConfig;
import com.mojang.blaze3d.platform.InputConstants;
import io.github.andrewcwhy.allthingsfoliage.FoliageConfigScreen;
import io.github.andrewcwhy.allthingsfoliage.FoliageSettings;
//? <=1.21.11{
/*import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
 */ //?}
//? >1.21.11{
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources./*? >= 1.21.11 {*/ Identifier /*?} else {*/ /*ResourceLocation */ /*?} */;

//?}

public class FoliageKeyBindings {

    public static KeyMapping openConfig;
    public static KeyMapping toggleMod;
    public static /*? >= 1.21.11 {*/ Identifier /*?} else {*/ /*ResourceLocation */ /*?} */ resourceLocation =
        Identifier /*?} else {*/ /*ResourceLocation */ /*?} */
            /*? <=1.20.1 {*/ /*new*/ /*?} */ /*? >= 1.21.11 {*/ /*? >1.20.1 {*/ .fromNamespaceAndPath/*?} */ (
                "mc2_interactivefoliage",
                "general"
            );

    public static void register() {
        //? > 1.21.1{
        KeyMapping.Category category = KeyMapping.Category.register(
            resourceLocation
        );
        //?}

        openConfig = KeyMappingHelper
            /*? > 1.21.11 {*/ .registerKeyMapping/*?} else {*/ /*KeyBindingHelper.registerKeyBinding */ /*?} */ (
                new KeyMapping(
                    "key.mc2_interactivefoliage.open_config",
                    InputConstants.UNKNOWN.getValue(),
                    //? <= 1.21.1{
                    /*"key.category.mc2_interactivefoliage.general"
                     */ //?}
                    //? > 1.21.1{
                    category
                    //?}
                )
            );

        toggleMod = KeyMappingHelper
            /*? > 1.21.11 {*/ .registerKeyMapping/*?} else {*/ /*KeyBindingHelper.registerKeyBinding */ /*?} */ (
                new KeyMapping(
                    "key.mc2_interactivefoliage.toggle",
                    InputConstants.UNKNOWN.getValue(),
                    //? <= 1.21.1{
                    /*"key.category.mc2_interactivefoliage.general"
                     */ //?}
                    //? > 1.21.1{
                    category
                    //?}
                )
            );
    }

    public static void tick(Minecraft mc) {
        FoliageSettings.applyDefaultRadiusOnce();
        while (openConfig.consumeClick()) {
            //? >=26.2{
            mc.setScreenAndShow(new FoliageConfigScreen(null));
            //?}else{
            /*mc.setScreen(new FoliageConfigScreen(null));
             */ //?}
        }

        while (toggleMod.consumeClick()) {
            SwayConfig.INSTANCE.enabled = !SwayConfig.INSTANCE.enabled;
            SwayConfig.save();
            if (mc.player != null) {
                //? <=1.21.11{
                /*mc.player.displayClientMessage(
                 */ //?}
                //? >1.21.11{
                mc.player.sendSystemMessage(
                    //?}
                    Component.translatable(
                        SwayConfig.INSTANCE.enabled
                            ? "key.mc2_interactivefoliage.toggle.on"
                            : "key.mc2_interactivefoliage.toggle.off"
                    )
                    //? <=1.21.11{
                    /*,true
                     */ //?}
                );
            }
        }
    }
}
//?}
