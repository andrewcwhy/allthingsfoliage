package io.github.andrewcwhy.allthingsfoliage.client.gui.screens

import com.github.razorplay01.sway.config.SwayConfig
import io.github.andrewcwhy.allthingsfoliage.Config
import io.github.andrewcwhy.allthingsfoliage.client.renderer.WavingWhitelist
import io.github.andrewcwhy.allthingsfoliage.gpu.GpuFoliageRenderer
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractSliderButton
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.CycleButton
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.util.ARGB
import net.minecraft.util.FormattedCharSequence

/** Provides graphics, interaction, passability, and wind configuration controls. */
class ConfigScreen(private val parent: Screen?) : Screen(translate("title")) {

    private val swayConfig: SwayConfig = SwayConfig.INSTANCE
    private val rows = mutableMapOf<Tab, MutableList<OptionRow>>()
    private val tabButtons = mutableMapOf<Tab, Button>()
    private val scrollPositions = mutableMapOf<Tab, Int>()
    private val synchronizers = mutableListOf<() -> Unit>()

    private var selectedTab = Tab.GENERAL
    private var initialized = false
    private var synchronizing = false

    private var contentX = 0
    private var contentWidth = 0
    private var contentBottom = 0
    private var labelWidth = 0
    private var controlWidth = 0
    private var firstVisibleRow = 0
    private var lastVisibleRowExclusive = 0

    private lateinit var scrollUpButton: Button
    private lateinit var scrollDownButton: Button
    private lateinit var presetSlider: ChoiceSlider<Config.Preset>
    private lateinit var gpuDistanceSlider: ChoiceSlider<Config.GpuDistance>

    private var pendingPreset: Config.Preset? = null
    private var pendingGpuDistance: Config.GpuDistance? = null

    private val displayedGpuDistance: Config.GpuDistance
        get() {
            return pendingGpuDistance ?: Config.gpuDistance
        }

    private val gpuRequirement: String?
        get() {
            return if (Config.gpuRenderer) null else "requires.gpu"
        }

    private val interactionRequirement: String?
        get() {
            return if (swayConfig.enabled) null else "requires.interaction"
        }

    private val isWavingAvailable: Boolean
        get() {
            return Config.gpuRenderer && Config.wavingFoliage
        }

    private val leafWalkingRequirement: String?
        get() {
            return if (Config.passability.alwaysLeafWalking) {
                "requires.leaf_walking_off"
            } else {
                null
            }
        }

    private val leavesRequirement: String?
        get() {
            val missingRequirement = wavingRequirement
            if (missingRequirement != null) {
                return missingRequirement
            }

            return if (WavingWhitelist.leavesCanWave()) null else "requires.fancy_leaves"
        }

    private val wavingRequirement: String?
        get() {
            if (!Config.gpuRenderer) {
                return "requires.gpu"
            }

            return if (Config.wavingFoliage) null else "requires.waving"
        }

    private val weatherRequirement: String?
        get() {
            if (!Config.gpuRenderer) {
                return "requires.gpu"
            }

            return if (Config.weatherWind) null else "requires.weather_wind"
        }

    override fun init() {
        applyPendingChanges()
        initialized = false

        rows.clear()
        tabButtons.clear()
        synchronizers.clear()

        for (tab in Tab.entries) {
            rows[tab] = mutableListOf()
        }

        contentWidth = (width - 24).coerceAtLeast(1).coerceAtMost(MAX_CONTENT_WIDTH)
        contentX = (width - contentWidth) / 2
        contentBottom = height - 62
        labelWidth = (contentWidth - COLUMN_GAP) / 2
        controlWidth = contentWidth - labelWidth - COLUMN_GAP

        createTabs()
        createGeneralOptions()
        createInteractionOptions()
        createPassabilityOptions()
        createWindOptions()
        createFooter()

        initialized = true
        synchronizeControls()
        selectTab(selectedTab)
    }

