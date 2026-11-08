package io.github.andrewcwhy.allthingsfoliage

import com.github.razorplay01.sway.config.SwayConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import com.google.gson.annotations.SerializedName
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.math.abs
import kotlin.math.roundToInt

/** Stores foliage settings and persists them in the mod's JSON configuration file. */
object Config {

    const val DEFAULT_INTERACTION_INTENSITY = 1.0f
    const val MAX_INTERACTION_INTENSITY = 2.0f
    const val MIN_INTERACTION_INTENSITY = 0.5f

    const val DEFAULT_INTERACTION_RADIUS = 16.0f
    const val INTERACTION_RADIUS_STEP = 4.0f
    const val MAX_INTERACTION_RADIUS = 32.0f
    const val MIN_INTERACTION_RADIUS = 8.0f

    const val DEFAULT_WAVING_INTENSITY = 1.0f
    const val MAX_WAVING_INTENSITY = 2.0f
    const val MIN_WAVING_INTENSITY = 0.5f

    private const val FLOAT_TOLERANCE = 0.001f
    private const val SWAY_DEFAULT_RADIUS = 8.0f
    private const val TRANSLATION_PREFIX = "config.allthingsfoliage."

    private val configPath: Path =
            AllThingsFoliage.xplat()
                    .configDir()
                    .resolve("${AllThingsFoliage.MOD_ID}.json")
                    .toAbsolutePath()

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    private val data: ConfigData = load()

    enum class GpuDistance(private val translationSuffix: String) {
        @SerializedName("performance") PERFORMANCE("performance"),
        @SerializedName("adaptive") ADAPTIVE("adaptive"),
        @SerializedName("half") HALF("half"),
        @SerializedName("full") FULL("full");

        fun translationKey(): String {
            return "${TRANSLATION_PREFIX}gpu.$translationSuffix"
        }
    }

    /**
     * Graphics presets leave intensity, interaction radius, leaf preferences, and passability
     * settings unchanged. A null distance keeps its current value.
     *
     * Declaration order determines which matching preset is selected first.
     */
    enum class Preset(
            private val translationSuffix: String,
            private val interactionEnabled: Boolean,
            private val gpuRendererEnabled: Boolean,
            private val presetGpuDistance: GpuDistance?,
            private val weatherWindEnabled: Boolean,
            private val presetWeatherWindDistance: GpuDistance?,
            private val foliageWavingEnabled: Boolean
    ) {
        CLASSIC("classic", true, false, null, false, null, false),
        LOW("low", true, true, GpuDistance.PERFORMANCE, false, null, true),
        MEDIUM("medium", true, true, GpuDistance.ADAPTIVE, true, GpuDistance.PERFORMANCE, true),
        DEFAULT("default", true, true, GpuDistance.ADAPTIVE, true, GpuDistance.HALF, true),
        HIGH("high", true, true, GpuDistance.HALF, true, GpuDistance.HALF, true),
        ULTRA("ultra", true, true, GpuDistance.FULL, true, GpuDistance.FULL, true),
        VISUALS("visuals", false, true, GpuDistance.FULL, true, GpuDistance.FULL, true);

        fun applyAllButGpuDistance() {
            SwayConfig.INSTANCE.enabled = interactionEnabled
            setGpuRenderer(gpuRendererEnabled)
            setWeatherWind(weatherWindEnabled)
            setWavingFoliage(foliageWavingEnabled)

            if (presetWeatherWindDistance != null) {
                setWeatherWindDistance(presetWeatherWindDistance)
            }
        }

        fun available(): Boolean {
            return gpuRendererEnabled || cpuRendererAvailable()
        }

        fun gpuDistance(): GpuDistance? {
            return presetGpuDistance
        }

        fun matches(currentGpuDistance: GpuDistance): Boolean {
            return SwayConfig.INSTANCE.enabled == interactionEnabled &&
                    Config.gpuRenderer() == gpuRendererEnabled &&
                    (presetGpuDistance == null || currentGpuDistance == presetGpuDistance) &&
                    Config.weatherWind() == weatherWindEnabled &&
                    (presetWeatherWindDistance == null ||
                            Config.weatherWindDistance() == presetWeatherWindDistance) &&
                    Config.wavingFoliage() == foliageWavingEnabled
        }

        fun translationKey(): String {
            return "${TRANSLATION_PREFIX}preset.$translationSuffix"
        }

        companion object {
            @JvmStatic
            fun current(currentGpuDistance: GpuDistance): Preset? {
                for (preset in entries) {
                    if (preset.available() && preset.matches(currentGpuDistance)) {
                        return preset
                    }
                }

                return null
            }
        }
    }

    /** Stores passability preferences only; gameplay effects are implemented elsewhere. */
    class Passability {

        private var alwaysLeafWalking = false
        private var alwaysNotViewBlocking = true
        private var fallDamageMultiplier = 0.5f
        private var fallDamageThreshold = 20
        private var headHitter = false
        private var modifyPathFinding = true
        private var playerOnly = false
        private var soundVolume = 1.0f
        private var soundsPlayerOnly = false
        private var speedMultiplierHorizontal = 0.9f
        private var speedMultiplierVertical = 0.9f

