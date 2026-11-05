package io.github.andrewcwhy.allthingsfoliage.gpu;

//? >=1.20.1 {

import com.github.razorplay01.sway.api.SwayAPI;
import io.github.andrewcwhy.allthingsfoliage.WavingWhitelist;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.LongConsumer;
//? >=26.1.2 {
import net.minecraft.client.renderer.block.BlockAndTintGetter;
//?} else {
/*import net.minecraft.world.level.BlockAndTintGetter;
 */ //?}
//? >=26.1.2 {
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
//?} elif >=1.21.11 {
/*import net.minecraft.client.renderer.block.model.BlockStateModel;
 */ //?} else {
/*import net.minecraft.client.resources.model.BakedModel;
 */ //?}
//? >=1.21.11 {
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
//?} else {
/*import net.minecraft.client.renderer.chunk.RenderChunkRegion;
 */ //?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Decides who draws each piece of foliage: {@link GpuFoliageRenderer} near the player, where its sway can be
 * seen, and the chunk mesh further out, where it cannot and the chunk mesh draws it for almost nothing.
 * <p>
 * The split is made in the block model. Every foliage model is wrapped in a model of the loader's -- GpuFoliageModel on
 * Fabric, NeoforgeFoliageModel and ForgeFoliageModel on the others -- and while a chunk mesher asks it for the geometry
 * of a block near the player it hands over nothing. Meshers hand the model the block's position as they ask -- through
 * Fabric's rendering API, NeoForge's model extensions or Forge's model data, which vanilla, Sodium from 0.6 on and
 * Embeddium all go through -- so the split needs nothing from inside any of them.
 * <p>
 * Each time a mesher builds a section the decision is recorded, and the mesher reports the moment the mesh it
 * built replaces the one on screen. The renderer changes what it draws for a section on that frame, not before,
 * which keeps the two in step as the player moves: a section is never drawn by both, and never left to neither.
 */
public final class GpuFoliageSplit {

    /**
     * Sodium's snapshot of the level for a chunk build, under the same name from Sodium 0.6 through 0.9, and as
     * {@code WorldSlice} in its old package before. Null without Sodium, where vanilla's own region is the only
     * snapshot there is.
     */
    //? >=1.21.1 {
    private static final Class<?> SODIUM_LEVEL_SLICE = findClass(
        "net.caffeinemc.mods.sodium.client.world.LevelSlice"
    );

    //?} else {
    /*private static final Class<?> SODIUM_LEVEL_SLICE = findClass("me.jellysquid.mods.sodium.client.world.WorldSlice");
     */ //?}

    /** The chunks around the player whose foliage the GPU renderer draws. Replaced whole, never edited. */
    private record Area(int centreX, int centreZ, int radius) {
        boolean contains(int chunkX, int chunkZ) {
            return (
                Math.max(
                    Math.abs(chunkX - centreX),
                    Math.abs(chunkZ - centreZ)
                ) <= radius
            );
        }
    }

    private record MeshDecision(long sectionKey, boolean leftFoliage) {}

    /**
     * The chunk columns of the sub-levels the renderer draws the foliage of, which lie in a plot far from the player
     * and count as near all the same; see SableBridge. Written by the render thread, read by the meshing threads.
     */
    private static final Set<Long> PLOT_COLUMNS = ConcurrentHashMap.newKeySet();
    /**
     * Sections of a plot a chunk build met foliage in and kept it, for the renderer to take up: that build is how the
     * renderer learns a sub-level is there at all. Written by the meshing threads, emptied by the render thread.
     * <p>
     * They are noted whether or not the renderer has an area yet, and kept until it asks: Sable builds a sub-level's
     * sections as the world is first drawn and again as a shader pack is switched, which is before the renderer's
     * first frame after either, and a section noted only while there was an area was never heard of again -- its
     * foliage stayed in Sable's mesh for good.
     */
    private static final Set<Long> PLOT_FOUND = ConcurrentHashMap.newKeySet();

