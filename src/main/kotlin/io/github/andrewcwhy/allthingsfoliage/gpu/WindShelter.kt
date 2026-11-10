package io.github.andrewcwhy.allthingsfoliage.gpu

import io.github.andrewcwhy.allthingsfoliage.client.renderer.WavingWhitelist
import io.github.andrewcwhy.allthingsfoliage.Config
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Reference2ByteOpenHashMap
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.SectionPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.LightLayer
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.FenceBlock
import net.minecraft.world.level.block.FenceGateBlock
import net.minecraft.world.level.block.IronBarsBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraft.resources.Identifier
import net.minecraft.util.Mth

/**
 * Calculates section-local wind exposure for baking into foliage vertices.
 *
 * Roofs and upwind walls each provide partial shelter. Walls meeting a ceiling retain their
 * shelter across covered stretches. Rows are swept east to west to match the shader's wind.
 *
 * Instances and shared shape caches are accessed on the render thread while meshing.
 */
internal class WindShelter(
    private val level: ClientLevel,
    sectionOrigin: BlockPos,
    private val isField: Boolean = false,
) {

    private val corners = Long2FloatOpenHashMap()
    private val cursor = BlockPos.MutableBlockPos()
    private val firstX = sectionOrigin.x - if (isField) 1 else 0
    private val rows = Long2ObjectOpenHashMap<ByteArray>()
    private val width = SectionPos.SECTION_SIZE + if (isField) 2 else 0

    init {
        refreshWindWalls()
    }

    /** Returns the first covering block's height, or [Int.MAX_VALUE] if none is nearby. */
    private fun ceilingHeight(x: Int, y: Int, z: Int): Int {
        val surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z)

        for (up in 0..MAX_CEILING_HEIGHT) {
            if (y + up >= surface) {
                break
            }

            if (covers(x, y + up, z)) {
                return up
            }
        }

        return Int.MAX_VALUE
    }

    /**
     * Averages exposure around a shared field corner so neighboring leaves move together.
     *
     * Leaves do not shelter one another, and sky brightness is ignored because leaves dim it.
     */
    fun cornerExposure(x: Int, y: Int, z: Int): Float {
        val key = BlockPos.asLong(x, y, z)

        if (corners.containsKey(key)) {
            return corners.get(key)
        }

        var sum = 0.0f
        var counted = 0

        for (dy in -1..0) {
            for (dz in -1..0) {
                for (dx in -1..0) {
                    val blockX = x + dx
                    val blockY = y + dy
                    val blockZ = z + dz

                    if (stopsWind(blockX, blockY, blockZ)) {
                        continue
                    }

                    val index = (blockX - firstX).coerceIn(0, width - 1)
                    val wall = row(blockY, blockZ)[index].toInt()
                    val covered = isCovered(blockX, blockY, blockZ)
                    val sheltered =
                        (wall and TALLEST_MASK) >= 1 ||
                                ((wall and SEALED_FLAG) != 0 && covered)

                    val roofShelter = if (covered) ROOF_SHELTER else 0.0f
                    val wallShelter = if (sheltered) WALL_SHELTER else 0.0f

                    sum += 1.0f - minOf(1.0f, roofShelter + wallShelter)
                    counted++
                }
            }
        }

        val exposure = if (counted == 0) 1.0f else sum / counted
        corners.put(key, exposure)

        return exposure
    }

    /** Returns cached wind and roof coverage, excluding leaves when meshing a field. */
    private fun coverage(x: Int, y: Int, z: Int): Int {
        cursor.set(x, y, z)
        val state = level.getBlockState(cursor)

        if (state.isAir) {
            return 0
        }

        if (isField && WavingWhitelist.isLeaves(state.block)) {
            return 0
        }

        if (coverageCache.containsKey(state)) {
            return coverageCache.getByte(state).toInt()
        }

        // Collision-free plants must not become walls merely because their outlines are large.
        val flags = if (state.getCollisionShape(level, cursor).isEmpty) {
            0
        } else {
            measure(state, state.getShape(level, cursor))
        }

        coverageCache.put(state, flags.toByte())

        return flags
    }

    private fun covers(x: Int, y: Int, z: Int): Boolean {
        return (coverage(x, y, z) and COVERS) != 0
    }

    /**
     * Returns exposure from zero to one for a plant anchored inside this section.
     *
     * A sealing wall shelters covered plants regardless of their height.
     */
    fun exposureAt(x: Int, y: Int, z: Int, plantHeight: Int): Float {
        cursor.set(x, y, z)

        val sky = level.getBrightness(LightLayer.SKY, cursor)
        val covered = isCovered(x, y, z)

        val skyShelter = if (sky >= SKY_ROOF) {
            ROOF_SHELTER * ease((15.0f - sky) / (15 - SKY_ROOF))
        } else {
            ROOF_SHELTER +
                    (1.0f - ROOF_SHELTER) *
                    ease((SKY_ROOF - sky).toFloat() / (SKY_ROOF - SKY_ENCLOSED))
        }

        val roofShelter = maxOf(
            if (covered) ROOF_SHELTER else 0.0f,
            skyShelter,
        )

        val wall = row(y, z)[x - firstX].toInt()
        val sheltered =
            (wall and TALLEST_MASK) >= plantHeight ||
                    ((wall and SEALED_FLAG) != 0 && covered)

        val shelter = roofShelter + if (sheltered) WALL_SHELTER else 0.0f

        return 1.0f - minOf(1.0f, shelter)
    }

    private fun isCovered(x: Int, y: Int, z: Int): Boolean {
        return ceilingHeight(x, y + 1, z) != Int.MAX_VALUE
    }

    private fun row(y: Int, z: Int): ByteArray {
        val key = (y.toLong() shl 32) or (z.toLong() and 0xFFFFFFFFL)
        val cached = rows.get(key)

        if (cached != null) {
            return cached
        }

        val result = sweep(y, z)
        rows.put(key, result)

        return result
    }

    /** Counts the contiguous wall and roof blocks that can seal the downwind space. */
    private fun sealedRun(x: Int, y: Int, z: Int): Int {
        var height = 0

        while (
            height < MAX_CEILING_HEIGHT &&
            (stopsWind(x, y + height, z) || covers(x, y + height, z))
        ) {
            height++
        }

        return height
    }

    private fun stopsWind(x: Int, y: Int, z: Int): Boolean {
        return (coverage(x, y, z) and STOPS_WIND) != 0
    }

    /**
     * Sweeps downwind, tracking the distance behind walls of each height.
     *
     * Open blocks shorten every shadow; covered blocks preserve shadows from sealing walls.
     */
    private fun sweep(y: Int, z: Int): ByteArray {
        val result = ByteArray(width)
        val behind = IntArray(MAX_WALL_HEIGHT + 1) { Int.MAX_VALUE / 2 }
        val sealed = BooleanArray(MAX_WALL_HEIGHT + 1)

        for (x in (firstX + width - 1 + REACH) downTo firstX) {
            val height = wallHeight(x, y, z)

            if (height > 0) {
                behind[height] = 0

                // Test the downwind ceiling against the wall's full wall-and-roof run.
                sealed[height] =
                    sealedRun(x, y, z) >= ceilingHeight(x - 1, y, z)

                continue
            }

            val open = !isCovered(x, y, z)

            for (wallHeight in 1..MAX_WALL_HEIGHT) {
                if (open || !sealed[wallHeight]) {
                    behind[wallHeight]++
                }
            }

            if (x < firstX + width) {
                var entry = 0

                for (wallHeight in MAX_WALL_HEIGHT downTo 1) {
                    if (behind[wallHeight] <= wallHeight * SHADOW_PER_BLOCK) {
                        val sealedFlag = if (sealed[wallHeight]) SEALED_FLAG else 0

                        entry = maxOf(entry and TALLEST_MASK, wallHeight) or
                                (entry and SEALED_FLAG) or
                                sealedFlag
                    }
                }

                result[x - firstX] = entry.toByte()
            }
        }

        return result
    }

    private fun wallHeight(x: Int, y: Int, z: Int): Int {
        return wallRun(x, y, z, MAX_WALL_HEIGHT)
    }

    private fun wallRun(x: Int, y: Int, z: Int, limit: Int): Int {
        var height = 0

        while (height < limit && stopsWind(x, y + height, z)) {
            height++
        }

        return height
    }

    companion object {

        /** Maximum distance searched upward for a ceiling. */
        const val MAX_CEILING_HEIGHT = 8

        /** Maximum wall height used to determine open-air shelter distance. */
        const val MAX_WALL_HEIGHT = 4

        /** Upwind search distance beyond the section. */
        const val REACH = SectionPos.SECTION_SIZE

        /** Sheltered blocks per block of wall height. */
        const val SHADOW_PER_BLOCK = 1

        private const val COVERS = 2
        private const val FACE_CELLS = 16
        private const val ROOF_SHELTER = 0.5f
        private const val SEALED_FLAG = 0x8
        private const val SKY_ENCLOSED = 6
        private const val SKY_ROOF = 12
        private const val SOLID_ALONG_X = 1
        private const val SOLID_ALONG_Z = 2
        private const val STOPS_WIND = 1
        private const val TALLEST_MASK = 0x7
        private const val WALL_COVERAGE = 0.8f
        private const val WALL_SHELTER = 0.5f

        private val coverageCache = Reference2ByteOpenHashMap<BlockState>()
        private val sideCache = Reference2ByteOpenHashMap<BlockState>()

        private var windWallIds: List<String> = emptyList()
        private var windWalls: Set<Block> = emptySet()

        private fun cell(coordinate: Double): Int {
            return Math.round(coordinate * FACE_CELLS)
                .toInt()
                .coerceIn(0, FACE_CELLS)
        }

        /** Measures the shape's projected coverage on a sixteenth-block grid. */
        private fun covered(shape: VoxelShape, axis: Direction.Axis): Float {
            val cells = BooleanArray(FACE_CELLS * FACE_CELLS)

            shape.forAllBoxes { minX, minY, minZ, maxX, maxY, maxZ ->
                val minU = if (axis == Direction.Axis.X) minY else minX
                val maxU = if (axis == Direction.Axis.X) maxY else maxX
                val minV = if (axis == Direction.Axis.Z) minY else minZ
                val maxV = if (axis == Direction.Axis.Z) maxY else maxZ

                for (u in cell(minU) until cell(maxU)) {
                    for (v in cell(minV) until cell(maxV)) {
                        cells[u * FACE_CELLS + v] = true
                    }
                }
            }

            return cells.count { it }.toFloat() / cells.size
        }

        private fun ease(value: Float): Float {
            val clamped = value.coerceIn(0.0f, 1.0f)
            return clamped * clamped * (3.0f - 2.0f * clamped)
        }

        private fun isOpenWork(block: Block): Boolean {
            if (block is FenceBlock || block is FenceGateBlock) {
                return true
            }

            // Glass panes share the bars superclass, so distinguish them by registry path.
            return block is IronBarsBlock &&
                    BuiltInRegistries.BLOCK.getKey(block).path.endsWith("bars")
        }

        /** Checks cached horizontal face coverage for wall-like blocks. */
        fun isSolidSide(
            level: ClientLevel,
            pos: BlockPos,
            state: BlockState,
            axis: Direction.Axis,
        ): Boolean {
            val sides: Int

            if (sideCache.containsKey(state)) {
                sides = sideCache.getByte(state).toInt()
            } else {
                var measured = 0
                val block = state.block

                if (
                    !state.isAir &&
                    !state.getCollisionShape(level, pos).isEmpty &&
                    !(isOpenWork(block) && block !in windWalls)
                ) {
                    val shape = state.getShape(level, pos)

                    if (!shape.isEmpty) {
                        if (covered(shape, Direction.Axis.X) >= WALL_COVERAGE) {
                            measured = measured or SOLID_ALONG_X
                        }

                        if (covered(shape, Direction.Axis.Z) >= WALL_COVERAGE) {
                            measured = measured or SOLID_ALONG_Z
                        }
                    }
                }

                sideCache.put(state, measured.toByte())
                sides = measured
            }

            val sideFlag = if (axis == Direction.Axis.X) {
                SOLID_ALONG_X
            } else {
                SOLID_ALONG_Z
            }

            return (sides and sideFlag) != 0
        }

        /** Measures wind-blocking and roof coverage, honoring configured openwork walls. */
        private fun measure(state: BlockState, shape: VoxelShape): Int {
            if (shape.isEmpty) {
                return 0
            }

            var eastFace = covered(shape, Direction.Axis.X) >= WALL_COVERAGE
            val topFace = covered(shape, Direction.Axis.Y) >= WALL_COVERAGE
            val block = state.block

            if (eastFace && isOpenWork(block) && block !in windWalls) {
                eastFace = false
            }

            val windFlag = if (eastFace) STOPS_WIND else 0
            val roofFlag = if (topFace) COVERS else 0

            return windFlag or roofFlag
        }

        /** Invalidates measured states when the configured wind-wall list changes. */
        private fun refreshWindWalls() {
            val ids = Config.windWalls

            if (ids == windWallIds) {
                return
            }

            val blocks = mutableSetOf<Block>()

            for (id in ids.toHashSet()) {
                val identifier = Identifier.tryParse(id) ?: continue
                val block = BuiltInRegistries.BLOCK.getOptional(identifier).orElse(null) ?: continue

                if (BuiltInRegistries.BLOCK.getKey(block).toString() == id) {
                    blocks.add(block)
                }
            }

            windWallIds = ids.toList()
            windWalls = blocks

            coverageCache.clear()
            sideCache.clear()
        }
    }
}