    private fun createTabs() {
        val tabs = Tab.entries
        val tabWidth = (contentWidth - CONTROL_GAP * (tabs.size - 1)) / tabs.size

        for ((index, tab) in tabs.withIndex()) {
            val x = contentX + index * (tabWidth + CONTROL_GAP)
            val buttonWidth = if (index == tabs.lastIndex) {
                contentX + contentWidth - x
            } else {
                tabWidth
            }

            val button = Button.builder(translate("tab.${tab.translationSuffix}")) { _ ->
                selectTab(tab)
            }
                .bounds(x, 34, buttonWidth, CONTROL_HEIGHT)
                .tooltip(Tooltip.create(translate("tab.${tab.translationSuffix}.tooltip")))
                .build()

            tabButtons[tab] = addRenderableWidget(button)
        }
    }

    private fun createGeneralOptions() {
        val presets = Config.Preset.entries.filter { preset -> preset.isAvailable }

        presetSlider = addChoice(
            Tab.GENERAL,
            "graphics",
            "preset",
            presets,
            Config.Preset.current(Config.gpuDistance),
            { preset ->
                if (preset == null) {
                    Component.translatable("options.graphics.custom")
                } else {
                    Component.translatable(preset.translationKey)
                }
            },
            { preset -> pendingPreset = preset },
            ALWAYS_AVAILABLE,
            NO_REQUIREMENT
        )

        gpuDistanceSlider = addChoice(
            Tab.GENERAL,
            "graphics",
            "gpu_distance",
            Config.GpuDistance.entries,
            Config.gpuDistance,
            { distance -> Component.translatable(requireNotNull(distance).translationKey) },
            { distance ->
                pendingGpuDistance = distance
                updatePresetIndicator()
            },
            { Config.gpuRenderer },
            ::gpuRequirement
        )

        synchronizers.add {
            gpuDistanceSlider.selection = displayedGpuDistance
        }

        val rendererSlider = addChoice(
            Tab.GENERAL,
            "graphics",
            "renderer",
            listOf(true, false),
            Config.gpuRenderer,
            { enabled -> Component.literal(if (enabled == true) "GPU" else "CPU") },
            { enabled ->
                Config.gpuRenderer = enabled
                refreshState()
            },
            { Config.cpuRendererAvailable },
            {
                if (Config.cpuRendererAvailable) {
                    null
                } else {
                    "requires.gpu_only"
                }
            }
        )

        synchronizers.add {
            rendererSlider.selection = Config.gpuRenderer
        }

        addToggle(
            Tab.GENERAL,
            "targeting",
            "target_alive_only",
            { Config.targetAliveOnly },
            { enabled -> Config.targetAliveOnly = enabled }
        )
    }

    private fun createInteractionOptions() {
        addNumber(
            Tab.INTERACTION,
            "",
            "bending_strength",
            BENDING_STRENGTH_RANGE,
            { swayConfig.intensity.toDouble() },
            { value -> swayConfig.intensity = value.toFloat() },
            ::decimal,
            { swayConfig.enabled },
            ::interactionRequirement
        )

        addToggle(
            Tab.INTERACTION,
            "",
            "entity_bending",
            { swayConfig.enabled },
            { enabled -> swayConfig.enabled = enabled }
        )

        addNumber(
            Tab.INTERACTION,
            "",
            "interaction_radius",
            INTERACTION_RADIUS_RANGE,
            { swayConfig.maxDistance.toDouble() },
            { value -> swayConfig.maxDistance = Config.snapInteractionRadius(value.toFloat()) },
            ::blocks,
            { swayConfig.enabled },
            ::interactionRequirement
        )
    }