    private static long columnKey(int chunkX, int chunkZ) {
        return (
            ((long) chunkX & 0xFFFFFFFFL) |
            (((long) chunkZ & 0xFFFFFFFFL) << 32)
        );
    }

    /** Render thread: the foliage of this column of a plot is the renderer's from now on. */
    static void addPlotColumn(int chunkX, int chunkZ) {
        if (PLOT_COLUMNS.add(columnKey(chunkX, chunkZ))) {
            generation++;
        }
    }

    /** Render thread: the sub-level this column of a plot held is gone, or no longer the renderer's. */
    static void removePlotColumn(int chunkX, int chunkZ) {
        if (PLOT_COLUMNS.remove(columnKey(chunkX, chunkZ))) {
            generation++;
        }
    }

    /** Render thread: no sub-level's foliage is the renderer's any more. */
    static void clearPlots() {
        if (!PLOT_COLUMNS.isEmpty()) {
            PLOT_COLUMNS.clear();
            generation++;
        }
    }

    /** Render thread: hands over each section of a plot a chunk build has met foliage in since last asked. */
    static void forEachPlotFound(LongConsumer found) {
        if (PLOT_FOUND.isEmpty()) {
            return;
        }
        var keys = PLOT_FOUND.iterator();
        while (keys.hasNext()) {
            long key = keys.next();
            keys.remove();
            found.accept(key);
        }
    }

    /** A chunk mesh built for this section has just replaced the one on screen. */
    private record MeshSwap(long sectionKey) {}

    /** The last decision a meshing thread recorded, so a build records once rather than once per block. */
    private static final class LastDecision {

        long sectionKey;
        boolean leftFoliage;
        int generation = -1;
    }

    private static volatile Area area;
    /**
     * Bumped whenever a recorded decision may no longer stand -- the area moved, or a section was forgotten --
     * so meshing threads record their next decisions again instead of skipping them as already known.
     */
    private static volatile int generation;

    private static final ThreadLocal<LastDecision> LAST_DECISION =
        ThreadLocal.withInitial(LastDecision::new);
    /**
     * Decisions reported by meshing threads, waiting for the render thread to apply them. The chunk mesh swaps
     * travel in the same queue, so each is seen after the decisions its own build recorded.
     */
    private static final ConcurrentLinkedQueue<Record> DECISIONS =
        new ConcurrentLinkedQueue<>();
    /** Render thread only: sections whose chunk mesh on screen was built without their foliage. */
    private static final LongOpenHashSet MESHED_WITHOUT_FOLIAGE =
        new LongOpenHashSet();

    /**
     * Render thread only: sections handed back to the chunk mesh whose new mesh is not on screen yet, and when to
     * stop waiting for it.
     * <p>
     * A section's decision is recorded while the chunk mesher reaches its foliage, not when the mesh it builds
     * replaces the old one: the rest of the section still has to be meshed and uploaded. Letting go at the
     * decision left the old mesh, the one without the foliage, on screen alone until then, and the foliage blinked
     * out. The renderer keeps drawing the section until the chunk mesh reports its new mesh in place instead
     * ({@link #onChunkMeshSwapped}), and stops on that very frame.
     */
    private static final Long2LongOpenHashMap HANDING_BACK =
        new Long2LongOpenHashMap();
    /**
     * A safety net, not the mechanism: should the new mesh never be reported -- its build dropped for good -- the
     * section is let go of after this long, so it is never drawn twice for good.
     */
    private static final long HAND_BACK_GIVE_UP_MILLIS = 5000L;

    /**
     * Render thread only: near sections whose latest chunk build kept their foliage when it should have left it out,
     * waiting for that build to be on screen so a rebuild can be asked for.
     * <p>
     * A build reads the near area as it starts, so one that started before the area reached its section keeps the
     * foliage. Asking again on a timer cancels vanilla's build in progress each time, and a build that takes longer
     * than the timer -- a heavy resource pack is enough -- never finishes. Asking once the build is in place never
     * interrupts one, and Sodium accepts it too: it turns down a rebuild for a section it has not built yet, and by
     * then the section has been built.
     */
    private static final LongOpenHashSet NEEDS_REBUILD = new LongOpenHashSet();
    /** Render thread only: sections whose chunk mesh is to be built again, collected for the renderer to ask for. */
    private static final LongArrayList REBUILD_NOW = new LongArrayList();

