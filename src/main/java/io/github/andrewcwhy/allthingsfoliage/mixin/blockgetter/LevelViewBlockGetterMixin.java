package net.karto.mc2.mc2_interactivefoliage.mixin.blockgetter;

//? neoforge && >=26.1.2 {

/*import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/^*
 * Only names the views of the level that BlockGetterMixinPlugin gives Forgified Fabric API's block getter interface;
 * the plugin adds it as each one is mixed. Sodium's level slice is named, not compiled against.
 ^/
@Pseudo
@Mixin(targets = {
		"net.caffeinemc.mods.sodium.client.world.LevelSlice",
		"net.minecraft.client.renderer.chunk.RenderSectionRegion",
		"net.minecraft.world.level.Level"
}, remap = false)
public abstract class LevelViewBlockGetterMixin {
}
*///?}
