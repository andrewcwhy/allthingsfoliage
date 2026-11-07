package io.github.andrewcwhy.allthingsfoliage.gpu.shader;

import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage;
import java.lang.reflect.Field;

/**
 * Keeps Iris from widening the mod's own meshing buffer, when Iris is installed.
 * <p>
 * While a shader pack is loaded and the level is being drawn, Iris rewrites every {@link
 * com.mojang.blaze3d.vertex.BufferBuilder} asked for {@code DefaultVertexFormat.BLOCK} into its own, wider
 * terrain format: it appends the tangents, mid-texture coordinates and block ids a shader pack expects.
 * That is right for anything drawn by a shader pack and wrong for the foliage renderer, which writes its
 * vertices for a pipeline of its own and reads them back at the block format's stride.
 * <p>
 * Iris leaves a way out for exactly this, a thread-local it checks before widening anything, so meshing is
 * wrapped in {@link #begin()} and {@link #end(boolean)}. It is reached by reflection so the mod neither
 * compiles against Iris nor needs it; without Iris, and if the field ever moves, both calls do nothing.
 */
public final class IrisVertexExtension {

    private static final String IMMEDIATE_STATE =
        "net.irisshaders.iris.vertices.ImmediateState";

    private static final ThreadLocal<Boolean> SKIP_EXTENSION;
    /**
     * Iris's own word for the level being drawn, which it widens buffers only under. Up to Iris 1.11.6 it also held while
     * a frame was extracted, when the renderer meshes on 26.3; from 1.11.7 it holds only while the level is drawn, so
     * {@link #allow()} raises it for as long as the renderer meshes for a shader pack. Null where it could not be found.
     */
    private static final Field RENDERING_LEVEL;
    /** What {@link #allow()} found Iris's word at, for {@link #end(boolean)} to put back; null when it did not raise it. */
    private static Boolean renderingLevelBefore;

    static {
        ThreadLocal<Boolean> skipExtension = null;
        Field renderingLevel = null;
        // Before 1.21.1 Iris has no way out, and needs none: it widens buffers exactly while a shader pack is loaded, and
        // while one is the renderer only meshes to draw through it.

        boolean hasWayOut = true;

        if (hasWayOut && AllThingsFoliage.xplat().isModLoaded("iris")) {
            try {
                Field field = Class.forName(IMMEDIATE_STATE).getField(
                    "skipExtension"
                );
                @SuppressWarnings("unchecked")
                ThreadLocal<Boolean> found = (ThreadLocal<Boolean>) field.get(
                    null
                );
                skipExtension = found;
            } catch (ReflectiveOperationException | ClassCastException e) {
                AllThingsFoliage.logger.warn(
                    "Iris is installed but its vertex format extension could not be turned " +
                        "off for the foliage renderer; drawing foliage on the GPU may fail while a shader " +
                        "pack is loaded",
                    e
                );
                skipExtension = null;
            }
            try {
                Field field = Class.forName(IMMEDIATE_STATE).getField(
                    "isRenderingLevel"
                );
                if (field.getType() == boolean.class) {
                    renderingLevel = field;
                }
            } catch (ReflectiveOperationException e) {
                // Left as Iris has it: foliage is only widened where Iris says the level is being drawn.
            }
        }
        SKIP_EXTENSION = skipExtension;
        RENDERING_LEVEL = renderingLevel;
    }

    private IrisVertexExtension() {}

    /**
     * Prevents Iris from expanding render buffers on the current thread.
     *
     * @return the previous extension state, which must be restored using {@link #end(boolean)}
     */
    public static boolean begin() {
        if (SKIP_EXTENSION == null) {
            return false;
        }

        boolean previous = Boolean.TRUE.equals(SKIP_EXTENSION.get());
        SKIP_EXTENSION.set(true);

        return previous;
    }

    /**
     * The opposite of {@link #begin()}: lets Iris widen buffers on this thread, which it does while a shader pack is
     * loaded and the level is being drawn. The renderer meshes this way when it draws through the pack, whose programs
     * read that wider format. The renderer meshes before the level is drawn, so Iris is told it is being drawn meanwhile.
     * Returns what to hand back to {@link #end(boolean)}. Render thread only.
     */
    public static boolean allow() {
        if (SKIP_EXTENSION == null) {
            return false;
        }

        boolean previous = Boolean.TRUE.equals(SKIP_EXTENSION.get());
        SKIP_EXTENSION.set(false);

        if (RENDERING_LEVEL != null && renderingLevelBefore == null) {
            try {
                renderingLevelBefore = RENDERING_LEVEL.getBoolean(null);
                RENDERING_LEVEL.setBoolean(null, true);
            } catch (IllegalAccessException e) {
                renderingLevelBefore = null;
            }
        }

        return previous;
    }

    /** Puts back what {@link #begin()} or {@link #allow()} found, so nothing else that asked for the same is cut short. */
    public static void end(boolean previous) {
        if (SKIP_EXTENSION != null) {
            SKIP_EXTENSION.set(previous);
        }

        if (renderingLevelBefore != null) {
            try {
                RENDERING_LEVEL.setBoolean(null, renderingLevelBefore);
            } catch (IllegalAccessException e) {
                // The field was previously writable, so restoration should succeed.
            }

            renderingLevelBefore = null;
        }
    }
}