    private fun createPassabilityOptions() {
        val passability = Config.passability

        addToggle(
            Tab.PASSABILITY,
            "behavior",
            "always_leaf_walking",
            { passability.alwaysLeafWalking },
            { enabled -> passability.alwaysLeafWalking = enabled }
        )

        addToggle(
            Tab.PASSABILITY,
            "behavior",
            "clear_leaf_view",
            { passability.alwaysNotViewBlocking },
            { enabled -> passability.alwaysNotViewBlocking = enabled },
        )

        addToggle(
            Tab.PASSABILITY,
            "behavior",
            "modify_pathfinding",
            { passability.modifyPathFinding },
            { enabled -> passability.modifyPathFinding = enabled },
        )

        addToggle(
            Tab.PASSABILITY,
            "behavior",
            "passability_players_only",
            { passability.playerOnly },
            { enabled -> passability.playerOnly = enabled },
        )

        addToggle(
            Tab.PASSABILITY,
            "behavior",
            "solid_leaves_below",
            { passability.headHitter },
            { enabled -> passability.headHitter = enabled },
        )

        val modifiersAvailable: () -> Boolean = { !passability.alwaysLeafWalking }

        addNumber(
            Tab.PASSABILITY,
            "fall_damage",
            "fall_damage_multiplier",
            NumericRange(0.0, 0.5, 1.0, 0.01, false),
            { passability.fallDamageMultiplier.toDouble() },
            { value -> passability.fallDamageMultiplier = value.toFloat() },
            ::percentage,
            modifiersAvailable,
            ::leafWalkingRequirement,
        )

        addNumber(
            Tab.PASSABILITY,
            "fall_damage",
            "fall_damage_threshold",
            NumericRange(5.0, 20.0, 255.0, 1.0, false),
            { passability.fallDamageThreshold.toDouble() },
            { value -> passability.fallDamageThreshold = value.roundToInt() },
            ::blocks,
            modifiersAvailable,
            ::leafWalkingRequirement,
        )

        addNumber(
            Tab.PASSABILITY,
            "movement",
            "horizontal_speed",
            NumericRange(0.0, 0.9, 1.0, 0.01, false),
            { passability.speedMultiplierHorizontal.toDouble() },
            { value -> passability.speedMultiplierHorizontal = value.toFloat() },
            ::percentage,
            modifiersAvailable,
            ::leafWalkingRequirement,
        )

        addNumber(
            Tab.PASSABILITY,
            "movement",
            "vertical_speed",
            NumericRange(0.0, 0.9, 1.0, 0.01, false),
            { passability.speedMultiplierVertical.toDouble() },
            { value -> passability.speedMultiplierVertical = value.toFloat() },
            ::percentage,
            modifiersAvailable,
            ::leafWalkingRequirement,
        )

        addToggle(
            Tab.PASSABILITY,
            "sounds",
            "sounds_players_only",
            { passability.soundsPlayerOnly },
            { enabled -> passability.soundsPlayerOnly = enabled },
        )

        addNumber(
            Tab.PASSABILITY,
            "sounds",
            "sound_volume",
            NumericRange(0.0, 1.0, 10.0, 0.05, false),
            { passability.soundVolume.toDouble() },
            { value -> passability.soundVolume = value.toFloat() },
            { value -> Component.literal("%.2f×".format(Locale.ROOT, value)) },
            ALWAYS_AVAILABLE,
            NO_REQUIREMENT,
        )
    }

    private fun createWindOptions() {
        addToggle(
            Tab.WIND,
            "ambient_waving",
            "ambient_waving",
            { Config.wavingFoliage },
            { enabled -> Config.wavingFoliage = enabled },
            { Config.gpuRenderer },
            ::gpuRequirement,
        )

        addToggle(
            Tab.WIND,
            "ambient_waving",
            "waving_leaves",
            { Config.wavingLeaves },
            { enabled ->
                Config.wavingLeaves = enabled
                GpuFoliageRenderer.foliageListChanged()
            },
            { isWavingAvailable && WavingWhitelist.leavesCanWave() },
            ::leavesRequirement,
        )

        addNumber(
            Tab.WIND,
            "ambient_waving",
            "waving_strength",
            WAVING_STRENGTH_RANGE,
            { Config.wavingIntensity.toDouble() },
            { value -> Config.wavingIntensity = value.toFloat() },
            ::decimal,
            ::isWavingAvailable,
            ::wavingRequirement,
        )

        addToggle(
            Tab.WIND,
            "weather_wind",
            "weather_wind",
            { Config.weatherWind },
            { enabled -> Config.weatherWind = enabled },
            { Config.gpuRenderer },
            ::gpuRequirement,
        )

        val weatherDistanceSlider = addChoice(
            Tab.WIND,
            "weather_wind",
            "weather_wind_distance",
            listOf(
                Config.GpuDistance.PERFORMANCE,
                Config.GpuDistance.HALF,
                Config.GpuDistance.FULL,
            ),
            Config.weatherWindDistance,
            { distance -> Component.translatable(requireNotNull(distance).translationKey) },
            { distance ->
                Config.weatherWindDistance = distance
                updatePresetIndicator()
            },
            { Config.gpuRenderer && Config.weatherWind },
            ::weatherRequirement,
        )

        synchronizers.add {
            weatherDistanceSlider.selection = Config.weatherWindDistance
        }
    }