    /**
     * Render thread only: sections whose foliage changed hands with a whitelist group switched, waiting for their new
     * chunk mesh to be on screen before the renderer meshes them again, and when to stop waiting.
     * <p>
     * The chunk mesh goes first and the renderer follows on the frame it is in place: a leaf let go is drawn twice or
     * not at all only for the few frames the renderer takes to catch up, rather than for as long as the chunk build.
     */
    private static final Long2LongOpenHashMap REMESH_ON_SWAP =
        new Long2LongOpenHashMap();
    private static final long REMESH_GIVE_UP_MILLIS = 2000L;
    /** Render thread only: sections due to be meshed again by the renderer, collected for it. */
    private static final LongArrayList REMESH_NOW = new LongArrayList();

    /**
     * Sections the renderer draws whose uploaded mesh was built before a whitelist group was switched on, so it holds
     * none of that group's blocks yet -- a tree's leaves, and what rests on them. The chunk mesh keeps those blocks there
     * until the renderer has uploaded a mesh built since, and is only then asked to let them go: the other way round, the
     * chunk mesh let them go at once and they were drawn by nobody for as long as the renderer took to mesh and upload
     * every section again. Written by the render thread, read by the meshing threads.
     */
    private static final Set<Long> AWAITING_GROUP =
        ConcurrentHashMap.newKeySet();
    /** Bumped each time the blocks that are foliage change, so a mesh says which list it was built with. */
    private static volatile int listGeneration;

    /**
     * Sections the GPU renderer holds uploaded geometry for. Only these may be left out of the chunk mesh: until then the
     * chunk mesh keeps their foliage, rather than leave it to a renderer with nothing to show yet -- as it would for the
     * sections of a chunk that arrives inside the near area while the renderer is still working through its queue.
     * Written by the render thread, read by the meshing threads.
     */
    private static final Set<Long> GPU_READY = ConcurrentHashMap.newKeySet();

    /**
     * Render thread only: sections whose chunk mesh has been built without their foliage, but is not on screen yet. The
     * renderer starts drawing them the moment it is, and not before: until then the old mesh, still holding the
     * foliage, is what is showing.
     */
    private static final LongOpenHashSet TAKING_OVER = new LongOpenHashSet();

    /**
     * The models the mod wrapped, by state. Where Sway deforms inside the model rather than through a
     * rendering API -- NeoForge -- meshing through vanilla's model set would bake its own deformation into
     * geometry the GPU is about to move again. Meshing goes through these instead: they sit inside Sway's
     * wrapper and hand over plain geometry.
     */
    //? >=1.21.11 {
    private static final Map<BlockState, BlockStateModel> GPU_MODELS =
        new IdentityHashMap<>();
    //?} else {
    /*// Before 1.21.11 a block's model is a BakedModel.
	private static final Map<BlockState, BakedModel> GPU_MODELS = new IdentityHashMap<>();
	*/ //?}

    /**
     * Which blocks are foliage now, found on first use and forgotten when a whitelist group is switched: read by the
     * chunk builders' threads too, which see it whole or not at all.
     */
    private static volatile Set<Block> foliage;

    //? forge {
    /*private static final String EMBEDDIUM_SLICE_VIEW = "org.embeddedt.embeddium.render.world.WorldSliceLocal";
     */ //?}

    private GpuFoliageSplit() {}

