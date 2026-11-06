package io.github.andrewcwhy.allthingsfoliage;

import com.github.razorplay01.sway.config.SwayConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The mod's own settings, next to Sway's.
 * <p>
 * Sway's {@code SwayConfig} is written by serialising its own class, so anything added to its file would be
 * dropped on the next save. These live in a file of their own instead.
 */
public final class FoliageSettings {

    public static final float MIN_WAVING_INTENSITY = 0.5F;
    public static final float DEFAULT_WAVING_INTENSITY = 1.0F;
    public static final float MAX_WAVING_INTENSITY = 2.0F;

    /** How far from the player entities push plants, in blocks. Sway itself refuses anything past 32. */
    public static final float MIN_INTERACTION_RADIUS = 8.0F;
    /** A chunk's width, so the plants around the player's whole chunk respond. */
    public static final float DEFAULT_INTERACTION_RADIUS = 16.0F;
    public static final float MAX_INTERACTION_RADIUS = 32.0F;
    public static final float INTERACTION_RADIUS_STEP = 4.0F;
    /** Sway's own default for the radius, in every supported version. */
    private static final float SWAY_DEFAULT_RADIUS = 8.0F;

    private static final Path PATH = AllThingsFoliage.xplat()
        .configDir()
        .resolve(AllThingsFoliage.MOD_ID + ".json");
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();

    /** How far out from the player the GPU renderer draws the foliage, leaving the rest to the chunk mesh. */
    public enum GpuDistance {
        /** Only as far as entities can push plants: every plant that can move is drawn by the renderer. */
        @SerializedName("performance")
        PERFORMANCE("performance"),

        /** A share of the render distance that shrinks as it grows, since a sway is too small to see far off. */
        @SerializedName("adaptive")
        ADAPTIVE("adaptive"),

        /** Half of the render distance. */
        @SerializedName("half")
        HALF("half"),

        /** The whole render distance. */
        @SerializedName("full")
        FULL("full");

        private final String name;

        GpuDistance(String name) {
            this.name = name;
        }

        public String translationKey() {
            return "config.allthingsfoliage.gpu." + name;
        }
    }

    /**
     * Presets for the options that decide what the mod costs: the interaction, the renderer and its distance, the weather
     * wind and its distance, and the calm sway. How strongly things move -- the intensities and the interaction radius --
     * is a matter of taste and never set by one. An option a preset leaves null it does not touch; it is hidden under
     * that preset anyway, so it keeps whatever it was set to for when it shows again. Default is what the mod starts
     * with.
     */
    public enum Preset {
        /** The mod as it was before the GPU renderer: plants pushed in the chunk mesh, and nothing else. */
        CLASSIC("classic", true, false, null, false, null, false),
        LOW("low", true, true, GpuDistance.PERFORMANCE, false, null, true),
        MEDIUM(
            "medium",
            true,
            true,
            GpuDistance.ADAPTIVE,
            true,
            GpuDistance.PERFORMANCE,
            true
        ),
        DEFAULT(
            "default",
            true,
            true,
            GpuDistance.ADAPTIVE,
            true,
            GpuDistance.HALF,
            true
        ),
        HIGH(
            "high",
            true,
            true,
            GpuDistance.HALF,
            true,
            GpuDistance.HALF,
            true
        ),
        ULTRA(
            "ultra",
            true,
            true,
            GpuDistance.FULL,
            true,
            GpuDistance.FULL,
            true
        ),
        /** Everything the GPU renderer shows, with no entities pushing plants. */
        VISUALS(
            "visuals",
            false,
            true,
            GpuDistance.FULL,
            true,
            GpuDistance.FULL,
            true
        );

        private final String name;
        private final boolean interaction;
        private final boolean gpuRenderer;
        private final GpuDistance gpuDistance;
        private final boolean weatherWind;
        private final GpuDistance weatherWindDistance;
        private final boolean wavingFoliage;

        Preset(
            String name,
            boolean interaction,
            boolean gpuRenderer,
            GpuDistance gpuDistance,
            boolean weatherWind,
            GpuDistance weatherWindDistance,
            boolean wavingFoliage
        ) {
            this.name = name;
            this.interaction = interaction;
            this.gpuRenderer = gpuRenderer;
            this.gpuDistance = gpuDistance;
            this.weatherWind = weatherWind;
            this.weatherWindDistance = weatherWindDistance;
            this.wavingFoliage = wavingFoliage;
        }

        public String translationKey() {
            return "config.allthingsfoliage.preset." + name;
        }

