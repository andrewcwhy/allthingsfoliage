package io.github.andrewcwhy.allthingsfoliage;

import com.github.razorplay01.sway.config.SwayConfig;
import io.github.andrewcwhy.allthingsfoliage.gpu.GpuFoliageRenderer;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
//? >=1.20.1 {
import net.minecraft.client.gui.components.Checkbox;
//?}
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class FoliageConfigScreen extends Screen {

    private final Screen parent;
    private final SwayConfig config = SwayConfig.INSTANCE;

    /** Sway accepts weaker than this, but below half strength the interaction is barely visible. */
    private static final float MIN_INTENSITY = 0.5f;
    private static final float DEFAULT_INTENSITY = 1.0f;
    private static final float MAX_INTENSITY = 2.0f;

    private IntensitySlider intensitySlider;
    private RadiusSlider radiusSlider;
    private Button resetIntensityBtn;
    private Button resetRadiusBtn;
    /** How far apart the options sit, and how close they come when the window is too short for that. */
    private static final int ROW_SPACING = 30;
    private static final int TIGHT_ROW_SPACING = 24;
    /** The least room kept free above the title and below the save button together. */
    private static final int SCREEN_MARGIN = 10;
    /** A full-width option, and the two halves options share a row in, with the gap between them. */
    private static final int FULL_WIDTH = 200;
    private static final int COLUMN_WIDTH = 150;
    private static final int COLUMN_GAP = 4;
    /** A slider that can be reset leaves room beside it for its reset button. */
    private static final int RESET_WIDTH = 18;
    private static final int RESET_GAP = 4;

    private CycleButton<Boolean> interactionBtn;
    //? >=1.20.1 {
    private PresetSlider presetSlider;
    private CycleButton<Boolean> rendererBtn;
    private GpuDistanceSlider gpuDistanceSlider;
    private CycleButton<Boolean> weatherWindBtn;
    private WeatherWindDistanceSlider weatherWindDistanceSlider;
    private CycleButton<Boolean> wavingFoliageBtn;
    private WavingIntensitySlider wavingIntensitySlider;
    private Button resetWavingIntensityBtn;
    //?}
    //? >=1.20.1 {
    private Checkbox wavingLeavesBox;

    //?}

    public FoliageConfigScreen(Screen parent) {
        super(Component.translatable("config.allthingsfoliage.title"));
        this.parent = parent;
    }

    /**
     * Where the first option goes, so the whole screen sits centred whatever its height: the window's, the
     * interface scale's, or the mod's own, since some versions have fewer options than others.
     * <p>
     * The title sits 20 above this, the options follow a row apart, and the save button comes a row and 10 more
     * after the last one. Options that are hidden keep their place, so the height never changes while the screen is
     * open.
     */
    private int topOfOptions() {
        return Math.max((this.height - heightWith(rowSpacing())) / 2 + 20, 20);
    }

    /** The rows spread out as usual, unless the window is too short to fit them that way. */
    private int rowSpacing() {
        return this.height >= heightWith(ROW_SPACING) + SCREEN_MARGIN
            ? ROW_SPACING
            : TIGHT_ROW_SPACING;
    }

    /** From the top of the title to the bottom of the save button. */
    private static int heightWith(int spacing) {
        // The switch, then the interaction's two sliders.
        int rows = 2;
        int extra = 0;
        //? >=1.20.1 {
        // The preset, set apart from the options below it, then the renderer, the weather wind and the wind, each
        // beside its own setting.
        rows += 4;
        extra += 10;
        //?}
        //? >=1.20.1 {
        // The leaves' checkbox, under the wind.
        rows += 1;
        //?}
        return 20 + (rows - 1) * spacing + extra + (spacing + 10) + 20;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = topOfOptions();
        int spacing = rowSpacing();

        // Options share a row in two columns; the switch and the save button span the middle.
        int left = cx - COLUMN_WIDTH - COLUMN_GAP / 2;
        int right = cx + COLUMN_GAP / 2;
        int sliderWidth = COLUMN_WIDTH - RESET_WIDTH - RESET_GAP;

        //? >=1.20.1 {
        // ── Preset, across both columns and set apart: the first thing to choose, the rest follows from it ──
        presetSlider = new PresetSlider(
            left,
            y,
            COLUMN_WIDTH * 2 + COLUMN_GAP,
            20
        );
        this.addRenderableWidget(presetSlider);

        y += spacing + 10;
        //?}

        // ── Interaction ON/OFF ─────────────────────────────────────────────────
        interactionBtn = CycleButton.booleanBuilder(
            Component.translatable("config.allthingsfoliage.on"),
            Component.translatable("config.allthingsfoliage.off"),
            //? > 1.21.1 {
            config.enabled
            //?}
        )
            //? <=1.21.1 {
            /*.withInitialValue(config.enabled)
             */ //?}
            .create(
                cx - FULL_WIDTH / 2,
                y,
                FULL_WIDTH,
                20,
                Component.translatable("config.allthingsfoliage.enabled"),
                (btn, val) -> {
                    config.enabled = val;
                    updateInteractionOptions();
                    updatePreset();
                }
            );
        this.addRenderableWidget(interactionBtn);

        y += spacing;

        // ── Intensity and interaction radius, shown only with the interaction on ──
        // The reset button on the slider's outer side, so a hidden one leaves no gap in the middle of the screen.
        intensitySlider = new IntensitySlider(
            left + RESET_WIDTH + RESET_GAP,
            y,
            sliderWidth,
            20,
            toSliderPosition(
                config.intensity,
                MIN_INTENSITY,
                DEFAULT_INTENSITY,
                MAX_INTENSITY
            )
        );
        this.addRenderableWidget(intensitySlider);

        // Resets in place: rebuilding the screen flashes the world behind it black for a few frames.
        resetIntensityBtn = Button.builder(
            Component.translatable("config.allthingsfoliage.reset"),
            btn -> intensitySlider.reset()
        )
            .bounds(left, y, RESET_WIDTH, 20)
            .build();
        this.addRenderableWidget(resetIntensityBtn);

        radiusSlider = new RadiusSlider(
            right,
            y,
            sliderWidth,
            20,
            radiusToSlider(config.maxDistance)
        );
        this.addRenderableWidget(radiusSlider);

        resetRadiusBtn = Button.builder(
            Component.translatable("config.allthingsfoliage.reset"),
            btn -> radiusSlider.reset()
        )
            .bounds(right + sliderWidth + RESET_GAP, y, RESET_WIDTH, 20)
            .build();
        this.addRenderableWidget(resetRadiusBtn);
        updateInteractionOptions();

        //? >=1.20.1 {
        y += spacing;

        // ── Renderer: the chunk mesh as always, or the mod's own on the GPU; beside it how far the GPU one reaches ──
        rendererBtn = CycleButton.booleanBuilder(
            // Written out rather than translated: the two read the same in every language.
            Component.literal("GPU"),
            Component.literal("CPU"),
            //? >1.21.1 {
            FoliageSettings.gpuRenderer()
            //?}
        )
            //? <=1.21.1 {
            /*.withInitialValue(FoliageSettings.gpuRenderer())
             */ //?}
            .create(
                left,
                y,
                COLUMN_WIDTH,
                20,
                Component.translatable("config.allthingsfoliage.renderer"),
                (btn, val) -> {
                    FoliageSettings.setGpuRenderer(val);
                    updateGpuOptions();
                    updatePreset();
                }
            );
        // Shown but greyed out where the chunk mesh cannot move plants, so it reads GPU rather than offering a CPU
        // setting that would leave the mod doing nothing.
        rendererBtn.active = FoliageSettings.cpuRendererAvailable();
        this.addRenderableWidget(rendererBtn);

        gpuDistanceSlider = new GpuDistanceSlider(right, y, COLUMN_WIDTH, 20);
        this.addRenderableWidget(gpuDistanceSlider);

        y += spacing;

        // ── Weather wind, and how far from the player it reaches; with the GPU renderer only ──
        weatherWindBtn = CycleButton.booleanBuilder(
            Component.translatable("config.allthingsfoliage.on"),
            Component.translatable("config.allthingsfoliage.off"),
            //? >1.21.1 {
            FoliageSettings.weatherWind()
            //?}
        )
            //? <=1.21.1 {
            /*.withInitialValue(FoliageSettings.weatherWind())
             */ //?}
            .create(
                left,
                y,
                COLUMN_WIDTH,
                20,
                Component.translatable("config.allthingsfoliage.weather_wind"),
                (btn, val) -> {
                    FoliageSettings.setWeatherWind(val);
                    updateGpuOptions();
                    updatePreset();
                }
            );
        this.addRenderableWidget(weatherWindBtn);

        weatherWindDistanceSlider = new WeatherWindDistanceSlider(
            right,
            y,
            COLUMN_WIDTH,
            20
        );
        this.addRenderableWidget(weatherWindDistanceSlider);

        y += spacing;

        // ── Wind, and its strength; with the GPU renderer only ─────────────────
        wavingFoliageBtn = CycleButton.booleanBuilder(
            Component.translatable("config.allthingsfoliage.on"),
            Component.translatable("config.allthingsfoliage.off"),
            //? >1.21.1 {
            FoliageSettings.wavingFoliage()
            //?}
        )
            //? <=1.21.1 {
            /*.withInitialValue(FoliageSettings.wavingFoliage())
             */ //?}
            .create(
                left,
                y,
                COLUMN_WIDTH,
                20,
                Component.translatable(
                    "config.allthingsfoliage.waving_foliage"
                ),
                (btn, val) -> {
                    FoliageSettings.setWavingFoliage(val);
                    updateGpuOptions();
                    updatePreset();
                }
            );
        this.addRenderableWidget(wavingFoliageBtn);

        wavingIntensitySlider = new WavingIntensitySlider(
            right,
            y,
            sliderWidth,
            20,
            wavingIntensityToSlider(FoliageSettings.wavingIntensity())
        );
        this.addRenderableWidget(wavingIntensitySlider);

        resetWavingIntensityBtn = Button.builder(
            Component.translatable("config.allthingsfoliage.reset"),
            btn -> wavingIntensitySlider.reset()
        )
            .bounds(right + sliderWidth + RESET_GAP, y, RESET_WIDTH, 20)
            .build();
        this.addRenderableWidget(resetWavingIntensityBtn);

        y += spacing;

        // ── Whether leaves wave too, with the wind ──────────────────────────────
        // Switching them rebuilds the near sections holding leaves: whether a leaf is drawn by the chunk mesh or the
        // renderer changes there.
        //? >=1.21.1 {
        wavingLeavesBox = Checkbox.builder(
            Component.translatable("config.allthingsfoliage.waving_leaves"),
            this.font
        )
            .pos(left, y + (20 - Checkbox.getBoxSize(this.font)) / 2)
            .selected(FoliageSettings.wavingLeaves())
            .onValueChange((box, value) -> {
                FoliageSettings.setWavingLeaves(value);
                GpuFoliageRenderer.foliageListChanged();
            })
            .build();
        // Centred across both columns, as the interaction's switch is: the box and its text are as wide as the text.
        wavingLeavesBox.setX(cx - wavingLeavesBox.getWidth() / 2);
        //?} else {
        /*// Before 1.20.3 a checkbox is built whole, as wide as it is told, and only says it was pressed: a 20 pixel box,
		// then its text.
		Component wavingLeavesLabel = Component.translatable("config.allthingsfoliage.waving_leaves");
		int wavingLeavesWidth = 20 + 4 + this.font.width(wavingLeavesLabel);
		wavingLeavesBox = new WavingLeavesCheckbox(cx - wavingLeavesWidth / 2, y, wavingLeavesWidth, wavingLeavesLabel,
				FoliageSettings.wavingLeaves());
		*/ //?}
        this.addRenderableWidget(wavingLeavesBox);
        updateGpuOptions();
        updatePreset();
        //?}

        y += spacing + 10;

        // ── Guardar ────────────────────────────────────────────────────────────
        this.addRenderableWidget(
            Button.builder(
                Component.translatable("config.allthingsfoliage.save"),
                btn -> {
                    //? >=1.20.1 {
                    // A preset picked just before saving is applied first: it sets Sway's switch too.
                    applyPreset();
                    applyGpuDistance();
                    FoliageSettings.save();
                    //?}
                    SwayConfig.save();
                    //? >=26.2{
                    this.minecraft.setScreenAndShow(parent);
                    //?} else{
                    /*this.minecraft.setScreen(parent);
                     */ //?}
                }
            )
                .bounds(cx - FULL_WIDTH / 2, y, FULL_WIDTH, 20)
                .build()
        );
    }

    @Override
    public void onClose() {
        //? >=1.20.1 {
        applyPreset();
        applyGpuDistance();
        FoliageSettings.save();
        //?}
        SwayConfig.save();
        //? >=26.2{
        this.minecraft.setScreenAndShow(parent);
        //?} else{
        /*this.minecraft.setScreen(parent);
         */ //?}
    }

    //? <= 1.21.11 {
    /*@Override
	public void render(
			net.minecraft.client.gui.GuiGraphics graphics,
			int mouseX, int mouseY, float delta
	) {
		//? <1.21.1 {
		/^// Before 1.20.2 a screen draws no background of its own: this one dims the world behind it, or shows the dirt
		// background from the title screen, as vanilla's option screens do.
		this.renderBackground(graphics);
		^///?}
		super.render(graphics, mouseX, mouseY, delta);
		graphics.drawCenteredString(
				this.font, this.title,
				this.width / 2, topOfOptions() - 20,
				0xFFFFFF
		);
	}
	*/ //?}
    //? > 1.21.11 {
    @Override
    public void extractRenderState(
        net.minecraft.client.gui.GuiGraphicsExtractor graphics,
        int mouseX,
        int mouseY,
        float a
    ) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.centeredText(
            this.font,
            this.title,
            this.width / 2,
            topOfOptions() - 20,
            0xFFFFFF
        );
    }

    //?}

    /**
     * Where a value sits on a slider that holds its default in the middle: the left half runs down to the
     * minimum and the right half up to the maximum, so weaker and stronger each get half the travel.
     */
    private static double toSliderPosition(
        float value,
        float min,
        float def,
        float max
    ) {
        double position =
            value <= def
                ? (0.5 * (value - min)) / (def - min)
                : 0.5 + (0.5 * (value - def)) / (max - def);
        // A config file edited by hand may hold a value from outside the slider's range. Math.clamp would say
        // this better, but this screen is shared with 1.20.1, which is built against Java 17.
        return Math.max(0.0, Math.min(1.0, position));
    }

    /** The inverse of {@link #toSliderPosition}, snapped to steps of 0.1. */
    private static float fromSliderPosition(
        double position,
        float min,
        float def,
        float max
    ) {
        double value =
            position <= 0.5
                ? min + (position / 0.5) * (def - min)
                : def + ((position - 0.5) / 0.5) * (max - def);
        return Math.round(value * 10.0) / 10.0f;
    }

    private class IntensitySlider extends AbstractSliderButton {

        public IntensitySlider(
            int x,
            int y,
            int w,
            int h,
            double initialValue
        ) {
            super(x, y, w, h, Component.empty(), initialValue);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(
                Component.translatable(
                    "config.allthingsfoliage.intensity",
                    String.format("%.1f", config.intensity)
                )
            );
            updateInteractionOptions();
        }

        @Override
        protected void applyValue() {
            config.intensity = fromSliderPosition(
                value,
                MIN_INTENSITY,
                DEFAULT_INTENSITY,
                MAX_INTENSITY
            );
        }

        /** Back to the default in place, without rebuilding the screen. */
        void reset() {
            config.intensity = DEFAULT_INTENSITY;
            value = toSliderPosition(
                DEFAULT_INTENSITY,
                MIN_INTENSITY,
                DEFAULT_INTENSITY,
                MAX_INTENSITY
            );
            updateMessage();
        }
    }

    private class RadiusSlider extends AbstractSliderButton {

        public RadiusSlider(int x, int y, int w, int h, double initialValue) {
            super(x, y, w, h, Component.empty(), initialValue);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(
                Component.translatable(
                    "config.allthingsfoliage.radius",
                    String.format("%.0f", config.maxDistance)
                )
            );
            updateInteractionOptions();
        }

        @Override
        protected void applyValue() {
            // The handle slides freely; the radius itself moves a step at a time, as the intensities do.
            float range =
                FoliageSettings.MAX_INTERACTION_RADIUS -
                FoliageSettings.MIN_INTERACTION_RADIUS;
            config.maxDistance = FoliageSettings.snapInteractionRadius(
                FoliageSettings.MIN_INTERACTION_RADIUS + (float) value * range
            );
        }

        /** Back to the default in place, without rebuilding the screen. */
        void reset() {
            config.maxDistance = FoliageSettings.DEFAULT_INTERACTION_RADIUS;
            value = radiusToSlider(FoliageSettings.DEFAULT_INTERACTION_RADIUS);
            updateMessage();
        }
    }

    /** Where a radius sits along the slider, which runs straight from the minimum to the maximum. */
    private static double radiusToSlider(float radius) {
        double position =
            (radius - FoliageSettings.MIN_INTERACTION_RADIUS) /
            (FoliageSettings.MAX_INTERACTION_RADIUS -
                FoliageSettings.MIN_INTERACTION_RADIUS);
        return Math.max(0.0, Math.min(1.0, position));
    }

    private boolean isDefaultRadius() {
        return (
            Math.abs(
                config.maxDistance - FoliageSettings.DEFAULT_INTERACTION_RADIUS
            ) < 0.1f
        );
    }

    /**
     * The interaction's sliders show only while the interaction is on, and each one's reset button only while its value
     * sits off its default.
     */
    private void updateInteractionOptions() {
        boolean enabled = config.enabled;
        if (intensitySlider != null) {
            intensitySlider.visible = enabled;
        }
        if (radiusSlider != null) {
            radiusSlider.visible = enabled;
        }
        if (resetIntensityBtn != null) {
            resetIntensityBtn.visible =
                enabled &&
                Math.abs(config.intensity - DEFAULT_INTENSITY) > 0.01f;
        }
        if (resetRadiusBtn != null) {
            resetRadiusBtn.visible = enabled && !isDefaultRadius();
        }
    }

    //? >=1.20.1 {
    /**
     * The default sits in the middle of the slider: the left half runs down to the minimum and the right half
     * up to the maximum, so weaker and stronger each get half the travel.
     */
    private static double wavingIntensityToSlider(float intensity) {
        return toSliderPosition(
            intensity,
            FoliageSettings.MIN_WAVING_INTENSITY,
            FoliageSettings.DEFAULT_WAVING_INTENSITY,
            FoliageSettings.MAX_WAVING_INTENSITY
        );
    }

    /**
     * What the GPU renderer owns shows only while it is on: its distance, weather wind and the wind, and beside
     * those their distance and strength while each is on. The strength's reset button also waits until the value sits
     * off its default. Hidden widgets are neither drawn nor clickable, and their place is left empty.
     */
    private void updateGpuOptions() {
        boolean gpu = FoliageSettings.gpuRenderer();
        if (gpuDistanceSlider != null) {
            gpuDistanceSlider.visible = gpu;
        }
        if (weatherWindBtn != null) {
            weatherWindBtn.visible = gpu;
        }
        if (weatherWindDistanceSlider != null) {
            weatherWindDistanceSlider.visible =
                gpu && FoliageSettings.weatherWind();
        }
        if (wavingFoliageBtn != null) {
            wavingFoliageBtn.visible = gpu;
        }
        boolean wind = gpu && FoliageSettings.wavingFoliage();
        if (wavingIntensitySlider != null) {
            wavingIntensitySlider.visible = wind;
        }
        if (resetWavingIntensityBtn != null) {
            resetWavingIntensityBtn.visible =
                wind && !FoliageSettings.isDefaultWavingIntensity();
        }
        //? >=1.20.1 {
        if (wavingLeavesBox != null) {
            wavingLeavesBox.visible = wind;
            // The game draws Fast leaves solid, so where the renderer cannot draw them so too they only wave with Fancy
            // leaves: the box says so rather than doing nothing, greyed out and keeping the player's choice for then.
            boolean fancyLeaves = WavingWhitelist.leavesCanWave();
            wavingLeavesBox.active = fancyLeaves;
            wavingLeavesBox.setAlpha(fancyLeaves ? 1.0F : 0.5F);
            wavingLeavesBox.setTooltip(
                fancyLeaves
                    ? null
                    : Tooltip.create(
                          Component.translatable(
                              "config.allthingsfoliage.waving_leaves.needs_fancy"
                          )
                      )
            );
        }
        //?}
    }

    /**
     * The preset and the distance applied as soon as the handle is let go, or as soon as the arrow keys move it:
     * dragging across presets would otherwise switch the renderer at every one on the way.
     */
    @Override
    public void tick() {
        super.tick();
        if (!isDragging()) {
            applyPreset();
            applyGpuDistance();
        }
    }

    /** The preset under the preset slider's handle, once it differs from what the options stand as. */
    private void applyPreset() {
        if (presetSlider == null || presetSlider.chosen == null) {
            return;
        }
        FoliageSettings.Preset preset = presetSlider.chosen;
        presetSlider.chosen = null;
        if (preset.matches(gpuDistanceSlider.shown)) {
            return;
        }
        preset.applyAllButGpuDistance();
        if (preset.gpuDistance() != null) {
            gpuDistanceSlider.shown = preset.gpuDistance();
            gpuDistanceSlider.setPosition(
                gpuDistanceToSlider(preset.gpuDistance())
            );
        }
        // Every widget shows its new value in place.
        interactionBtn.setValue(config.enabled);
        rendererBtn.setValue(FoliageSettings.gpuRenderer());
        weatherWindBtn.setValue(FoliageSettings.weatherWind());
        wavingFoliageBtn.setValue(FoliageSettings.wavingFoliage());
        weatherWindDistanceSlider.setPosition(
            weatherWindDistanceToSlider(FoliageSettings.weatherWindDistance())
        );
        updateInteractionOptions();
        updateGpuOptions();
        updatePreset();
    }

    /**
     * Shows the preset the options stand as, or custom, after any of them changes. The GPU renderer's distance is read
     * off its slider, which the renderer only gets once the handle settles.
     */
    private void updatePreset() {
        if (presetSlider != null && gpuDistanceSlider != null) {
            presetSlider.showCurrent(
                FoliageSettings.Preset.current(gpuDistanceSlider.shown)
            );
        }
    }

    /**
     * Hands the renderer the distance the slider shows. Only a settled choice is applied: dragging from one end
     * to the other would otherwise hand the foliage over at every level on the way.
     */
    private void applyGpuDistance() {
        if (
            gpuDistanceSlider != null &&
            gpuDistanceSlider.shown != FoliageSettings.gpuDistance()
        ) {
            FoliageSettings.setGpuDistance(gpuDistanceSlider.shown);
        }
    }

    private static final FoliageSettings.GpuDistance[] GPU_DISTANCES =
        FoliageSettings.GpuDistance.values();

    private static double gpuDistanceToSlider(
        FoliageSettings.GpuDistance distance
    ) {
        return (double) distance.ordinal() / (GPU_DISTANCES.length - 1);
    }

    private class GpuDistanceSlider extends AbstractSliderButton {

        /** The level under the handle, which the renderer gets once the handle settles. */
        FoliageSettings.GpuDistance shown = FoliageSettings.gpuDistance();

        public GpuDistanceSlider(int x, int y, int w, int h) {
            super(
                x,
                y,
                w,
                h,
                Component.empty(),
                gpuDistanceToSlider(FoliageSettings.gpuDistance())
            );
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            // Minecraft's own "Render Distance" and its own "name: value", so both read right in every language.
            setMessage(
                Component.translatable(
                    "options.generic_value",
                    Component.translatable("options.renderDistance"),
                    Component.translatable(shown.translationKey())
                )
            );
        }

        @Override
        protected void applyValue() {
            shown = GPU_DISTANCES[
                (int) Math.round(value * (GPU_DISTANCES.length - 1))
            ];
            updatePreset();
        }

        /** Moves the handle without the player, as a preset does. */
        void setPosition(double position) {
            value = position;
            updateMessage();
        }
    }

    /** The presets the slider offers: all of them, less those on the chunk mesh where it cannot move plants. */
    private static final FoliageSettings.Preset[] PRESETS =
        java.util.Arrays.stream(FoliageSettings.Preset.values())
            .filter(FoliageSettings.Preset::available)
            .toArray(FoliageSettings.Preset[]::new);

    private static double presetToSlider(FoliageSettings.Preset preset) {
        int index = java.util.Arrays.asList(PRESETS).indexOf(preset);
        return PRESETS.length < 2
            ? 0.0
            : (double) Math.max(0, index) / (PRESETS.length - 1);
    }

    private class PresetSlider extends AbstractSliderButton {

        /** The preset the options stand as, or null for custom. */
        private FoliageSettings.Preset shown;
        /** A preset the player picked that is still to be applied; see {@link #applyPreset}. */
        FoliageSettings.Preset chosen;

        public PresetSlider(int x, int y, int w, int h) {
            super(x, y, w, h, Component.empty(), 0.0);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            Component name =
                shown != null
                    ? Component.translatable(shown.translationKey())
                    : //? >=1.21.11 {
                      Component.translatable("options.graphics.custom");
            //?} else {
            /*: Component.translatable("item.minecraft.firework_star.custom_color");
             */ //?}
            setMessage(
                Component.translatable(
                    "options.generic_value",
                    //? >=1.21.11 {
                    Component.translatable("options.graphics.preset"),
                    //?} else {
                    /*Component.translatable("createWorld.customize.presets"),
                     */ //?}
                    name
                )
            );
        }

        @Override
        protected void applyValue() {
            shown = PRESETS[(int) Math.round(value * (PRESETS.length - 1))];
            chosen = shown;
            updateMessage();
        }

        /** Shows what the options stand as, moving the handle onto it unless they match no preset. */
        void showCurrent(FoliageSettings.Preset current) {
            shown = current;
            if (current != null) {
                value = presetToSlider(current);
            }
            updateMessage();
        }
    }

    /** The distances the weather wind reaches: the renderer's own, less its adaptive one. */
    private static final FoliageSettings.GpuDistance[] WEATHER_WIND_DISTANCES =
        {
            FoliageSettings.GpuDistance.PERFORMANCE,
            FoliageSettings.GpuDistance.HALF,
            FoliageSettings.GpuDistance.FULL,
        };

    private static double weatherWindDistanceToSlider(
        FoliageSettings.GpuDistance distance
    ) {
        int index = java.util.Arrays.asList(WEATHER_WIND_DISTANCES).indexOf(
            distance
        );
        return (
            (double) Math.max(0, index) / (WEATHER_WIND_DISTANCES.length - 1)
        );
    }

    private class WeatherWindDistanceSlider extends AbstractSliderButton {

        public WeatherWindDistanceSlider(int x, int y, int w, int h) {
            super(
                x,
                y,
                w,
                h,
                Component.empty(),
                weatherWindDistanceToSlider(
                    FoliageSettings.weatherWindDistance()
                )
            );
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(
                Component.translatable(
                    "options.generic_value",
                    Component.translatable("config.allthingsfoliage.distance"),
                    Component.translatable(
                        FoliageSettings.weatherWindDistance().translationKey()
                    )
                )
            );
        }

        @Override
        protected void applyValue() {
            // Cheap to change: the renderer only meshes the sections it newly reaches again, so it applies at once.
            FoliageSettings.setWeatherWindDistance(
                WEATHER_WIND_DISTANCES[
                    (int) Math.round(
                        value * (WEATHER_WIND_DISTANCES.length - 1)
                    )
                ]
            );
            updatePreset();
        }

        /** Moves the handle without the player, as a preset does. */
        void setPosition(double position) {
            value = position;
            updateMessage();
        }
    }

    private static float sliderToWavingIntensity(double value) {
        return fromSliderPosition(
            value,
            FoliageSettings.MIN_WAVING_INTENSITY,
            FoliageSettings.DEFAULT_WAVING_INTENSITY,
            FoliageSettings.MAX_WAVING_INTENSITY
        );
    }

    private class WavingIntensitySlider extends AbstractSliderButton {

        public WavingIntensitySlider(
            int x,
            int y,
            int w,
            int h,
            double initialValue
        ) {
            super(x, y, w, h, Component.empty(), initialValue);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(
                Component.translatable(
                    "config.allthingsfoliage.intensity",
                    String.format("%.1f", FoliageSettings.wavingIntensity())
                )
            );
            updateGpuOptions();
        }

        @Override
        protected void applyValue() {
            FoliageSettings.setWavingIntensity(sliderToWavingIntensity(value));
        }

        /** Back to the default in place, without rebuilding the screen. */
        void reset() {
            FoliageSettings.setWavingIntensity(
                FoliageSettings.DEFAULT_WAVING_INTENSITY
            );
            value = wavingIntensityToSlider(
                FoliageSettings.DEFAULT_WAVING_INTENSITY
            );
            updateMessage();
        }
    }
    //?}

    //? <1.21.1 {
    /*/^*
	 * The leaves' checkbox before 1.20.3, which only says it was pressed. A class of its own rather than an anonymous one:
	 * an anonymous subclass takes the constructor's parameter names from the mappings, and those of this version's
	 * Checkbox clash when compiled on newer JDKs.
	 ^/
	private static final class WavingLeavesCheckbox extends Checkbox {

		WavingLeavesCheckbox(int x, int y, int width, Component label, boolean selected) {
			super(x, y, width, 20, label, selected);
		}

		@Override
		public void onPress() {
			super.onPress();
			FoliageSettings.setWavingLeaves(selected());
			GpuFoliageRenderer.foliageListChanged();
		}
	}
	*/ //?}
}