        fun alwaysLeafWalking(): Boolean {
            return alwaysLeafWalking
        }

        fun alwaysNotViewBlocking(): Boolean {
            return alwaysNotViewBlocking
        }

        fun fallDamageMultiplier(): Float {
            return fallDamageMultiplier
        }

        fun fallDamageThreshold(): Int {
            return fallDamageThreshold
        }

        fun headHitter(): Boolean {
            return headHitter
        }

        fun modifyPathFinding(): Boolean {
            return modifyPathFinding
        }

        fun playerOnly(): Boolean {
            return playerOnly
        }

        fun setAlwaysLeafWalking(enabled: Boolean) {
            alwaysLeafWalking = enabled
        }

        fun setAlwaysNotViewBlocking(enabled: Boolean) {
            alwaysNotViewBlocking = enabled
        }

        fun setFallDamageMultiplier(multiplier: Float) {
            fallDamageMultiplier = finiteClamp(multiplier, 0.0f, 1.0f, 0.5f)
        }

        fun setFallDamageThreshold(blocks: Int) {
            fallDamageThreshold = blocks.coerceIn(5, 255)
        }

        fun setHeadHitter(enabled: Boolean) {
            headHitter = enabled
        }

        fun setModifyPathFinding(enabled: Boolean) {
            modifyPathFinding = enabled
        }

        fun setPlayerOnly(enabled: Boolean) {
            playerOnly = enabled
        }

        fun setSoundVolume(volume: Float) {
            soundVolume = finiteClamp(volume, 0.0f, 10.0f, 1.0f)
        }

        fun setSoundsPlayerOnly(enabled: Boolean) {
            soundsPlayerOnly = enabled
        }

        fun setSpeedMultiplierHorizontal(multiplier: Float) {
            speedMultiplierHorizontal = finiteClamp(multiplier, 0.0f, 1.0f, 0.9f)
        }

        fun setSpeedMultiplierVertical(multiplier: Float) {
            speedMultiplierVertical = finiteClamp(multiplier, 0.0f, 1.0f, 0.9f)
        }

        fun soundVolume(): Float {
            return soundVolume
        }

        fun soundsPlayerOnly(): Boolean {
            return soundsPlayerOnly
        }

        fun speedMultiplierHorizontal(): Float {
            return speedMultiplierHorizontal
        }

        fun speedMultiplierVertical(): Float {
            return speedMultiplierVertical
        }

        internal fun sanitize() {
            setFallDamageMultiplier(fallDamageMultiplier)
            setFallDamageThreshold(fallDamageThreshold)
            setSoundVolume(soundVolume)
            setSpeedMultiplierHorizontal(speedMultiplierHorizontal)
            setSpeedMultiplierVertical(speedMultiplierVertical)
        }
    }

    /** Applies the default interaction radius once Sway has loaded its settings. */
    @JvmStatic
    fun applyDefaultRadiusOnce() {
        if (data.defaultRadiusApplied) {
            return
        }

        data.defaultRadiusApplied = true

        if (abs(SwayConfig.INSTANCE.maxDistance - SWAY_DEFAULT_RADIUS) < FLOAT_TOLERANCE) {
            SwayConfig.INSTANCE.maxDistance = DEFAULT_INTERACTION_RADIUS
            SwayConfig.save()
        }

        save()
    }

    @JvmStatic
    fun cpuRendererAvailable(): Boolean {
        return true
    }

    @JvmStatic
    fun gpuDistance(): GpuDistance {
        return data.gpuDistance ?: GpuDistance.ADAPTIVE
    }

    @JvmStatic
    fun gpuRenderer(): Boolean {
        return data.gpuRenderer || !cpuRendererAvailable()
    }

    @JvmStatic
    fun isDefaultWavingIntensity(): Boolean {
        return abs(data.wavingIntensity - DEFAULT_WAVING_INTENSITY) < FLOAT_TOLERANCE
    }

    @JvmStatic
    fun passability(): Passability {
        return data.passability ?: Passability()
    }

