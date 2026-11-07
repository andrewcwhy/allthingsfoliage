package io.github.andrewcwhy.allthingsfoliage.client.gui.screens;

import com.github.razorplay01.sway.config.SwayConfig;
import io.github.andrewcwhy.allthingsfoliage.gpu.GpuFoliageRenderer;
import io.github.andrewcwhy.allthingsfoliage.Config;
import io.github.andrewcwhy.allthingsfoliage.WavingWhitelist;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

public final class ConfigScreen extends Screen {

    private static final String TRANSLATION_PREFIX = "config.allthingsfoliage.";

    private static final int MAX_CONTENT_WIDTH = 400;
    private static final int CONTROL_HEIGHT = 20;
    private static final int RESET_WIDTH = 20;
    private static final int COLUMN_GAP = 8;
    private static final int CONTROL_GAP = 4;
    private static final int ROW_GAP = 8;
    private static final int GROUP_HEIGHT = 16;
    private static final int CONTENT_TOP = 66;

    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int DISABLED_TEXT_COLOR = 0xFF999999;
    private static final int GROUP_TEXT_COLOR = 0xFFCCCCCC;

    private static final BooleanSupplier ALWAYS_AVAILABLE = () -> true;
    private static final Supplier<String> NO_REQUIREMENT = () -> null;

    private static final NumericRange BENDING_STRENGTH_RANGE = new NumericRange(
        Config.MIN_INTERACTION_INTENSITY,
        Config.DEFAULT_INTERACTION_INTENSITY,
        Config.MAX_INTERACTION_INTENSITY,
        0.1,
        true
    );

    private static final NumericRange INTERACTION_RADIUS_RANGE = new NumericRange(
        Config.MIN_INTERACTION_RADIUS,
        Config.DEFAULT_INTERACTION_RADIUS,
        Config.MAX_INTERACTION_RADIUS,
        Config.INTERACTION_RADIUS_STEP,
        false
    );

    private static final NumericRange WAVING_STRENGTH_RANGE = new NumericRange(
        Config.MIN_WAVING_INTENSITY,
        Config.DEFAULT_WAVING_INTENSITY,
        Config.MAX_WAVING_INTENSITY,
        0.1,
        true
    );

    private final Screen parent;
    private final SwayConfig swayConfig = SwayConfig.INSTANCE;

    private final EnumMap<Tab, List<OptionRow>> rows = new EnumMap<>(Tab.class);
    private final EnumMap<Tab, Button> tabButtons = new EnumMap<>(Tab.class);
    private final EnumMap<Tab, Integer> scrollPositions = new EnumMap<>(Tab.class);
    private final List<Runnable> synchronizers = new ArrayList<>();

    private Tab selectedTab = Tab.GENERAL;
    private boolean initialized;
    private boolean synchronizing;

    private int contentX;
    private int contentWidth;
    private int contentBottom;
    private int labelWidth;
    private int controlWidth;
    private int firstVisibleRow;
    private int lastVisibleRowExclusive;

    private Button scrollUpButton;
    private Button scrollDownButton;

    private Config.Preset pendingPreset;
    private Config.GpuDistance pendingGpuDistance;

    private ChoiceSlider<Config.Preset> presetSlider;
    private ChoiceSlider<Config.GpuDistance> gpuDistanceSlider;

