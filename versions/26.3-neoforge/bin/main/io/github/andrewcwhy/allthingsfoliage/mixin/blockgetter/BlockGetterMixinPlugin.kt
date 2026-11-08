package io.github.andrewcwhy.allthingsfoliage.mixin.blockgetter

//? neoforge {

import net.neoforged.fml.loading.FMLLoader
import org.objectweb.asm.tree.ClassNode
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin
import org.spongepowered.asm.mixin.extensibility.IMixinInfo

/**
 * Adds Forgified Fabric API's block-getter interface to level and chunk-rendering views.
 *
 * Works around early loading of BlockGetter by Iris, which can prevent the API's interface
 * mixin from being applied.
 */
class BlockGetterMixinPlugin : IMixinConfigPlugin {
    override fun acceptTargets(
        myTargets: Set<String>,
        otherTargets: Set<String>,
    ) {}

    override fun getMixins(): List<String>? {
        return null
    }

    override fun getRefMapperConfig(): String? {
        return null
    }

    override fun onLoad(mixinPackage: String) {}

    override fun postApply(
        targetClassName: String,
        targetClass: ClassNode,
        mixinClassName: String,
        mixinInfo: IMixinInfo,
    ) {}

    override fun preApply(
        targetClassName: String,
        targetClass: ClassNode,
        mixinClassName: String,
        mixinInfo: IMixinInfo,
    ) {
        if (FABRIC_BLOCK_GETTER_INTERFACE !in targetClass.interfaces) {
            targetClass.interfaces.add(FABRIC_BLOCK_GETTER_INTERFACE)
        }
    }

    override fun shouldApplyMixin(
        targetClassName: String,
        mixinClassName: String,
    ): Boolean {
        return isFabricBlockGetterAvailable
    }

    private companion object {
        private const val FABRIC_BLOCK_GETTER_INTERFACE =
            "net/fabricmc/fabric/api/blockgetter/v2/FabricBlockGetter"

        // Mixin configuration loads before ModList is available.
        private val isFabricBlockGetterAvailable =
            FMLLoader.getCurrent()
                .loadingModList
                .getModFileById("fabric_block_getter_api_v2") != null
    }
}

//?}