    private fun createFooter() {
        scrollUpButton = addRenderableWidget(
            Button.builder(translate("scroll.up")) { _ -> scrollBy(-1) }
                .bounds(contentX, height - 54, 32, CONTROL_HEIGHT)
                .tooltip(Tooltip.create(translate("scroll.up.tooltip")))
                .build()
        )

        scrollDownButton = addRenderableWidget(
            Button.builder(translate("scroll.down")) { _ -> scrollBy(1) }
                .bounds(contentX + contentWidth - 32, height - 54, 32, CONTROL_HEIGHT)
                .tooltip(Tooltip.create(translate("scroll.down.tooltip")))
                .build()
        )

        addRenderableWidget(
            Button.builder(translate("save")) { _ -> onClose() }
                .bounds(contentX, height - 28, contentWidth, CONTROL_HEIGHT)
                .tooltip(Tooltip.create(translate("save.tooltip")))
                .build()
        )
    }

    private fun addToggle(
        tab: Tab,
        group: String,
        key: String,
        getter: () -> Boolean,
        setter: (Boolean) -> Unit,
        available: () -> Boolean = ALWAYS_AVAILABLE,
        requirement: () -> String? = NO_REQUIREMENT
    ) {
        val button: CycleButton<Boolean> = CycleButton.booleanBuilder(
            translate("on"),
            translate("off"),
            getter()
        )
            .displayOnlyValue()
            .create(
                0,
                0,
                controlWidth,
                CONTROL_HEIGHT,
                translate("$key.label")
            ) { _, enabled ->
                if (!synchronizing) {
                    setter(enabled)
                    refreshState()
                }
            }

        addRow(tab, group, key, button, null, available, requirement)
        synchronizers.add {
            button.value = getter()
        }
    }

    private fun addNumber(
        tab: Tab,
        group: String,
        key: String,
        range: NumericRange,
        getter: () -> Double,
        setter: (Double) -> Unit,
        formatter: (Double) -> Component,
        available: () -> Boolean,
        requirement: () -> String?
    ) {
        val slider = NumericSlider(
            controlWidth - RESET_WIDTH - CONTROL_GAP,
            key,
            range,
            getter,
            setter,
            formatter
        )

        addRow(tab, group, key, slider, slider.resetButton, available, requirement)
        synchronizers.add(slider::refresh)
    }

    private fun <T : Any> addChoice(
        tab: Tab,
        group: String,
        key: String,
        choices: List<T>,
        initialSelection: T?,
        formatter: (T?) -> Component,
        onChange: (T) -> Unit,
        available: () -> Boolean,
        requirement: () -> String?
    ): ChoiceSlider<T> {
        val slider = ChoiceSlider(
            controlWidth,
            translate("$key.label"),
            choices,
            initialSelection,
            formatter,
            onChange
        )

        addRow(tab, group, key, slider, null, available, requirement)
        return slider
    }

    private fun addRow(
        tab: Tab,
        group: String,
        key: String,
        control: AbstractWidget,
        resetButton: Button?,
        available: () -> Boolean,
        requirement: () -> String?
    ) {
        addRenderableWidget(control)

        if (resetButton != null) {
            addRenderableWidget(resetButton)
        }

        rows.getValue(tab).add(
            OptionRow(
                group,
                key,
                control,
                resetButton,
                available,
                requirement,
                font.split(translate("$key.label"), labelWidth).toList()
            )
        )
    }