    /**
     * Called by a foliage model while its geometry is requested: whether to hand over nothing because the GPU
     * renderer draws this block. Only chunk builds are split. Anything else that asks -- a block pushed by a
     * piston, a falling block, the GPU renderer meshing its own sections -- always gets the full geometry.
     */
    public static boolean leaveToGpu(BlockAndTintGetter level, BlockPos pos) {
        //? >=1.21.11 {
        boolean chunkBuild = level instanceof RenderSectionRegion;
        //?} else {
        /*boolean chunkBuild = level instanceof RenderChunkRegion;
         */ //?}
        //? forge {
        /*// Embeddium hands a model a view of its level snapshot, a class it makes up as the game runs, rather than the
		// snapshot itself, so it is known by name.
		chunkBuild = chunkBuild || EMBEDDIUM_SLICE_VIEW.equals(level.getClass().getName());
		*/ //?}
        if (
            !chunkBuild &&
            (SODIUM_LEVEL_SLICE == null ||
                !SODIUM_LEVEL_SLICE.isInstance(level)) &&
            !VulkanBridge.isChunkBuild(level)
        ) {
            return false;
        }
        return decide(pos);
    }

    /** Whether a chunk build leaves the foliage at this position to the GPU renderer, decided once per section. */
    private static boolean decide(BlockPos pos) {
        int sectionX = SectionPos.blockToSectionCoord(pos.getX());
        int sectionZ = SectionPos.blockToSectionCoord(pos.getZ());
        long sectionKey = SectionPos.asLong(
            sectionX,
            SectionPos.blockToSectionCoord(pos.getY()),
            sectionZ
        );
        // A build asks once per foliage block, so the answer is kept for the rest of its section until something it
        // depends on changes: the near area, or what the renderer holds.
        LastDecision last = LAST_DECISION.get();
        int currentGeneration = generation;
        if (
            last.sectionKey == sectionKey &&
            last.generation == currentGeneration
        ) {
            return last.leftFoliage;
        }
        Area current = area;
        boolean near = current != null && current.contains(sectionX, sectionZ);
        // A sub-level's blocks are far from the player, in a plot of their own, and are the renderer's as near ones are.
        boolean plot =
            !near &&
            SableBridge.LOADED &&
            (PLOT_COLUMNS.contains(columnKey(sectionX, sectionZ)) ||
                SableBridge.inPlot(sectionX, sectionZ));
        boolean leave =
            current != null && (near || plot) && GPU_READY.contains(sectionKey);
        if (plot && !leave) {
            PLOT_FOUND.add(sectionKey);
        }
        last.sectionKey = sectionKey;
        last.leftFoliage = leave;
        last.generation = currentGeneration;
        DECISIONS.add(new MeshDecision(sectionKey, leave));
        return leave;
    }

    /** Whether the plant inside the Snow! Real Magic snow Indigo is drawing now is left to the GPU renderer. */
    private static final ThreadLocal<Boolean> SNOW_PLANT_LEFT =
        ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * Called as Indigo starts drawing a block in a chunk build, on Forge, where Snow! Real Magic draws through its own
     * copy of it. Its snow draws the plant inside through the plant's own model, but hands it the snow's model data, so
     * the plant's model never decides for itself; the decision is made here instead, the same way, at the snow.
     */
    public static void beginIndigoBlock(BlockState state, BlockPos pos) {
        SNOW_PLANT_LEFT.set(
            SnowRealMagicCompat.isSnowBlock(state) && decide(pos)
        );
    }

    /**
     * The same, from Indigo's block context, which Embeddium draws Fabric's models through and Indigo also draws blocks
     * outside chunk builds with -- falling or moved ones, which always keep their plant.
     */
    public static void beginIndigoBlock(
        BlockAndTintGetter level,
        BlockState state,
        BlockPos pos
    ) {
        SNOW_PLANT_LEFT.set(
            SnowRealMagicCompat.isSnowBlock(state) && leaveToGpu(level, pos)
        );
    }

    /** Indigo is done with the block. */
    public static void endIndigoBlock() {
        SNOW_PLANT_LEFT.set(Boolean.FALSE);
    }

    /** Whether the plant being drawn inside Snow! Real Magic's snow is the GPU renderer's to draw; see beginIndigoBlock. */
    public static boolean snowPlantLeftToGpu() {
        return SNOW_PLANT_LEFT.get();
    }

