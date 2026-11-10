//? neoforge {
/*
import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage
import io.github.andrewcwhy.allthingsfoliage.passability.AlwaysLeafWalkingCondition
import io.github.andrewcwhy.allthingsfoliage.passability.FoliageEnchantments
import io.github.andrewcwhy.allthingsfoliage.passability.FoliagePassability
import net.minecraft.core.registries.Registries
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.TagsUpdatedEvent
import net.neoforged.neoforge.registries.NeoForgeRegistries
import net.neoforged.neoforge.registries.RegisterEvent

object NeoForgePassability {
    @JvmStatic
    fun register(bus: IEventBus) {
        bus.addListener(::registerContent)
        NeoForge.EVENT_BUS.addListener { event: TagsUpdatedEvent ->
            if (event.shouldUpdateStaticData()) {
                FoliagePassability.tagsLoaded()
            }
        }
    }

    private fun registerContent(event: RegisterEvent) {
        event.register(Registries.ENCHANTMENT_EFFECT_COMPONENT_TYPE) { helper ->
            helper.register(
                AllThingsFoliage.id("leaf_walker"),
                FoliageEnchantments.leafWalkerEffect,
            )
        }
        event.register(NeoForgeRegistries.Keys.CONDITION_CODECS) { helper ->
            helper.register(
                AllThingsFoliage.id("always_leaf_walking"),
                AlwaysLeafWalkingCondition.codec,
            )
        }
    }
}
*/
//?}
