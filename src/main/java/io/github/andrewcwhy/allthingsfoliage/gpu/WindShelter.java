package io.github.andrewcwhy.allthingsfoliage.gpu;

import io.github.andrewcwhy.allthingsfoliage.FoliageSettings;
import io.github.andrewcwhy.allthingsfoliage.WavingWhitelist;
//? >=1.20.1 {

import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ByteOpenHashMap;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * How much of rain's wind reaches the plants of one section, worked out while the section is meshed and baked into
 * its vertices, so the shader only multiplies by it.
 * <p>
 * Shelter adds up, from 0 for none to 1 for all of it, and the plant keeps what is left of the wind:
 * <ul>
 * <li>a roof: a block covering the plant, not far above it, takes half the wind. Deep inside, far from any opening,
 * the sky light at the plant -- which a torch never raises and night never lowers -- takes all of it;</li>
 * <li>a wall upwind: the wind blows west, as in sway.glsl, so each row of blocks is swept from east to west. A wall
 * takes half the wind from the plants behind it that are no taller than it is, as many blocks of open sky back as it
 * stands tall: the wind comes down over the top of it, so a taller plant still catches it. A wall that reaches the
 * ceiling on its downwind side seals the room behind it: the wind has no way down, so its shadow does not wear off
 * along a covered stretch, and it shelters a covered plant of any height. Covered means a roof over the block: sky
 * light alone cannot say, since it comes in sideways through doors and windows that let no wind in from upwind. A wall with a gap between its top and the
 * ceiling -- a window above a sill -- lets the wind in over it, as a wall in the open does.</li>
 * </ul>
 * A row is swept once, the first time a plant on it asks, and shared by every plant along it.
 */
final class WindShelter {

    /** Sky light at which a plant counts as under a roof, and at or below which it is shut in with no wind at all. */
    private static final int SKY_ROOF = 12;
    private static final int SKY_ENCLOSED = 6;
    /** How much of the wind a roof takes, and how much a wall does. */
    private static final float ROOF_SHELTER = 0.5F;
    private static final float WALL_SHELTER = 0.5F;

    /** How many blocks behind it a wall shelters for every block it stands tall. */
    static final int SHADOW_PER_BLOCK = 1;
    /** Walls taller than this count as this tall, and plants taller than this are never sheltered by one. */
    static final int MAX_WALL_HEIGHT = 4;
    /**
     * How far upwind of a section walls are looked for: past the longest shadow in the open, since under a roof a
     * shadow carries on as far as the roof does -- across a house, say.
     */
    static final int REACH = SectionPos.SECTION_SIZE;
    /** How far above a row a ceiling is looked for, to tell a wall that seals a room from one the wind blows over. */
    static final int MAX_CEILING_HEIGHT = 8;
    /** In a row's entries: the tallest wall sheltering the block in its low bits, and whether a sealing wall does. */
    private static final int TALLEST_MASK = 0x7;
    private static final int SEALED_FLAG = 0x8;
    /**
     * How much of a block's face its shape has to cover to stop the wind: a stone wall's side, 14/16 tall, does; a slab,
     * an open door seen edge on, a lone glass pane or a closed trapdoor do not.
     */
    private static final float WALL_COVERAGE = 0.8F;
    /** A block shape's face, in sixteenths of a block a side. */
    private static final int FACE_CELLS = 16;
    /** In {@link #COVERAGE}: the block stops wind blowing against its side, and it covers what is below it. */
    private static final byte STOPS_WIND = 1;
    private static final byte COVERS = 2;
    /**
     * What each block state does for the wind, worked out from its shape the first time it is met: shapes do not change,
     * and states are few. Only read and written while meshing, on the render thread.
     */
    private static final Reference2ByteOpenHashMap<BlockState> COVERAGE =
        new Reference2ByteOpenHashMap<>();
    /** In {@link #SIDES}: the block stands solid seen along X, and seen along Z. */
    private static final byte SOLID_ALONG_X = 1;
    private static final byte SOLID_ALONG_Z = 2;
    /** Whether each block state stands solid on its sides, as a wall does, seen along each horizontal axis; see isSolidSide. */
    private static final Reference2ByteOpenHashMap<BlockState> SIDES =
        new Reference2ByteOpenHashMap<>();
    /** The ids in the settings' wind walls, as last read; see {@link #isWindWall}. */
    private static List<String> windWallIds = List.of();
    private static Set<Block> windWalls = Set.of();