    /** Render thread: applies the decisions and chunk mesh swaps reported since the last frame, in order. */
    static void applyMeshDecisions() {
        Record event;
        while ((event = DECISIONS.poll()) != null) {
            if (event instanceof MeshSwap swap) {
                long key = swap.sectionKey();
                // The chunk mesh that took the foliage back is on screen now: the renderer can stop drawing it.
                HANDING_BACK.remove(key);
                // Its foliage changed hands with a whitelist group: the renderer follows the chunk mesh now.
                if (REMESH_ON_SWAP.remove(key) != 0L) {
                    REMESH_NOW.add(key);
                }
                // The chunk mesh that let the foliage go is on screen now: the renderer starts drawing it, on this frame.
                if (TAKING_OVER.remove(key)) {
                    MESHED_WITHOUT_FOLIAGE.add(key);
                }
                // A near section built with its foliage still in: now that the build is in place, ask for one without.
                if (
                    NEEDS_REBUILD.remove(key) &&
                    isNear(SectionPos.x(key), SectionPos.z(key)) &&
                    !MESHED_WITHOUT_FOLIAGE.contains(key)
                ) {
                    REBUILD_NOW.add(key);
                }
                continue;
            }
            MeshDecision decision = (MeshDecision) event;
            long key = decision.sectionKey();
            if (decision.leftFoliage()) {
                // Not drawn yet: the old mesh, which still holds the foliage, stays on screen until this build replaces it.
                // A section still being handed back keeps being drawn meanwhile, as that is what is showing.
                if (!MESHED_WITHOUT_FOLIAGE.contains(key)) {
                    TAKING_OVER.add(key);
                }
                NEEDS_REBUILD.remove(key);
            } else {
                TAKING_OVER.remove(key);
                if (MESHED_WITHOUT_FOLIAGE.remove(key)) {
                    HANDING_BACK.put(
                        key,
                        System.currentTimeMillis() + HAND_BACK_GIVE_UP_MILLIS
                    );
                }
                // Kept on purpose while the renderer has nothing for it; kept by mistake once it has, and asked again.
                if (
                    isNear(SectionPos.x(key), SectionPos.z(key)) &&
                    GPU_READY.contains(key)
                ) {
                    NEEDS_REBUILD.add(key);
                }
            }
        }
    }

    /**
     * Render thread: the chunk mesh of a section has just been replaced by a newly built one. Called by the chunk
     * mesher itself, vanilla's or Sodium's, as the new mesh takes the old one's place.
     */
    public static void onChunkMeshSwapped(
        int sectionX,
        int sectionY,
        int sectionZ
    ) {
        long key = SectionPos.asLong(sectionX, sectionY, sectionZ);
        // Only a section the renderer draws, or a near one it is about to, can be waiting on its chunk mesh; every other
        // one is left out of the queue.
        if (
            MESHED_WITHOUT_FOLIAGE.contains(key) ||
            HANDING_BACK.containsKey(key) ||
            TAKING_OVER.contains(key) ||
            isNear(sectionX, sectionZ)
        ) {
            DECISIONS.add(new MeshSwap(key));
        }
    }

    /**
     * Any thread: as {@link #onChunkMeshSwapped}, for a chunk mesh that took its place off the render thread. Vanilla
     * does that for a build with nothing to draw at all -- a section holding only foliage, once the foliage is left out
     * of it -- so it cannot be skipped. Only the sections the renderer holds geometry for are queued.
     */
    public static void onChunkMeshSwappedElsewhere(
        int sectionX,
        int sectionY,
        int sectionZ
    ) {
        long key = SectionPos.asLong(sectionX, sectionY, sectionZ);
        if (GPU_READY.contains(key)) {
            DECISIONS.add(new MeshSwap(key));
        }
    }

