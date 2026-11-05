package io.github.andrewcwhy.allthingsfoliage.gpu;

//? >=1.20.1 {

import com.github.razorplay01.sway.client.SwayData;
import com.github.razorplay01.sway.client.SwayEngine;
import com.github.razorplay01.sway.config.SwayConfig;
//?} else {
/*import com.mojang.blaze3d.buffers.GpuBuffer;
 */ //?}
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
//?} else {
/*import com.mojang.blaze3d.shaders.UniformType;
 */ //?}
import com.mojang.blaze3d.systems.RenderSystem;
//? >=1.21.11 {
//? >=26.3 {
import com.mojang.renderpearl.api.buffers.GpuBuffer;
//?}
//? >=26.2 {
//? >=26.3 {
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
//?} else {
/*import com.mojang.blaze3d.pipeline.BindGroupLayout;
 */ //?}
//?}
//? >=1.21.11 {
//? >=26.3 {
import com.mojang.renderpearl.api.pipeline.UniformType;
//?}
import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage;
//?} else {
/*import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
*/ //?}

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
//? >=1.21.11 {
import org.lwjgl.system.MemoryStack;

/**
 * Hands Sway's plant pushes to the foliage shader, so plants near the player lean away from whoever walks
 * through them without a single section being meshed again.
 * <p>
 * Sway still decides which plants are pushed and how hard, by its own rules and settings. Its force for each
 * plant is followed here through a damped spring, so a plant leans in smoothly and rocks back when Sway lets
 * go, and is sent with the plant's anchor block: every vertex of a plant reads the same force, and the whole
 * plant leans as one piece. The shape of the bend lives in the shader.
 * <p>
 * While the GPU renderer draws the foliage, entities push plants harder and from further away than Sway's
 * settings say, so the push holds its own next to the waving. Without it, Sway's settings apply unchanged.
 */
public final class GpuFoliageInteraction {

    /** How many plant pushes are sent. The nearest win. */
    static final int MAX_CELLS = 128;

    /** The name of the uniform block the shader reads these pushes from. */
    static final String UNIFORM = "FoliageInteraction";

    //? >=26.2 {
    /** From 26.2 a pipeline declares its uniforms in bind group layouts. */
    static final BindGroupLayout LAYOUT = BindGroupLayout.builder()
        .withUniform(UNIFORM, UniformType.UNIFORM_BUFFER)
        .build();
    //?}

    //? >=1.21.11 {
    private static final int BUFFER_SIZE = bufferSize();
    private static GpuBuffer buffer;
    //?}

    /**
     * How many times wider an entity counts as when it pushes plants, along x and z. Its height is kept. Only
     * plant pushes see it; the entity's real hitbox is never changed.
     * <p>
     * Sway only looks for plants within 2 blocks of an entity's centre, so beyond about 6 a player reaches no
     * further.
     */
    public static final double PLANT_HITBOX_SCALE = 1.5D;
    /**
     * How many times harder than Sway's own push plants are pushed. It is applied inside Sway's push itself, so
     * whoever draws the foliage -- the chunk mesh or the GPU renderer -- moves it by the very same amount.
     */
    private static final float PUSH_BOOST = 1.5F;
    /** Sway's own cap on how hard a plant can be pushed, as a multiple of its intensity setting. */
    private static final float SWAY_PUSH_CAP = 2.0F;
    /** The top of the config screen's intensity slider, which bounds how far the GPU renderer lets a plant lean. */
    private static final float MAX_SLIDER_INTENSITY = 2.0F;

    /** How hard a plant is pulled towards Sway's push. About 1.5 swings a second. */
    private static final float CELL_STIFFNESS = 90.0F;
    /** How much of its swing a plant loses each second; lower rocks longer. */
    private static final float CELL_DAMPING = 7.0F;
    /** Below this, in both push and speed, a plant Sway has let go of counts as settled and is dropped. */
    private static final float CELL_SETTLED = 0.005F;

    /**
     * Sway's forces by block, as it computes them each frame. Sway has no API for reading them all, so its
     * internal map is reached by reflection. Empty if it cannot be found, and plants then simply do not react.
     */
    private static final Map<BlockPos, SwayData> SWAY_FORCES = findSwayForces();

