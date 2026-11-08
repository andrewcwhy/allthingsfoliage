package io.github.andrewcwhy.allthingsfoliage.mixin.compat.iris

//? fabric {
/*import net.fabricmc.loader.api.FabricLoader
*///?} else {
import net.neoforged.fml.loading.FMLLoader
//?}

import org.objectweb.asm.tree.ClassNode
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin
import org.spongepowered.asm.mixin.extensibility.IMixinInfo

/**
 * Enables Iris compatibility mixins only when Iris is installed.
 *
 * Loader-specific detection runs during Mixin configuration initialization.
 */
class IrisMixinPlugin : IMixinConfigPlugin {

    override fun onLoad(mixinPackage: String) {
    }

    override fun getRefMapperConfig(): String? {
        return null
    }

    override fun shouldApplyMixin(
        targetClassName: String,
        mixinClassName: String
    ): Boolean {
        return irisLoaded
    }

    override fun acceptTargets(
        myTargets: MutableSet<String>,
        otherTargets: MutableSet<String>
    ) {
    }

    /**
     * Returns additional Iris mixins, if any.
     *
     * Shared mixins remain declared in the Mixin configuration.
     */
    override fun getMixins(): MutableList<String> {
        if (!irisLoaded) {
            return mutableListOf()
        }

        return mutableListOf()
    }

    override fun preApply(
        targetClassName: String,
        targetClass: ClassNode,
        mixinClassName: String,
        mixinInfo: IMixinInfo
    ) {
    }

    override fun postApply(
        targetClassName: String,
        targetClass: ClassNode,
        mixinClassName: String,
        mixinInfo: IMixinInfo
    ) {
    }

    private companion object {

        //? fabric {
        /*private val irisLoaded: Boolean = FabricLoader.getInstance().isModLoaded("iris")
        *///?} else {
        private val irisLoaded: Boolean =
            FMLLoader.getCurrent().loadingModList.getModFileById("iris") != null
        //?}
    }
}