    private final ClientLevel level;
    /**
     * Whether this one is for a field -- a tree's leaves -- rather than for plants: leaves are then neither walls nor
     * roofs, so a tree does not shelter itself, and its rows reach a block past the section on either side, for the
     * corners on its edges.
     */
    private final boolean field;
    /** The first block of a row, west, and how many it holds. */
    private final int firstX;
    private final int width;
    /**
     * Along each row the section's plants stand on, by height and z: for each block of the row, west to east, the tallest
     * wall sheltering it (0 for none) and whether a wall sealing it under a roof does.
     */
    private final Long2ObjectOpenHashMap<byte[]> rows =
        new Long2ObjectOpenHashMap<>();
    /** For a field: how much of the wind reaches each corner asked for so far; see cornerExposure. */
    private final Long2FloatOpenHashMap corners = new Long2FloatOpenHashMap();
    private final BlockPos.MutableBlockPos cursor =
        new BlockPos.MutableBlockPos();

    WindShelter(ClientLevel level, BlockPos sectionOrigin) {
        this(level, sectionOrigin, false);
    }

    WindShelter(ClientLevel level, BlockPos sectionOrigin, boolean field) {
        this.level = level;
        this.field = field;
        this.firstX = sectionOrigin.getX() - (field ? 1 : 0);
        this.width = SectionPos.SECTION_SIZE + (field ? 2 : 0);
        // Once a section rather than for every block looked at: a change to the list applies to states already measured.
        refreshWindWalls();
    }

    /**
     * How much of the wind reaches a plant anchored at this block and this many blocks tall, from 0 for none to 1 for
     * all of it. The anchor's column must lie inside the section; its height need not.
     */
    float exposureAt(int x, int y, int z, int plantHeight) {
        cursor.set(x, y, z);
        int sky = level.getBrightness(LightLayer.SKY, cursor);
        boolean covered = isCovered(x, y, z);
        // By the sky: none in the open, half by the time it is behind something, all of it once the plant is shut in.
        float bySky =
            sky >= SKY_ROOF
                ? ROOF_SHELTER * ease((15.0F - sky) / (15 - SKY_ROOF))
                : ROOF_SHELTER +
                  (1.0F - ROOF_SHELTER) *
                      ease(
                          (float) (SKY_ROOF - sky) / (SKY_ROOF - SKY_ENCLOSED)
                      );
        // A roof takes its half whatever light comes in beside it.
        float roof = Math.max(covered ? ROOF_SHELTER : 0.0F, bySky);
        int wall = row(y, z)[x - firstX];
        // Under a roof a wall that seals the room leaves the wind no way down onto a taller plant.
        boolean sheltered =
            (wall & TALLEST_MASK) >= plantHeight ||
            ((wall & SEALED_FLAG) != 0 && covered);
        float shelter = roof + (sheltered ? WALL_SHELTER : 0.0F);
        return 1.0F - Math.min(1.0F, shelter);
    }