    /** Sway's forces by block, read-only; see SWAY_FORCES. */
    static Map<BlockPos, SwayData> swayForces() {
        return SWAY_FORCES;
    }

    /** A plant Sway is pushing, or recently was, with its spring. */
    private static final class Cell {

        final BlockPos pos;
        float pushX;
        float pushZ;
        float velocityX;
        float velocityZ;
        float targetX;
        float targetZ;
        boolean touched;
        double distanceSq;

        Cell(BlockPos pos) {
            this.pos = pos;
        }
    }

    private static final Map<BlockPos, Cell> CELLS = new HashMap<>();
    /** How far from where it is drawn from a pushed plant can be and still be sent, squared: 256 blocks. */
    private static final double CELL_REACH_SQ = 256.0D * 256.0D;
    private static final List<Cell> NEAREST = new ArrayList<>();
    private static final Comparator<Cell> BY_DISTANCE =
        Comparator.comparingDouble(cell -> cell.distanceSq);

    private static final float[] CELL_POSITIONS = new float[MAX_CELLS * 4];
    private static final float[] CELL_FORCES = new float[MAX_CELLS * 4];
    /** Min then max corner of the cells sent, relative to the camera. */
    private static final float[] CELL_BOUNDS = new float[6];

    private static long lastNanos;

    private GpuFoliageInteraction() {}

    /** The box an entity pushes plants with: a wider copy of its hitbox, so the entity's own is never changed. */
    public static AABB plantHitbox(AABB hitbox) {
        double growX =
            (hitbox.maxX - hitbox.minX) * (PLANT_HITBOX_SCALE - 1.0D) * 0.5D;
        double growZ =
            (hitbox.maxZ - hitbox.minZ) * (PLANT_HITBOX_SCALE - 1.0D) * 0.5D;
        return hitbox.inflate(growX, 0.0D, growZ);
    }

    /**
     * Sway's intensity as its push reads it, raised. Only that read is changed: the setting itself, and the cap
     * Sway puts on a plant's push, which it takes from the setting separately, stay as the player set them.
     */
    public static float plantPushIntensity(float intensity) {
        return intensity * PUSH_BOOST;
    }

    /** Forgets every plant, as when the world is swapped. */
    static void reset() {
        CELLS.clear();
        lastNanos = 0L;
    }

    /** Moves every spring on and gathers this frame's pushes, nearest first. Returns how many there are. */
    private static int update(Vec3 camera) {
        long now = System.nanoTime();
        // Real time rather than game time, so a plant rocks the same at any frame rate.
        float delta =
            lastNanos == 0L ? 0.0F : Math.min((now - lastNanos) / 1.0E9F, 0.1F);
        lastNanos = now;

        Arrays.fill(CELL_POSITIONS, 0.0F);
        Arrays.fill(CELL_FORCES, 0.0F);
        Arrays.fill(CELL_BOUNDS, 0.0F);
        // Sway's own interaction switch turns it off.
        if (!SwayConfig.INSTANCE.enabled) {
            CELLS.clear();
            return 0;
        }
        followSway(delta);
        return collectCells(camera);
    }

    //? >=1.21.11 {
    /** Writes this frame's pushes. Render thread only, and before any render pass is opened. */
    static GpuBuffer upload(Vec3 camera) {
        int cellCount = update(camera);
        if (buffer == null) {
            buffer = RenderSystem.getDevice().createBuffer(
                () -> "MC2 foliage interaction",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                BUFFER_SIZE
            );
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Std140Builder data = Std140Builder.onStack(stack, BUFFER_SIZE)
                .putInt(cellCount)
                .putVec4(CELL_BOUNDS[0], CELL_BOUNDS[1], CELL_BOUNDS[2], 0.0F)
                .putVec4(CELL_BOUNDS[3], CELL_BOUNDS[4], CELL_BOUNDS[5], 0.0F);
            putAll(data, CELL_POSITIONS);
            putAll(data, CELL_FORCES);
            RenderSystem.getDevice()
                .createCommandEncoder()
                .writeToBuffer(buffer.slice(), data.get());
        }
        return buffer;
    }

    //? >=26.3 {
    /**
     * The pushes as last written, or null before any were: for drawing the foliage again in the same frame, as into a
     * shader pack's shadow map, without moving the plants' springs a second time.
     */
    static GpuBuffer uploaded() {
        return buffer;
    }