    /** Writes the configuration to a temporary file before replacing the current file. */
    @JvmStatic
    fun save() {
        var temporaryPath: Path? = null

        try {
            val json = gson.toJson(data)
            val directory = configPath.parent

            Files.createDirectories(directory)
            temporaryPath = Files.createTempFile(directory, "${AllThingsFoliage.MOD_ID}-", ".tmp")

            Files.writeString(temporaryPath, json)

            try {
                Files.move(
                        temporaryPath,
                        configPath,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporaryPath, configPath, StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (exception: Exception) {
            if (exception !is IOException && exception !is JsonParseException) {
                throw exception
            }

            AllThingsFoliage.logger.error(
                    "Failed to save configuration to {}",
                    configPath,
                    exception
            )
        } finally {
            if (temporaryPath != null) {
                try {
                    Files.deleteIfExists(temporaryPath)
                } catch (exception: IOException) {
                    AllThingsFoliage.logger.warn(
                            "Failed to remove temporary configuration file {}",
                            temporaryPath,
                            exception
                    )
                }
            }
        }
    }

    @JvmStatic
    fun setGpuDistance(distance: GpuDistance) {
        data.gpuDistance = distance
    }

    @JvmStatic
    fun setGpuRenderer(enabled: Boolean) {
        data.gpuRenderer = enabled
    }

    @JvmStatic
    fun setTargetAliveOnly(enabled: Boolean) {
        data.targetAliveOnly = enabled
    }

    @JvmStatic
    fun setWavingFoliage(enabled: Boolean) {
        data.wavingFoliage = enabled
    }

    @JvmStatic
    fun setWavingIntensity(intensity: Float) {
        data.wavingIntensity =
                finiteClamp(
                        intensity,
                        MIN_WAVING_INTENSITY,
                        MAX_WAVING_INTENSITY,
                        DEFAULT_WAVING_INTENSITY
                )
    }

    @JvmStatic
    fun setWavingLeaves(enabled: Boolean) {
        data.wavingLeaves = enabled
    }

    @JvmStatic
    fun setWeatherWind(enabled: Boolean) {
        data.weatherWind = enabled
    }

    @JvmStatic
    fun setWeatherWindDistance(distance: GpuDistance) {
        require(distance != GpuDistance.ADAPTIVE) {
            "Adaptive distance is only supported by the GPU renderer"
        }

        data.weatherWindDistance = distance
    }

    @JvmStatic
    fun snapInteractionRadius(radius: Float): Float {
        val clamped =
                finiteClamp(
                        radius,
                        MIN_INTERACTION_RADIUS,
                        MAX_INTERACTION_RADIUS,
                        DEFAULT_INTERACTION_RADIUS
                )
        val steps = ((clamped - MIN_INTERACTION_RADIUS) / INTERACTION_RADIUS_STEP).roundToInt()

        return (MIN_INTERACTION_RADIUS + steps * INTERACTION_RADIUS_STEP).coerceIn(
                MIN_INTERACTION_RADIUS,
                MAX_INTERACTION_RADIUS
        )
    }

    @JvmStatic
    fun targetAliveOnly(): Boolean {
        return data.targetAliveOnly
    }

    @JvmStatic
    fun wavingFoliage(): Boolean {
        return data.wavingFoliage
    }

    @JvmStatic
    fun wavingIntensity(): Float {
        return data.wavingIntensity
    }

    @JvmStatic
    fun wavingLeaves(): Boolean {
        return data.wavingLeaves
    }

    @JvmStatic
    fun weatherWind(): Boolean {
        return data.weatherWind
    }

    @JvmStatic
    fun weatherWindDistance(): GpuDistance {
        return data.weatherWindDistance ?: GpuDistance.HALF
    }

    @JvmStatic
    fun windWalls(): List<String> {
        return data.windWalls ?: emptyList()
    }

    private fun finiteClamp(value: Float, minimum: Float, maximum: Float, fallback: Float): Float {
        if (!value.isFinite()) {
            return fallback
        }

        return value.coerceIn(minimum, maximum)
    }

    private fun load(): ConfigData {
        if (Files.notExists(configPath)) {
            return ConfigData()
        }

        try {
            Files.newBufferedReader(configPath).use { reader ->
                val loaded = gson.fromJson(reader, ConfigData::class.java) ?: return ConfigData()
                loaded.sanitize()
                return loaded
            }
        } catch (exception: Exception) {
            if (exception !is IOException && exception !is JsonParseException) {
                throw exception
            }

            AllThingsFoliage.logger.error(
                    "Failed to load configuration from {}; using defaults",
                    configPath,
                    exception
            )
            return ConfigData()
        }
    }

    /** Field names are retained to preserve the existing Gson JSON format. */
    private class ConfigData {
        var defaultRadiusApplied = false
        var gpuDistance: GpuDistance? = GpuDistance.ADAPTIVE
        var gpuRenderer = true
        var passability: Passability? = Passability()
        var targetAliveOnly = true
        var wavingFoliage = true
        var wavingIntensity = DEFAULT_WAVING_INTENSITY
        var wavingLeaves = true
        var weatherWind = true
        var weatherWindDistance: GpuDistance? = GpuDistance.HALF
        var windWalls: List<String>? = emptyList()

        fun sanitize() {
            wavingIntensity =
                    finiteClamp(
                            wavingIntensity,
                            MIN_WAVING_INTENSITY,
                            MAX_WAVING_INTENSITY,
                            DEFAULT_WAVING_INTENSITY
                    )

            if (gpuDistance == null) {
                gpuDistance = GpuDistance.ADAPTIVE
            }

            if (weatherWindDistance == null || weatherWindDistance == GpuDistance.ADAPTIVE) {
                weatherWindDistance = GpuDistance.HALF
            }

            windWalls =
                    windWalls
                            .orEmpty()
                            .filterNotNull()
                            .map { it.trim { character -> Character.isWhitespace(character) } }
                            .filter(String::isNotEmpty)
                            .distinct()

            if (passability == null) {
                passability = Passability()
            }

            passability?.sanitize()
        }
    }
}