    private fun selectTab(tab: Tab) {
        applyPendingChanges()
        selectedTab = tab

        for ((owner, button) in tabButtons) {
            button.active = owner != selectedTab
        }

        layoutVisibleRows()
        focused = null
    }

    private fun scrollBy(amount: Int) {
        if (isDragging) {
            return
        }

        applyPendingChanges()

        val current = scrollPositions.getOrDefault(selectedTab, 0)
        val maximum = (rows.getValue(selectedTab).size - 1).coerceAtLeast(0)
        val next = (current + amount).coerceIn(0, maximum)

        if (next != current) {
            scrollPositions[selectedTab] = next
            layoutVisibleRows()
            focused = null
        }
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        horizontalAmount: Double,
        verticalAmount: Double
    ): Boolean {
        val insideContent = mouseX >= contentX &&
                mouseX < contentX + contentWidth &&
                mouseY >= CONTENT_TOP &&
                mouseY < height - 58

        if (insideContent && verticalAmount != 0.0) {
            if (verticalAmount > 0.0 && firstVisibleRow > 0) {
                scrollBy(-1)
            } else if (
                verticalAmount < 0.0 &&
                lastVisibleRowExclusive < rows.getValue(selectedTab).size
            ) {
                scrollBy(1)
            }

            return true
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    /** Keeps partially visible rows outside the interactive region. */
    private fun layoutVisibleRows() {
        for (tabRows in rows.values) {
            for (row in tabRows) {
                row.isVisible = false
            }
        }

        val selectedRows = rows.getValue(selectedTab)
        firstVisibleRow = scrollPositions.getOrDefault(selectedTab, 0)
            .coerceIn(0, (selectedRows.size - 1).coerceAtLeast(0))
        lastVisibleRowExclusive = firstVisibleRow

        var y = CONTENT_TOP
        var previousGroup: String? = null

        for (index in firstVisibleRow until selectedRows.size) {
            val row = selectedRows[index]
            val showGroup = row.group.isNotEmpty() && row.group != previousGroup
            val headingHeight = if (showGroup) GROUP_HEIGHT else 0
            val labelHeight = row.labelLines.size * font.lineHeight
            val bodyHeight = maxOf(CONTROL_HEIGHT, labelHeight)
            val rowHeight = headingHeight + bodyHeight + ROW_GAP

            if (y + rowHeight > contentBottom) {
                break
            }

            row.headingY = if (showGroup) y else -1
            row.labelY = y + headingHeight + (bodyHeight - labelHeight) / 2

            row.control.x = contentX + labelWidth + COLUMN_GAP
            row.control.y = y + headingHeight + (bodyHeight - CONTROL_HEIGHT) / 2

            row.resetButton?.let { resetButton ->
                resetButton.x = contentX + contentWidth - RESET_WIDTH
                resetButton.y = row.control.y
            }

            row.isVisible = true

            previousGroup = row.group
            lastVisibleRowExclusive = index + 1
            y += rowHeight
        }

        scrollPositions[selectedTab] = firstVisibleRow

        val scrollable = firstVisibleRow > 0 || lastVisibleRowExclusive < selectedRows.size
        scrollUpButton.visible = scrollable
        scrollDownButton.visible = scrollable
        scrollUpButton.active = firstVisibleRow > 0
        scrollDownButton.active = lastVisibleRowExclusive < selectedRows.size
    }

    private fun refreshState() {
        if (!initialized || synchronizing) {
            return
        }

        for (tabRows in rows.values) {
            for (row in tabRows) {
                row.refreshAvailability()
            }
        }

        updatePresetIndicator()
    }

    private fun synchronizeControls() {
        synchronizing = true

        try {
            for (synchronize in synchronizers) {
                synchronize()
            }
        } finally {
            synchronizing = false
        }

        refreshState()
    }

    private fun updatePresetIndicator() {
        if (initialized && pendingPreset == null) {
            presetSlider.selection = Config.Preset.current(displayedGpuDistance)
        }
    }

    /** Applies deferred graphics changes before reinitialization, navigation, and closing. */
    private fun applyPendingChanges() {
        if (!initialized) {
            return
        }

        val preset = pendingPreset
        pendingPreset = null

        if (preset != null && !preset.matches(displayedGpuDistance)) {
            preset.applyAllButGpuDistance()
            preset.gpuDistance?.let { distance ->
                pendingGpuDistance = distance
            }
        }

        val distance = pendingGpuDistance
        if (distance != null) {
            if (distance != Config.gpuDistance) {
                Config.gpuDistance = distance
            }

            pendingGpuDistance = null
        }

        if (preset != null) {
            synchronizeControls()
        }
    }

    override fun tick() {
        super.tick()

        if (!isDragging) {
            applyPendingChanges()
        }
    }

    override fun onClose() {
        applyPendingChanges()
        Config.save()
        SwayConfig.save()
        minecraft.gui.setScreen(parent)
    }

    override fun extractRenderState(
        graphics: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float
    ) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick)
        graphics.centeredText(font, title, width / 2, 15, TEXT_COLOR)

        val selectedRows = rows.getValue(selectedTab)
        for (index in firstVisibleRow until lastVisibleRowExclusive) {
            val row = selectedRows[index]

            if (row.headingY >= 0) {
                graphics.text(
                    font,
                    translate("group.${row.group}"),
                    contentX,
                    row.headingY,
                    GROUP_TEXT_COLOR
                )
            }

            var y = row.labelY
            val color = if (row.control.active) TEXT_COLOR else DISABLED_TEXT_COLOR

            for (line in row.labelLines) {
                graphics.text(font, line, contentX, y, color)
                y += font.lineHeight
            }
        }

        if (scrollUpButton.visible || scrollDownButton.visible) {
            graphics.centeredText(
                font,
                translate(
                    "scroll.position",
                    if (lastVisibleRowExclusive > firstVisibleRow) firstVisibleRow + 1 else 0,
                    lastVisibleRowExclusive,
                    selectedRows.size
                ),
                width / 2,
                height - 48,
                GROUP_TEXT_COLOR
            )
        }
    }