    //?}
    //?}

    /** Moves every plant's spring towards Sway's current push, or back to upright once Sway lets go. */
    private static void followSway(float delta) {
        for (Cell cell : CELLS.values()) {
            cell.touched = false;
            cell.targetX = 0.0F;
            cell.targetZ = 0.0F;
        }
        followForces(SWAY_FORCES);
        //? <1.21.11 {
        /*followForces(PLOT_FORCES);
         */ //?}
        stepSprings(delta);
    }

    /** Aims every spring Sway pushes at its push: its own forces, or the ones worked out for a sub-level's plants. */
    private static void followForces(Map<BlockPos, SwayData> forces) {
        for (Map.Entry<BlockPos, SwayData> entry : forces.entrySet()) {
            SwayData force = entry.getValue();
            if (force.intensity < 0.01F) {
                continue;
            }
            Cell cell = CELLS.get(entry.getKey());
            if (cell == null) {
                cell = new Cell(entry.getKey().immutable());
                CELLS.put(cell.pos, cell);
            }
            cell.touched = true;
            // Already raised inside Sway's push, as the chunk mesh sees it.
            cell.targetX = force.nx * force.intensity;
            cell.targetZ = force.nz * force.intensity;
        }
    }

    private static void stepSprings(float delta) {
        Iterator<Cell> iterator = CELLS.values().iterator();
        while (iterator.hasNext()) {
            Cell cell = iterator.next();
            // Velocity first, then position: stable at every frame time this sees, which is capped at 0.1 s.
            cell.velocityX +=
                ((cell.targetX - cell.pushX) * CELL_STIFFNESS -
                    cell.velocityX * CELL_DAMPING) *
                delta;
            cell.velocityZ +=
                ((cell.targetZ - cell.pushZ) * CELL_STIFFNESS -
                    cell.velocityZ * CELL_DAMPING) *
                delta;
            cell.pushX += cell.velocityX * delta;
            cell.pushZ += cell.velocityZ * delta;
            if (
                !cell.touched &&
                Math.abs(cell.pushX) < CELL_SETTLED &&
                Math.abs(cell.pushZ) < CELL_SETTLED &&
                Math.abs(cell.velocityX) < CELL_SETTLED &&
                Math.abs(cell.velocityZ) < CELL_SETTLED
            ) {
                iterator.remove();
            }
        }
    }