        /** Whether it can be chosen: a preset on the chunk mesh cannot where the chunk mesh cannot move plants. */
        public boolean available() {
            return gpuRenderer || cpuRendererAvailable();
        }

        /** The GPU renderer's distance a preset sets, or null where it leaves it alone. */
        public GpuDistance gpuDistance() {
            return gpuDistance;
        }

        /** Sets every option the preset sets, all but the GPU renderer's distance, which the screen hands over itself. */
        public void applyAllButGpuDistance() {
            SwayConfig.INSTANCE.enabled = interaction;
            setGpuRenderer(gpuRenderer);
            setWeatherWind(weatherWind);
            if (weatherWindDistance != null) {
                setWeatherWindDistance(weatherWindDistance);
            }
            setWavingFoliage(wavingFoliage);
        }

        /** Whether the options stand as this preset sets them, with the GPU renderer's distance as given. */
        public boolean matches(GpuDistance currentGpuDistance) {
            return (
                SwayConfig.INSTANCE.enabled == interaction &&
                gpuRenderer() == gpuRenderer &&
                (gpuDistance == null || currentGpuDistance == gpuDistance) &&
                weatherWind() == weatherWind &&
                (weatherWindDistance == null ||
                    weatherWindDistance() == weatherWindDistance) &&
                wavingFoliage() == wavingFoliage
            );
        }

        /** The preset the options stand as, or null for none of them: custom. */
        public static Preset current(GpuDistance currentGpuDistance) {
            for (Preset preset : values()) {
                if (preset.available() && preset.matches(currentGpuDistance)) {
                    return preset;
                }
            }
            return null;
        }
    }

    /** The file's contents. Fields missing from an older file keep the defaults set here. */
    private static final class Values {

        boolean gpuRenderer = true;
        GpuDistance gpuDistance = GpuDistance.ADAPTIVE;
        boolean wavingFoliage = true;
        /** Whether the leaves of the waving whitelist wave too, with the GPU renderer. */
        boolean wavingLeaves = true;
        /** Whether rain and storms turn the sway into wind, sheltered by roofs and walls. */
        boolean weatherWind = true;
        /** How far from the player that happens: one of performance, half and full. */
        GpuDistance weatherWindDistance = GpuDistance.HALF;
        float wavingIntensity = DEFAULT_WAVING_INTENSITY;
        /** Whether the mod's default radius has been offered yet; see {@link #applyDefaultRadiusOnce}. */
        boolean defaultRadiusApplied;
        /**
         * Whether only living entities can be targeted, so an entity that has died and only remains to play its death
         * animation no longer absorbs hits.
         */
        boolean targetAliveOnly = true;
        /**
         * Blocks that stop rain's wind although they are fences, fence gates or bars, by id: a mod's solid fence, say.
         * Only set by editing the file.
         */
        List<String> windWalls = new ArrayList<>();
    }

    private static Values values = load();

    private FoliageSettings() {}

    /**
     * Whether the mod draws the foliage near the player itself, on the GPU. Turned off, every plant is drawn by
     * the chunk mesh as it always was, and the mod adds nothing to it.
     */
    public static boolean gpuRenderer() {
        return values.gpuRenderer || !cpuRendererAvailable();
    }

    /**
     * Whether the chunk mesh can draw the foliage the way Sway moves it. Embeddium meshes blocks without going through
     * the vanilla call Sway reads each block's position from, so under it plants in the chunk mesh never move: the
     * GPU renderer is then the only way the mod does anything, and is kept on whatever the file says.
     */
    public static boolean cpuRendererAvailable() {
        return true;
    }

    public static void setGpuRenderer(boolean enabled) {
        values.gpuRenderer = enabled;
    }

    public static GpuDistance gpuDistance() {
        return values.gpuDistance;
    }

    public static void setGpuDistance(GpuDistance distance) {
        values.gpuDistance = distance;
    }

    /**
     * The ids of the blocks that stop rain's wind although they are fences, fence gates or bars, as written in the file,
     * such as {@code "somemod:solid_fence"}.
     */
    public static List<String> windWalls() {
        return Collections.unmodifiableList(values.windWalls);
    }

    /** Whether rain and storms turn the sway into wind, sheltered by roofs and walls. */
    public static boolean weatherWind() {
        return values.weatherWind;
    }

    public static void setWeatherWind(boolean enabled) {
        values.weatherWind = enabled;
    }