    private enum class Tab(val translationSuffix: String) {
        GENERAL("general"),
        INTERACTION("interaction"),
        PASSABILITY("passability"),
        WIND("wind")
    }

    private class OptionRow(
        val group: String,
        private val key: String,
        val control: AbstractWidget,
        val resetButton: Button?,
        private val available: () -> Boolean,
        private val requirement: () -> String?,
        val labelLines: List<FormattedCharSequence>
    ) {
        private var previousRequirement: String? = null
        private var tooltipInitialized = false

        var headingY = -1
        var labelY = 0

        var isVisible: Boolean
            get() {
                return control.visible
            }
            set(value) {
                control.visible = value
                resetButton?.visible = value
            }

        fun refreshAvailability() {
            control.active = available()
            resetButton?.active = control.active &&
                    control is NumericSlider && !control.isDefault

            val missingRequirement = requirement()
            if (tooltipInitialized && previousRequirement == missingRequirement) {
                return
            }

            val tooltip = translate("$key.tooltip")
            if (missingRequirement != null) {
                tooltip.append("\n\n").append(translate(missingRequirement))
            }

            control.setTooltip(Tooltip.create(tooltip))

            if (resetButton != null) {
                val resetTooltip = translate("reset_to_default")
                if (missingRequirement != null) {
                    resetTooltip.append("\n\n").append(translate(missingRequirement))
                }

                resetButton.setTooltip(Tooltip.create(resetTooltip))
            }

            previousRequirement = missingRequirement
            tooltipInitialized = true
        }
    }