    /**
     * The plants nearest the camera, with the box around them. Returns how many.
     * <p>
     * Each is sent with the most it may lean, which is the cap Sway itself puts on a plant's push, so a plant the
     * GPU renderer draws can lean exactly as far as one in the chunk mesh. It stops where the config screen's
     * slider does: Sway accepts more from a hand-edited file, which would push plants out past the room the
     * renderer leaves for them when culling.
     */
    private static int collectCells(Vec3 camera) {
        float limit =
            SWAY_PUSH_CAP *
            Math.min(SwayConfig.INSTANCE.intensity, MAX_SLIDER_INTENSITY);
        NEAREST.clear();
        for (Cell cell : CELLS.values()) {
            double dx = cell.pos.getX() + 0.5D - camera.x;
            double dy = cell.pos.getY() + 0.5D - camera.y;
            double dz = cell.pos.getZ() + 0.5D - camera.z;
            cell.distanceSq = dx * dx + dy * dy + dz * dz;
            // A sub-level's plants are kept far from everything, and are no nearer the camera than the world's are the
            // place a sub-level is drawn from: each is left out of the other's.
            if (cell.distanceSq < CELL_REACH_SQ) {
                NEAREST.add(cell);
            }
        }
        NEAREST.sort(BY_DISTANCE);

        int count = Math.min(NEAREST.size(), MAX_CELLS);
        for (int i = 0; i < count; i++) {
            Cell cell = NEAREST.get(i);
            float x = (float) (cell.pos.getX() - camera.x);
            float y = (float) (cell.pos.getY() - camera.y);
            float z = (float) (cell.pos.getZ() - camera.z);
            CELL_POSITIONS[i * 4] = x;
            CELL_POSITIONS[i * 4 + 1] = y;
            CELL_POSITIONS[i * 4 + 2] = z;
            CELL_FORCES[i * 4] = cell.pushX;
            CELL_FORCES[i * 4 + 1] = cell.pushZ;
            CELL_FORCES[i * 4 + 2] = limit;
            if (i == 0) {
                CELL_BOUNDS[0] = CELL_BOUNDS[3] = x;
                CELL_BOUNDS[1] = CELL_BOUNDS[4] = y;
                CELL_BOUNDS[2] = CELL_BOUNDS[5] = z;
            } else {
                CELL_BOUNDS[0] = Math.min(CELL_BOUNDS[0], x);
                CELL_BOUNDS[1] = Math.min(CELL_BOUNDS[1], y);
                CELL_BOUNDS[2] = Math.min(CELL_BOUNDS[2], z);
                CELL_BOUNDS[3] = Math.max(CELL_BOUNDS[3], x);
                CELL_BOUNDS[4] = Math.max(CELL_BOUNDS[4], y);
                CELL_BOUNDS[5] = Math.max(CELL_BOUNDS[5], z);
            }
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private static Map<BlockPos, SwayData> findSwayForces() {
        try {
            Field current = SwayEngine.class.getDeclaredField("CURRENT");
            current.setAccessible(true);
            return (Map<BlockPos, SwayData>) current.get(null);
        } catch (ReflectiveOperationException | RuntimeException e) {
            AllThingsFoliage.LOGGER.warn(
                "Could not read Sway's forces; foliage drawn on the GPU will not react to entities",
                e
            );
            return Map.of();
        }
    }

    //? <1.21.11 {
    /*/^*
	 * The {@code FoliageInteraction} block laid out by hand, the same way std140 lays it out on newer versions: a
	 * count, then vec4s from byte 16 on, the positions and then the forces.
	 ^/
	private static final int LEGACY_VEC4_START = 16;
	private static final int LEGACY_BUFFER_SIZE = LEGACY_VEC4_START + (2 + MAX_CELLS * 2) * 16;

	private static ByteBuffer legacyData;
	private static int legacyBuffer;

	/^*
	 * Writes this frame's pushes into a uniform buffer and binds it where the foliage shader reads them. Render
	 * thread only. Before 1.21.11 vanilla has no uniform buffers of its own, so this is plain OpenGL; the block
	 * is too large for the loose uniforms a shader's JSON can declare.
	 ^/
	static void uploadLegacy(Vec3 camera, int binding) {
		ByteBuffer data = legacyCells(camera);
		if (legacyBuffer == 0) {
			legacyBuffer = GlStateManager._glGenBuffers();
			GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, legacyBuffer);
			GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, LEGACY_BUFFER_SIZE, GL15.GL_DYNAMIC_DRAW);
		}
		GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, legacyBuffer);
		GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0L, data);
		GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, binding, legacyBuffer);
	}

	/^*
	 * Sway's pushes on the plants of Sable's sub-levels, worked out again every frame. Sway looks for plants around
	 * each entity where the entity is, and a sub-level's plants are kept somewhere else altogether, in its plot: so
	 * the same is asked of Sway for an entity standing where this one would be inside the plot, and what it answers
	 * is followed beside its own forces. The plants' places are the plot's, which are no plant's in the world.
	 ^/
	private static final Map<BlockPos, SwayData> PLOT_FORCES = new HashMap<>();
	private static java.lang.reflect.Method processBlock;
	private static boolean processBlockSought;
	private static net.minecraft.world.entity.Entity standIn;

	/^* Forgets last frame's pushes on the sub-levels' plants, before this frame's are worked out. ^/
	static void beginPlotForces() {
		PLOT_FORCES.clear();
	}

	/^*
	 * Works out an entity's pushes on the plants its box reaches in a plot: the box as the sub-level's pose puts it
	 * there, which for a sub-level lying on its side is the entity lying across the plot. Sway reads nothing of an
	 * entity but its box while it does, so a stand-in that is in no level is given that box and handed to Sway's own
	 * working-out of a block -- which is not part of its API, and is reached by name: without it the plants are not
	 * pushed.
	 ^/
	static void pushPlot(net.minecraft.client.multiplayer.ClientLevel level, AABB box) {
		try {
			if (!processBlockSought) {
				processBlockSought = true;
				processBlock = SwayEngine.class.getDeclaredMethod("processBlock", BlockPos.MutableBlockPos.class,
						net.minecraft.client.multiplayer.ClientLevel.class, net.minecraft.world.entity.Entity.class,
						com.github.razorplay01.sway.api.behavior.context.SwayBehaviorContext.class, Map.class, float.class,
						float.class);
				processBlock.setAccessible(true);
			}
			if (processBlock == null) {
				return;
			}
			if (standIn == null || standIn.level() != level) {
				standIn = new net.minecraft.world.entity.Marker(net.minecraft.world.entity.EntityType.MARKER, level);
			}
			standIn.setPos((box.minX + box.maxX) * 0.5D, box.minY, (box.minZ + box.maxZ) * 0.5D);
			standIn.setBoundingBox(box);
			var context = new com.github.razorplay01.sway.api.behavior.context.SwayBehaviorContext(level, standIn,
					SwayConfig.INSTANCE, 0.016F);
			// Every block the box comes within Sway's reach of. Its own update looks around an entity's feet and well
			// above them, for an entity standing; here the box may lie any way, so it is its whole length.
			float radius = SwayConfig.INSTANCE.influenceRadius;
			int reach = (int) Math.ceil(radius);
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			for (int x = (int) Math.floor(box.minX - reach); x <= (int) Math.floor(box.maxX + reach); x++) {
				for (int z = (int) Math.floor(box.minZ - reach); z <= (int) Math.floor(box.maxZ + reach); z++) {
					for (int y = (int) Math.floor(box.minY - reach); y <= (int) Math.floor(box.maxY + reach); y++) {
						pos.set(x, y, z);
						processBlock.invoke(null, pos, level, standIn, context, PLOT_FORCES, radius,
								SwayConfig.INSTANCE.intensity);
					}
				}
			}
		} catch (ReflectiveOperationException | RuntimeException e) {
			processBlock = null;
			AllThingsFoliage.LOGGER.warn("Sway's forces can't be worked out for the plants of Sable's sub-levels; they sway "
					+ "but are not pushed", e);
		}
	}

	/^*
	 * This frame's pushes laid out as the block of them is, in memory that stays where it is: for the uniform buffer
	 * above, and for VulkanMod, which reads them from there itself. Render thread only.
	 ^/
	static ByteBuffer legacyCells(Vec3 camera) {
		int cellCount = update(camera);
		if (legacyData == null) {
			legacyData = MemoryUtil.memCalloc(LEGACY_BUFFER_SIZE);
		}
		ByteBuffer data = legacyData;
		data.putInt(0, cellCount);
		putVec4(data, 0, CELL_BOUNDS[0], CELL_BOUNDS[1], CELL_BOUNDS[2], 0.0F);
		putVec4(data, 1, CELL_BOUNDS[3], CELL_BOUNDS[4], CELL_BOUNDS[5], 0.0F);
		for (int i = 0; i < MAX_CELLS; i++) {
			int at = i * 4;
			putVec4(data, 2 + i,
					CELL_POSITIONS[at], CELL_POSITIONS[at + 1], CELL_POSITIONS[at + 2], CELL_POSITIONS[at + 3]);
			putVec4(data, 2 + MAX_CELLS + i,
					CELL_FORCES[at], CELL_FORCES[at + 1], CELL_FORCES[at + 2], CELL_FORCES[at + 3]);
		}
		return data;
	}

	private static void putVec4(ByteBuffer data, int vec4, float x, float y, float z, float w) {
		int at = LEGACY_VEC4_START + vec4 * 16;
		data.putFloat(at, x).putFloat(at + 4, y).putFloat(at + 8, z).putFloat(at + 12, w);
	}
	*/ //?}

    //? >=1.21.11 {
    private static void putAll(Std140Builder data, float[] vectors) {
        for (int i = 0; i < vectors.length; i += 4) {
            data.putVec4(
                vectors[i],
                vectors[i + 1],
                vectors[i + 2],
                vectors[i + 3]
            );
        }
    }

    /** Matches the block in the shader: a count, two bounds, then positions and forces. */
    private static int bufferSize() {
        Std140SizeCalculator size = new Std140SizeCalculator()
            .putInt()
            .putVec4()
            .putVec4();
        for (int i = 0; i < MAX_CELLS * 2; i++) {
            size.putVec4();
        }
        return size.get();
    }
    //?}
}
//?}