    /**
     * Render thread: the GPU renderer now holds uploaded geometry for this section. Returns whether it did not before,
     * in which case a near section's chunk mesh is due to be built again without its foliage.
     */
    static boolean markGpuReady(long sectionKey) {
        if (!GPU_READY.add(sectionKey)) {
            return false;
        }
        generation++;
        // A near section whose chunk mesh still holds the foliage is asked to be built again right away, but a build of
        // it may already be under way: one that started before this, found nothing ready and keeps the foliage. Sodium
        // folds the request into that build rather than starting another, and the decision it reports was recorded
        // before the section was ready, so nothing would ask again. Waiting for that build to be on screen, as for any
        // other build that kept its foliage by mistake, asks again once it is.
        if (
            isNear(SectionPos.x(sectionKey), SectionPos.z(sectionKey)) &&
            !MESHED_WITHOUT_FOLIAGE.contains(sectionKey) &&
            !TAKING_OVER.contains(sectionKey)
        ) {
            NEEDS_REBUILD.add(sectionKey);
        }
        return true;
    }

    /** Render thread: the GPU renderer no longer holds geometry for this section. */
    static void unmarkGpuReady(long sectionKey) {
        if (GPU_READY.remove(sectionKey)) {
            generation++;
        }
        AWAITING_GROUP.remove(sectionKey);
    }

    /** Whether the GPU renderer holds uploaded geometry for this section. */
    static boolean isGpuReady(long sectionKey) {
        return GPU_READY.contains(sectionKey);
    }

    /** Which list of foliage a section meshed now is built with; see AWAITING_GROUP. */
    static int listGeneration() {
        return listGeneration;
    }

    /**
     * Render thread: a whitelist group was switched on and the renderer already draws this section, without the group's
     * blocks. The chunk mesh keeps them until {@link #groupTakenOver} says the renderer holds them.
     */
    static void awaitGroup(long sectionKey) {
        AWAITING_GROUP.add(sectionKey);
    }

    /**
     * Render thread: the renderer has uploaded this section, meshed with the list of that generation. Returns whether it
     * was waiting for a group's blocks and now holds them, in which case its chunk mesh is due to be built again without.
     */
    static boolean groupTakenOver(long sectionKey, int meshedGeneration) {
        return (
            meshedGeneration == listGeneration &&
            AWAITING_GROUP.remove(sectionKey)
        );
    }

    /**
     * Render thread: hands over each near section whose build has just come back, and is on screen, still holding
     * its foliage, so a rebuild of it can be asked for.
     */
    static void forEachRebuildDue(LongConsumer rebuild) {
        for (int i = 0; i < REBUILD_NOW.size(); i++) {
            rebuild.accept(REBUILD_NOW.getLong(i));
        }
        REBUILD_NOW.clear();
    }

    /**
     * Render thread: the renderer is to mesh this section again once its new chunk mesh is on screen, the chunk mesh
     * having just been asked for; see REMESH_ON_SWAP.
     */
    static void remeshOnSwap(long sectionKey) {
        REMESH_ON_SWAP.put(
            sectionKey,
            System.currentTimeMillis() + REMESH_GIVE_UP_MILLIS
        );
    }

    /** Render thread: hands over each section the renderer is to mesh again now, its chunk mesh in place or long due. */
    static void forEachRemeshDue(LongConsumer remesh) {
        if (!REMESH_ON_SWAP.isEmpty()) {
            long now = System.currentTimeMillis();
            var entries = REMESH_ON_SWAP.long2LongEntrySet().fastIterator();
            while (entries.hasNext()) {
                var entry = entries.next();
                if (now > entry.getLongValue()) {
                    REMESH_NOW.add(entry.getLongKey());
                    entries.remove();
                }
            }
        }
        for (int i = 0; i < REMESH_NOW.size(); i++) {
            remesh.accept(REMESH_NOW.getLong(i));
        }
        REMESH_NOW.clear();
    }

    /** Render thread: whether the chunk mesh on screen for this section went without its foliage. */
    static boolean chunkMeshLeftFoliage(long sectionKey) {
        if (MESHED_WITHOUT_FOLIAGE.contains(sectionKey)) {
            return true;
        }
        // Handed back, but the chunk mesh holding the foliage again is not on screen yet.
        long giveUp = HANDING_BACK.get(sectionKey);
        if (giveUp == 0L) {
            return false;
        }
        if (System.currentTimeMillis() > giveUp) {
            HANDING_BACK.remove(sectionKey);
            return false;
        }
        return true;
    }