    /**
     * How far from the player rain's wind blows and is sheltered: as far as entities push plants, half of the GPU
     * renderer's area, or all of it.
     */
    public static GpuDistance weatherWindDistance() {
        return values.weatherWindDistance;
    }

    public static void setWeatherWindDistance(GpuDistance distance) {
        values.weatherWindDistance = distance;
    }

    /** Whether the wind sways the foliage the GPU renderer draws. */
    public static boolean wavingFoliage() {
        return values.wavingFoliage;
    }

    public static void setWavingFoliage(boolean enabled) {
        values.wavingFoliage = enabled;
    }

    /** Whether the leaves of the waving whitelist wave too, with the GPU renderer. */
    public static boolean wavingLeaves() {
        return values.wavingLeaves;
    }

    public static void setWavingLeaves(boolean enabled) {
        values.wavingLeaves = enabled;
    }

    /** How strongly the wind sways foliage, as a multiplier of the shader's own strength. */
    public static float wavingIntensity() {
        return values.wavingIntensity;
    }

    public static void setWavingIntensity(float intensity) {
        values.wavingIntensity = clamp(
            intensity,
            MIN_WAVING_INTENSITY,
            MAX_WAVING_INTENSITY
        );
    }

    public static boolean isDefaultWavingIntensity() {
        return (
            Math.abs(values.wavingIntensity - DEFAULT_WAVING_INTENSITY) < 0.001F
        );
    }

    /**
     * Whether only living entities can be targeted. Entities that have died and only remain to play their death
     * animation are then looked through, so they no longer absorb hits.
     */
    public static boolean targetAliveOnly() {
        return values.targetAliveOnly;
    }

    public static void setTargetAliveOnly(boolean enabled) {
        values.targetAliveOnly = enabled;
    }

    /** The interaction radius on the slider's steps, from the minimum up to where Sway stops. */
    public static float snapInteractionRadius(float radius) {
        float steps = Math.round(
            (radius - MIN_INTERACTION_RADIUS) / INTERACTION_RADIUS_STEP
        );
        return clamp(
            MIN_INTERACTION_RADIUS + steps * INTERACTION_RADIUS_STEP,
            MIN_INTERACTION_RADIUS,
            MAX_INTERACTION_RADIUS
        );
    }

    /**
     * Gives the interaction radius the mod's default, once. The radius is Sway's setting, and Sway starts it at
     * 8; a fresh install should start at 16. It is moved only if it still holds Sway's own default, so a radius
     * someone already chose is left alone. Called every client tick, by which time Sway has loaded its file --
     * doing it any earlier would be overwritten as it does.
     */
    public static void applyDefaultRadiusOnce() {
        if (values.defaultRadiusApplied) {
            return;
        }
        values.defaultRadiusApplied = true;
        if (
            Math.abs(SwayConfig.INSTANCE.maxDistance - SWAY_DEFAULT_RADIUS) <
            0.001F
        ) {
            SwayConfig.INSTANCE.maxDistance = DEFAULT_INTERACTION_RADIUS;
            SwayConfig.save();
        }
        save();
    }

    /** Math.clamp would say this better, but these settings are shared with 1.20.1, built against Java 17. */
    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(values, writer);
            }
        } catch (IOException e) {
            AllThingsFoliage.LOGGER.error("Failed to save {}", PATH, e);
        }
    }

    private static Values load() {
        if (Files.exists(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH)) {
                Values loaded = GSON.fromJson(reader, Values.class);
                if (loaded != null) {
                    // A hand-edited file may hold anything.
                    loaded.wavingIntensity = clamp(
                        loaded.wavingIntensity,
                        MIN_WAVING_INTENSITY,
                        MAX_WAVING_INTENSITY
                    );
                    // Gson leaves a name it does not know as null.
                    if (loaded.gpuDistance == null) {
                        loaded.gpuDistance = GpuDistance.ADAPTIVE;
                    }
                    // Adaptive is the renderer's alone; a hand-edited file may still name it.
                    if (
                        loaded.weatherWindDistance == null ||
                        loaded.weatherWindDistance == GpuDistance.ADAPTIVE
                    ) {
                        loaded.weatherWindDistance = GpuDistance.HALF;
                    }
                    if (loaded.windWalls == null) {
                        loaded.windWalls = new ArrayList<>();
                    }
                    return loaded;
                }
            } catch (IOException | RuntimeException e) {
                AllThingsFoliage.LOGGER.error(
                    "Failed to load {}, using defaults",
                    PATH,
                    e
                );
            }
        }
        return new Values();
    }
}