    private data class NumericRange(
        val minimum: Double,
        val defaultValue: Double,
        val maximum: Double,
        val step: Double,
        val centeredDefault: Boolean
    ) {
        init {
            require(
                minimum.isFinite() &&
                        defaultValue.isFinite() &&
                        maximum.isFinite() &&
                        step.isFinite() &&
                        minimum < maximum &&
                        defaultValue in minimum..maximum &&
                        step > 0.0 &&
                        (!centeredDefault || (defaultValue != minimum && defaultValue != maximum))
            ) {
                "Invalid numeric slider range"
            }
        }

        fun toPosition(number: Double): Double {
            val sanitized = if (number.isFinite()) {
                number.coerceIn(minimum, maximum)
            } else {
                defaultValue
            }

            if (!centeredDefault) {
                return (sanitized - minimum) / (maximum - minimum)
            }

            return if (sanitized <= defaultValue) {
                0.5 * (sanitized - minimum) / (defaultValue - minimum)
            } else {
                0.5 + 0.5 * (sanitized - defaultValue) / (maximum - defaultValue)
            }
        }

        fun fromPosition(position: Double): Double {
            val clamped = position.coerceIn(0.0, 1.0)

            val number = when {
                !centeredDefault -> minimum + clamped * (maximum - minimum)
                clamped <= 0.5 -> minimum + clamped * 2.0 * (defaultValue - minimum)
                else -> defaultValue + (clamped - 0.5) * 2.0 * (maximum - defaultValue)
            }

            val snapped = minimum + ((number - minimum) / step).roundToLong() * step
            return snapped.coerceIn(minimum, maximum)
        }
    }

    private class NumericSlider(
        width: Int,
        private val key: String,
        private val range: NumericRange,
        private val getter: () -> Double,
        private val setter: (Double) -> Unit,
        private val formatter: (Double) -> Component
    ) : AbstractSliderButton(
        0,
        0,
        width,
        CONTROL_HEIGHT,
        Component.empty(),
        range.toPosition(getter())
    ) {
        val resetButton: Button = Button.builder(translate("reset")) { _ -> reset() }
            .bounds(0, 0, RESET_WIDTH, CONTROL_HEIGHT)
            .build()

        private var displayedValue = 0.0
        private var messageInitialized = false

        val isDefault: Boolean
            get() {
                return sameValue(getter(), range.defaultValue)
            }

        init {
            updateMessage()
        }

        override fun updateMessage() {
            val currentValue = getter()

            if (messageInitialized && displayedValue.compareTo(currentValue) == 0) {
                return
            }

            message = formatter(currentValue)
            displayedValue = currentValue
            messageInitialized = true
        }

        override fun createNarrationMessage(): MutableComponent {
            return Component.translatable("gui.narrate.slider", labeledValue(key, message))
        }

        override fun applyValue() {
            val nextValue = range.fromPosition(value)
            if (sameValue(nextValue, getter())) {
                return
            }

            setter(nextValue)
            updateMessage()
            refreshResetButton()
        }

        fun refresh() {
            value = range.toPosition(getter())
            updateMessage()
            refreshResetButton()
        }

        private fun refreshResetButton() {
            resetButton.active = active && !isDefault
        }

        private fun reset() {
            if (!isDefault) {
                setter(range.defaultValue)
            }

            // Realign the slider even if its stored value did not change.
            refresh()
        }
    }

