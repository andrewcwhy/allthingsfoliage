package io.github.andrewcwhy.allthingsfoliage.client.renderer

import com.google.gson.JsonParser
import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage
import io.github.andrewcwhy.allthingsfoliage.Config
import java.util.Collections
import java.util.IdentityHashMap
import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.Block

/**
 * Identifies blocks that the GPU renderer waves independently of Sway.
 *
 * Blocks are grouped by waving behavior rather than mod. Missing blocks are
 * skipped, and tags are resolved from resource files before model baking.
 */
object WavingWhitelist {

    private val blocks = IdentityHashMap<Block, Group>()

    private val followers: MutableSet<Block> =
        Collections.newSetFromMap(IdentityHashMap<Block, Boolean>())

    private val resting: MutableSet<Block> =
        Collections.newSetFromMap(IdentityHashMap<Block, Boolean>())

    /** Whether Minecraft last rendered leaves with transparency. */
    @Volatile
    private var cutoutLeaves = true

    /** Whether the GPU renderer can draw opaque leaves. */
    @Volatile
    private var opaqueLeavesDrawn = false

    /** Publishes the completed collections across model-baking threads. */
    @Volatile
    private var resolved = false

    /** Waving intensity and movement mode for whitelisted blocks. */
    enum class Group(
        val intensity: Float,
        val fieldBased: Boolean
    ) {
        /** Small plants that always wave at twice the standard intensity. */
        GENERAL(2.0f, false),

        /** Leaves that wave as a field at the shader's matching 0.20 intensity. */
        LEAVES(0.20f, true);

        /** Whether the group currently contributes GPU waving. */
        fun enabled(): Boolean {
            return this != LEAVES || WavingWhitelist.leavesWave()
        }
    }

    /** Whether this block is currently enabled for GPU waving. */
    @JvmStatic
    fun contains(block: Block): Boolean {
        ensureResolved()
        return blocks[block]?.enabled() == true
    }

    /**
     * Updates the cached leaf-rendering state.
     *
     * @return `true` if either rendering capability changed.
     */
    @JvmStatic
    fun followGameLeaves(canDrawOpaque: Boolean): Boolean {
        val currentCutoutLeaves = gameCutoutLeaves()

        if (currentCutoutLeaves == cutoutLeaves && canDrawOpaque == opaqueLeavesDrawn) {
            return false
        }

        cutoutLeaves = currentCutoutLeaves
        opaqueLeavesDrawn = canDrawOpaque

        return true
    }

    /** Whether the block follows nearby leaf movement. */
    @JvmStatic
    fun followsLeaves(block: Block): Boolean {
        ensureResolved()
        return block in followers
    }

    /** Whether Minecraft currently renders leaves with transparency. */
    fun gameCutoutLeaves(): Boolean {
        return Minecraft.getInstance().options.cutoutLeaves().get()
    }

    /** Returns the block's active waving intensity, or `1.0f` by default. */
    @JvmStatic
    fun intensityOf(block: Block): Float {
        if (restsOnLeaves(block)) {
            return Group.LEAVES.intensity
        }

        val group = blocks[block]

        if (group != null && group.enabled()) {
            return group.intensity
        }

        return 1.0f
    }

    /** Whether the block belongs to the leaves group, even when disabled. */
    @JvmStatic
    fun isLeaves(block: Block): Boolean {
        ensureResolved()
        return blocks[block] == Group.LEAVES
    }

    /** Whether Fancy leaves or supported opaque leaves can wave. */
    @JvmStatic
    fun leavesCanWave(): Boolean {
        return gameCutoutLeaves() || opaqueLeavesDrawn
    }

    /** Whether leaf waving is enabled and supported by the current rendering mode. */
    @JvmStatic
    fun leavesWave(): Boolean {
        return Config.wavingLeaves && (cutoutLeaves || opaqueLeavesDrawn)
    }

    /** Whether the block is whitelisted, regardless of its group's state. */
    @JvmStatic
    fun listed(block: Block): Boolean {
        ensureResolved()
        return blocks.containsKey(block)
    }

    /** Whether the GPU renderer currently draws leaves as opaque geometry. */
    @JvmStatic
    fun opaqueLeaves(): Boolean {
        return !cutoutLeaves && opaqueLeavesDrawn
    }

    /** Whether this block moves with leaves when resting on top of them. */
    @JvmStatic
    fun restsOnLeaves(block: Block): Boolean {
        ensureResolved()
        return block in resting
    }

    /** Whether this block currently uses world-space field waving. */
    @JvmStatic
    fun wavesAsField(block: Block): Boolean {
        if (restsOnLeaves(block)) {
            return true
        }

        val group = blocks[block]

        return group != null && group.enabled() && group.fieldBased
    }