    private static float ease(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private byte[] row(int y, int z) {
        long key = ((long) y << 32) | (z & 0xFFFFFFFFL);
        byte[] row = rows.get(key);
        if (row == null) {
            row = sweep(y, z);
            rows.put(key, row);
        }
        return row;
    }

    /**
     * How much of the wind reaches a corner of a field's blocks, from 0 to 1: the same whichever block sharing the corner
     * asks, so neighbouring leaves move as one. The eight blocks around it count alike, but for those standing solid in
     * the wind's way, which only shelter the others. Each of them is sheltered half by a roof over it and half by a wall
     * upwind, as a plant a block tall is; the sky's light plays no part, since the leaves themselves dim it.
     */
    float cornerExposure(int x, int y, int z) {
        long key = BlockPos.asLong(x, y, z);
        if (corners.containsKey(key)) {
            return corners.get(key);
        }
        float sum = 0.0F;
        int counted = 0;
        for (int dy = -1; dy <= 0; dy++) {
            for (int dz = -1; dz <= 0; dz++) {
                for (int dx = -1; dx <= 0; dx++) {
                    int bx = x + dx;
                    int by = y + dy;
                    int bz = z + dz;
                    if (stopsWind(bx, by, bz)) {
                        continue;
                    }
                    int wall = row(by, bz)[
                        Mth.clamp(bx - firstX, 0, width - 1)
                    ];
                    boolean covered = isCovered(bx, by, bz);
                    boolean sheltered =
                        (wall & TALLEST_MASK) >= 1 ||
                        ((wall & SEALED_FLAG) != 0 && covered);
                    sum +=
                        1.0F -
                        Math.min(
                            1.0F,
                            (covered ? ROOF_SHELTER : 0.0F) +
                                (sheltered ? WALL_SHELTER : 0.0F)
                        );
                    counted++;
                }
            }
        }
        float exposure = counted == 0 ? 1.0F : sum / counted;
        corners.put(key, exposure);
        return exposure;
    }

    /**
     * Walks one row from REACH blocks east of the section to its west edge, the way the wind blows, keeping for each wall
     * height how many blocks back the last wall that tall stood. A block is sheltered by the tallest of them still close
     * enough, since a wall reaches as far back as it stands tall. The wind comes down behind a wall it blows over at
     * every block; behind a wall sealing a room only where the sky is open, so covered blocks do not count towards it.
     */
    private byte[] sweep(int y, int z) {
        byte[] row = new byte[width];
        int[] behind = new int[MAX_WALL_HEIGHT + 1];
        boolean[] sealed = new boolean[MAX_WALL_HEIGHT + 1];
        Arrays.fill(behind, Integer.MAX_VALUE / 2);
        for (int x = firstX + width - 1 + REACH; x >= firstX; x--) {
            int height = wallHeight(x, y, z);
            if (height > 0) {
                behind[height] = 0;
                // Against the ceiling on the downwind side, where the plants it shelters stand, and with the wall's full
                // height up into the roof it carries: a gable end stands taller than any wall counts for its shadow, and
                // roof stairs laid along it cover without facing the wind.
                sealed[height] =
                    sealedRun(x, y, z) >= ceilingHeight(x - 1, y, z);
                continue;
            }
            boolean open = !isCovered(x, y, z);
            for (int h = 1; h <= MAX_WALL_HEIGHT; h++) {
                if (open || !sealed[h]) {
                    behind[h]++;
                }
            }
            if (x < firstX + width) {
                int entry = 0;
                for (int h = MAX_WALL_HEIGHT; h >= 1; h--) {
                    if (behind[h] <= h * SHADOW_PER_BLOCK) {
                        entry =
                            Math.max(entry & TALLEST_MASK, h) |
                            (entry & SEALED_FLAG) |
                            (sealed[h] ? SEALED_FLAG : 0);
                    }
                }
                row[x - firstX] = (byte) entry;
            }
        }
        return row;
    }

    /**
     * How far above this block the ceiling is: the first block up that covers what is below it, counting the block
     * itself as 0. A wall at least this tall reaches the ceiling. With none within MAX_CEILING_HEIGHT the sky is open
     * above, which no wall reaches.
     */
    /** Whether a roof covers this block: a block covering what is below it, within MAX_CEILING_HEIGHT above. */
    private boolean isCovered(int x, int y, int z) {
        return ceilingHeight(x, y + 1, z) != Integer.MAX_VALUE;
    }

    private int ceilingHeight(int x, int y, int z) {
        // Nothing stands above the column's highest block, so the search stops there: out in the open at once.
        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        for (int up = 0; up <= MAX_CEILING_HEIGHT && y + up < surface; up++) {
            if (covers(x, y + up, z)) {
                return up;
            }
        }
        return Integer.MAX_VALUE;
    }

    /** How many blocks of wall stand at this block and straight above it, up to MAX_WALL_HEIGHT; 0 if it is not a wall. */
    private int wallHeight(int x, int y, int z) {
        return wallRun(x, y, z, MAX_WALL_HEIGHT);
    }

    private int wallRun(int x, int y, int z, int limit) {
        int height = 0;
        while (height < limit && stopsWind(x, y + height, z)) {
            height++;
        }
        return height;
    }

    /** How many blocks of wall, and of roof resting on it, stand at this block and straight above it. */
    private int sealedRun(int x, int y, int z) {
        int height = 0;
        while (
            height < MAX_CEILING_HEIGHT &&
            (stopsWind(x, y + height, z) || covers(x, y + height, z))
        ) {
            height++;
        }
        return height;
    }

    /** Whether the block here stops wind blowing against its east face. */
    private boolean stopsWind(int x, int y, int z) {
        return (coverage(x, y, z) & STOPS_WIND) != 0;
    }

    /** Whether the block here covers what is below it, as a ceiling does. */
    private boolean covers(int x, int y, int z) {
        return (coverage(x, y, z) & COVERS) != 0;
    }

    private byte coverage(int x, int y, int z) {
        cursor.set(x, y, z);
        BlockState state = level.getBlockState(cursor);
        if (state.isAir()) {
            return 0;
        }
        // A tree's leaves shelter the plants below them, but not one another.
        if (field && WavingWhitelist.isLeaves(state.getBlock())) {
            return 0;
        }
        if (COVERAGE.containsKey(state)) {
            return COVERAGE.getByte(state);
        }
        // Nothing the wind can blow through the way an entity walks through it is a wall or a roof: plants, above all.
        // Their outline says nothing about that -- a two block plant's is a whole block.
        byte found = state.getCollisionShape(level, cursor).isEmpty()
            ? 0
            : measure(state, state.getShape(level, cursor));
        COVERAGE.put(state, found);
        return found;
    }

    /**
     * Whether this block stands solid on its sides seen along this horizontal axis, as a wall the wind cannot blow
     * through does: the same test walls pass, from whichever side. A torch, a fence, a slab or another plant does not.
     * Measured once per state, on the render thread, while meshing.
     */
    static boolean isSolidSide(
        ClientLevel level,
        BlockPos pos,
        BlockState state,
        Direction.Axis axis
    ) {
        byte sides;
        if (SIDES.containsKey(state)) {
            sides = SIDES.getByte(state);
        } else {
            sides = 0;
            if (
                !state.isAir() &&
                !state.getCollisionShape(level, pos).isEmpty() &&
                !(
                    isOpenWork(state.getBlock()) &&
                    !windWalls.contains(state.getBlock())
                )
            ) {
                VoxelShape shape = state.getShape(level, pos);
                if (!shape.isEmpty()) {
                    sides = (byte) ((covered(shape, Direction.Axis.X) >=
                    WALL_COVERAGE
                        ? SOLID_ALONG_X
                        : 0) |
                        (covered(shape, Direction.Axis.Z) >= WALL_COVERAGE
                            ? SOLID_ALONG_Z
                            : 0));
                }
            }
            SIDES.put(state, sides);
        }
        return (
            (sides &
                (axis == Direction.Axis.X ? SOLID_ALONG_X : SOLID_ALONG_Z)) !=
            0
        );
    }

    /**
     * What a block state does for the wind, from the outline it shows: it stops the wind if, seen from the east, it
     * covers WALL_COVERAGE of its face, and covers what is below if it does as much seen from above. Fences, fence gates
     * and bars never stop it -- the wind goes through their gaps, which their outline does not show -- unless the
     * settings name them. Glass panes are not bars: joined up they fill the gap they stand in.
     */
    private static byte measure(BlockState state, VoxelShape shape) {
        if (shape.isEmpty()) {
            return 0;
        }
        boolean eastFace = covered(shape, Direction.Axis.X) >= WALL_COVERAGE;
        boolean topFace = covered(shape, Direction.Axis.Y) >= WALL_COVERAGE;
        if (
            eastFace &&
            isOpenWork(state.getBlock()) &&
            !windWalls.contains(state.getBlock())
        ) {
            eastFace = false;
        }
        return (byte) ((eastFace ? STOPS_WIND : 0) | (topFace ? COVERS : 0));
    }

    private static boolean isOpenWork(Block block) {
        if (block instanceof FenceBlock || block instanceof FenceGateBlock) {
            return true;
        }
        // Glass panes are bars as far as the game is concerned, shape and all; only the name tells them apart.
        return (
            block instanceof IronBarsBlock &&
            BuiltInRegistries.BLOCK.getKey(block).getPath().endsWith("bars")
        );
    }

    /** Reads the settings' wind walls again when they have changed, and measures every state again with them. */
    private static void refreshWindWalls() {
        List<String> ids = FoliageSettings.windWalls();
        if (ids.equals(windWallIds)) {
            return;
        }
        Set<Block> blocks = new HashSet<>();
        for (Block candidate : BuiltInRegistries.BLOCK) {
            if (
                ids.contains(
                    BuiltInRegistries.BLOCK.getKey(candidate).toString()
                )
            ) {
                blocks.add(candidate);
            }
        }
        windWallIds = List.copyOf(ids);
        windWalls = blocks;
        COVERAGE.clear();
        SIDES.clear();
    }

    /**
     * How much of a block's face the shape covers seen along an axis, from 0 to 1: its boxes projected onto the face,
     * counted in sixteenths.
     */
    private static float covered(VoxelShape shape, Direction.Axis axis) {
        boolean[] cells = new boolean[FACE_CELLS * FACE_CELLS];
        for (AABB box : shape.toAabbs()) {
            // The face's two other axes.
            double minU = axis == Direction.Axis.X ? box.minY : box.minX;
            double maxU = axis == Direction.Axis.X ? box.maxY : box.maxX;
            double minV = axis == Direction.Axis.Z ? box.minY : box.minZ;
            double maxV = axis == Direction.Axis.Z ? box.maxY : box.maxZ;
            for (int u = cell(minU); u < cell(maxU); u++) {
                for (int v = cell(minV); v < cell(maxV); v++) {
                    cells[u * FACE_CELLS + v] = true;
                }
            }
        }
        int count = 0;
        for (boolean cell : cells) {
            if (cell) {
                count++;
            }
        }
        return (float) count / cells.length;
    }

    private static int cell(double coordinate) {
        return Mth.clamp(
            (int) Math.round(coordinate * FACE_CELLS),
            0,
            FACE_CELLS
        );
    }
}
//?}