    public ConfigScreen(Screen parent) {
        super(translate("title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        applyPendingChanges();
        initialized = false;

        rows.clear();
        tabButtons.clear();
        synchronizers.clear();

        for (Tab tab : Tab.values()) {
            rows.put(tab, new ArrayList<>());
        }

        contentWidth = Math.min(MAX_CONTENT_WIDTH, Math.max(1, width - 24));
        contentX = (width - contentWidth) / 2;
        contentBottom = height - 62;

        labelWidth = (contentWidth - COLUMN_GAP) / 2;
        controlWidth = contentWidth - labelWidth - COLUMN_GAP;

        createTabs();
        createGeneralOptions();
        createInteractionOptions();
        createPassabilityOptions();
        createWindOptions();
        createFooter();

        initialized = true;
        synchronizeControls();
        selectTab(selectedTab);
    }

    private void createTabs() {
        Tab[] tabs = Tab.values();
        int tabWidth = (contentWidth - CONTROL_GAP * (tabs.length - 1))
            / tabs.length;

        for (int index = 0; index < tabs.length; index++) {
            Tab tab = tabs[index];
            int x = contentX + index * (tabWidth + CONTROL_GAP);
            int buttonWidth = index == tabs.length - 1
                ? contentX + contentWidth - x
                : tabWidth;

            Button button = Button.builder(
                translate("tab." + tab.translationSuffix),
                ignored -> selectTab(tab)
            )
                .bounds(x, 34, buttonWidth, CONTROL_HEIGHT)
                .tooltip(Tooltip.create(
                    translate("tab." + tab.translationSuffix + ".tooltip")
                ))
                .build();

            tabButtons.put(tab, addRenderableWidget(button));
        }
    }

    private void createGeneralOptions() {
        List<Config.Preset> presets = Arrays.stream(Config.Preset.values())
            .filter(Config.Preset::available)
            .toList();

        presetSlider = addChoice(
            Tab.GENERAL, "graphics", "preset",
            presets,
            Config.Preset.current(Config.gpuDistance()),
            preset -> preset == null
                ? Component.translatable("options.graphics.custom")
                : Component.translatable(preset.translationKey()),
            preset -> pendingPreset = preset,
            ALWAYS_AVAILABLE,
            NO_REQUIREMENT
        );

        gpuDistanceSlider = addChoice(
            Tab.GENERAL, "graphics", "gpu_distance",
            List.of(Config.GpuDistance.values()),
            Config.gpuDistance(),
            distance -> Component.translatable(distance.translationKey()),
            distance -> {
                pendingGpuDistance = distance;
                updatePresetIndicator();
            },
            Config::gpuRenderer,
            this::gpuRequirement
        );

        synchronizers.add(
            () -> gpuDistanceSlider.setSelected(displayedGpuDistance())
        );

        ChoiceSlider<Boolean> rendererSlider = addChoice(
            Tab.GENERAL, "graphics", "renderer",
            List.of(true, false),
            Config.gpuRenderer(),
            enabled -> Component.literal(enabled ? "GPU" : "CPU"),
            enabled -> {
                Config.setGpuRenderer(enabled);
                refreshState();
            },
            Config::cpuRendererAvailable,
            () -> Config.cpuRendererAvailable()
                ? null
                : "requires.gpu_only"
        );

        synchronizers.add(
            () -> rendererSlider.setSelected(Config.gpuRenderer())
        );

        addToggle(
            Tab.GENERAL, "targeting", "target_alive_only",
            Config::targetAliveOnly,
            Config::setTargetAliveOnly
        );
    }

    private void createInteractionOptions() {
        addNumber(
            Tab.INTERACTION, "", "bending_strength",
            BENDING_STRENGTH_RANGE,
            () -> swayConfig.intensity,
            value -> swayConfig.intensity = (float) value,
            ConfigScreen::decimal,
            () -> swayConfig.enabled,
            this::interactionRequirement
        );

        addToggle(
            Tab.INTERACTION, "", "entity_bending",
            () -> swayConfig.enabled,
            enabled -> swayConfig.enabled = enabled
        );

        addNumber(
            Tab.INTERACTION, "", "interaction_radius",
            INTERACTION_RADIUS_RANGE,
            () -> swayConfig.maxDistance,
            value -> swayConfig.maxDistance =
                Config.snapInteractionRadius((float) value),
            ConfigScreen::blocks,
            () -> swayConfig.enabled,
            this::interactionRequirement
        );
    }

    private void createPassabilityOptions() {
        Config.Passability passability = Config.passability();

        addToggle(
            Tab.PASSABILITY, "behavior", "always_leaf_walking",
            passability::alwaysLeafWalking,
            passability::setAlwaysLeafWalking
        );

        addToggle(
            Tab.PASSABILITY, "behavior", "clear_leaf_view",
            passability::alwaysNotViewBlocking,
            passability::setAlwaysNotViewBlocking
        );

        addToggle(
            Tab.PASSABILITY, "behavior", "modify_pathfinding",
            passability::modifyPathFinding,
            passability::setModifyPathFinding
        );

        addToggle(
            Tab.PASSABILITY, "behavior", "passability_players_only",
            passability::playerOnly,
            passability::setPlayerOnly
        );

        addToggle(
            Tab.PASSABILITY, "behavior", "solid_leaves_below",
            passability::headHitter,
            passability::setHeadHitter
        );

        BooleanSupplier modifiersAvailable =
            () -> !passability.alwaysLeafWalking();

        addNumber(
            Tab.PASSABILITY, "fall_damage", "fall_damage_multiplier",
            new NumericRange(0.0, 0.5, 1.0, 0.01, false),
            passability::fallDamageMultiplier,
            value -> passability.setFallDamageMultiplier((float) value),
            ConfigScreen::percentage,
            modifiersAvailable,
            this::leafWalkingRequirement
        );

        addNumber(
            Tab.PASSABILITY, "fall_damage", "fall_damage_threshold",
            new NumericRange(5.0, 20.0, 255.0, 1.0, false),
            passability::fallDamageThreshold,
            value -> passability.setFallDamageThreshold((int) Math.round(value)),
            ConfigScreen::blocks,
            modifiersAvailable,
            this::leafWalkingRequirement
        );

        addNumber(
            Tab.PASSABILITY, "movement", "horizontal_speed",
            new NumericRange(0.0, 0.9, 1.0, 0.01, false),
            passability::speedMultiplierHorizontal,
            value -> passability.setSpeedMultiplierHorizontal((float) value),
            ConfigScreen::percentage,
            modifiersAvailable,
            this::leafWalkingRequirement
        );

        addNumber(
            Tab.PASSABILITY, "movement", "vertical_speed",
            new NumericRange(0.0, 0.9, 1.0, 0.01, false),
            passability::speedMultiplierVertical,
            value -> passability.setSpeedMultiplierVertical((float) value),
            ConfigScreen::percentage,
            modifiersAvailable,
            this::leafWalkingRequirement
        );

        addToggle(
            Tab.PASSABILITY, "sounds", "sounds_players_only",
            passability::soundsPlayerOnly,
            passability::setSoundsPlayerOnly
        );

        addNumber(
            Tab.PASSABILITY, "sounds", "sound_volume",
            new NumericRange(0.0, 1.0, 10.0, 0.05, false),
            passability::soundVolume,
            value -> passability.setSoundVolume((float) value),
            value -> Component.literal(
                String.format(Locale.ROOT, "%.2f×", value)
            ),
            ALWAYS_AVAILABLE,
            NO_REQUIREMENT
        );
    }

    private void createWindOptions() {
        addToggle(
            Tab.WIND, "ambient_waving", "ambient_waving",
            Config::wavingFoliage,
            Config::setWavingFoliage,
            Config::gpuRenderer,
            this::gpuRequirement
        );

        addToggle(
            Tab.WIND, "ambient_waving", "waving_leaves",
            Config::wavingLeaves,
            enabled -> {
                Config.setWavingLeaves(enabled);
                GpuFoliageRenderer.foliageListChanged();
            },
            () -> wavingAvailable() && WavingWhitelist.leavesCanWave(),
            this::leavesRequirement
        );

        addNumber(
            Tab.WIND, "ambient_waving", "waving_strength",
            WAVING_STRENGTH_RANGE,
            Config::wavingIntensity,
            value -> Config.setWavingIntensity((float) value),
            ConfigScreen::decimal,
            this::wavingAvailable,
            this::wavingRequirement
        );

        addToggle(
            Tab.WIND, "weather_wind", "weather_wind",
            Config::weatherWind,
            Config::setWeatherWind,
            Config::gpuRenderer,
            this::gpuRequirement
        );

        ChoiceSlider<Config.GpuDistance> weatherDistanceSlider = addChoice(
            Tab.WIND, "weather_wind", "weather_wind_distance",
            List.of(
                Config.GpuDistance.PERFORMANCE,
                Config.GpuDistance.HALF,
                Config.GpuDistance.FULL
            ),
            Config.weatherWindDistance(),
            distance -> Component.translatable(distance.translationKey()),
            distance -> {
                Config.setWeatherWindDistance(distance);
                updatePresetIndicator();
            },
            () -> Config.gpuRenderer() && Config.weatherWind(),
            this::weatherRequirement
        );

        synchronizers.add(
            () -> weatherDistanceSlider.setSelected(Config.weatherWindDistance())
        );
    }

    private void createFooter() {
        scrollUpButton = addRenderableWidget(
            Button.builder(translate("scroll.up"), ignored -> scrollBy(-1))
                .bounds(contentX, height - 54, 32, CONTROL_HEIGHT)
                .tooltip(Tooltip.create(translate("scroll.up.tooltip")))
                .build()
        );

        scrollDownButton = addRenderableWidget(
            Button.builder(translate("scroll.down"), ignored -> scrollBy(1))
                .bounds(
                    contentX + contentWidth - 32,
                    height - 54,
                    32,
                    CONTROL_HEIGHT
                )
                .tooltip(Tooltip.create(translate("scroll.down.tooltip")))
                .build()
        );

        addRenderableWidget(
            Button.builder(translate("save"), ignored -> onClose())
                .bounds(contentX, height - 28, contentWidth, CONTROL_HEIGHT)
                .tooltip(Tooltip.create(translate("save.tooltip")))
                .build()
        );
    }

    private void addToggle(
        Tab tab,
        String group,
        String key,
        BooleanSupplier getter,
        Consumer<Boolean> setter
    ) {
        addToggle(
            tab, group, key, getter, setter,
            ALWAYS_AVAILABLE, NO_REQUIREMENT
        );
    }

    private void addToggle(
        Tab tab,
        String group,
        String key,
        BooleanSupplier getter,
        Consumer<Boolean> setter,
        BooleanSupplier available,
        Supplier<String> requirement
    ) {
        CycleButton<Boolean> button = CycleButton.booleanBuilder(
            translate("on"),
            translate("off"),
            getter.getAsBoolean()
        )
            .displayOnlyValue()
            .create(
                0, 0, controlWidth, CONTROL_HEIGHT,
                translate(key + ".label"),
                (control, enabled) -> {
                    if (!synchronizing) {
                        setter.accept(enabled);
                        refreshState();
                    }
                }
            );

        addRow(tab, group, key, button, null, available, requirement);
        synchronizers.add(() -> button.setValue(getter.getAsBoolean()));
    }

    private void addNumber(
        Tab tab,
        String group,
        String key,
        NumericRange range,
        DoubleSupplier getter,
        DoubleConsumer setter,
        DoubleFunction<Component> formatter,
        BooleanSupplier available,
        Supplier<String> requirement
    ) {
        NumericSlider slider = new NumericSlider(
            controlWidth - RESET_WIDTH - CONTROL_GAP,
            key,
            range,
            getter,
            setter,
            formatter
        );

        addRow(
            tab,
            group,
            key,
            slider,
            slider.resetButton,
            available,
            requirement
        );

        synchronizers.add(slider::refresh);
    }

    private <T> ChoiceSlider<T> addChoice(
        Tab tab,
        String group,
        String key,
        List<T> choices,
        T initialSelection,
        Function<T, Component> formatter,
        Consumer<T> onChange,
        BooleanSupplier available,
        Supplier<String> requirement
    ) {
        ChoiceSlider<T> slider = new ChoiceSlider<>(
            controlWidth,
            translate(key + ".label"),
            choices,
            initialSelection,
            formatter,
            onChange
        );

        addRow(tab, group, key, slider, null, available, requirement);
        return slider;
    }

    private void addRow(
        Tab tab,
        String group,
        String key,
        AbstractWidget control,
        Button resetButton,
        BooleanSupplier available,
        Supplier<String> requirement
    ) {
        addRenderableWidget(control);

        if (resetButton != null) {
            addRenderableWidget(resetButton);
        }

        rows.get(tab).add(
            new OptionRow(
                group,
                key,
                control,
                resetButton,
                available,
                requirement,
                font.split(translate(key + ".label"), labelWidth)
            )
        );
    }

    private void selectTab(Tab tab) {
        applyPendingChanges();
        selectedTab = tab;

        tabButtons.forEach(
            (owner, button) -> button.active = owner != selectedTab
        );

        layoutVisibleRows();
        setFocused(null);
    }

    private void scrollBy(int amount) {
        if (isDragging()) {
            return;
        }

        applyPendingChanges();

        int current = scrollPositions.getOrDefault(selectedTab, 0);
        int maximum = Math.max(0, rows.get(selectedTab).size() - 1);
        int next = Math.clamp(current + amount, 0, maximum);

        if (next != current) {
            scrollPositions.put(selectedTab, next);
            layoutVisibleRows();
            setFocused(null);
        }
    }

    @Override
    public boolean mouseScrolled(
        double mouseX,
        double mouseY,
        double horizontalAmount,
        double verticalAmount
    ) {
        boolean insideContent = mouseX >= contentX
            && mouseX < contentX + contentWidth
            && mouseY >= CONTENT_TOP
            && mouseY < height - 58;

        if (insideContent && verticalAmount != 0.0) {
            if (verticalAmount > 0.0 && firstVisibleRow > 0) {
                scrollBy(-1);
            } else if (
                verticalAmount < 0.0
                    && lastVisibleRowExclusive < rows.get(selectedTab).size()
            ) {
                scrollBy(1);
            }

            return true;
        }

        return super.mouseScrolled(
            mouseX, mouseY, horizontalAmount, verticalAmount
        );
    }

    /**
     * Only complete rows are visible and interactive.
     */
    private void layoutVisibleRows() {
        for (List<OptionRow> tabRows : rows.values()) {
            for (OptionRow row : tabRows) {
                row.setVisible(false);
            }
        }

        List<OptionRow> selectedRows = rows.get(selectedTab);

        firstVisibleRow = Math.clamp(
            scrollPositions.getOrDefault(selectedTab, 0),
            0,
            Math.max(0, selectedRows.size() - 1)
        );
        lastVisibleRowExclusive = firstVisibleRow;

        int y = CONTENT_TOP;
        String previousGroup = null;

        for (int index = firstVisibleRow; index < selectedRows.size(); index++) {
            OptionRow row = selectedRows.get(index);

            boolean showGroup = !row.group.isEmpty()
                && !row.group.equals(previousGroup);

            int headingHeight = showGroup ? GROUP_HEIGHT : 0;
            int labelHeight = row.labelLines.size() * font.lineHeight;
            int bodyHeight = Math.max(CONTROL_HEIGHT, labelHeight);
            int rowHeight = headingHeight + bodyHeight + ROW_GAP;

            if (y + rowHeight > contentBottom) {
                break;
            }

            row.headingY = showGroup ? y : -1;
            row.labelY = y + headingHeight + (bodyHeight - labelHeight) / 2;

            row.control.setX(contentX + labelWidth + COLUMN_GAP);
            row.control.setY(
                y + headingHeight + (bodyHeight - CONTROL_HEIGHT) / 2
            );

            if (row.resetButton != null) {
                row.resetButton.setX(contentX + contentWidth - RESET_WIDTH);
                row.resetButton.setY(row.control.getY());
            }

            row.setVisible(true);

            previousGroup = row.group;
            lastVisibleRowExclusive = index + 1;
            y += rowHeight;
        }

        scrollPositions.put(selectedTab, firstVisibleRow);

        boolean scrollable = firstVisibleRow > 0
            || lastVisibleRowExclusive < selectedRows.size();

        scrollUpButton.visible = scrollable;
        scrollDownButton.visible = scrollable;
        scrollUpButton.active = firstVisibleRow > 0;
        scrollDownButton.active = lastVisibleRowExclusive < selectedRows.size();
    }

    private void refreshState() {
        if (!initialized || synchronizing) {
            return;
        }

        for (List<OptionRow> tabRows : rows.values()) {
            for (OptionRow row : tabRows) {
                row.refreshAvailability();
            }
        }

        updatePresetIndicator();
    }

    private void synchronizeControls() {
        synchronizing = true;

        try {
            for (Runnable synchronize : synchronizers) {
                synchronize.run();
            }
        } finally {
            synchronizing = false;
        }

        refreshState();
    }

    private void updatePresetIndicator() {
        if (initialized && pendingPreset == null) {
            presetSlider.setSelected(
                Config.Preset.current(displayedGpuDistance())
            );
        }
    }

    private Config.GpuDistance displayedGpuDistance() {
        return pendingGpuDistance == null
            ? Config.gpuDistance()
            : pendingGpuDistance;
    }

    /**
     * Applies deferred graphics changes after dragging, before switching
     * tabs, before rebuilding the screen, and before closing.
     */
    private void applyPendingChanges() {
        if (!initialized) {
            return;
        }

        Config.Preset preset = pendingPreset;
        pendingPreset = null;

        if (preset != null && !preset.matches(displayedGpuDistance())) {
            preset.applyAllButGpuDistance();

            if (preset.gpuDistance() != null) {
                pendingGpuDistance = preset.gpuDistance();
            }
        }

        if (pendingGpuDistance != null) {
            if (pendingGpuDistance != Config.gpuDistance()) {
                Config.setGpuDistance(pendingGpuDistance);
            }

            pendingGpuDistance = null;
        }

        if (preset != null) {
            synchronizeControls();
        }
    }

    private String gpuRequirement() {
        return Config.gpuRenderer() ? null : "requires.gpu";
    }

    private String interactionRequirement() {
        return swayConfig.enabled ? null : "requires.interaction";
    }

    private boolean wavingAvailable() {
        return Config.gpuRenderer() && Config.wavingFoliage();
    }

    private String wavingRequirement() {
        if (!Config.gpuRenderer()) {
            return "requires.gpu";
        }

        return Config.wavingFoliage() ? null : "requires.waving";
    }

    private String leavesRequirement() {
        String requirement = wavingRequirement();

        if (requirement != null) {
            return requirement;
        }

        return WavingWhitelist.leavesCanWave()
            ? null
            : "requires.fancy_leaves";
    }

    private String weatherRequirement() {
        if (!Config.gpuRenderer()) {
            return "requires.gpu";
        }

        return Config.weatherWind() ? null : "requires.weather_wind";
    }

    private String leafWalkingRequirement() {
        return Config.passability().alwaysLeafWalking()
            ? "requires.leaf_walking_off"
            : null;
    }

    @Override
    public void tick() {
        super.tick();

        if (!isDragging()) {
            applyPendingChanges();
        }
    }

    @Override
    public void onClose() {
        applyPendingChanges();
        Config.save();
        SwayConfig.save();
        minecraft.setScreenAndShow(parent);
    }

    @Override
    public void extractRenderState(
        GuiGraphicsExtractor graphics,
        int mouseX,
        int mouseY,
        float partialTick
    ) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        graphics.centeredText(font, title, width / 2, 15, TEXT_COLOR);

        List<OptionRow> selectedRows = rows.get(selectedTab);

        for (
            int index = firstVisibleRow;
            index < lastVisibleRowExclusive;
            index++
        ) {
            OptionRow row = selectedRows.get(index);

            if (row.headingY >= 0) {
                graphics.text(
                    font,
                    translate("group." + row.group),
                    contentX,
                    row.headingY,
                    GROUP_TEXT_COLOR
                );
            }

            int y = row.labelY;
            int color = row.control.active ? TEXT_COLOR : DISABLED_TEXT_COLOR;

            for (FormattedCharSequence line : row.labelLines) {
                graphics.text(font, line, contentX, y, color);
                y += font.lineHeight;
            }
        }

        if (scrollUpButton.visible || scrollDownButton.visible) {
            graphics.centeredText(
                font,
                translate(
                    "scroll.position",
                    lastVisibleRowExclusive > firstVisibleRow
                        ? firstVisibleRow + 1
                        : 0,
                    lastVisibleRowExclusive,
                    selectedRows.size()
                ),
                width / 2,
                height - 48,
                GROUP_TEXT_COLOR
            );
        }
    }

    private static MutableComponent translate(String key, Object... arguments) {
        return Component.translatable(TRANSLATION_PREFIX + key, arguments);
    }

    private static MutableComponent labeledValue(String key, Component value) {
        return Component.translatable(
            "options.generic_value",
            translate(key + ".label"),
            value
        );
    }

    private static Component decimal(double value) {
        return Component.literal(String.format(Locale.ROOT, "%.1f", value));
    }

    private static Component percentage(double value) {
        return Component.literal(
            String.format(Locale.ROOT, "%.0f%%", value * 100.0)
        );
    }

    private static Component blocks(double value) {
        return translate("value.blocks", Math.round(value));
    }

    private enum Tab {
        GENERAL("general"),
        INTERACTION("interaction"),
        PASSABILITY("passability"),
        WIND("wind");

        private final String translationSuffix;

        Tab(String translationSuffix) {
            this.translationSuffix = translationSuffix;
        }
    }

    private static final class OptionRow {

        private final String group;
        private final String key;
        private final AbstractWidget control;
        private final Button resetButton;
        private final BooleanSupplier available;
        private final Supplier<String> requirement;
        private final List<FormattedCharSequence> labelLines;

        private String previousRequirement;
        private boolean tooltipInitialized;
        private int headingY = -1;
        private int labelY;

        private OptionRow(
            String group,
            String key,
            AbstractWidget control,
            Button resetButton,
            BooleanSupplier available,
            Supplier<String> requirement,
            List<FormattedCharSequence> labelLines
        ) {
            this.group = group;
            this.key = key;
            this.control = control;
            this.resetButton = resetButton;
            this.available = available;
            this.requirement = requirement;
            this.labelLines = List.copyOf(labelLines);
        }

        private void setVisible(boolean visible) {
            control.visible = visible;

            if (resetButton != null) {
                resetButton.visible = visible;
            }
        }

        private void refreshAvailability() {
            control.active = available.getAsBoolean();

            if (resetButton != null) {
                resetButton.active = control.active
                    && control instanceof NumericSlider slider
                    && !slider.isDefault();
            }

            String missingRequirement = requirement.get();

            if (
                tooltipInitialized
                    && Objects.equals(previousRequirement, missingRequirement)
            ) {
                return;
            }

            MutableComponent tooltip = translate(key + ".tooltip");

            if (missingRequirement != null) {
                tooltip.append("\n\n").append(translate(missingRequirement));
            }

            control.setTooltip(Tooltip.create(tooltip));

            if (resetButton != null) {
                MutableComponent resetTooltip = translate("reset_to_default");

                if (missingRequirement != null) {
                    resetTooltip.append("\n\n")
                        .append(translate(missingRequirement));
                }

                resetButton.setTooltip(Tooltip.create(resetTooltip));
            }

            previousRequirement = missingRequirement;
            tooltipInitialized = true;
        }
    }

    private record NumericRange(
        double minimum,
        double defaultValue,
        double maximum,
        double step,
        boolean centeredDefault
    ) {

        private NumericRange {
            if (
                !Double.isFinite(minimum)
                    || !Double.isFinite(defaultValue)
                    || !Double.isFinite(maximum)
                    || !Double.isFinite(step)
                    || minimum >= maximum
                    || defaultValue < minimum
                    || defaultValue > maximum
                    || step <= 0.0
                    || (
                        centeredDefault
                            && (defaultValue == minimum || defaultValue == maximum)
                    )
            ) {
                throw new IllegalArgumentException("Invalid numeric slider range");
            }
        }

        private double toPosition(double number) {
            double sanitized = Double.isFinite(number)
                ? Math.clamp(number, minimum, maximum)
                : defaultValue;

            if (!centeredDefault) {
                return (sanitized - minimum) / (maximum - minimum);
            }

            return sanitized <= defaultValue
                ? 0.5 * (sanitized - minimum) / (defaultValue - minimum)
                : 0.5 + 0.5 * (sanitized - defaultValue)
                    / (maximum - defaultValue);
        }

        private double fromPosition(double position) {
            double clamped = Math.clamp(position, 0.0, 1.0);
            double number;

            if (!centeredDefault) {
                number = minimum + clamped * (maximum - minimum);
            } else if (clamped <= 0.5) {
                number = minimum + clamped * 2.0 * (defaultValue - minimum);
            } else {
                number = defaultValue
                    + (clamped - 0.5) * 2.0 * (maximum - defaultValue);
            }

            double snapped = minimum
                + Math.round((number - minimum) / step) * step;

            return Math.clamp(snapped, minimum, maximum);
        }
    }

    private static final class NumericSlider extends AbstractSliderButton {

        private final String key;
        private final NumericRange range;
        private final DoubleSupplier getter;
        private final DoubleConsumer setter;
        private final DoubleFunction<Component> formatter;
        private final Button resetButton;

        private double displayedValue;
        private boolean messageInitialized;

        private NumericSlider(
            int width,
            String key,
            NumericRange range,
            DoubleSupplier getter,
            DoubleConsumer setter,
            DoubleFunction<Component> formatter
        ) {
            super(
                0,
                0,
                width,
                CONTROL_HEIGHT,
                Component.empty(),
                range.toPosition(getter.getAsDouble())
            );

            this.key = key;
            this.range = range;
            this.getter = getter;
            this.setter = setter;
            this.formatter = formatter;

            resetButton = Button.builder(
                translate("reset"),
                ignored -> reset()
            )
                .bounds(0, 0, RESET_WIDTH, CONTROL_HEIGHT)
                .build();

            updateMessage();
        }

        @Override
        protected void updateMessage() {
            double currentValue = getter.getAsDouble();

            if (
                messageInitialized
                    && Double.compare(displayedValue, currentValue) == 0
            ) {
                return;
            }

            setMessage(formatter.apply(currentValue));
            displayedValue = currentValue;
            messageInitialized = true;
        }

        @Override
        protected MutableComponent createNarrationMessage() {
            return Component.translatable(
                "gui.narrate.slider",
                labeledValue(key, getMessage())
            );
        }

        @Override
        protected void applyValue() {
            double nextValue = range.fromPosition(value);

            if (sameValue(nextValue, getter.getAsDouble())) {
                return;
            }

            setter.accept(nextValue);
            updateMessage();
            refreshResetButton();
        }

        private boolean isDefault() {
            return sameValue(getter.getAsDouble(), range.defaultValue());
        }

        private void refresh() {
            value = range.toPosition(getter.getAsDouble());
            updateMessage();
            refreshResetButton();
        }

        private void refreshResetButton() {
            resetButton.active = active && !isDefault();
        }

        private void reset() {
            if (!isDefault()) {
                setter.accept(range.defaultValue());
            }

            // Synchronize the handle even when the stored value is unchanged.
            refresh();
        }

        private static boolean sameValue(double first, double second) {
            /*
             * These settings are stored as floats or small integers.
             * Compare at their storage precision to avoid redundant writes
             * caused by float-to-double conversion.
             */
            return Float.compare((float) first, (float) second) == 0;
        }
    }

    private static final class ChoiceSlider<T> extends AbstractSliderButton {

        private final Component optionLabel;
        private final List<T> choices;
        private final Function<T, Component> formatter;
        private final Consumer<T> onChange;

        private T selected;
        private T displayedSelection;
        private boolean messageInitialized;

        private ChoiceSlider(
            int width,
            Component optionLabel,
            List<T> choices,
            T initialSelection,
            Function<T, Component> formatter,
            Consumer<T> onChange
        ) {
            super(
                0,
                0,
                width,
                CONTROL_HEIGHT,
                Component.empty(),
                initialPosition(choices, initialSelection)
            );

            this.optionLabel = optionLabel;
            this.choices = List.copyOf(choices);
            this.selected = initialSelection;
            this.formatter = formatter;
            this.onChange = onChange;

            updateMessage();
        }

        @Override
        protected void updateMessage() {
            if (
                messageInitialized
                    && Objects.equals(displayedSelection, selected)
            ) {
                return;
            }

            setMessage(formatter.apply(selected));
            displayedSelection = selected;
            messageInitialized = true;
        }

        @Override
        protected MutableComponent createNarrationMessage() {
            return Component.translatable(
                "gui.narrate.slider",
                Component.translatable(
                    "options.generic_value",
                    optionLabel,
                    getMessage()
                )
            );
        }

        @Override
        protected void applyValue() {
            int index = (int) Math.round(value * (choices.size() - 1));
            T nextSelection = choices.get(
                Math.clamp(index, 0, choices.size() - 1)
            );

            if (Objects.equals(selected, nextSelection)) {
                return;
            }

            selected = nextSelection;
            updateMessage();
            onChange.accept(nextSelection);
        }

        private void setSelected(T selection) {
            selected = selection;

            // A null selection represents a custom preset.
            int index = selection == null ? -1 : choices.indexOf(selection);

            if (index >= 0) {
                /*
                 * Always synchronize the handle. An unchanged selection
                 * does not necessarily mean its handle is already aligned.
                 */
                value = choices.size() == 1
                    ? 0.0
                    : (double) index / (choices.size() - 1);
            }

            updateMessage();
        }

        private static <T> double initialPosition(
            List<T> choices,
            T selection
        ) {
            if (choices.isEmpty()) {
                throw new IllegalArgumentException(
                    "A choice slider requires at least one choice"
                );
            }

            int index = selection == null
                ? 0
                : Math.max(0, choices.indexOf(selection));

            return choices.size() == 1
                ? 0.0
                : (double) index / (choices.size() - 1);
        }
    }
}
