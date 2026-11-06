package io.github.andrewcwhy.allthingsfoliage.gpu;

import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * What the mod needs from VulkanMod when VulkanMod owns the terrain, as {@link SodiumBridge} is for Sodium.
 * <p>
 * VulkanMod meshes and draws the chunks itself, with classes of its own that kept their names and shapes from 0.5 to
 * 0.6.8: a chunk build reads the level through its {@code RenderRegion}, and a finished build is put in place by
 * {@code TaskDispatcher.doSectionUpdate}. Both are reached by name, so the mod neither compiles against VulkanMod nor
 * needs it. It asks models for their geometry through its own implementation of Fabric's rendering API, which is how
 * the model wrapper keeps foliage out of its chunk meshes, and the vanilla call that asks for a section to be built
 * again is one it answers itself.
 * <p>
 * From 0.6.8 it stands in for the game's graphics device, so the renderer draws through it as it does through the
 * game's own, with three things to mind. Its shader converter takes no arrays in a uniform block and drops the
 * {@code out} of a function's parameter, so the foliage shaders use neither (see foliage.vsh and sway.glsl). It writes
 * a block's colours for any vertex consumer but its own with red and blue changed over, which the vertex writer
 * undoes. And it draws the chunks with shaders it reads from its own jar, which no resource pack replaces, so the
 * foliage is drawn with the mod's own shader rather than the terrain's.
 */
// Public for the mixin that reports VulkanMod's uploads.
public final class VulkanBridge {

    private static final String VULKAN = "net.vulkanmod.render.chunk.";
    private static final boolean LOADED = AllThingsFoliage.xplat().isModLoaded(
        "vulkanmod"
    );
    // From 0.6.8, which is to say from 1.21.11, it stands in for the game's graphics device, and the renderer draws
    // through it as through the game's own. Before that it stands in for OpenGL call by call and has none of the calls
    // the renderer draws with, so the foliage is drawn through VulkanMod itself (see VulkanLegacyDraw), which is
    // written against 0.6.7 on 1.21.1; on 1.20.1 its chunk builds are left alone and the foliage stays in them.
    
    private static final boolean DEVICE = true;

    private static final boolean SUPPORTED = true;
    
    private static final Class<?> RENDER_REGION =
        LOADED && SUPPORTED ? find(VULKAN + "build.RenderRegion") : null;
    private static boolean warned;

    private VulkanBridge() {}

    /** Whether VulkanMod draws the chunks, with shaders of its own rather than the game's. */
    static boolean drawsChunks() {
        return LOADED;
    }

    /**
     * Whether VulkanMod is installed in a version the renderer cannot draw through: the GPU renderer stays out
     * altogether then, and the foliage stays in VulkanMod's chunk meshes, where Sway moves it.
     */
    static boolean keepsRendererOut() {
        return LOADED && (!SUPPORTED || (!DEVICE && VulkanLegacyDraw.failed()));
    }

    /** Whether the foliage is drawn through VulkanMod's own pipelines rather than through the game's. */
    static boolean drawsThroughItself() {
        return LOADED && SUPPORTED && !DEVICE;
    }

    /**
     * Whether a block's colours reach a vertex consumer other than VulkanMod's own with red and blue changed over: it
     * packs them its own way where it writes a block's quads, which it does itself from 0.6.8.
     */
    static boolean swapsColours() {
        return LOADED && DEVICE;
    }

    /** Whether a model is asked for its geometry by one of VulkanMod's chunk builds. */
    static boolean isChunkBuild(Object level) {
        return RENDER_REGION != null && RENDER_REGION.isInstance(level);
    }

    /**
     * Called as VulkanMod has put a finished build in place of a section's chunk mesh, on the render thread. A result
     * that only sorts the see-through faces again is not a new build.
     */
    public static void onSectionUpdated(Object result) {
        if (Uploads.SECTION == null) {
            return;
        }
        try {
            if (
                Uploads.FULL_UPDATE != null &&
                !((boolean) Uploads.FULL_UPDATE.invokeExact(result))
            ) {
                return;
            }
            Object section = (Object) Uploads.SECTION.invokeExact(result);
            GpuFoliageSplit.onChunkMeshSwapped(
                (int) Uploads.X.invokeExact(section) >> 4,
                (int) Uploads.Y.invokeExact(section) >> 4,
                (int) Uploads.Z.invokeExact(section) >> 4
            );
        } catch (Throwable e) {
            if (!warned) {
                warned = true;
                AllThingsFoliage.LOGGER.warn(
                    "VulkanMod's chunk uploads can't be followed by the GPU foliage renderer; foliage " +
                        "may show twice or not at all for a moment as it changes hands",
                    e
                );
            }
        }
    }

    private static Class<?> find(String name) {
        try {
            return Class.forName(
                name,
                false,
                VulkanBridge.class.getClassLoader()
            );
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }

    /** Found the first time an upload is reported, when VulkanMod's classes are certainly loaded. */
    private static final class Uploads {

        static final MethodHandle SECTION;
        static final MethodHandle FULL_UPDATE;
        static final MethodHandle X;
        static final MethodHandle Y;
        static final MethodHandle Z;

        static {
            MethodHandle section = null;
            MethodHandle fullUpdate = null;
            MethodHandle x = null;
            MethodHandle y = null;
            MethodHandle z = null;
            try {
                Class<?> result = Class.forName(
                    VULKAN + "build.task.CompileResult"
                );
                Class<?> renderSection = Class.forName(
                    VULKAN + "RenderSection"
                );
                MethodHandles.Lookup lookup = MethodHandles.publicLookup();
                section = lookup
                    .findGetter(result, "renderSection", renderSection)
                    .asType(MethodType.methodType(Object.class, Object.class));
                MethodType offset = MethodType.methodType(int.class);
                MethodType erased = MethodType.methodType(
                    int.class,
                    Object.class
                );
                try {
                    x = lookup
                        .findVirtual(renderSection, "xOffset", offset)
                        .asType(erased);
                    y = lookup
                        .findVirtual(renderSection, "yOffset", offset)
                        .asType(erased);
                    z = lookup
                        .findVirtual(renderSection, "zOffset", offset)
                        .asType(erased);
                } catch (ReflectiveOperationException e) {
                    // Before 0.6.8 they are fields.
                    x = lookup
                        .findGetter(renderSection, "xOffset", int.class)
                        .asType(erased);
                    y = lookup
                        .findGetter(renderSection, "yOffset", int.class)
                        .asType(erased);
                    z = lookup
                        .findGetter(renderSection, "zOffset", int.class)
                        .asType(erased);
                }
                try {
                    fullUpdate = lookup
                        .findGetter(result, "fullUpdate", boolean.class)
                        .asType(
                            MethodType.methodType(boolean.class, Object.class)
                        );
                } catch (ReflectiveOperationException e) {
                    // Older builds have no such flag: every result is a build.
                    fullUpdate = null;
                }
            } catch (ReflectiveOperationException | LinkageError e) {
                section = null;
                AllThingsFoliage.LOGGER.warn(
                    "VulkanMod is installed, but not a version whose chunk uploads the GPU foliage " +
                        "renderer can follow",
                    e
                );
            }
            SECTION = section;
            FULL_UPDATE = fullUpdate;
            X = x;
            Y = y;
            Z = z;
        }
    }
}
