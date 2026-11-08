
package io.github.andrewcwhy.allthingsfoliage.mixin.compat.polytone

//? fabric {
/*import net.fabricmc.loader.api.FabricLoader
*///?} else {
import net.neoforged.fml.loading.FMLLoader

//?}

import org.objectweb.asm.tree.ClassNode
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin
import org.spongepowered.asm.mixin.extensibility.IMixinInfo

/**
 * Applies Polytone compatibility mixins only when Polytone is installed.
 */
class PolytoneMixinPlugin : IMixinConfigPlugin {

    companion object {
        //? fabric {
        /*private val polytoneLoaded = FabricLoader.getInstance().isModLoaded("polytone")
        *///?} else {
        // Mixin configurations load before NeoForge's ModList is available.
        private val polytoneLoaded =
            FMLLoader.getCurrent().getLoadingModList().getModFileById("polytone") != null
        
        //?}
    }

    override fun acceptTargets(
        myTargets: MutableSet<String>,
        otherTargets: MutableSet<String>
    ) {
    }

    override fun getMixins(): MutableList<String>? {
        return null
    }

    override fun getRefMapperConfig(): String? {
        return null
    }

    override fun onLoad(mixinPackage: String) {
    }

    override fun postApply(
        targetClassName: String,
        targetClass: ClassNode,
        mixinClassName: String,
        mixinInfo: IMixinInfo
    ) {
    }

    override fun preApply(
        targetClassName: String,
        targetClass: ClassNode,
        mixinClassName: String,
        mixinInfo: IMixinInfo
    ) {
    }

    override fun shouldApplyMixin(
        targetClassName: String,
        mixinClassName: String
    ): Boolean {
        return polytoneLoaded
    }
}