    //? >=1.21.11 {
    /** Remembers a model the mod wrapped, so the renderer can mesh from it. */
    public static void registerGpuModel(
        BlockState state,
        BlockStateModel model
    ) {
        GPU_MODELS.put(state, model);
    }

    /** The model to mesh this block from: the one the mod wrapped where there is one, or vanilla's. */
    static BlockStateModel modelFor(
        BlockState state,
        BlockStateModel fallback
    ) {
        BlockStateModel model = GPU_MODELS.get(state);
        return model == null ? fallback : model;
    }

    //?} else {
    /*/^* Remembers a model the mod wrapped, so the renderer can mesh from it. ^/
	public static void registerGpuModel(BlockState state, BakedModel model) {
		GPU_MODELS.put(state, model);
	}

	/^* The model to mesh this block from: the one the mod wrapped where there is one, or vanilla's. ^/
	static BakedModel modelFor(BlockState state, BakedModel fallback) {
		BakedModel model = GPU_MODELS.get(state);
		return model == null ? fallback : model;
	}
	*/ //?}

    /** Render thread: the section was unloaded, and its next build has to be recorded afresh. */
    static void forgetSection(long sectionKey) {
        HANDING_BACK.remove(sectionKey);
        REMESH_ON_SWAP.remove(sectionKey);
        NEEDS_REBUILD.remove(sectionKey);
        TAKING_OVER.remove(sectionKey);
        if (MESHED_WITHOUT_FOLIAGE.remove(sectionKey)) {
            generation++;
        }
    }

    /** Whether the GPU renderer draws the foliage of this chunk column. */
    static boolean isNear(int chunkX, int chunkZ) {
        Area current = area;
        return (
            current != null &&
            (current.contains(chunkX, chunkZ) ||
                (!PLOT_COLUMNS.isEmpty() &&
                    PLOT_COLUMNS.contains(columnKey(chunkX, chunkZ))))
        );
    }

    static boolean hasArea() {
        return area != null;
    }

    static int centreX() {
        Area current = area;
        return current == null ? 0 : current.centreX();
    }

    static int centreZ() {
        Area current = area;
        return current == null ? 0 : current.centreZ();
    }

    static int radius() {
        Area current = area;
        return current == null ? 0 : current.radius();
    }

    /** Render thread: moves the area, published as one object so no thread ever reads half of a move. */
    static void setArea(int centreX, int centreZ, int radius) {
        area = new Area(centreX, centreZ, radius);
        generation++;
    }

    /** Render thread: no area at all, so every chunk build keeps its foliage. */
    static void clearArea() {
        area = null;
        HANDING_BACK.clear();
        NEEDS_REBUILD.clear();
        REBUILD_NOW.clear();
        TAKING_OVER.clear();
        REMESH_ON_SWAP.clear();
        REMESH_NOW.clear();
        AWAITING_GROUP.clear();
        PLOT_COLUMNS.clear();
        generation++;
    }

    /**
     * Whether this block is foliage the GPU renderer draws: every block Sway animates, the vanilla foliage it
     * registers itself plus whatever {@link io.github.andrewcwhy.allthingsfoliage.ModCompatRegistry} found, and the
     * blocks of the waving whitelist.
     * <p>
     * Resolved once and cached, because {@code isInteractive} walks Sway's registries; any thread may ask, and two
     * that find it gone at once both work it out the same.
     */
    public static boolean isFoliage(BlockState state) {
        Set<Block> known = foliage;
        if (known == null) {
            Set<Block> found = Collections.newSetFromMap(
                new IdentityHashMap<>()
            );
            for (Block block : BuiltInRegistries.BLOCK) {
                if (waves(block)) {
                    found.add(block);
                }
            }
            foliage = known = found;
        }
        return known.contains(state.getBlock());
    }