    private class ChoiceSlider<T : Any>(
        width: Int,
        private val optionLabel: Component,
        choices: List<T>,
        initialSelection: T?,
        private val formatter: (T?) -> Component,
        private val onChange: (T) -> Unit
    ) : AbstractSliderButton(
        0,
        0,
        width,
        CONTROL_HEIGHT,
        Component.empty(),
        initialChoicePosition(choices, initialSelection)
    ) {
        private val choices = choices.toList()
        private var selected: T? = initialSelection
        private var displayedSelection: T? = null
        private var messageInitialized = false

        var selection: T?
            get() {
                return selected
            }
            set(value) {
                selected = value

                // A null preset represents a custom configuration; leave its handle in place.
                val index = if (value == null) -1 else choices.indexOf(value)

                if (index >= 0) {
                    this.value =
                        if (choices.size == 1) {
                            0.0
                        } else {
                            index.toDouble() / (choices.size - 1)
                        }
                }

                updateMessage()
            }

        init {
            updateMessage()
        }

        override fun updateMessage() {
            if (messageInitialized && displayedSelection == selected) {
                return
            }

            message = formatter(selected)
            displayedSelection = selected
            messageInitialized = true
        }

        override fun createNarrationMessage(): MutableComponent {
            return Component.translatable(
                "gui.narrate.slider",
                Component.translatable(
                    "options.generic_value",
                    optionLabel,
                    message,
                ),
            )
        }

        override fun applyValue() {
            val index = (value * (choices.size - 1)).roundToInt()
            val nextSelection = choices[index.coerceIn(0, choices.lastIndex)]

            if (selected == nextSelection) {
                return
            }

            selected = nextSelection
            updateMessage()
            onChange(nextSelection)
        }
    }

    private companion object {
        private const val TRANSLATION_PREFIX = "config.allthingsfoliage."

        private const val MAX_CONTENT_WIDTH = 400
        private const val CONTROL_HEIGHT = 20
        private const val RESET_WIDTH = 20
        private const val COLUMN_GAP = 8
        private const val CONTROL_GAP = 4
        private const val ROW_GAP = 8
        private const val GROUP_HEIGHT = 16
        private const val CONTENT_TOP = 66

        private val TEXT_COLOR = ARGB.color(255, 255, 255)
        private val DISABLED_TEXT_COLOR = ARGB.color(153, 153, 153)
        private val GROUP_TEXT_COLOR = ARGB.color(204, 204, 204)

        private val ALWAYS_AVAILABLE: () -> Boolean = { true }
        private val NO_REQUIREMENT: () -> String? = { null }

        private val BENDING_STRENGTH_RANGE = NumericRange(
            Config.MIN_INTERACTION_INTENSITY.toDouble(),
            Config.DEFAULT_INTERACTION_INTENSITY.toDouble(),
            Config.MAX_INTERACTION_INTENSITY.toDouble(),
            0.1,
            true
        )

        private val INTERACTION_RADIUS_RANGE = NumericRange(
            Config.MIN_INTERACTION_RADIUS.toDouble(),
            Config.DEFAULT_INTERACTION_RADIUS.toDouble(),
            Config.MAX_INTERACTION_RADIUS.toDouble(),
            Config.INTERACTION_RADIUS_STEP.toDouble(),
            false
        )

        private val WAVING_STRENGTH_RANGE = NumericRange(
            Config.MIN_WAVING_INTENSITY.toDouble(),
            Config.DEFAULT_WAVING_INTENSITY.toDouble(),
            Config.MAX_WAVING_INTENSITY.toDouble(),
            0.1,
            true
        )

        private fun blocks(value: Double): Component {
            return translate("value.blocks", value.roundToLong())
        }

        private fun decimal(value: Double): Component {
            return Component.literal("%.1f".format(Locale.ROOT, value))
        }

        private fun labeledValue(key: String, value: Component): MutableComponent {
            return Component.translatable("options.generic_value", translate("$key.label"), value)
        }

        private fun percentage(value: Double): Component {
            return Component.literal("%.0f%%".format(Locale.ROOT, value * 100.0))
        }

        private fun sameValue(first: Double, second: Double): Boolean {
            // The settings are stored as floats or small integers.
            return first.toFloat().compareTo(second.toFloat()) == 0
        }

        private fun <T : Any> initialChoicePosition(choices: List<T>, selection: T?): Double {
            require(choices.isNotEmpty()) {
                "A choice slider requires at least one choice"
            }

            val index = if (selection == null) {
                0
            } else {
                choices.indexOf(selection).coerceAtLeast(0)
            }

            return if (choices.size == 1) {
                0.0
            } else {
                index.toDouble() / (choices.size - 1)
            }
        }

        private fun translate(key: String, vararg arguments: Any): MutableComponent {
            return Component.translatable(TRANSLATION_PREFIX + key, *arguments)
        }
    }
}
