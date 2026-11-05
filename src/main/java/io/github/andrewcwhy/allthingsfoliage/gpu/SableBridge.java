package io.github.andrewcwhy.allthingsfoliage.gpu;

//? >=1.20.1 {

import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * What the renderer asks of Sable, whose sub-levels are pieces of the world that move: a felled tree, a ship.
 * <p>
 * A sub-level's blocks are not where it is seen. They are kept in a plot of the same level, far from everything, and
 * drawn where the sub-level is by its pose: a position, a rotation, and the point of the plot it turns about. Sable
 * builds a plot's sections with the game's own section compiler -- with Sodium installed too -- so they reach the
 * foliage's models as any chunk build does, and it draws them with the game's own vertex buffers under that pose.
 * <p>
 * So the foliage of a sub-level is the renderer's the same way the world's is, with two things said here: whether a
 * section lies in a plot, which no distance from the player says, and the pose it is to be drawn under.
 * <p>
 * Everything is asked of Sable Companion, the small library Sable publishes for other mods and answers through, by
 * name: the mod is not compiled against it. Whatever is not found leaves a sub-level's foliage in Sable's own meshes.
 */
final class SableBridge {

    static final boolean LOADED = AllThingsFoliage.xplat().isModLoaded("sable");
    /**
     * Whether what happens to a sub-level's foliage is written to the log: while a file of this name stands in the
     * game's folder. For a report: make the file, fell a tree, send the log.
     */
    static final boolean LOG =
        LOADED &&
        java.nio.file.Files.exists(java.nio.file.Path.of("mc2-foliage-sable"));
    private static volatile boolean failed;

    private SableBridge() {}

    /** Whether a chunk column lies where Sable keeps the blocks of its sub-levels. Any thread. */
    static boolean inPlot(int chunkX, int chunkZ) {
        if (!LOADED || failed) {
            return false;
        }
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return false;
        }
        try {
            return (boolean) Names.IN_PLOT.invoke(
                Names.COMPANION,
                level,
                chunkX,
                chunkZ
            );
        } catch (Throwable e) {
            fail(e);
            return false;
        }
    }

    /** The sub-level whose blocks are kept in this chunk column, as Sable's own object for it, or null. */
    static Object subLevelAt(int chunkX, int chunkZ) {
        if (!LOADED || failed) {
            return null;
        }
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }
        try {
            Object subLevel = Names.CONTAINING.invoke(
                Names.COMPANION,
                level,
                chunkX,
                chunkZ
            );
            return Names.CLIENT_SUB_LEVEL.isInstance(subLevel)
                ? subLevel
                : null;
        } catch (Throwable e) {
            fail(e);
            return null;
        }
    }

    /**
     * Where a sub-level is drawn this frame: a point of its plot goes to position + orientation * (point -
     * rotationPoint). Returns whether it was found.
     */
    static boolean pose(
        Object subLevel,
        Vector3d position,
        Quaterniond orientation,
        Vector3d rotationPoint
    ) {
        if (failed) {
            return false;
        }
        try {
            Object pose = Names.RENDER_POSE.invoke(subLevel);
            position.set((Vector3dc) Names.POSITION.invoke(pose));
            orientation.set((Quaterniondc) Names.ORIENTATION.invoke(pose));
            rotationPoint.set((Vector3dc) Names.ROTATION_POINT.invoke(pose));
            return true;
        } catch (Throwable e) {
            fail(e);
            return false;
        }
    }

    private static Method shadesByFacing;
    private static boolean shadesByFacingSought;

    /**
     * Whether Sable shades a sub-level's faces by where they look once it has turned, rather than as they were
     * meshed: its dynamic shading setting. It builds its meshes with no shade in them then, and so does the renderer.
     * This one is not Sable Companion's to say; where it cannot be asked the shade stays in the mesh.
     */
    static boolean shadesByFacing() {
        if (!LOADED) {
            return false;
        }
        try {
            if (!shadesByFacingSought) {
                shadesByFacingSought = true;
                shadesByFacing = Class.forName(
                    "dev.ryanhcode.sable.render.dynamic_shade.SableDynamicDirectionalShading"
                ).getMethod("isEnabled");
            }
            return (
                shadesByFacing != null && (boolean) shadesByFacing.invoke(null)
            );
        } catch (Throwable e) {
            shadesByFacing = null;
            return false;
        }
    }

    private static void fail(Throwable cause) {
        if (!failed) {
            failed = true;
            AllThingsFoliage.LOGGER.warn(
                "Sable is installed, but not a version the GPU foliage renderer can follow; the " +
                    "foliage of its sub-levels stays in Sable's own meshes",
                cause
            );
        }
    }

    /** Found the first time Sable is asked, when its classes are certainly loaded. */
    private static final class Names {

        static final Object COMPANION;
        static final Method IN_PLOT;
        static final Method CONTAINING;
        static final Class<?> CLIENT_SUB_LEVEL;
        static final Method RENDER_POSE;
        static final Method POSITION;
        static final Method ORIENTATION;
        static final Method ROTATION_POINT;

        static {
            try {
                String companion = "dev.ryanhcode.sable.companion.";
                Class<?> api = Class.forName(companion + "SableCompanion");
                COMPANION = api.getField("INSTANCE").get(null);
                IN_PLOT = api.getMethod(
                    "isInPlotGrid",
                    Level.class,
                    int.class,
                    int.class
                );
                CONTAINING = api.getMethod(
                    "getContaining",
                    Level.class,
                    int.class,
                    int.class
                );
                CLIENT_SUB_LEVEL = Class.forName(
                    companion + "ClientSubLevelAccess"
                );
                RENDER_POSE = CLIENT_SUB_LEVEL.getMethod("renderPose");
                Class<?> pose = Class.forName(companion + "math.Pose3dc");
                POSITION = pose.getMethod("position");
                ORIENTATION = pose.getMethod("orientation");
                ROTATION_POINT = pose.getMethod("rotationPoint");
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }
}
//?}