    /**
     * Whether Sway bends this block, which makes it foliage: its models are wrapped and the GPU renderer draws it. A
     * block Sway only pushes, with no deformation of its own -- Snow! Real Magic's snow, whose plant inside bends by its
     * own pipeline -- is not foliage; the plant inside it is.
     */
    public static boolean bends(Block block) {
        return (
            SwayAPI.isInteractive(block) &&
            !SwayAPI.getBehaviorPipeline(block)
                .getDeformationContributors()
                .isEmpty()
        );
    }

    /**
     * Whether the GPU renderer draws and waves this block, which makes it foliage: its models are wrapped so a chunk
     * mesher leaves it to the renderer. Every block Sway bends, and the blocks of the waving whitelist, which Sway
     * does not.
     */
    public static boolean waves(Block block) {
        return bends(block) || WavingWhitelist.contains(block);
    }

    /**
     * Whether the GPU renderer may ever draw this block, which is what its models are wrapped for as they are baked:
     * every block Sway bends and every block on the waving whitelist, its group on or off.
     */
    public static boolean mayWave(Block block) {
        return (
            bends(block) ||
            WavingWhitelist.listed(block) ||
            WavingWhitelist.restsOnLeaves(block)
        );
    }

    /**
     * Whether the block at this position is foliage the GPU renderer draws: foliage by its kind, or a block resting on
     * leaves while they wave -- snow on a tree -- which is foliage only there.
     */
    public static boolean isFoliageAt(
        BlockGetter level,
        BlockPos pos,
        BlockState state
    ) {
        // A section the renderer draws without this block yet keeps it in the chunk mesh; see AWAITING_GROUP.
        if (
            !AWAITING_GROUP.isEmpty() &&
            (WavingWhitelist.isLeaves(state.getBlock()) ||
                WavingWhitelist.restsOnLeaves(state.getBlock())) &&
            AWAITING_GROUP.contains(
                SectionPos.asLong(
                    SectionPos.blockToSectionCoord(pos.getX()),
                    SectionPos.blockToSectionCoord(pos.getY()),
                    SectionPos.blockToSectionCoord(pos.getZ())
                )
            )
        ) {
            return false;
        }
        if (isFoliage(state)) {
            return true;
        }
        // Snow is everywhere in a cold biome, and is only ever the renderer's while the leaves wave and near the player:
        // the block under it is only looked at then, and there.
        if (
            !WavingWhitelist.leavesWave() ||
            !WavingWhitelist.restsOnLeaves(state.getBlock())
        ) {
            return false;
        }
        Area current = area;
        return (
            current != null &&
            current.contains(
                SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getZ())
            ) &&
            WavingWhitelist.isLeaves(
                level.getBlockState(pos.below()).getBlock()
            )
        );
    }

    /** Whether this is a block resting on a leaf while the leaves wave, drawn and waved with them; see isFoliageAt. */
    public static boolean restsOnWavingLeaves(
        BlockGetter level,
        BlockPos pos,
        BlockState state
    ) {
        return (
            WavingWhitelist.leavesWave() &&
            WavingWhitelist.restsOnLeaves(state.getBlock()) &&
            WavingWhitelist.isLeaves(
                level.getBlockState(pos.below()).getBlock()
            )
        );
    }

    /**
     * Forgets which blocks are foliage, after a whitelist group was switched on or off. The chunks have to be built
     * again after it, the chunk mesh's and the renderer's.
     */
    public static void foliageChanged() {
        foliage = null;
        listGeneration++;
    }

    /**
     * Whether the GPU renderer may have anything to draw at a block like this: foliage, snow holding a plant, or a block
     * that may rest on leaves while they wave.
     */
    static boolean mayHoldFoliage(BlockState state) {
        return (
            isFoliage(state) ||
            SnowRealMagicCompat.isSnowBlock(state) ||
            (WavingWhitelist.leavesWave() &&
                WavingWhitelist.restsOnLeaves(state.getBlock()))
        );
    }

    private static Class<?> findClass(String name) {
        try {
            return Class.forName(
                name,
                false,
                GpuFoliageSplit.class.getClassLoader()
            );
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
//?}
