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
        AllThingsFoliage.platform
            .configDir
            .resolve("${AllThingsFoliage.MOD_ID}.json")
            .toAbsolutePath()

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    private val data: ConfigData = load()

    val cpuRendererAvailable: Boolean
        get() {
            return true
        }

    var gpuDistance: GpuDistance
        get() {
            return data.gpuDistance ?: GpuDistance.ADAPTIVE
        }
        set(value) {
            data.gpuDistance = value
        }

    var gpuRenderer: Boolean
        get() {
            return data.gpuRenderer || !cpuRendererAvailable
        }
        set(value) {
            data.gpuRenderer = value
        }

    val isDefaultWavingIntensity: Boolean
        get() {
            return abs(data.wavingIntensity - DEFAULT_WAVING_INTENSITY) < FLOAT_TOLERANCE
        }

    val passability: Passability
        get() {
            return data.passability ?: Passability()
        }

    var targetAliveOnly: Boolean
        get() {
            return data.targetAliveOnly
        }
        set(value) {
            data.targetAliveOnly = value
        }

    var wavingFoliage: Boolean
        get() {
            return data.wavingFoliage
        }
        set(value) {
            data.wavingFoliage = value
        }

    var wavingIntensity: Float
        get() {
            return data.wavingIntensity
        }
        set(value) {
            data.wavingIntensity =
                finiteClamp(
                    value,
                    MIN_WAVING_INTENSITY,
                    MAX_WAVING_INTENSITY,
                    DEFAULT_WAVING_INTENSITY,
                )
        }

    var wavingLeaves: Boolean
        get() {
            return data.wavingLeaves
        }
        set(value) {
            data.wavingLeaves = value
        }

    var weatherWind: Boolean
        get() {
            return data.weatherWind
        }
        set(value) {
            data.weatherWind = value
        }

    var weatherWindDistance: GpuDistance
        get() {
            return data.weatherWindDistance ?: GpuDistance.HALF
        }
        set(value) {
            require(value != GpuDistance.ADAPTIVE) {
                "Adaptive distance is only supported by the GPU renderer"
            }

            data.weatherWindDistance = value
        }

    val windWalls: List<String>
        get() {
            return data.windWalls ?: emptyList()
        }

    enum class GpuDistance(private val translationSuffix: String) {
        @SerializedName("performance")
        PERFORMANCE("performance"),

        @SerializedName("adaptive")
        ADAPTIVE("adaptive"),

        @SerializedName("half")
        HALF("half"),

        @SerializedName("full")
        FULL("full");

        val translationKey: String
            get() {
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

        val gpuDistance: GpuDistance?
            get() {
                return presetGpuDistance
            }

        val isAvailable: Boolean
            get() {
                return gpuRendererEnabled || Config.cpuRendererAvailable
            }

        val translationKey: String
            get() {
                return "${TRANSLATION_PREFIX}preset.$translationSuffix"
            }

        fun applyAllButGpuDistance() {
            SwayConfig.INSTANCE.enabled = interactionEnabled
            Config.gpuRenderer = gpuRendererEnabled
            Config.weatherWind = weatherWindEnabled
            Config.wavingFoliage = foliageWavingEnabled

            if (presetWeatherWindDistance != null) {
                Config.weatherWindDistance = presetWeatherWindDistance
            }
        }

        fun matches(currentGpuDistance: GpuDistance): Boolean {
            return SwayConfig.INSTANCE.enabled == interactionEnabled &&
                    Config.gpuRenderer == gpuRendererEnabled &&
                    (presetGpuDistance == null || currentGpuDistance == presetGpuDistance) &&
                    Config.weatherWind == weatherWindEnabled &&
                    (presetWeatherWindDistance == null ||
                            Config.weatherWindDistance == presetWeatherWindDistance) &&
                    Config.wavingFoliage == foliageWavingEnabled
        }

        companion object {

            fun current(currentGpuDistance: GpuDistance): Preset? {
                for (preset in entries) {
                    if (preset.isAvailable && preset.matches(currentGpuDistance)) {
                        return preset
                    }
                }

                return null
            }
        }
    }

    /** Stores passability preferences only; gameplay effects are implemented elsewhere. */
    class Passability {
        var alwaysLeafWalking = false
        var alwaysNotViewBlocking = true

        var fallDamageMultiplier = 0.5f
            set(value) {
                field = finiteClamp(value, 0.0f, 1.0f, 0.5f)
            }

        var fallDamageThreshold = 20
            set(value) {
                field = value.coerceIn(5, 255)
            }

        var headHitter = false
        var modifyPathFinding = true
        var playerOnly = false

        var soundVolume = 1.0f
            set(value) {
                field = finiteClamp(value, 0.0f, 10.0f, 1.0f)
            }

        var soundsPlayerOnly = false

        var speedMultiplierHorizontal = 0.9f
            set(value) {
                field = finiteClamp(value, 0.0f, 1.0f, 0.9f)
            }

        var speedMultiplierVertical = 0.9f
            set(value) {
                field = finiteClamp(value, 0.0f, 1.0f, 0.9f)
            }

        internal fun sanitize() {
            // Gson writes backing fields directly, so reapply the validating setters.
            fallDamageMultiplier = fallDamageMultiplier
            fallDamageThreshold = fallDamageThreshold
            soundVolume = soundVolume
            speedMultiplierHorizontal = speedMultiplierHorizontal
            speedMultiplierVertical = speedMultiplierVertical
        }
    }

    /** Applies the default interaction radius once Sway has loaded its settings. */
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

    /** Writes the configuration to a temporary file before replacing the current file. */
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