    private fun addBlocks(group: Group, vararg blockIds: String) {
        forEachBlock({ block -> blocks.putIfAbsent(block, group) }, *blockIds)
    }

    /** Reads block tags from mod resources, which are available before world tags. */
    private fun addTaggedBlocks(action: (Block) -> Unit, vararg tagIds: String) {
        for (tagId in tagIds) {
            val blockIds = mutableListOf<String>()

            readTag(tagId, blockIds, mutableSetOf())
            forEachBlock(action, *blockIds.toTypedArray())
        }
    }

    private fun ensureResolved() {
        if (!resolved) {
            resolve()
        }
    }

    private fun forEachBlock(action: (Block) -> Unit, vararg blockIds: String) {
        for (blockId in blockIds) {
            val identifier = Identifier.tryParse(blockId) ?: continue
            val block = BuiltInRegistries.BLOCK.getOptional(identifier).orElse(null)
                ?: continue

            action(block)
        }
    }

    /** Collects block identifiers from a tag and its recursively referenced tags. */
    private fun readTag(
        tagId: String,
        blockIds: MutableList<String>,
        visitedTags: MutableSet<String>
    ) {
        if (!visitedTags.add(tagId)) {
            return
        }

        val separator = tagId.indexOf(':')
        val namespace = if (separator < 0) "minecraft" else tagId.substring(0, separator)
        val path = if (separator < 0) tagId else tagId.substring(separator + 1)
        val identifier = Identifier.tryParse(tagId) ?: return
        val resourcePath = "data/${identifier.namespace}/tags/block/${identifier.path}.json"

        try {
            for (source in AllThingsFoliage.platform.readAll(resourcePath)) {
                val values = JsonParser.parseString(source.decodeToString()).asJsonObject["values"] ?: continue

                for (value in values.asJsonArray) {
                    val entry = if (value.isJsonObject) {
                        value.asJsonObject.get("id").asString
                    } else {
                        value.asString
                    }

                    if (entry.startsWith("#")) {
                        readTag(entry.substring(1), blockIds, visitedTags)
                    } else {
                        blockIds.add(entry)
                    }
                }
            }
        } catch (exception: Exception) {
            AllThingsFoliage.logger.warn(
                "Could not read the block tag {} for the waving whitelist",
                tagId,
                exception
            )
        }
    }

    private fun registerGeneralBlocks() {
        // Vanilla
        addBlocks(
            Group.GENERAL,
            "minecraft:wildflowers",
            "minecraft:pink_petals"
        )

        // Biomes O' Plenty
        addBlocks(
            Group.GENERAL,
            "biomesoplenty:white_petals",
            "biomesoplenty:purple_wildflowers",
            "biomesoplenty:wildflower",
            "biomesoplenty:clover",
            "biomesoplenty:huge_clover_petal"
        )

        // No Man's Land
        addBlocks(Group.GENERAL, "nomansland:clover_patch")
    }

    /** Registers vines that follow neighboring leaves and hang freely below them. */
    private fun registerLeafFollowers() {
        // Vanilla
        forEachBlock({ block -> followers.add(block) }, "minecraft:vine")

        // Biomes O' Plenty
        forEachBlock({ block -> followers.add(block) }, "biomesoplenty:willow_vine")
    }

    /** Registers thin layers that move with leaves only when resting on them. */
    private fun registerLeafRestingBlocks() {
        // Vanilla and Biomes O' Plenty
        forEachBlock(
            { block -> resting.add(block) },
            "minecraft:snow",
            "minecraft:moss_carpet",
            "minecraft:pale_moss_carpet",
            "biomesoplenty:glowing_moss_carpet"
        )

        addTaggedBlocks({ block -> resting.add(block) }, "minecraft:wool_carpets")
    }

    /** Registers leaves from Minecraft's shared block tag. */
    private fun registerTags() {
        addTaggedBlocks(
            { block -> blocks.putIfAbsent(block, Group.LEAVES) },
            "minecraft:leaves"
        )
    }

    @Synchronized
    private fun resolve() {
        if (resolved) {
            return
        }

        // Registration order determines which group wins for repeated blocks.
        registerGeneralBlocks()
        registerLeafFollowers()
        registerLeafRestingBlocks()
        registerTags()

        val counts = Group.entries
            .associateWith { group -> blocks.values.count { it == group } }
            .filterValues { it > 0 }

        AllThingsFoliage.logger.info(
            "Waving whitelist: {}, following the leaves: {}, resting on them: {}",
            counts,
            followers.size,
            resting.size
        )

        resolved = true
    }
}
