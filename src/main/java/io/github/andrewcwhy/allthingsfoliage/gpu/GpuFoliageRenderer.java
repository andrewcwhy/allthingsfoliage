package io.github.andrewcwhy.allthingsfoliage.gpu;

//? >=1.20.1 {

import com.github.razorplay01.sway.api.SwayAPI;
import com.github.razorplay01.sway.api.behavior.BehaviorPipeline;
import com.github.razorplay01.sway.api.behavior.contributors.DeformationContributor;
import com.github.razorplay01.sway.api.behavior.contributors.MultiBlockContributor;
import com.github.razorplay01.sway.client.behavior.multiblock.HangingVineMultiblockBehavior;
import com.github.razorplay01.sway.config.SwayConfig;
//?} else {
/*import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
*/ //?}
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
//?} else {
/*import com.mojang.blaze3d.pipeline.RenderPipeline;
 */ //?}
import com.mojang.blaze3d.pipeline.RenderTarget;
//?} else {
/*import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
*/ //?}
import com.mojang.blaze3d.systems.RenderSystem;
//?} else {
/*import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
*/ //?}
//?} else {
/*import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;
*/ //?}
import com.mojang.blaze3d.vertex.BufferBuilder;
//? >=1.21.1 {
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
//?} else {
/*import com.google.common.collect.ImmutableMap;
 */ //?}
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
//? >=26.2 {
//? >=26.3 {
import com.mojang.renderpearl.api.GpuFormat;
//?} else {
/*import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
*/ //?}
//?}
//? >=1.21.11 {
//? >=26.3 {
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
//?}
//? >=26.2 {
//? >=26.3 {
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
//?} else {
/*import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.platform.CompareOp;
*/ //?}
//?} elif >=26.1.2 {
/*import com.mojang.blaze3d.pipeline.DepthStencilState;
 */ //?} elif >=1.21.11 {
/*import com.mojang.blaze3d.platform.DepthTestFunction;
 */ //?}
//? >=1.21.11 {
//? >=26.3 {
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
//? >=26.3 {
import com.mojang.renderpearl.api.pipeline.UniformType;
//? >=26.3 {
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTextureView;
//? >=26.3 {
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.mojang.renderpearl.api.vertex.VertexFormatElement;
import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage;
//?} else {
/*import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
*/ //?}
import io.github.andrewcwhy.allthingsfoliage.FoliageSettings;
AllThingsFoliageimport io.github.andrewcwhy.allthingsfoliage.WavingWhitelist;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
//? <26.2 {
/*import java.util.OptionalInt;
 */ //?}
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
//? >=26.2 {
import net.minecraft.client.renderer.BindGroupLayouts;
//?}
//? >=1.21.11 {
//? >=26.3 {
import net.minecraft.client.renderer.DynamicGpuData;
//?} else {
/*import net.minecraft.client.renderer.DynamicUniforms;
 */ //?}
import net.minecraft.client.renderer.RenderPipelines;
//?}
//? >=26.1.2 {
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.BlockStateModelSet;
//?} else {
/*import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.RandomSource;
*/ //?}
//? >=1.21.11 && <26.1.2 {
/*import net.minecraft.client.renderer.block.model.BlockModelPart;
 */ //?}
//? <1.21.11 {
/*import net.minecraft.client.resources.model.BakedModel;
 */ //?}
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.culling.Frustum;
//? >=26.1.2 {
import net.minecraft.client.renderer.state.level.LevelRenderState;
//?} elif >=1.21.11 {
/*import net.minecraft.client.renderer.state.LevelRenderState;
 */ //?}
//? >=1.21.11 {
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
//? >=1.21.11 {
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
//?}
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
//? >=1.21.1 {
import net.minecraft.world.level.chunk.status.ChunkStatus;
//?} else {
/*import net.minecraft.world.level.chunk.ChunkStatus;
 */ //?}
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/**
 * Draws the foliage near the player, the part {@link GpuFoliageSplit} took out of the chunk mesh, and
 * sways it on the GPU.
 * <p>
 * Geometry comes from {@link ModelBlockRenderer#tesselateBlock}, the very method the section compiler
 * uses, so ambient occlusion, light coordinates and biome tint are produced by vanilla and not
 * reimplemented here.
 * <p>
 * Each chunk section is meshed on its own, so a change only rebuilds the section it happened in.
 * Sections are then packed into regions of 8x4x8 for drawing -- the grouping Sodium uses -- so the GPU
 * sees a handful of buffers and draw calls instead of one of each per section. Within a region, visible
 * sections that sit next to each other in its buffer go out in a single draw call.
 */
public final class GpuFoliageRenderer {

    /**
     * Rebuilds run on the render thread, so only so many are allowed per frame, the nearest first. What really holds
     * them back is the time they may take, see {@link #frameBudgetNanos}; this is only how many are looked up.
     */
    private static final int REBUILD_BUDGET = 16;
    /**
     * A region is uploaded once the sections queued for it are built, so one whose sections arrive over several frames
     * is uploaded once rather than once a frame. If sections keep arriving for longer than this -- flying over new
     * ground -- it is uploaded anyway, so what is built reaches the screen.
     */
    private static final long REGION_UPLOAD_MAX_WAIT_MILLIS = 500L;
    /**
     * Regions are uploaded nearest first, so one further out could wait for as long as nearer ones keep coming in; past
     * this it is uploaded before them.
     */
    private static final long REGION_UPLOAD_STARVE_MILLIS = 2_000L;
    /**
     * And only for this long a frame, after the first one, so a frame is never held up by meshing: a heavy resource pack,
     * a slow processor or rain starting -- which meshes every section again -- would otherwise stall a frame for as long
     * as all of them take. A section not yet meshed again keeps what it showed, so this only spreads the work out.
     * <p>
     * This is the least a frame gets. A slow frame gets a share of its own length instead, see
     * {@link #frameBudgetNanos}: counted in frames alone, a change of light -- some thirty sections around a torch --
     * took three times as long to reach the foliage at 30 frames a second as at 90, while the chunk mesh, built off
     * the render thread, showed it at once.
     */
    private static final long REBUILD_BUDGET_NANOS = 4_000_000L;
    /** The share of a frame's length rebuilds may take, and the most that may come to. */
    private static final long REBUILD_BUDGET_FRAME_SHARE = 4L;
    private static final long REBUILD_BUDGET_MAX_NANOS = 16_000_000L;
    /**
     * How long region uploads may take a frame after the first, by the same reckoning: regions are small as a rule,
     * a millisecond or so, and a light change touches several, which one a frame kept waiting as many frames.
     */
    private static final long UPLOAD_BUDGET_SHARE = 2L;
    /**
     * How long a frame may spend rebuilding sections already on screen whose light or blocks changed, see
     * {@link #URGENT}: twice what the frame gets for everything else, within these bounds. What does not fit goes on in
     * the next frame, still ahead of everything else.
     * <p>
     * All of them in the frame they are queued was tried, up to 50 ms: one frame for a torch where a section meshes in a
     * millisecond, but where it takes two to five -- 70 to 200 ms for the forty around a torch, measured on 26.3 --
     * that was several frames of 50 ms in a row, a stutter worse than light a few frames late.
     */
    private static final long URGENT_BUDGET_MIN_NANOS = 8_000_000L;
    private static final long URGENT_BUDGET_MAX_NANOS = 24_000_000L;
    /** When the frame before this one was drawn, and how long rebuilds may take in this one. */
    private static long lastFrameNanos;
    private static long frameBudgetNanos = REBUILD_BUDGET_NANOS;
    /**
     * Padding on cull boxes so foliage leaning into view is not culled away: room for the strongest push from an entity
     * -- about 1.8 blocks, on a tall plant with Sway's intensity turned up -- plus the furthest the wind reaches, a storm
     * at the highest intensity: 0.66 blocks of calm sway, times the storm, times the lean and tug in sway.glsl, about 3
     * blocks. Grow it with those.
     */
    private static final double SWAY_MARGIN = 5.0D;
    private static final int SECTION_SIZE = 16;
    /** The near area never shrinks below this many chunks, however short the render distance. */
    private static final int MIN_NEAR_RADIUS = 2;

    /**
     * Up to this render distance, in chunks, the near area reaches half of it. Further than this the share
     * shrinks, since a sway is too small to see far off while the foliage the renderer holds grows with the
     * square of the distance.
     */
    private static final int FULL_NEAR_SHARE_UP_TO = 8;
    private static final float FULL_NEAR_SHARE = 0.5F;
    /** How much the share shrinks for each chunk of render distance past that: 30% by 16. */
    private static final float NEAR_SHARE_DROP_PER_CHUNK = 0.025F;
    /**
     * The smallest share, reached at 18 chunks. At 32 it leaves the near area 8 chunks out, about as far as a
     * sway of a third of a block still shows on screen.
     */
    private static final float MIN_NEAR_SHARE = 0.25F;
    /**
     * How far past Sway's interaction radius a plant can still be pushed, in blocks. Sway takes the entities whose
     * hitbox reaches into that radius around the player, and pushes the plants that hitbox overlaps once widened,
     * which covers the entity's own width and the plant's block.
     */
    private static final float PUSH_REACH_PAST_RADIUS = 4.0F;
    /**
     * How fast the edge the wind eases off at follows the near area, in blocks a second. The near area moves a
     * whole chunk at once; were the edge to jump with it, a band of plants would change how far they sway mid-swing
     * and visibly jerk. Following it lets each plant's sway grow or settle over about a second.
     */
    private static final double WIND_EDGE_SPEED = 16.0D;
    /**
     * Past this many blocks from the near area the edge jumps straight to it rather than following: after a
     * teleport, or a new render distance or setting, following would take seconds.
     */
    private static final double WIND_EDGE_JUMP = 48.0D;
    /** Sections are built this many chunks past the near area, so ones crossing into it are ready. */
    private static final int BUILD_MARGIN = 2;

    /** A region spans 8 sections along X and Z and 4 along Y. */
    private static final int REGION_WIDTH_SHIFT = 3;
    private static final int REGION_HEIGHT_SHIFT = 2;
    private static final int REGION_WIDTH = 1 << REGION_WIDTH_SHIFT;
    private static final int REGION_HEIGHT = 1 << REGION_HEIGHT_SHIFT;
    private static final int SECTIONS_PER_REGION =
        REGION_WIDTH * REGION_HEIGHT * REGION_WIDTH;

    /**
     * How far from its anchor a vertex has to be before it sways at full strength, in blocks, on a plant no
     * longer than this. A longer strand reaches full strength only at its tip, bending along its whole length
     * instead of whipping above the span; see SwayAnchor.weightAt.
     */
    private static final float SWAY_SPAN = 3.0F;
    /**
     * How strands longer than the span curve along their length, as Sway's own deformations do: a stalk by the square
     * of the way along it, a hanging vine by the cube. They take on the curve over this many blocks past the span.
     */
    private static final float STALK_CURVE = 2.0F;
    private static final float HANGING_CURVE = 3.0F;
    private static final float COLUMN_CURVE_BLOCKS = 2.0F;
    /** How much further a strand that has taken on the curve sways and leans, eased in along with the curve. */
    private static final float LONG_STRAND_SWAY = 2.0F;
    /** A strand hanging on leaves this long or shorter only moves as the leaves do; see SwayAnchor.hangOnLeaves. */
    private static final int SHORT_HANGING_STRAND = 2;
    /**
     * How a vine's free part sways, from where it starts to hang: straight as a strip of cloth, further the further
     * down, every strand at the same slant up to this many blocks of it. A longer one reaches no further at its tip, so
     * it hangs steeper, as a heavier one would. And how freely its tip sways, against a plant's.
     */
    private static final float VINE_SPAN = 4.0F;
    private static final float VINE_TIP_SWAY = 0.6F;

    /**
     * How much of its sway a column loses per block of length, and how far that can go: a strand of
     * five blocks or more keeps only a fifth of its movement.
     */
    private static final float LENGTH_DAMPING_PER_BLOCK = 0.2F;
    private static final float MAX_LENGTH_DAMPING = 0.8F;

    /**
     * What the shader needs to move each vertex: how freely it may sway, from 0 at the anchor to 1 at the far
     * end, and which block that anchor is, relative to the region, so every vertex of a plant looks up the
     * same push. Either way vanilla writes the standard attributes itself, untouched.
     */
    // They ride with the block's own attributes in a single format, interleaved as a region is uploaded.
    // A binding of their own would read better, and 26.2 allows one, but Iris rewrites the bindings of any
    // pipeline whose first one is the block format, collapsing them to the single one a shader pack wants.
    // Sharing one format keeps this pipeline unmistakably the mod's own, and matches what older versions,
    // which read one vertex buffer per pipeline, can do anyway.
    //? >=26.2 {
    private static final VertexFormat FOLIAGE_FORMAT = foliageFormat();

    private static VertexFormat foliageFormat() {
        VertexFormat.Builder builder = VertexFormat.builder(0);
        // The block's attributes first and in their own order, so they keep the offsets vanilla writes them at.
        for (VertexFormatElement element : DefaultVertexFormat.BLOCK.getElements()) {
            builder.addAttribute(element.name(), element.format());
        }
        return builder
            .addAttribute("SwayCell", GpuFormat.R32_FLOAT)
            .addAttribute("SwayWeights", GpuFormat.R32_FLOAT)
            .build();
    }

    //?} elif >=1.21.1 {
    /*// Elements are registered under ids of their own, and Iris registers its five at the first free ones too, so the
	// two are taken the same way: fixed ids would collide with Iris's whichever registered first.
	//? >=26.1.2 {
	private static final VertexFormatElement SWAY_CELL_ELEMENT =
			VertexFormatElement.register(freeElementId(), 0, VertexFormatElement.Type.FLOAT, false, 1);
	private static final VertexFormatElement SWAY_WEIGHTS_ELEMENT =
			VertexFormatElement.register(freeElementId(), 0, VertexFormatElement.Type.FLOAT, false, 1);
	//?} else {
	/^private static final VertexFormatElement SWAY_CELL_ELEMENT =
			VertexFormatElement.register(freeElementId(), 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 1);
	private static final VertexFormatElement SWAY_WEIGHTS_ELEMENT =
			VertexFormatElement.register(freeElementId(), 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 1);
	^///?}
	private static final VertexFormat FOLIAGE_FORMAT = withSwayValues(DefaultVertexFormat.BLOCK);

	private static int freeElementId() {
		int id = 0;
		while (VertexFormatElement.byId(id) != null) {
			id++;
		}
		return id;
	}

	// A format holding another's attributes at their own offsets, then the renderer's two values: the block's, for the
	// renderer's own pipeline, or Iris's terrain format, for drawing through a shader pack.
	static VertexFormat withSwayValues(VertexFormat base) {
		VertexFormat.Builder builder = VertexFormat.builder();
		int written = 0;
		for (VertexFormatElement element : base.getElements()) {
			// Formats may pad between attributes or after the last one -- 1.21.11's block format ends in padding, Iris's
			// terrain format pads after the normal -- and rebuilding from the elements alone would drop it, so every
			// vertex would be read at a stride it was never written at.
			int offset = base.getOffset(element);
			if (offset > written) {
				builder.padding(offset - written);
			}
			builder.add(base.getElementName(element), element);
			written = offset + element.byteSize();
		}
		if (written < base.getVertexSize()) {
			builder.padding(base.getVertexSize() - written);
		}
		return builder
				.add("SwayCell", SWAY_CELL_ELEMENT)
				.add("SwayWeights", SWAY_WEIGHTS_ELEMENT)
				.build();
	}
	*/ //?} else {
    /*// Before 1.21.1 a format is a map of named elements, and a generic attribute is read from the location its place
	// in the map gives it -- the block's padding counts as a place -- so the shader lists its attributes the same way.
	private static final VertexFormat FOLIAGE_FORMAT = withSwayValues(DefaultVertexFormat.BLOCK);

	// Another format's attributes, then the renderer's two values: the block's, for the renderer's own shader, or Iris's
	// terrain format, for drawing through a shader pack.
	static VertexFormat withSwayValues(VertexFormat base) {
		ImmutableMap.Builder<String, VertexFormatElement> elements = ImmutableMap.builder();
		// The names and elements come in the same order, the one they are written in, padding included.
		for (int i = 0; i < base.getElements().size(); i++) {
			elements.put(base.getElementAttributeNames().get(i), base.getElements().get(i));
		}
		return new VertexFormat(elements
				.put("SwayCell", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 1))
				.put("SwayWeights", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 1))
				.build());
	}
	*/ //?}

    /**
     * The {@code FoliageSway} uniform block: the settings the shader reads that the player can change while
     * playing. They are data in a buffer rather than constants in the shader, so a change shows on the next
     * frame without recompiling anything.
     */
    private static final String SWAY_SETTINGS_UNIFORM = "FoliageSway";
    //? >=26.2 {
    private static final BindGroupLayout SWAY_SETTINGS =
        BindGroupLayout.builder()
            .withUniform(SWAY_SETTINGS_UNIFORM, UniformType.UNIFORM_BUFFER)
            .build();
    //?}
    //? >=1.21.11 {
    /** The intensity, whether the calm sway is on, then the edge the wind eases off at, then the weather. */
    private static final int SWAY_SETTINGS_SIZE = new Std140SizeCalculator()
        .putFloat()
        .putFloat()
        .putVec4()
        .putVec4()
        .get();
    //?}

    /**
     * Our own pipeline, so the vertex shader can displace foliage on the GPU.
     * <p>
     * It inherits everything from vanilla's block snippet -- vertex format, bind groups, depth and
     * blend state -- and only swaps in our shaders and adds the sway weight binding and the sway settings.
     * The shader files are discovered by resource pack scanning, so no registration call is needed.
     */
    //? >=26.3 {
    // Vanilla's block snippet is private again on 26.3, so what it holds is listed: the uniform blocks and samplers the
    // shaders read, the block format with the mod's two values, and the depth state every block pipeline uses. The
    // foliage is drawn into vanilla's own pass, which writes one colour target; a pipeline whose targets do not match
    // the pass is refused.
    private static final RenderPipeline PIPELINE = modernPipeline(
        "pipeline/foliage",
        true,
        GpuFormat.RGBA8_UNORM
    );
    /**
     * The same, keeping every pixel: for leaves drawn solid, as the game's Fast leaves are, the way vanilla's solid terrain
     * differs from its cutout terrain.
     */
    private static final RenderPipeline OPAQUE_PIPELINE = modernPipeline(
        "pipeline/foliage_opaque",
        false,
        GpuFormat.RGBA8_UNORM
    );

    /**
     * A pipeline's colour target, written as the format the pass it is drawn into holds: the main target's, which is
     * vanilla's unless another mod allocates it with more bits.
     */
    private static ColorTargetState colorTarget(GpuFormat format) {
        return new ColorTargetState(
            ColorTargetState.DEFAULT.blendFunction(),
            format,
            ColorTargetState.DEFAULT.writeMask()
        );
    }

    /** A pipeline's name, told apart by the format of its colour target where that is not vanilla's. */
    private static Identifier pipelineLocation(
        String location,
        GpuFormat format
    ) {
        return Identifier.fromNamespaceAndPath(
            AllThingsFoliage.MOD_ID,
            format == GpuFormat.RGBA8_UNORM
                ? location
                : location +
                      "/" +
                      format.name().toLowerCase(java.util.Locale.ROOT)
        );
    }

    private static RenderPipeline modernPipeline(
        String location,
        boolean cutout,
        GpuFormat format
    ) {
        RenderPipeline.Builder builder = RenderPipeline.builder()
            .withLocation(pipelineLocation(location, format))
            .withVertexShader(
                Identifier.fromNamespaceAndPath(
                    AllThingsFoliage.MOD_ID,
                    "core/foliage"
                )
            )
            .withFragmentShader(
                Identifier.fromNamespaceAndPath(
                    AllThingsFoliage.MOD_ID,
                    "core/foliage"
                )
            )
            .withVertexBinding(0, FOLIAGE_FORMAT)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(DepthStencilState.DEFAULT)
            .withColorTargetState(colorTarget(format))
            .withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withBindGroupLayout(BindGroupLayouts.FOG)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER2)
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withBindGroupLayout(SWAY_SETTINGS)
            .withBindGroupLayout(GpuFoliageInteraction.LAYOUT);
        if (cutout) {
            builder.withShaderDefine("ALPHA_CUTOUT", 0.5F);
        }
        return builder.build();
    }

    //?} elif >=26.2 {
    /*private static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.BLOCK_SNIPPET)
			.withLocation(Identifier.fromNamespaceAndPath(AllThingsFoliage.MOD_ID, "pipeline/foliage"))
			.withVertexShader(Identifier.fromNamespaceAndPath(AllThingsFoliage.MOD_ID, "core/foliage"))
			.withFragmentShader(Identifier.fromNamespaceAndPath(AllThingsFoliage.MOD_ID, "core/foliage"))
			.withVertexBinding(0, FOLIAGE_FORMAT)
			.withBindGroupLayout(SWAY_SETTINGS)
			.withBindGroupLayout(GpuFoliageInteraction.LAYOUT)
			.withShaderDefine("ALPHA_CUTOUT", 0.5F)
			.build();
	// The same, keeping every pixel: for leaves drawn solid, as the game's Fast leaves are.
	private static final RenderPipeline OPAQUE_PIPELINE = RenderPipeline.builder(RenderPipelines.BLOCK_SNIPPET)
			.withLocation(Identifier.fromNamespaceAndPath(AllThingsFoliage.MOD_ID, "pipeline/foliage_opaque"))
			.withVertexShader(Identifier.fromNamespaceAndPath(AllThingsFoliage.MOD_ID, "core/foliage"))
			.withFragmentShader(Identifier.fromNamespaceAndPath(AllThingsFoliage.MOD_ID, "core/foliage"))
			.withVertexBinding(0, FOLIAGE_FORMAT)
			.withBindGroupLayout(SWAY_SETTINGS)
			.withBindGroupLayout(GpuFoliageInteraction.LAYOUT)
			.build();
	*/ //?} elif >=1.21.11 && <26.2 {
    /*// Vanilla's block snippet is private before 26.2, so the same state is spelled out here: the samplers
	// and uniforms its shaders read, one vertex format, and the depth state every block pipeline uses.
	private static final RenderPipeline PIPELINE = blockPipeline("pipeline/foliage", FOLIAGE_FORMAT);
	// The same, keeping every pixel: for leaves drawn solid, as the game's Fast leaves are.
	private static final RenderPipeline OPAQUE_PIPELINE = blockPipeline("pipeline/foliage_opaque", FOLIAGE_FORMAT, false);

	private static RenderPipeline blockPipeline(String location, VertexFormat format) {
		return blockPipeline(location, format, true);
	}

	private static RenderPipeline blockPipeline(String location, VertexFormat format, boolean cutout) {
		RenderPipeline.Builder builder = RenderPipeline.builder()
				.withLocation(Identifier.fromNamespaceAndPath(AllThingsFoliage.MOD_ID, location))
				.withVertexShader(Identifier.fromNamespaceAndPath(AllThingsFoliage.MOD_ID, "core/foliage"))
				.withFragmentShader(Identifier.fromNamespaceAndPath(AllThingsFoliage.MOD_ID, "core/foliage"))
				.withVertexFormat(format, VertexFormat.Mode.QUADS)
				.withSampler("Sampler0")
				.withSampler("Sampler2")
				.withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
				.withUniform("Projection", UniformType.UNIFORM_BUFFER)
				.withUniform("Fog", UniformType.UNIFORM_BUFFER)
				.withUniform("Globals", UniformType.UNIFORM_BUFFER)
				.withUniform(SWAY_SETTINGS_UNIFORM, UniformType.UNIFORM_BUFFER)
				.withUniform(GpuFoliageInteraction.UNIFORM, UniformType.UNIFORM_BUFFER)
				//? >=26.1.2 {
				.withDepthStencilState(DepthStencilState.DEFAULT);
				//?} else {
				/^.withDepthWrite(true)
				.withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST);
				^///?}
		if (cutout) {
			builder.withShaderDefine("ALPHA_CUTOUT", 0.5F);
		}
		if (VulkanBridge.drawsChunks()) {
			// VulkanMod's shader converter takes no arrays in a uniform block; see foliage.vsh.
			builder.withShaderDefine("MC2_NO_UNIFORM_ARRAYS");
		}
		return builder.build();
	}
	*/ //?}

    //? >=26.3 {
    /**
     * The pipeline foliage is drawn with while Sodium draws the chunks and a copy of its chunk shaders reads the mod's
     * vertices: the shaders the chunks are drawn with then. See {@link SodiumFoliageShader}, which compiles it.
     */
    private static final RenderPipeline SODIUM_PIPELINE = sodiumPipeline(
        "pipeline/foliage_sodium",
        GpuFormat.RGBA8_UNORM
    );
    /**
     * The same for the copy of Sodium's solid pass, which keeps every pixel: for leaves drawn as the game's Fast leaves
     * are. The alpha test is a define in the copy's own source, so the two pipelines differ only in which copy they hold.
     */
    private static final RenderPipeline SODIUM_OPAQUE_PIPELINE = sodiumPipeline(
        "pipeline/foliage_sodium_opaque",
        GpuFormat.RGBA8_UNORM
    );

    private static RenderPipeline sodiumPipeline(
        String location,
        GpuFormat format
    ) {
        return RenderPipeline.builder()
            .withLocation(pipelineLocation(location, format))
            .withVertexShader(SodiumFoliageShader.SHADER)
            .withFragmentShader(SodiumFoliageShader.SHADER)
            .withVertexBinding(0, FOLIAGE_FORMAT)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(DepthStencilState.DEFAULT)
            .withColorTargetState(colorTarget(format))
            .withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withBindGroupLayout(BindGroupLayouts.FOG)
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withBindGroupLayout(
                BindGroupLayout.builder()
                    .withUniform(
                        SodiumFoliageShader.BLOCK_TEXTURE,
                        UniformType.COMBINED_IMAGE_SAMPLER
                    )
                    .withUniform(
                        SodiumFoliageShader.LIGHT_TEXTURE,
                        UniformType.COMBINED_IMAGE_SAMPLER
                    )
                    .build()
            )
            .withBindGroupLayout(SWAY_SETTINGS)
            .withBindGroupLayout(GpuFoliageInteraction.LAYOUT)
            .build();
    }

    /**
     * The pipeline foliage is drawn with while the terrain's shaders compile with the sway spliced in: the chunk mesh's
     * own shaders, so foliage is lit and coloured as the terrain around it, a resource pack's shaders included. See
     * {@link TerrainFoliageShader}, which compiles it. Vanilla's terrain snippet is private, so what it holds is listed:
     * the section's block and the terrain's own, and no transform, which the terrain's shaders do not read.
     */
    private static final RenderPipeline TERRAIN_PIPELINE = terrainPipeline(
        "pipeline/foliage_terrain",
        true,
        GpuFormat.RGBA8_UNORM
    );
    /** The same, keeping every pixel, as vanilla's solid terrain: for leaves drawn as the game's Fast leaves are. */
    private static final RenderPipeline TERRAIN_OPAQUE_PIPELINE =
        terrainPipeline(
            "pipeline/foliage_terrain_opaque",
            false,
            GpuFormat.RGBA8_UNORM
        );

    private static RenderPipeline terrainPipeline(
        String location,
        boolean cutout,
        GpuFormat format
    ) {
        RenderPipeline.Builder builder = RenderPipeline.builder()
            .withLocation(pipelineLocation(location, format))
            .withVertexShader(TerrainFoliageShader.VERTEX)
            .withFragmentShader(TerrainFoliageShader.TERRAIN)
            .withVertexBinding(0, FOLIAGE_FORMAT)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(DepthStencilState.DEFAULT)
            .withColorTargetState(colorTarget(format))
            .withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withBindGroupLayout(BindGroupLayouts.FOG)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER2)
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.CHUNK_SECTION)
            .withBindGroupLayout(BindGroupLayouts.TERRAIN_INFO)
            .withBindGroupLayout(SWAY_SETTINGS)
            .withBindGroupLayout(GpuFoliageInteraction.LAYOUT);
        if (cutout) {
            builder.withShaderDefine("ALPHA_CUTOUT", 0.5F);
        }
        return builder.build();
    }

    //?}

    //? >=26.2 && <26.3 {
    /*/^*
	 * The pipeline foliage is drawn with while the terrain's shaders compile with the sway spliced in, which is nearly
	 * always: the chunk mesh's own shaders, so foliage is lit and coloured as the terrain around it, a resource pack's
	 * shaders included. See {@link TerrainFoliageShader}; {@link #PIPELINE} is what is left otherwise.
	 ^/
	private static final RenderPipeline TERRAIN_PIPELINE = RenderPipeline.builder(RenderPipelines.TERRAIN_SNIPPET)
			.withLocation(Identifier.fromNamespaceAndPath(AllThingsFoliage.MOD_ID, "pipeline/foliage_terrain"))
			.withVertexShader(TerrainFoliageShader.VERTEX)
			.withFragmentShader(TerrainFoliageShader.TERRAIN)
			.withVertexBinding(0, FOLIAGE_FORMAT)
			.withBindGroupLayout(SWAY_SETTINGS)
			.withBindGroupLayout(GpuFoliageInteraction.LAYOUT)
			.withShaderDefine("ALPHA_CUTOUT", 0.5F)
			.build();
	// The same, keeping every pixel, as vanilla's solid terrain: for leaves drawn as the game's Fast leaves are.
	private static final RenderPipeline TERRAIN_OPAQUE_PIPELINE = RenderPipeline.builder(RenderPipelines.TERRAIN_SNIPPET)
			.withLocation(Identifier.fromNamespaceAndPath(AllThingsFoliage.MOD_ID, "pipeline/foliage_terrain_opaque"))
			.withVertexShader(TerrainFoliageShader.VERTEX)
			.withFragmentShader(TerrainFoliageShader.TERRAIN)
			.withVertexBinding(0, FOLIAGE_FORMAT)
			.withBindGroupLayout(SWAY_SETTINGS)
			.withBindGroupLayout(GpuFoliageInteraction.LAYOUT)
			.build();

	/^*
	 * The pipeline foliage is drawn with while Sodium draws the chunks and its shaders compile with the mod's vertices
	 * read in: the shaders the chunks are drawn with then. See {@link SodiumFoliageShader}.
	 ^/
	private static final RenderPipeline SODIUM_PIPELINE = sodiumPipeline("pipeline/foliage_sodium", SodiumFoliageShader.SHADER);
	// The same with the copy of Sodium's solid pass, which keeps every pixel: for leaves drawn as the game's Fast leaves
	// are.
	private static final RenderPipeline SODIUM_OPAQUE_PIPELINE = sodiumPipeline("pipeline/foliage_sodium_opaque",
			SodiumFoliageShader.SHADER_OPAQUE);

	private static RenderPipeline sodiumPipeline(String location, Identifier shader) {
		return RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
				.withLocation(Identifier.fromNamespaceAndPath(AllThingsFoliage.MOD_ID, location))
				.withVertexShader(shader)
				.withFragmentShader(shader)
				.withVertexBinding(0, FOLIAGE_FORMAT)
				.withPrimitiveTopology(PrimitiveTopology.QUADS)
				.withDepthStencilState(DepthStencilState.DEFAULT)
				.withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
				.withBindGroupLayout(BindGroupLayouts.PROJECTION)
				.withBindGroupLayout(BindGroupLayouts.FOG)
				.withBindGroupLayout(BindGroupLayout.builder()
						.withSampler(SodiumFoliageShader.BLOCK_TEXTURE)
						.withSampler(SodiumFoliageShader.LIGHT_TEXTURE)
						.build())
				.withBindGroupLayout(SWAY_SETTINGS)
				.withBindGroupLayout(GpuFoliageInteraction.LAYOUT)
				.build();
	}
	*/ //?} elif >=1.21.11 && <26.2 {
    /*// Drawn with Sodium's chunk shaders while Sodium draws the chunks and they compile with the mod's vertices read in;
	// see the newer pipeline.
	private static final RenderPipeline SODIUM_PIPELINE = sodiumPipeline("pipeline/foliage_sodium", SodiumFoliageShader.SHADER);
	// The same with the copy of Sodium's solid pass, which keeps every pixel: for leaves drawn as the game's Fast leaves
	// are.
	private static final RenderPipeline SODIUM_OPAQUE_PIPELINE = sodiumPipeline("pipeline/foliage_sodium_opaque",
			SodiumFoliageShader.SHADER_OPAQUE);

	private static RenderPipeline sodiumPipeline(String location, Identifier shader) {
		return RenderPipeline.builder()
				.withLocation(Identifier.fromNamespaceAndPath(AllThingsFoliage.MOD_ID, location))
				.withVertexShader(shader)
				.withFragmentShader(shader)
				.withVertexFormat(FOLIAGE_FORMAT, VertexFormat.Mode.QUADS)
				.withSampler(SodiumFoliageShader.BLOCK_TEXTURE)
				.withSampler(SodiumFoliageShader.LIGHT_TEXTURE)
				.withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
				.withUniform("Projection", UniformType.UNIFORM_BUFFER)
				.withUniform("Fog", UniformType.UNIFORM_BUFFER)
				.withUniform("Globals", UniformType.UNIFORM_BUFFER)
				//? <26.1.2 {
				/^.withUniform(SodiumFoliageShader.CHUNK_DATA, UniformType.UNIFORM_BUFFER)
				^///?}
				.withUniform(SWAY_SETTINGS_UNIFORM, UniformType.UNIFORM_BUFFER)
				.withUniform(GpuFoliageInteraction.UNIFORM, UniformType.UNIFORM_BUFFER)
				//? >=26.1.2 {
				.withDepthStencilState(DepthStencilState.DEFAULT)
				//?} else {
				/^.withDepthWrite(true)
				.withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
				^///?}
				.build();
	}

	// Drawn with the terrain's shaders where they compile with the sway spliced in; see the newer pipeline. Vanilla's
	// terrain snippet is private before 26.2, so the state it holds is spelled out, as for the block pipeline.
	private static final RenderPipeline TERRAIN_PIPELINE = terrainPipeline("pipeline/foliage_terrain", true);
	// The same, keeping every pixel, as vanilla's solid terrain: for leaves drawn as the game's Fast leaves are.
	private static final RenderPipeline TERRAIN_OPAQUE_PIPELINE = terrainPipeline("pipeline/foliage_terrain_opaque", false);

	private static RenderPipeline terrainPipeline(String location, boolean cutout) {
		RenderPipeline.Builder builder = RenderPipeline.builder()
				.withLocation(Identifier.fromNamespaceAndPath(AllThingsFoliage.MOD_ID, location))
				.withVertexShader(TerrainFoliageShader.VERTEX)
				.withFragmentShader(TerrainFoliageShader.TERRAIN)
				.withVertexFormat(FOLIAGE_FORMAT, VertexFormat.Mode.QUADS)
				.withSampler("Sampler0")
				.withSampler("Sampler2")
				.withUniform("Projection", UniformType.UNIFORM_BUFFER)
				.withUniform("ChunkSection", UniformType.UNIFORM_BUFFER)
				.withUniform("Fog", UniformType.UNIFORM_BUFFER)
				.withUniform("Globals", UniformType.UNIFORM_BUFFER)
				.withUniform(SWAY_SETTINGS_UNIFORM, UniformType.UNIFORM_BUFFER)
				.withUniform(GpuFoliageInteraction.UNIFORM, UniformType.UNIFORM_BUFFER)
				//? >=26.1.2 {
				.withDepthStencilState(DepthStencilState.DEFAULT);
				//?} else {
				/^.withDepthWrite(true)
				.withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST);
				^///?}
		if (cutout) {
			builder.withShaderDefine("ALPHA_CUTOUT", 0.5F);
		}
		return builder.build();
	}
	*/ //?}

    private static final int VERTEX_BYTES =
        DefaultVertexFormat.BLOCK.getVertexSize();

    /** The format the regions hold: the mod's own, or the one a shader pack's programs read while one is loaded. */
    private static VertexFormat regionFormat() {
        //? iris {
        return meshedForShaderPack
            ? IrisCompat.shaderPackFormat()
            : FOLIAGE_FORMAT;
        //?} else {
        /*return FOLIAGE_FORMAT;
         */ //?}
    }

    /** Our own per-vertex data: the plant's anchor block and its two weights, one float each. */
    private static final int WEIGHT_BYTES = Float.BYTES * 2;

    /**
     * A float carries whole numbers up to 2^24 exactly, which is room for three bytes. Both of the mod's
     * per-vertex values fit in that, so each travels packed into one float and is taken apart in the shader.
     * Eight bytes a vertex instead of twenty, which is bandwidth the GPU pays for on every vertex of every
     * frame -- far more than the handful of instructions it costs to unpack.
     */
    private static final float PACK_BYTE = 256.0F;
    /** Anchors may sit a little outside their region, so a coordinate is shifted before it is packed. */
    private static final int CELL_BIAS = 64;
    /**
     * Each weight keeps ten bits and the wind's reach four, so the three together stay inside a float's exact whole
     * numbers.
     */
    private static final float WEIGHT_SCALE = 1023.0F;
    private static final float PUSH_SCALE = 255.0F;
    private static final float EXPOSURE_SCALE = 15.0F;
    /**
     * A block hanging on leaves gives the lowest of the wind's four bits to whether the leaves wave where it is, so it
     * keeps three for the wind's reach.
     */
    private static final float HANGING_EXPOSURE_SCALE = 7.0F;

    /**
     * The anchor block, relative to the region, as one float: a byte per axis. A region is 128 blocks wide
     * and 64 tall, and an anchor can be a few blocks outside it, which the bias leaves room for.
     */
    private static float packCell(int x, int y, int z) {
        int cx = Mth.clamp(x + CELL_BIAS, 0, 255);
        int cy = Mth.clamp(y + CELL_BIAS, 0, 255);
        int cz = Mth.clamp(z + CELL_BIAS, 0, 255);
        return (cx * 256 + cy) * 256 + cz;
    }

    /**
     * The wind weight, the push weight, whether the plant stands still in calm weather, whether it keeps its sway while
     * pushed (a field, or what follows the leaves), and how much of rain's wind reaches the plant, as one float: ten
     * bits, eight, one, one and four. The weights are between 0 and 1. A block hanging on leaves -- still and steady at
     * once -- keeps three bits for the wind and gives the last to whether the leaves wave where the vertex is, rather
     * than stand still against a block holding them (leafHeld), so it moves exactly as the leaf it hangs on.
     */
    private static float packWeights(
        float wave,
        float push,
        boolean still,
        boolean steady,
        float exposure,
        boolean leafHeld
    ) {
        int w = Math.round(Mth.clamp(wave, 0.0F, 1.0F) * WEIGHT_SCALE);
        int p = Math.round(Mth.clamp(push, 0.0F, 1.0F) * PUSH_SCALE);
        int e =
            still && steady
                ? Math.round(
                      Mth.clamp(exposure, 0.0F, 1.0F) * HANGING_EXPOSURE_SCALE
                  ) *
                      2 +
                  (leafHeld ? 0 : 1)
                : Math.round(Mth.clamp(exposure, 0.0F, 1.0F) * EXPOSURE_SCALE);
        return (
            (((w * 256 + p) * 2 + (still ? 1 : 0)) * 2 + (steady ? 1 : 0)) *
                16 +
            e
        );
    }

    private static final int INITIAL_SCRATCH_QUADS = 1024;

    /**
     * The edge the wind eases off at, in world blocks: lowest x, lowest z, highest x, highest z. It follows the near
     * area's edge; see {@link #followWindEdge}. NaN until there is a near area to follow.
     */
    private static final double[] WIND_EDGE = {
        Double.NaN,
        Double.NaN,
        Double.NaN,
        Double.NaN,
    };
    private static long windEdgeMovedNanos;

    private static final Map<Long, Region> REGIONS = new HashMap<>();
    /** Sections waiting to be meshed, by section key. */
    private static final Set<Long> DIRTY = new LinkedHashSet<>();
    /**
     * The sections of the plots whose foliage the renderer draws, each with the sub-level it belongs to, and each
     * sub-level with its sections; see SableBridge. Empty without Sable, and from 1.21.11 on, where there is none.
     */
    private static final Long2ObjectOpenHashMap<Object> PLOT_OWNERS =
        new Long2ObjectOpenHashMap<>();
    /**
     * Set as a section of a sub-level is meshed while the quad being written takes no shade, as a plant's does not: its
     * vertices are written looking up, as Sable writes them, so a plant is shaded as the top of the block it grows on
     * once the sub-level has turned; see drawSubLevels.
     */
    private static boolean normalsDropped;
    private static final Map<Object, LongOpenHashSet> SUB_LEVELS =
        new java.util.IdentityHashMap<>();
    /** Regions whose buffers no longer match their sections. */
    private static final Set<Region> DIRTY_REGIONS = new LinkedHashSet<>();

    private static final Predicate<BlockState> FOLIAGE =
        GpuFoliageSplit::mayHoldFoliage;
    /**
     * How much of its calm sway a plant under Snow! Real Magic's snow keeps, as if the snow weighed it down: a little less
     * than a plant keeps while an entity pushes it (MC2_PUSHED_SWAY in sway.glsl). A push moves it as far as any other.
     */
    private static final float SNOW_LADEN_SWAY = 0.20F;
    /**
     * How much of the weather's wind such a plant keeps: nearly all of it, a storm shaking it whatever lies on it. The
     * plant is meshed with this much of its weight and marked still without steady, which the shaders read as keeping
     * only MC2_LADEN_CALM of its calm sway: that constant has to be SNOW_LADEN_SWAY over this.
     */
    private static final float SNOW_LADEN_WIND = 0.80F;

    private static ModelBlockRenderer modelRenderer;
    //? >=26.1.2 {
    /**
     * The same, drawing every face, culled or not: for leaves touching a block they part from as they wave (see
     * partsFromNeighbour), whose faces against it the chunk mesh hides, and which would leave a hole there as they move
     * away from it.
     */
    private static ModelBlockRenderer uncullingRenderer;
    //?}
    /** Whether the renderer is in charge of the near foliage, following the waving foliage setting. */
    private static boolean active;
    //? <26.1.2 {
    /*/^* The frustum vanilla culls the terrain with this frame, caught on its way past. ^/
	private static Frustum cullFrustum;

	/^* Called as vanilla prepares its cull frustum, which the camera state does not carry before 26.1.2. ^/
	public static void onCullFrustum(Frustum frustum) {
		cullFrustum = frustum;
	}
	*/ //?}
    /** Set when every buffer was thrown away, so the chunks already loaded get queued again. */
    private static boolean reseedPending;
    /**
     * Whether what the renderer holds was meshed to be drawn through a shader pack, and for which pack. A pack's
     * programs read a wider vertex format, and its own block ids, so turning one on or off, or loading another,
     * hands the foliage back to the chunk mesh and meshes it all again.
     */
    private static boolean meshedForShaderPack;
    /**
     * Whether sections are meshed with the wind shelter worked out. Only while it rains: the shelter is half of what
     * meshing a section costs, and without rain the shader has no use for it. When rain starts every section is meshed
     * again with it, over the seconds the game takes to ease the rain in.
     */
    private static boolean shelterActive;
    /** Over how many blocks rain's wind eases off at the edge of the distance it reaches; see sway.glsl. */
    private static final float WEATHER_WIND_FADE_BLOCKS = 4.0F;
    /** Rain's reach when it has no edge: further than any area the renderer draws. */
    private static final float UNBOUNDED_REACH = 100_000.0F;
    /** A player moving less than this since the sections were last checked leaves them as they are. */
    private static final double SHELTER_RECHECK_BLOCKS = 2.0D;
    private static float shelterCheckedReach = Float.NaN;
    private static double shelterCheckedX;
    private static double shelterCheckedZ;
    private static int meshedPackGeneration;
    //? iris {
    private static boolean shaderPackSetUp;
    //? >=1.21.11 {
    /** The pipeline drawn with while a shader pack is loaded; see {@link IrisFoliageShaders}. */
    private static RenderPipeline shaderPackPipeline;
    /** The same, for drawing into a shader pack's shadow map. */
    private static RenderPipeline shaderPackShadowPipeline;
    //?}
    //? >=1.21.11 {
    /** The same, for leaves drawn solid through a shader pack, as the game's Fast leaves are. */
    private static RenderPipeline shaderPackOpaquePipeline;
    //?}
    //? >=1.21.11 && <26.3 {
    /*/^* The same, for drawing them into a shader pack's shadow map. ^/
	private static RenderPipeline shaderPackShadowOpaquePipeline;
	*/ //?}
    //?} elif >=26.3 {
    /*// Never set where there is no shader pack support: no frame is drawn through a pack there.
	private static RenderPipeline shaderPackPipeline;
	private static RenderPipeline shaderPackOpaquePipeline;
	*/ //?}

    //? >=1.21.11 {
    /** Holds the sway settings the shader reads; rewritten only when one of them changes. */
    private static GpuBuffer swaySettings;
    private static float uploadedIntensity = Float.NaN;
    private static float uploadedCalmSway = Float.NaN;
    private static final Vector4f UPLOADED_EDGE = new Vector4f(Float.NaN);
    private static final Vector4f UPLOADED_WEATHER = new Vector4f(Float.NaN);
    //?}

    /** Reused by every rebuild and grown to the largest section seen, so no rebuild has a size limit. */
    //? >=1.21.1 {
    private static ByteBufferBuilder vertexScratch;
    //?} else {
    /*private static BufferBuilder vertexScratch;
     */ //?}
    private static ByteBuffer weightScratch;

    /** This frame's draw calls, three ints each: index into the drawn regions, first vertex, vertex count. */
    private static int[] draws = new int[256 * 3];
    private static int drawsSize;
    /** The same, for the leaves drawn solid; see {@link Section#opaqueVertexCount}. */
    private static int[] opaqueDraws = new int[64 * 3];
    private static int opaqueDrawsSize;
    /** Regions with at least one draw this frame, in the order their draws were queued. */
    private static final List<Region> DRAWN = new ArrayList<>();

    private GpuFoliageRenderer() {}

    /** One section's geometry, kept in memory so its region can be reassembled without re-meshing. */
    private static final class Section {

        final long key;
        final BlockPos origin;
        final AABB bounds;
        /** The block's attributes and ours, interleaved, ready to be copied into a region as it stands. */
        final ByteBuffer data;
        final int vertexCount;
        /**
         * How many of its vertices, at the end of {@link #data}, are leaves drawn solid, as the game's Fast leaves are, with
         * a pipeline that keeps every pixel. The ones before them are drawn cut out.
         */
        final int opaqueVertexCount;
        /** Bytes per vertex in {@link #data}: wider while a shader pack is loaded, whose format carries more. */
        final int stride;
        /** Whether its plants were meshed with the wind shelter worked out; see {@link #followRain}. */
        boolean sheltered;
        /** Which list of foliage it was meshed with; see GpuFoliageSplit.AWAITING_GROUP. */
        int listGeneration;

        /** The animated textures its blocks are drawn with, for Sodium, which has to be told; null where there are none. */
        net.minecraft.client.renderer.texture.TextureAtlasSprite[] animatedSprites;

        Section(
            long key,
            BlockPos origin,
            ByteBuffer data,
            int vertexCount,
            int opaqueVertexCount,
            int stride
        ) {
            this.opaqueVertexCount = opaqueVertexCount;
            this.stride = stride;
            this.key = key;
            this.origin = origin;
            this.bounds = new AABB(
                origin.getX() - SWAY_MARGIN,
                origin.getY() - SWAY_MARGIN,
                origin.getZ() - SWAY_MARGIN,
                origin.getX() + SECTION_SIZE + SWAY_MARGIN,
                origin.getY() + SECTION_SIZE + SWAY_MARGIN,
                origin.getZ() + SECTION_SIZE + SWAY_MARGIN
            );
            this.data = data;
            this.vertexCount = vertexCount;
        }

        /** How many of its vertices, from the start of {@link #data}, are drawn cut out. */
        int cutoutVertexCount() {
            return vertexCount - opaqueVertexCount;
        }

        void free() {
            MemoryUtil.memFree(data);
        }
    }

    /**
     * A block of sections drawn from one pair of buffers. Its sections' vertices are relative to the
     * region's corner, which keeps them small enough for float precision however far out it sits.
     */
    private static final class Region {

        final long key;
        /** The sub-level whose plot the region lies in, as Sable's own object for it, or null in the world itself. */
        Object subLevel;
        final BlockPos origin;
        final AABB bounds;
        /** The region's blocks without padding, for asking whether it lies wholly inside the frustum. */
        final BoundingBox blockBounds;
        final Section[] sections = new Section[SECTIONS_PER_REGION];
        /** Where each section starts in the region's buffers, valid once uploaded. */
        final int[] firstVertex = new int[SECTIONS_PER_REGION];
        /**
         * Where each section's solid leaves start, valid once uploaded: after every section's cut out part, so each kind is
         * one stretch of the buffer and goes out in as few draw calls as the other.
         */
        final int[] firstOpaqueVertex = new int[SECTIONS_PER_REGION];
        /** The occupied slots in buffer order, so drawing never walks the empty ones. Valid once uploaded. */
        final int[] occupied = new int[SECTIONS_PER_REGION];
        int occupiedCount;
        int sectionCount;
        /**
         * The sections as they were when the region was last uploaded, which is what its buffer holds and what is drawn
         * from. A section rebuilt since keeps its old counterpart here until the region is uploaded again.
         */
        final Section[] uploadedSections = new Section[SECTIONS_PER_REGION];
        /** When the region first had a change waiting to be uploaded. */
        long pendingSince;
        //? >=1.21.11 {
        GpuBuffer vertices;

        //?} else {
        /*VertexBuffer vertices;
         */ //?}

        Region(long regionKey) {
            key = regionKey;
            origin = regionOrigin(regionKey);
            bounds = new AABB(
                origin.getX() - SWAY_MARGIN,
                origin.getY() - SWAY_MARGIN,
                origin.getZ() - SWAY_MARGIN,
                origin.getX() + REGION_WIDTH * SECTION_SIZE + SWAY_MARGIN,
                origin.getY() + REGION_HEIGHT * SECTION_SIZE + SWAY_MARGIN,
                origin.getZ() + REGION_WIDTH * SECTION_SIZE + SWAY_MARGIN
            );
            blockBounds = new BoundingBox(
                origin.getX(),
                origin.getY(),
                origin.getZ(),
                origin.getX() + REGION_WIDTH * SECTION_SIZE - 1,
                origin.getY() + REGION_HEIGHT * SECTION_SIZE - 1,
                origin.getZ() + REGION_WIDTH * SECTION_SIZE - 1
            );
        }

        /** Squared distance from the camera to the region's centre. */
        double distanceSqTo(Vec3 camera) {
            double x =
                origin.getX() + (REGION_WIDTH * SECTION_SIZE) / 2.0D - camera.x;
            double y =
                origin.getY() +
                (REGION_HEIGHT * SECTION_SIZE) / 2.0D -
                camera.y;
            double z =
                origin.getZ() + (REGION_WIDTH * SECTION_SIZE) / 2.0D - camera.z;
            return x * x + y * y + z * z;
        }

        void put(int slot, Section section) {
            if (sections[slot] == null) {
                sectionCount++;
            } else {
                sections[slot].free();
            }
            sections[slot] = section;
        }

        /** Returns whether there was a section in the slot to remove. */
        boolean remove(int slot) {
            Section section = sections[slot];
            if (section == null) {
                return false;
            }
            GpuFoliageSplit.unmarkGpuReady(section.key);
            section.free();
            sections[slot] = null;
            sectionCount--;
            return true;
        }

        /**
         * Writes a section over the stretch of the buffer the one in its slot was uploaded to, which is the same size:
         * its cutout part and its solid part, each where the last upload laid it. Returns whether it was written.
         */
        boolean patch(int slot, Section section) {
            int stride = section.stride;
            int cutoutBytes = section.cutoutVertexCount() * stride;
            int opaqueBytes = section.opaqueVertexCount * stride;
            if (cutoutBytes + opaqueBytes == 0) {
                return true;
            }
            //? >=1.21.11 {
            var encoder = RenderSystem.getDevice().createCommandEncoder();
            if (cutoutBytes > 0) {
                encoder.writeToBuffer(
                    vertices.slice(firstVertex[slot] * stride, cutoutBytes),
                    section.data.slice(0, cutoutBytes)
                );
            }
            if (opaqueBytes > 0) {
                encoder.writeToBuffer(
                    vertices.slice(
                        firstOpaqueVertex[slot] * stride,
                        opaqueBytes
                    ),
                    section.data.slice(cutoutBytes, opaqueBytes)
                );
            }
            return true;
            //?} else {
            /*if (VulkanBridge.drawsChunks()) {
				// No OpenGL to write into the buffer with: the region is uploaded whole.
				return false;
			}
			// A vertex buffer keeps its buffer to itself; the array it binds says which one its attributes read from.
			vertices.bind();
			int buffer = org.lwjgl.opengl.GL20.glGetVertexAttribi(0, org.lwjgl.opengl.GL20.GL_VERTEX_ATTRIB_ARRAY_BUFFER_BINDING);
			if (buffer == 0) {
				VertexBuffer.unbind();
				return false;
			}
			com.mojang.blaze3d.platform.GlStateManager._glBindBuffer(org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER, buffer);
			if (cutoutBytes > 0) {
				org.lwjgl.opengl.GL15.glBufferSubData(org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER, (long) firstVertex[slot] * stride,
						section.data.slice(0, cutoutBytes));
			}
			if (opaqueBytes > 0) {
				org.lwjgl.opengl.GL15.glBufferSubData(org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER, (long) firstOpaqueVertex[slot] * stride,
						section.data.slice(cutoutBytes, opaqueBytes));
			}
			VertexBuffer.unbind();
			return true;
			*/ //?}
        }

        void upload() {
            // Before 1.21.11 the buffer is uploaded over rather than deleted and made again.
            //? >=1.21.11 {
            closeBuffers();
            //?}
            int vertexCount = 0;
            occupiedCount = 0;
            for (int slot = 0; slot < SECTIONS_PER_REGION; slot++) {
                uploadedSections[slot] = sections[slot];
                if (sections[slot] != null) {
                    firstVertex[slot] = vertexCount;
                    vertexCount += sections[slot].cutoutVertexCount();
                    occupied[occupiedCount++] = slot;
                }
            }
            // The solid leaves after all of it, so each kind is drawn in long runs.
            for (int i = 0; i < occupiedCount; i++) {
                firstOpaqueVertex[occupied[i]] = vertexCount;
                vertexCount += sections[occupied[i]].opaqueVertexCount;
            }
            // Its sections are already interleaved, so each one is a single copy. A region is rebuilt
            // whenever any of its sections changes, and there are up to 256 of them, so doing the
            // per-vertex work here instead would repeat it for every section that did not change. Every section
            // in a region was meshed the same way: switching to or from a shader pack meshes everything again.
            int stride =
                occupiedCount == 0
                    ? VERTEX_BYTES + WEIGHT_BYTES
                    : sections[occupied[0]].stride;
            //? >=1.21.11 {
            // Staging memory is borrowed rather than asked for: uploads happen one after another on the
            // render thread, and a region can be hundreds of kilobytes, so this saves an allocation and a
            // free every time any section in any region changes.
            ByteBuffer vertexData = uploadScratch(vertexCount * stride);
            for (int i = 0; i < occupiedCount; i++) {
                int slot = occupied[i];
                Section section = sections[slot];
                int cutoutBytes = section.cutoutVertexCount() * stride;
                vertexData.put(
                    firstVertex[slot] * stride,
                    section.data,
                    0,
                    cutoutBytes
                );
                if (section.opaqueVertexCount > 0) {
                    vertexData.put(
                        firstOpaqueVertex[slot] * stride,
                        section.data,
                        cutoutBytes,
                        section.opaqueVertexCount * stride
                    );
                }
            }
            vertices = RenderSystem.getDevice().createBuffer(
                () -> "MC2 foliage vertices",
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                vertexData
            );
            //?} else {
            /*// Handed over as a mesh, the way vanilla uploads a chunk section, so the vertex buffer sets up its
			// attributes from the mod's format and its indices from vanilla's shared quad index buffer. The
			// staging builder is borrowed, as above, and is empty again once the mesh is closed.
			//? >=1.21.1 {
			ByteBufferBuilder staging = uploadBuilder();
			long pointer = staging.reserve(vertexCount * stride);
			for (int i = 0; i < occupiedCount; i++) {
				int slot = occupied[i];
				Section section = sections[slot];
				long cutoutBytes = (long) section.cutoutVertexCount() * stride;
				MemoryUtil.memCopy(MemoryUtil.memAddress(section.data, 0),
						pointer + (long) firstVertex[slot] * stride, cutoutBytes);
				if (section.opaqueVertexCount > 0) {
					MemoryUtil.memCopy(MemoryUtil.memAddress(section.data, 0) + cutoutBytes,
							pointer + (long) firstOpaqueVertex[slot] * stride, (long) section.opaqueVertexCount * stride);
				}
			}
			MeshData mesh = new MeshData(staging.build(), new MeshData.DrawState(regionFormat(), vertexCount,
					indexCountFor(vertexCount), VertexFormat.Mode.QUADS, VertexFormat.IndexType.least(vertexCount)));
			//?} elif fabric {
			/^// Before 1.21.1 a mesh only comes out of a buffer builder, and the builder has no way to take a block of
			// vertices written elsewhere; it is opened up for this (see the access widener), and written straight into.
			BufferBuilder staging = uploadBuilder();
			staging.begin(VertexFormat.Mode.QUADS, regionFormat());
			staging.ensureCapacity(vertexCount * stride + stride);
			long pointer = MemoryUtil.memAddress(staging.buffer, staging.nextElementByte);
			for (int i = 0; i < occupiedCount; i++) {
				int slot = occupied[i];
				Section section = sections[slot];
				long cutoutBytes = (long) section.cutoutVertexCount() * stride;
				MemoryUtil.memCopy(MemoryUtil.memAddress(section.data, 0),
						pointer + (long) firstVertex[slot] * stride, cutoutBytes);
				if (section.opaqueVertexCount > 0) {
					MemoryUtil.memCopy(MemoryUtil.memAddress(section.data, 0) + cutoutBytes,
							pointer + (long) firstOpaqueVertex[slot] * stride, (long) section.opaqueVertexCount * stride);
				}
			}
			staging.nextElementByte += vertexCount * stride;
			staging.vertices = vertexCount;
			BufferBuilder.RenderedBuffer mesh = staging.end();
			^///?} else {
			/^// Before 1.21.1 a mesh only comes out of a buffer builder. Forge's takes a block of vertices whole, so each
			// section's parts go in as they stand, in the order the region's layout was worked out in: every cutout part,
			// then every solid one.
			BufferBuilder staging = uploadBuilder();
			staging.begin(VertexFormat.Mode.QUADS, regionFormat());
			for (int kind = 0; kind < 2; kind++) {
				for (int i = 0; i < occupiedCount; i++) {
					Section section = sections[occupied[i]];
					int cutoutBytes = section.cutoutVertexCount() * stride;
					int from = kind == 0 ? 0 : cutoutBytes;
					int bytes = kind == 0 ? cutoutBytes : section.opaqueVertexCount * stride;
					if (bytes > 0) {
						// A view of its own, so the section's data is left where it was for the region's next upload.
						staging.putBulkData(section.data.slice(from, bytes));
					}
				}
			}
			BufferBuilder.RenderedBuffer mesh = staging.end();
			^///?}
			if (vertices == null) {
				vertices = new VertexBuffer(VertexBuffer.Usage.STATIC);
			}
			vertices.bind();
			// Closes the mesh once it is on the GPU.
			vertices.upload(mesh);
			VertexBuffer.unbind();
			*/ //?}
            markSectionsReady();
        }

        /**
         * Every section now on the GPU may have its foliage left out of the chunk mesh. A near one that was not ready
         * before still has it there, so its chunk mesh is asked to be built again.
         */
        void markSectionsReady() {
            Minecraft minecraft = Minecraft.getInstance();
            for (int i = 0; i < occupiedCount; i++) {
                long key = sections[occupied[i]].key;
                if (
                    GpuFoliageSplit.markGpuReady(key) &&
                    GpuFoliageSplit.isNear(SectionPos.x(key), SectionPos.z(key))
                ) {
                    rebuildChunkMesh(
                        minecraft,
                        SectionPos.x(key),
                        SectionPos.y(key),
                        SectionPos.z(key)
                    );
                } else if (
                    GpuFoliageSplit.groupTakenOver(
                        key,
                        sections[occupied[i]].listGeneration
                    )
                ) {
                    // It now holds the blocks of a group switched on since: the chunk mesh, which kept them meanwhile, lets go.
                    rebuildChunkMesh(
                        minecraft,
                        SectionPos.x(key),
                        SectionPos.y(key),
                        SectionPos.z(key)
                    );
                }
            }
        }

        //? >=26.3 {
        /**
         * The terrain shaders' ChunkSection block for this region, made when first asked for, while a frame is prepared.
         * <p>
         * It holds the region's corner and full visibility, which never change for a region, so it is written once. The
         * game's own storage for these blocks is not used: it lends one storage out for one kind of use at a time, and on
         * 26.3 that storage is also the vertex buffer the chunks are drawn from with multidraw, which asking for the
         * other kind closes mid-frame.
         */
        private GpuBuffer chunkSection;

        GpuBuffer chunkSection() {
            if (chunkSection == null) {
                ByteBuffer data = MemoryUtil.memAlloc(
                    DynamicGpuData.CHUNK_SECTION_UBO_SIZE
                );
                try {
                    Std140Builder.intoBuffer(data)
                        .putIVec3(origin.getX(), origin.getY(), origin.getZ())
                        .putFloat(1.0F);
                    data.position(0);
                    chunkSection = RenderSystem.getDevice().createBuffer(
                        () -> "MC2 foliage chunk section",
                        GpuBuffer.USAGE_UNIFORM,
                        data
                    );
                } finally {
                    MemoryUtil.memFree(data);
                }
            }
            return chunkSection;
        }

        //?}

        void closeBuffers() {
            if (vertices != null) {
                vertices.close();
                vertices = null;
            }
            //? >=26.3 {
            if (chunkSection != null) {
                chunkSection.close();
                chunkSection = null;
            }
            // A frame is prepared as the level's extraction ends and drawn a little later, and in between the regions can
            // be let go of: a shader pack loading as the level starts to be drawn reloads everything, which discards them
            // all. What was prepared from them is not drawn then; the next frame is prepared from what is there.
            pendingDraw = null;
            pendingLevelUniforms = null;
            //?}
        }

        void free() {
            closeBuffers();
            for (int slot = 0; slot < SECTIONS_PER_REGION; slot++) {
                remove(slot);
            }
        }
    }

    /**
     * Turns Sway's multiblock topology into a per-vertex sway weight.
     * <p>
     * Sway already knows where a plant is held: the bottom of a sugar cane stack, the ceiling a vine
     * hangs from, or the block itself for ordinary foliage. Measuring each vertex against that
     * anchor is what keeps a tall strand bending as one piece rather than every block swaying on its
     * own, which is how it would look if the weight came from the height within a single block.
     */
    private static final class SwayAnchor {

        /** Column length is shared by every block of a strand, so it is resolved once per anchor. */
        private final Map<BlockPos, Integer> lengthByAnchor = new HashMap<>();
        /** Whether a strand starts on leaves, where its free part starts and how long it is, by anchor; see hangOnLeaves. */
        private final Long2ObjectOpenHashMap<float[]> freeByAnchor =
            new Long2ObjectOpenHashMap<>();
        /** Whether this is a block hanging on leaves while they wave; see hangOnLeaves. */
        private boolean hangsOnLeaves;
        /** Whether a push leaves it where it is: a block that follows the leaves, clinging to anything else. */
        private boolean pushless;
        /** Where each vine starts to hang free, and whether leaves hold it there, by anchor; see hangVine. */
        private final Long2ObjectOpenHashMap<int[]> holdByAnchor =
            new Long2ObjectOpenHashMap<>();
        /** Whether this block of a vine hangs free, below everything holding the strand; see hangVine. */
        private boolean vineFree;
        /** Whether this block of a vine moves as the leaves do: held by them, or hanging free from them. */
        private boolean vineOnLeaves;
        /** Where its free part starts, and how many blocks long it is: none for a strand held all the way down. */
        private float freeTop;
        private int freeLength;

        private float anchorY;
        /** The anchor block, where Sway keeps the force for the whole plant. */
        private int cellX;
        private int cellY;
        private int cellZ;
        private boolean hanging;
        /** How many blocks tall the plant stands, from its anchor. */
        private int length = 1;
        /** How much of rain's wind reaches the plant; see WindShelter. Set once the anchor is prepared. */
        private float exposure = 1.0F;
        /** Where a field's wind is worked out, corner by corner, while it rains; null while it does not. */
        private WindShelter fieldShelter;
        /**
         * How much of its sway the plant keeps, wind included: all of it, SNOW_LADEN_SWAY for a plant weighed down by
         * Snow! Real Magic's snow, or more for a block of the waving whitelist, as its group says. The weight it scales
         * never passes a plant's tip. A push moves it as far as any other.
         */
        private float waving = 1.0F;
        /**
         * Whether the plant stands still in calm weather, the weather's wind moving it all the same. Only a block that
         * follows the leaves is marked so, together with steady; see WavingWhitelist.followLeaves().
         */
        private boolean still;
        /**
         * Whether the block waves as a field, as a tree's leaves do: every vertex as far, by where it is in the world, so
         * blocks that touch move together. Nothing pushes it, and the wind reaches it wherever it is.
         */
        private boolean field;
        /** Whether a push leaves the plant's sway as it is: a field, and what follows the leaves while they wave. */
        private boolean steady;
        /** Sway's own deformation for this plant, which decides how far a push moves each vertex. */
        private DeformationContributor deformation;
        private float deformationScale;
        private BlockState state;
        private BlockPos pos;
        private ClientLevel level;
        /**
         * For a block resting on leaves -- snow, a carpet -- the level it lies at, which is the top of the leaf under it;
         * NOT_RESTING for any other. See cornerY.
         */
        private int restY = NOT_RESTING;
        private static final int NOT_RESTING = Integer.MIN_VALUE;
        /** For a field: whether each corner asked for so far stands still, 0, or waves, 1; see pinAt. */
        private final Long2FloatOpenHashMap fieldPins =
            new Long2FloatOpenHashMap();
        private final BlockPos.MutableBlockPos pinCursor =
            new BlockPos.MutableBlockPos();

        void prepare(BlockState state, BlockPos pos, ClientLevel level) {
            this.level = level;
            hanging = HangingVineMultiblockBehavior.isHangingVine(state);
            BlockPos anchor = pos;
            MultiBlockContributor multiblock = null;
            BehaviorPipeline pipeline = SwayAPI.getBehaviorPipeline(
                state.getBlock()
            );
            if (pipeline != null) {
                for (MultiBlockContributor contributor : pipeline.getMultiBlockContributors()) {
                    if (contributor.appliesTo(state)) {
                        multiblock = contributor;
                        anchor = contributor.getAnchorPosition(pos, state);
                        break;
                    }
                }
            }
            deformation = null;
            if (pipeline != null) {
                for (DeformationContributor contributor : pipeline.getDeformationContributors()) {
                    if (contributor.appliesTo(state)) {
                        deformation = contributor;
                        break;
                    }
                }
            }
            deformationScale =
                deformation == null
                    ? 0.0F
                    : deformation.getDeformationScale(state, pos);
            this.state = state;
            this.pos = pos;
            // Hanging plants are held at the top of their anchor block, everything else at the base -- lowered with the
            // plant where it stands on a slab.
            anchorY = hanging
                ? anchor.getY() + 1
                : anchor.getY() +
                  TerrainSlabsCompat.offsetY(
                      level,
                      anchor,
                      anchor.equals(pos)
                          ? state
                          : SnowRealMagicCompat.plantAt(level, anchor)
                  );
            cellX = anchor.getX();
            cellY = anchor.getY();
            cellZ = anchor.getZ();
            length =
                multiblock == null ? 1 : lengthOf(multiblock, anchor, level);
        }

        /**
         * How many blocks long a strand is, from its anchor.
         * <p>
         * Long strands sway less as a whole: past the span the weight saturates, so without damping every
         * block above it moves at full strength and a tall cane or vine thrashes rather than drifts. Damping
         * the column by its length keeps short plants lively while long ones stay heavy. Only a wall at least
         * as tall shelters a plant from rain's wind.
         */
        private int lengthOf(
            MultiBlockContributor multiblock,
            BlockPos anchor,
            ClientLevel level
        ) {
            Integer cached = lengthByAnchor.get(anchor);
            if (cached != null) {
                return cached;
            }
            // A tall plant's lower half may stand inside Snow! Real Magic's snow, its upper half above it.
            BlockState anchorState = SnowRealMagicCompat.plantAt(level, anchor);
            int value =
                multiblock.getLinkedBlocks(anchor, anchorState, level).size() +
                1;
            lengthByAnchor.put(anchor.immutable(), value);
            return value;
        }

        /**
         * How far a push moves this vertex, straight from Sway: the same curve and scale the chunk mesh uses,
         * so a plant bends the same whoever draws it. The wind's own weight is a different shape, spread over
         * several blocks, which would leave a short plant barely pushed.
         *
         * @param localY the vertex's height within its own block, as Sway measures it
         */
        float pushWeightAt(float localY) {
            return deformation == null
                ? 0.0F
                : deformation.getVertexWeight(localY, state, pos) *
                      deformationScale;
        }

        /**
         * How freely a vertex sways, from the anchor up (or down, for a hanging plant) to the tip. Up to
         * SWAY_SPAN blocks the weight grows in step with the distance. A longer strand spreads it over its whole
         * length instead, curving along it the way Sway bends one -- the square of the way along for a stalk, the
         * cube for a hanging vine, so it hangs like a rope -- easing into that curve over COLUMN_CURVE_BLOCKS
         * past the span, so no length looks different from the next.
         */
        float weightAt(float worldY) {
            float distance = hanging ? anchorY - worldY : worldY - anchorY;
            return curveAt(distance, length, hanging);
        }

        /** The sway's weight this far along a strand this long, from where it is held; see weightAt. */
        private static float curveAt(
            float distance,
            int length,
            boolean hanging
        ) {
            float along = Mth.clamp(
                distance / Math.max(SWAY_SPAN, length),
                0.0F,
                1.0F
            );
            float curving = Mth.clamp(
                (length - SWAY_SPAN) / COLUMN_CURVE_BLOCKS,
                0.0F,
                1.0F
            );
            float eased = curving * curving * (3.0F - 2.0F * curving);
            float exponent = Mth.lerp(
                eased,
                1.0F,
                hanging ? HANGING_CURVE : STALK_CURVE
            );
            float damping =
                1.0F -
                Math.min(
                    MAX_LENGTH_DAMPING,
                    (length - 1) * LENGTH_DAMPING_PER_BLOCK
                );
            return (
                (float) Math.pow(along, exponent) *
                damping *
                Mth.lerp(eased, 1.0F, LONG_STRAND_SWAY)
            );
        }

        /**
         * How freely a vertex at this height sways, as meshed: a field waves the same all over, and a block hanging on
         * leaves sways as a plant only where it hangs free.
         */
        float waveAt(float worldY) {
            if (field) {
                return waving;
            }
            if (hangsOnLeaves) {
                return freeLength == 0
                    ? 0.0F
                    : curveAt(freeTop - worldY, freeLength, true) * waving;
            }
            if (vineFree) {
                // Straight, from nothing where it starts to hang to its tip.
                return (
                    VINE_TIP_SWAY *
                    Mth.clamp(
                        (freeTop - worldY) / Math.max(VINE_SPAN, freeLength),
                        0.0F,
                        1.0F
                    ) *
                    waving
                );
            }
            return weightAt(worldY) * waving;
        }

        /**
         * How much of rain's wind reaches a vertex at this position: the plant's own, or for a field and a block hanging on
         * it, the corner's it sits on, which every block sharing that corner reads alike. Where a strand hangs free, below
         * what holds it, the whole of it reads one reach instead (see strandExposure): corner by corner each stretch of it,
         * and each side of a stretch, caught a different wind near a wall or a roof, and the strand twisted against it.
         */
        float exposureAt(float worldX, float worldY, float worldZ) {
            if (
                !(field || hangsOnLeaves || vineFree || vineOnLeaves) ||
                fieldShelter == null
            ) {
                return exposure;
            }
            // The row a strand starts to hang from is shared with the block holding it, and reads the corner as that block
            // does: read any other way the two parted there by as much as the wind reached them differently.
            if (
                (vineFree || (hangsOnLeaves && freeLength > 0)) &&
                worldY < freeTop - FREE_PART_MARGIN
            ) {
                return strandExposure();
            }
            return fieldShelter.cornerExposure(
                Math.round(worldX),
                cornerY(worldY),
                Math.round(worldZ)
            );
        }

        /** How far below where a strand starts to hang free a vertex has to be to count as hanging free itself. */
        private static final float FREE_PART_MARGIN = 0.01F;
        /** The one reach of rain's wind each strand's free part reads, by anchor; see strandExposure. */
        private final Long2FloatOpenHashMap strandExposureByAnchor =
            new Long2FloatOpenHashMap();

        /**
         * How much of rain's wind reaches the part of this strand that hangs free: what reaches the four corners of its
         * column halfway down that part, as the leaves would read them, evened out. Worked out once per strand.
         */
        private float strandExposure() {
            long anchor = BlockPos.asLong(cellX, cellY, cellZ);
            if (strandExposureByAnchor.containsKey(anchor)) {
                return strandExposureByAnchor.get(anchor);
            }
            int middle = Math.round(freeTop - freeLength * 0.5F);
            float sum = 0.0F;
            for (int dx = 0; dx <= 1; dx++) {
                for (int dz = 0; dz <= 1; dz++) {
                    sum += fieldShelter.cornerExposure(
                        cellX + dx,
                        middle,
                        cellZ + dz
                    );
                }
            }
            float reach = sum / 4.0F;
            strandExposureByAnchor.put(anchor, reach);
            return reach;
        }

        /**
         * The level of the corner a vertex at this height reads: the nearest, or for a block resting on leaves the leaf's
         * top under it, whatever the vertex's own height, so the whole layer moves as that leaf does and never shears.
         */
        private int cornerY(float worldY) {
            return restY != NOT_RESTING ? restY : Math.round(worldY);
        }

        /**
         * How much of its wave a field's vertex at this position keeps: none at a corner touching a block that holds the
         * leaves still (see holdsLeavesStill) -- a trunk, the ground -- so they never sink into it as they wave, nor part
         * from it; all of it otherwise. Worked out from the corner alone, the eight blocks around it, so every block sharing
         * it reads alike and neighbouring leaves never part. A block hanging on the leaves is told the same; see leafHeldAt.
         */
        float pinAt(float worldX, float worldY, float worldZ) {
            return field ? cornerPin(worldX, worldY, worldZ) : 1.0F;
        }

        /**
         * Whether the leaves a block hangs on stand still where this vertex of it is, against a block holding them (see
         * pinAt): read from the same corner they read, so it moves exactly as they do there. Only while it moves as the
         * leaves do -- still and steady at once.
         */
        boolean leafHeldAt(float worldX, float worldY, float worldZ) {
            if (vineFree) {
                // A vine's free part moves with the leaf it hangs from all the way down: as that leaf does at the corner
                // above this vertex, where the strand starts to hang.
                worldY = freeTop;
            }
            return (
                (hangsOnLeaves || vineOnLeaves) &&
                still &&
                steady &&
                cornerPin(worldX, worldY, worldZ) == 0.0F
            );
        }

        /** 0 where a block holding the leaves still is among the eight around the corner nearest this position, else 1. */
        private float cornerPin(float worldX, float worldY, float worldZ) {
            int x = Math.round(worldX);
            int y = cornerY(worldY);
            int z = Math.round(worldZ);
            long key = BlockPos.asLong(x, y, z);
            if (fieldPins.containsKey(key)) {
                return fieldPins.get(key);
            }
            float pin = 1.0F;
            for (int dy = -1; dy <= 0 && pin > 0.0F; dy++) {
                for (int dz = -1; dz <= 0 && pin > 0.0F; dz++) {
                    for (int dx = -1; dx <= 0; dx++) {
                        pinCursor.set(x + dx, y + dy, z + dz);
                        if (
                            holdsLeavesStill(
                                level,
                                pinCursor,
                                level.getBlockState(pinCursor)
                            )
                        ) {
                            pin = 0.0F;
                            break;
                        }
                    }
                }
            }
            fieldPins.put(key, pin);
            return pin;
        }

        /** How far a push moves a vertex at this height, as meshed: Sway's own weight, none for a block Sway does not push. */
        float pushAt(float localY, float worldY) {
            if (pushless) {
                return 0.0F;
            }
            // A vine is only pushed where it hangs free, from nothing where something holds it.
            return vineFree
                ? pushWeightAt(localY) * Mth.clamp(freeTop - worldY, 0.0F, 1.0F)
                : pushWeightAt(localY);
        }

        /**
         * Sets this block of a vine up, block by block rather than the strand as a whole:
         * <ul>
         * <li>with leaves behind it, it moves exactly as they do, as a layer lying on them does, corners held still against
         * a trunk included, and nothing pushes it; while the leaves stand still, so does it;</li>
         * <li>with anything else solid behind it, it stands still and nothing pushes it;</li>
         * <li>below the lowest block with something behind it, it hangs free: straight as a strip of cloth (see VINE_SPAN),
         * from the movement of what holds it -- the leaves', or none -- growing evenly down to the tip, in calm weather
         * and in the weather's wind, whatever the leaves do; pushes bend it and never calm it.</li>
         * </ul>
         * A strand with nothing behind any of its blocks hangs from what is above its top.
         */
        void hangVine(ClientLevel level, BlockPos pos, BlockState state) {
            long anchor = BlockPos.asLong(cellX, cellY, cellZ);
            int[] hold = holdByAnchor.get(anchor);
            if (hold == null) {
                hold = measureHold(level);
                holdByAnchor.put(anchor, hold);
            }
            int tipY = cellY - length + 1;
            freeTop = hold[0];
            freeLength = hold[0] - tipY;
            boolean leavesWave = WavingWhitelist.leavesWave();
            exposure = 1.0F;
            if (pos.getY() >= hold[0]) {
                waving = 0.0F;
                pushless = true;
                // Not against anything solid: with the leaves beside it, corner by corner.
                vineOnLeaves =
                    leavesWave && behind(level, pos, state) != BEHIND_OTHER;
                still = vineOnLeaves;
                steady = vineOnLeaves;
                return;
            }
            vineFree = true;
            vineOnLeaves = leavesWave && hold[1] != 0;
            still = vineOnLeaves;
            steady = true;
        }

        /** Where a vine starts to hang free -- the bottom of its lowest block with something behind -- and whether leaves hold it there. */
        private int[] measureHold(ClientLevel level) {
            BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
            for (int y = cellY - length + 1; y <= cellY; y++) {
                at.set(cellX, y, cellZ);
                int behind = behind(level, at, level.getBlockState(at));
                if (behind != BEHIND_NOTHING) {
                    return new int[] { y, behind == BEHIND_LEAVES ? 1 : 0 };
                }
            }
            // Nothing behind any of it: it hangs from what is above its top.
            at.set(cellX, cellY + 1, cellZ);
            return new int[] {
                cellY + 1,
                WavingWhitelist.isLeaves(level.getBlockState(at).getBlock())
                    ? 1
                    : 0,
            };
        }

        /**
         * Whether this strand -- a hanging plant other than a vine, which hangVine sets up -- literally hangs from leaves:
         * what is above its top decides, whatever is beside it. It then moves as the leaves do and sways as a plant where
         * it hangs free, from its lowest block with something behind it down to its tip; a strand of one or two blocks
         * only moves as the leaves. Sets where it hangs free; whether it then moves as the leaves is the caller's to set
         * (hangsOnLeaves). Worked out once per strand, each time its section is meshed.
         */
        boolean hangOnLeaves(ClientLevel level) {
            freeLength = 0;
            if (!hanging) {
                return false;
            }
            long anchor = BlockPos.asLong(cellX, cellY, cellZ);
            float[] known = freeByAnchor.get(anchor);
            if (known == null) {
                known = measureHang(level);
                freeByAnchor.put(anchor, known);
            }
            if (known[0] == 0.0F) {
                return false;
            }
            freeTop = known[1];
            freeLength = (int) known[2];
            return true;
        }

        /** Whether the strand hangs from leaves (1 or 0), the top of its free part and how long it is; see hangOnLeaves. */
        private float[] measureHang(ClientLevel level) {
            BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(
                cellX,
                cellY + 1,
                cellZ
            );
            // A strand not hanging from leaves is not walked at all, nor one too short to hang free.
            if (!WavingWhitelist.isLeaves(level.getBlockState(at).getBlock())) {
                return new float[] { 0.0F, 0.0F, 0.0F };
            }
            if (length <= SHORT_HANGING_STRAND) {
                return new float[] { 1.0F, 0.0F, 0.0F };
            }
            int tipY = cellY - length + 1;
            // The lowest block with something behind it: everything below it hangs free.
            int heldY = cellY + 1;
            for (int y = cellY; y >= tipY; y--) {
                at.set(cellX, y, cellZ);
                if (
                    behind(level, at, level.getBlockState(at)) != BEHIND_NOTHING
                ) {
                    heldY = y;
                }
            }
            // Nothing behind any of it: it hangs free from its anchor, as any hanging plant.
            return heldY > cellY
                ? new float[] { 1.0F, anchorY, length }
                : new float[] { 1.0F, heldY, heldY - tipY };
        }

        private static final int BEHIND_NOTHING = 0;
        private static final int BEHIND_LEAVES = 1;
        private static final int BEHIND_OTHER = 2;

        /**
         * What is behind this block of a strand, on a side it clings to, or any side for one that clings to none: leaves
         * where any of those sides has them, anything else, or nothing.
         */
        private static int behind(
            ClientLevel level,
            BlockPos pos,
            BlockState state
        ) {
            boolean clings = false;
            int found = BEHIND_NOTHING;
            BlockPos.MutableBlockPos beside = new BlockPos.MutableBlockPos();
            for (Direction side : Direction.Plane.HORIZONTAL) {
                BooleanProperty face = PipeBlock.PROPERTY_BY_DIRECTION.get(
                    side
                );
                if (state.hasProperty(face) && state.getValue(face)) {
                    clings = true;
                    found = Math.max(
                        found,
                        supportAt(
                            level,
                            beside.setWithOffset(pos, side),
                            side.getAxis()
                        )
                    );
                }
            }
            if (!clings) {
                for (Direction side : Direction.Plane.HORIZONTAL) {
                    found = Math.max(
                        found,
                        supportAt(
                            level,
                            beside.setWithOffset(pos, side),
                            side.getAxis()
                        )
                    );
                }
            }
            // Leaves on any side outweigh anything else on another.
            return found == BEHIND_LEAVES_RANK ? BEHIND_LEAVES : found;
        }

        /** Ranks what is at a position for behind(): leaves above anything solid, anything solid above nothing. */
        private static final int BEHIND_LEAVES_RANK = 3;

        /**
         * What is at a position for behind(). Only a block solid on that side counts as something, by the test the rain's
         * wind uses for walls: a torch, a fence, a slab or another vine behind a strand is as good as nothing.
         */
        private static int supportAt(
            ClientLevel level,
            BlockPos pos,
            Direction.Axis axis
        ) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                return BEHIND_NOTHING;
            }
            if (WavingWhitelist.isLeaves(state.getBlock())) {
                return BEHIND_LEAVES_RANK;
            }
            return WindShelter.isSolidSide(level, pos, state, axis)
                ? BEHIND_OTHER
                : BEHIND_NOTHING;
        }
    }

    /**
     * Whether a block has a neighbour it would part from as it waves, on any side: anything but air, leaves, or a block
     * that holds the leaves still. A field's faces against such a neighbour are drawn all the same: the leaves move and it
     * does not, so a culled face would leave a hole between them. Against a block holding them still, every corner of the
     * face touching it stands still (SwayAnchor.pinAt), so the face never moves and is culled as the chunk mesh culls it.
     */
    private static boolean partsFromNeighbour(
        ClientLevel level,
        LevelChunkSection blocks,
        int dx,
        int dy,
        int dz,
        BlockPos pos,
        BlockPos.MutableBlockPos at
    ) {
        for (Direction side : SIDES) {
            int x = dx + side.getStepX();
            int y = dy + side.getStepY();
            int z = dz + side.getStepZ();
            at.setWithOffset(pos, side);
            // Inside the section it is read from the section itself, as the mesher reads every block; only the sides on
            // its faces go through the level.
            BlockState beside =
                (x | y | z) >= 0 &&
                x < SECTION_SIZE &&
                y < SECTION_SIZE &&
                z < SECTION_SIZE
                    ? blocks.getBlockState(x, y, z)
                    : level.getBlockState(at);
            if (
                !beside.isAir() &&
                !WavingWhitelist.isLeaves(beside.getBlock()) &&
                !holdsLeavesStill(level, at, beside)
            ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether a block holds the leaves touching it still: a full block that is not leaves -- a trunk, the ground, a wall.
     * The corners of a field touching one do not wave, so leaves never sink into it nor part from it. A block that is not
     * full (a fence, stairs, a slab) does not: leaves are not flush with it, and keep every face against it instead.
     * <p>
     * A layer that would rest on leaves but lies on something else -- a carpet on a fence, beside one on a leaf -- holds
     * them still too: it stays with the chunk mesh, unmoved, so a corner it shares with a layer that does rest on a leaf
     * stands still for both, and no gap opens between the two.
     */
    private static boolean holdsLeavesStill(
        ClientLevel level,
        BlockPos pos,
        BlockState state
    ) {
        if (
            state.isAir() ||
            state.getBlock() instanceof LeavesBlock ||
            WavingWhitelist.isLeaves(state.getBlock())
        ) {
            return false;
        }
        if (WavingWhitelist.restsOnLeaves(state.getBlock())) {
            return !WavingWhitelist.isLeaves(
                level.getBlockState(pos.below()).getBlock()
            );
        }
        return state.isCollisionShapeFullBlock(level, pos);
    }

    /** Every side of a block, made once: Direction.values() hands out a new array every time. */
    private static final Direction[] SIDES = Direction.values();

    /** Whether this renderer is drawing the near foliage, which is while it is switched on in a world. */
    static boolean isActive() {
        return active;
    }

    /**
     * The chunk mesh of a section was asked to be built again, by whatever route. Only sections already holding foliage
     * care, and they are rebuilt in their turn: what asks without a block or the light engine saying so is a dynamic
     * lights mod, for the sections around a light on the move.
     */
    public static void onSectionAsked(
        int sectionX,
        int sectionY,
        int sectionZ
    ) {
        if (!active) {
            return;
        }
        long key = SectionPos.asLong(sectionX, sectionY, sectionZ);
        Region region = REGIONS.get(regionKeyOf(key));
        if (
            region != null &&
            region.sections[slotOf(key)] != null &&
            DIRTY.add(key)
        ) {
            burstAsked++;
        }
    }

    /** Light arrived or changed in a section. Only sections already holding foliage care. */
    public static void onLightChanged(
        int sectionX,
        int sectionY,
        int sectionZ
    ) {
        long key = SectionPos.asLong(sectionX, sectionY, sectionZ);
        // A light that keeps changing -- one carried about, with a dynamic lights mod, which asks for the sections around
        // it again every tick it moves -- is not rebuilt at once each time: that would be every section around the player,
        // on the render thread, twenty times a second. Past the first, it takes its turn within the frame's budget.
        long now = System.currentTimeMillis();
        long last = LIGHT_CHANGED_AT.put(key, now);
        if (LIGHT_CHANGED_AT.size() > 8192) {
            LIGHT_CHANGED_AT.clear();
        }
        if (now - last < LIGHT_STREAM_MILLIS) {
            Region region = REGIONS.get(regionKeyOf(key));
            if (
                region != null &&
                region.sections[slotOf(key)] != null &&
                DIRTY.add(key)
            ) {
                burstLightQueued++;
                burstLightStreamed++;
            }
            return;
        }
        if (queueUrgent(key)) {
            burstLightQueued++;
        }
    }

    /**
     * A block's data changed without the block itself changing. Only Snow! Real Magic's snow cares: the plant it holds
     * lives in its block entity, which arrives after the block and changes on its own -- when the snow is placed on a
     * plant, or a plant is put into or taken out of it.
     */
    public static void onBlockUpdated(BlockPos pos, BlockState state) {
        if (SnowRealMagicCompat.isSnowBlock(state)) {
            onBlockChanged(pos);
        }
    }

    /**
     * A block changed. The sections above and below are rebuilt with its own because a column's sway
     * depends on its length and anchor, and a column can cross a section boundary; sideways
     * neighbours are only touched when the block sits on that edge.
     */
    public static void onBlockChanged(BlockPos pos) {
        if (!active) {
            return;
        }
        int sectionX = SectionPos.blockToSectionCoord(pos.getX());
        int sectionY = SectionPos.blockToSectionCoord(pos.getY());
        int sectionZ = SectionPos.blockToSectionCoord(pos.getZ());
        int localX = pos.getX() & (SECTION_SIZE - 1);
        int localZ = pos.getZ() & (SECTION_SIZE - 1);
        // The ones on screen already show the change at once; one with nothing meshed yet waits its turn.
        for (int dy = -1; dy <= 1; dy++) {
            queueChanged(SectionPos.asLong(sectionX, sectionY + dy, sectionZ));
        }
        if (localX == 0) {
            queueChanged(SectionPos.asLong(sectionX - 1, sectionY, sectionZ));
        } else if (localX == SECTION_SIZE - 1) {
            queueChanged(SectionPos.asLong(sectionX + 1, sectionY, sectionZ));
        }
        if (localZ == 0) {
            queueChanged(SectionPos.asLong(sectionX, sectionY, sectionZ - 1));
        } else if (localZ == SECTION_SIZE - 1) {
            queueChanged(SectionPos.asLong(sectionX, sectionY, sectionZ + 1));
        }
        if (!shelterActive) {
            return;
        }
        // A wall shelters the plants downwind of it -- west, as far as WindShelter looks for walls -- and a wall or a roof
        // the plants below it, so the sections holding them are rebuilt too.
        int westX = SectionPos.blockToSectionCoord(
            pos.getX() - WindShelter.REACH
        );
        int lowY = SectionPos.blockToSectionCoord(
            pos.getY() -
                Math.max(
                    WindShelter.MAX_WALL_HEIGHT,
                    WindShelter.MAX_CEILING_HEIGHT
                )
        );
        for (int x = westX; x <= sectionX; x++) {
            for (int y = lowY; y <= sectionY; y++) {
                markIfHeld(SectionPos.asLong(x, y, sectionZ));
            }
        }
    }

    /**
     * Meshes every section again with the wind shelter as rain starts; a section meshed without it leaves every plant
     * exposed. As rain stops nothing is meshed: the shader no longer reads the shelter.
     */
    private static void followRain(Minecraft minecraft) {
        boolean raining =
            FoliageSettings.weatherWind() &&
            minecraft.level != null &&
            minecraft.level.getRainLevel(1.0F) > 0.0F;
        if (raining != shelterActive) {
            shelterActive = raining;
            shelterCheckedReach = Float.NaN;
        }
        if (!shelterActive || minecraft.player == null) {
            return;
        }
        // Sections are meshed with the shelter only within rain's reach; those it has come to since are meshed again.
        float reach = weatherWindReach();
        double x = minecraft.player.getX();
        double z = minecraft.player.getZ();
        if (
            reach == shelterCheckedReach &&
            (reach >= UNBOUNDED_REACH ||
                Math.abs(x - shelterCheckedX) + Math.abs(z - shelterCheckedZ) <
                    SHELTER_RECHECK_BLOCKS)
        ) {
            return;
        }
        shelterCheckedReach = reach;
        shelterCheckedX = x;
        shelterCheckedZ = z;
        for (Region region : REGIONS.values()) {
            // Every section held now, not the layout last uploaded: one built or removed since is not in that yet.
            for (Section section : region.sections) {
                if (
                    section != null &&
                    !section.sheltered &&
                    withinReach(section.origin, x, z, reach)
                ) {
                    DIRTY.add(section.key);
                }
            }
        }
    }

    /**
     * How far from the player rain's wind blows, as the config screen's distance asks: as far as entities push plants,
     * half of the renderer's area, or no edge at all.
     */
    private static float weatherWindReach() {
        return switch (FoliageSettings.weatherWindDistance()) {
            case PERFORMANCE -> SwayConfig.INSTANCE.maxDistance;
            case HALF -> ((GpuFoliageSplit.radius() + 0.5F) * SECTION_SIZE) /
                2.0F;
            default -> UNBOUNDED_REACH;
        };
    }

    /** Whether any of a section's columns is within rain's reach of the player, easing included. */
    private static boolean withinReach(
        BlockPos sectionOrigin,
        double x,
        double z,
        float reach
    ) {
        if (reach >= UNBOUNDED_REACH) {
            return true;
        }
        double dx = Math.max(
            0.0D,
            Math.max(
                sectionOrigin.getX() - x,
                x - (sectionOrigin.getX() + SECTION_SIZE)
            )
        );
        double dz = Math.max(
            0.0D,
            Math.max(
                sectionOrigin.getZ() - z,
                z - (sectionOrigin.getZ() + SECTION_SIZE)
            )
        );
        return dx * dx + dz * dz <= reach * (double) reach;
    }

    /** Queues a section to be meshed again, if it holds foliage the renderer draws. */
    /** Queues a section a block changed in or beside: at once if it is drawn already, in its turn otherwise. */
    private static void queueChanged(long key) {
        if (!queueUrgent(key)) {
            DIRTY.add(key);
        }
    }

    private static void markIfHeld(long key) {
        Region region = REGIONS.get(regionKeyOf(key));
        if (region != null && region.sections[slotOf(key)] != null) {
            DIRTY.add(key);
        }
    }

    /** Everything changed: resource packs reloaded, a video option altered, world swapped. */
    public static void discardAll() {
        REGIONS.values().forEach(Region::free);
        REGIONS.clear();
        DIRTY.clear();
        // Nothing rediscovers a sub-level's sections the way the chunks around the player are: they are queued again.
        DIRTY.addAll(PLOT_OWNERS.keySet());
        DIRTY_REGIONS.clear();
        URGENT.clear();
        URGENT_REGIONS.clear();
        // The textures are the atlas's, which a resource reload makes anew.
        LIGHT_CHANGED_AT.clear();
        SPRITE_ANIMATES.clear();
        STATE_SPRITES.clear();
        // The renderer snapshots options such as smooth lighting, which are among the things that
        // land here, so it is rebuilt from the current options on next use.
        modelRenderer = null;
        //? >=26.1.2 {
        uncullingRenderer = null;
        //?}
        // Nothing queues the chunks that are already loaded again: with Sodium in charge a resource
        // reload rebuilds its own meshes without passing any hook. They are rediscovered on next draw.
        reseedPending = true;
        GpuFoliageInteraction.reset();
    }

    /** Queues the sections of a chunk whose palette could hold foliage. */
    public static void discoverChunk(ClientLevel level, LevelChunk chunk) {
        if (!active) {
            return;
        }
        int chunkX = SectionPos.blockToSectionCoord(
            chunk.getPos().getMinBlockX()
        );
        int chunkZ = SectionPos.blockToSectionCoord(
            chunk.getPos().getMinBlockZ()
        );
        LevelChunkSection[] sections = chunk.getSections();
        for (int index = 0; index < sections.length; index++) {
            int sectionY = level.getSectionYFromSectionIndex(index);
            if (mayHoldFoliage(sections[index])) {
                DIRTY.add(SectionPos.asLong(chunkX, sectionY, chunkZ));
            }
            // The chunk west of this one looked for walls here before it was loaded, and found none.
            if (shelterActive) {
                markIfHeld(SectionPos.asLong(chunkX - 1, sectionY, chunkZ));
            }
        }
    }

    public static void forgetChunk(ClientLevel level, LevelChunk chunk) {
        int chunkX = SectionPos.blockToSectionCoord(
            chunk.getPos().getMinBlockX()
        );
        int chunkZ = SectionPos.blockToSectionCoord(
            chunk.getPos().getMinBlockZ()
        );
        for (
            int sectionY = minSectionY(level);
            sectionY <= maxSectionY(level);
            sectionY++
        ) {
            long key = SectionPos.asLong(chunkX, sectionY, chunkZ);
            DIRTY.remove(key);
            removeSection(key);
            GpuFoliageSplit.forgetSection(key);
        }
    }

    private static void reseedLoadedChunks(
        Minecraft minecraft,
        ClientLevel level
    ) {
        int radius = buildRadius();
        int centreX = SectionPos.blockToSectionCoord(
            minecraft.player.getBlockX()
        );
        int centreZ = SectionPos.blockToSectionCoord(
            minecraft.player.getBlockZ()
        );
        for (
            int chunkX = centreX - radius;
            chunkX <= centreX + radius;
            chunkX++
        ) {
            for (
                int chunkZ = centreZ - radius;
                chunkZ <= centreZ + radius;
                chunkZ++
            ) {
                LevelChunk chunk = level
                    .getChunkSource()
                    .getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                if (chunk != null) {
                    discoverChunk(level, chunk);
                }
            }
        }
    }

    /**
     * Whether a section is worth scanning block by block. The palette check is conservative -- it may
     * answer yes for a block that has since gone, never no for one that is there -- and it skips whole
     * sections of stone or air without looking at a single block.
     */
    private static boolean mayHoldFoliage(LevelChunkSection section) {
        return !section.hasOnlyAir() && section.maybeHas(FOLIAGE);
    }

    private static boolean mayHoldFoliage(ClientLevel level, long key) {
        int sectionY = SectionPos.y(key);
        if (sectionY < minSectionY(level) || sectionY > maxSectionY(level)) {
            return false;
        }
        return mayHoldFoliage(
            level
                .getChunk(SectionPos.x(key), SectionPos.z(key))
                .getSection(level.getSectionIndexFromSectionY(sectionY))
        );
    }

    /** The lowest section of the world. */
    private static int minSectionY(ClientLevel level) {
        //? >=1.21.11 {
        return level.getMinSectionY();
        //?} else {
        /*return level.getMinSection();
         */ //?}
    }

    /** The highest section of the world, itself included. */
    private static int maxSectionY(ClientLevel level) {
        //? >=1.21.11 {
        return level.getMaxSectionY();
        //?} else {
        /*// Before 1.21.11 the world's top section is given as the one past it.
		return level.getMaxSection() - 1;
		*/ //?}
    }

    private static long regionKeyOf(long sectionKey) {
        return SectionPos.asLong(
            SectionPos.x(sectionKey) >> REGION_WIDTH_SHIFT,
            SectionPos.y(sectionKey) >> REGION_HEIGHT_SHIFT,
            SectionPos.z(sectionKey) >> REGION_WIDTH_SHIFT
        );
    }

    private static BlockPos regionOrigin(long regionKey) {
        return new BlockPos(
            SectionPos.sectionToBlockCoord(
                SectionPos.x(regionKey) << REGION_WIDTH_SHIFT
            ),
            SectionPos.sectionToBlockCoord(
                SectionPos.y(regionKey) << REGION_HEIGHT_SHIFT
            ),
            SectionPos.sectionToBlockCoord(
                SectionPos.z(regionKey) << REGION_WIDTH_SHIFT
            )
        );
    }

    private static int slotOf(long sectionKey) {
        int x = SectionPos.x(sectionKey) & (REGION_WIDTH - 1);
        int y = SectionPos.y(sectionKey) & (REGION_HEIGHT - 1);
        int z = SectionPos.z(sectionKey) & (REGION_WIDTH - 1);
        return (y * REGION_WIDTH + z) * REGION_WIDTH + x;
    }

    private static void putSection(long key, Section section) {
        Region region = REGIONS.computeIfAbsent(regionKeyOf(key), Region::new);
        // A plot is far wider than a region and starts where a region does, so a region is one sub-level's or none's.
        region.subLevel = PLOT_OWNERS.get(key);
        int slot = slotOf(key);
        // A section that comes out the size it is on screen -- its light changed, as a rule, and nothing else -- is
        // written over its own stretch of the region's buffer rather than the region uploaded whole: a region is up to
        // 256 sections, 8 ms and more to upload where the leaves wave, and a carried light asks for a few sections of
        // it every tick.
        Section shown = region.uploadedSections[slot];
        boolean sameSize =
            shown != null &&
            region.vertices != null &&
            shown.stride == section.stride &&
            shown.vertexCount == section.vertexCount &&
            shown.opaqueVertexCount == section.opaqueVertexCount;
        region.put(slot, section);
        if (sameSize) {
            long start = System.nanoTime();
            if (region.patch(slot, section)) {
                region.uploadedSections[slot] = section;
                burstPatched++;
                burstPatchNanos += System.nanoTime() - start;
                // Unless it waits to be uploaded whole for another section's sake, it is up to date.
                return;
            }
        }
        if (DIRTY_REGIONS.add(region)) {
            region.pendingSince = System.currentTimeMillis();
        }
    }

    private static void removeSection(long key) {
        long regionKey = regionKeyOf(key);
        Region region = REGIONS.get(regionKey);
        if (region == null || !region.remove(slotOf(key))) {
            return;
        }
        if (region.sectionCount == 0) {
            region.free();
            REGIONS.remove(regionKey);
            DIRTY_REGIONS.remove(region);
        } else if (DIRTY_REGIONS.add(region)) {
            region.pendingSince = System.currentTimeMillis();
        }
    }

    //? >=1.21.11 {
    /**
     * Called by each loader's hooks once the opaque terrain is drawn -- on 26.3, as the level's extraction ends, to
     * prepare what vanilla's pass will draw; see {@link #submitDraws}.
     */
    public static void draw(LevelRenderState levelState) {
        // The render state carries the camera and the cull frustum vanilla already prepared this
        // frame, which is available whether or not Sodium owns the terrain renderer.
        var cameraState = levelState.cameraRenderState;
        boolean hasCamera = cameraState != null && cameraState.pos != null;
        //? >=26.3 {
        // Nothing from an earlier frame is drawn, whatever this one decides.
        pendingDraw = null;
        pendingLevelUniforms = null;
        if (hasCamera) {
            LEVEL_MODEL_VIEW.set(cameraState.viewRotationMatrix);
        }
        //?}
        //? >=26.1.2 {
        Frustum frustum = hasCamera ? cameraState.cullFrustum : null;
        //?} else {
        /*// Before 26.1.2 the camera state does not carry it, so it is caught as vanilla prepares it.
		Frustum frustum = cullFrustum;
		*/ //?}
        drawFrame(hasCamera ? cameraState.pos : null, frustum);
    }

    //?} else {
    /*/^*
	 * Called by each loader's hooks once the opaque terrain is drawn. Before 1.21.11 there is no render state
	 * to read the camera, the cull frustum and the terrain's matrices from, so the hook hands them over.
	 ^/
	public static void draw(Vec3 camera, Frustum frustum, Matrix4f modelView, Matrix4f projection) {
		// The moment the hooks mark comes round again while a shader pack's shadow map is drawn, where the chunks are
		// drawn a second time: with Sodium on NeoForge, which fires the stage as it draws each of its layers. The
		// matrices are the sun's then, and what belongs in the shadow map is drawn by drawShadow; drawn here as well it
		// went onto the screen as the sun sees it, small and in the wrong place, wherever nothing culled it.
		if (IrisCompat.renderingShadows()) {
			return;
		}
		legacyModelView = modelView;
		legacyProjection = projection;
		drawFrame(camera, frustum);
	}
	*/ //?}

    /**
     * Whether leaves can be drawn solid now, as the game's Fast leaves are: with shaders that keep every pixel, which the
     * renderer has for the terrain's shaders, Sodium's and its own -- where one of those copies does not compile the
     * leaves are drawn cut out or with the mod's own instead, see opaqueOf and drawLegacy -- and for a shader pack whose
     * solid programs the mod could copy. Only under a pack whose solid programs it could not are Fast leaves left to the
     * game.
     */
    private static boolean canDrawOpaqueLeaves() {
        return !IrisCompat.shaderPackInUse() || IrisCompat.hasOpaquePrograms();
    }

    private static void drawFrame(Vec3 camera, Frustum frustum) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        // Decisions keep being applied while switched off, so the record is accurate when switched back on.
        GpuFoliageSplit.applyMeshDecisions();
        // Fast leaves are drawn solid by the game, and leaves the renderer waved cut out would show their holes: where it
        // cannot draw them solid too, switching between Fast and Fancy hands the leaves over, as the waving leaves setting
        // does. Where it can, they are meshed again to be drawn the other way.
        if (WavingWhitelist.followGameLeaves(canDrawOpaqueLeaves())) {
            foliageListChanged();
        }
        // Where the chunk mesher cannot be told to leave foliage out, the renderer stays out of the way too, or every
        // plant it drew would be drawn twice.
        if (
            !FoliageSettings.gpuRenderer() ||
            !SodiumBridge.canSplitChunkMeshes() ||
            VulkanBridge.keepsRendererOut()
        ) {
            if (active) {
                deactivate(minecraft);
            }
            return;
        }
        boolean shaderPack = IrisCompat.shaderPackInUse();
        //? iris {
        if (shaderPack) {
            setUpShaderPack();
        }
        //?}
        // Drawn through a shader pack only with the programs built for it. Where they could not be, the chunk mesh keeps
        // the foliage for as long as that pack is loaded, drawn by the pack as it draws any other block.
        if (shaderPack && !IrisCompat.programsReady()) {
            if (active) {
                deactivate(minecraft);
            }
            return;
        }
        int packGeneration = shaderPack ? IrisCompat.programGeneration() : 0;
        if (
            active &&
            (shaderPack != meshedForShaderPack ||
                packGeneration != meshedPackGeneration)
        ) {
            // Meshed for another pack or for none: handed back to the chunk mesh, and taken up again below.
            deactivate(minecraft);
        }
        if (!active) {
            // Chunks that loaded while switched off were never queued.
            active = true;
            reseedPending = true;
            meshedForShaderPack = shaderPack;
            meshedPackGeneration = packGeneration;
        }

        if (camera == null) {
            return;
        }
        updateNearArea(minecraft);
        //? <1.21.11 {
        /*followSubLevels(minecraft);
         */ //?}
        followWindEdge();
        followRain(minecraft);
        // Near sections whose build is on screen still holding their foliage are asked for again; see
        // GpuFoliageSplit.NEEDS_REBUILD for why only once the build is in place.
        GpuFoliageSplit.forEachRebuildDue(key ->
            rebuildChunkMesh(
                minecraft,
                SectionPos.x(key),
                SectionPos.y(key),
                SectionPos.z(key)
            )
        );
        // Sections whose foliage changed hands with a whitelist group, their chunk mesh now in place.
        GpuFoliageSplit.forEachRemeshDue(DIRTY::add);
        if (reseedPending && minecraft.player != null) {
            reseedPending = false;
            reseedLoadedChunks(minecraft, minecraft.level);
        }
        measureFrame();
        buildNearest(minecraft, camera, frustum);
        // One region is uploaded a frame, and each is drawn as last uploaded meanwhile, so its buffers and the layout used
        // to draw from them always agree.
        uploadDueRegion(camera);
        measureBurst();

        //? <26.1.2 {
        /*if (frustum == null) {
			return;
		}
		*/ //?}
        // Regions that fell out of range are let go before anything is drawn from them.
        var regions = REGIONS.entrySet().iterator();
        while (regions.hasNext()) {
            var entry = regions.next();
            if (!regionWithinRange(minecraft, entry.getKey())) {
                entry.getValue().free();
                regions.remove();
                DIRTY_REGIONS.remove(entry.getValue());
            }
        }
        boolean anything = collectDraws(frustum, SodiumBridge.renderer());

        //? >=1.21.11 {
        if (anything) {
            submitDraws(minecraft, camera, null, null, null, null);
        }
        //?} else {
        /*//? iris {
		if (meshedForShaderPack) {
			IrisCompat.inTerrainPhase(() -> {
				if (anything) {
					drawLegacy(minecraft, camera, DRAWN, null, null);
				}
				drawSubLevels(minecraft, camera, legacyModelView, legacyProjection, false);
			});
			return;
		}
		//?}
		if (anything) {
			drawLegacy(minecraft, camera, DRAWN, null, null);
		}
		drawSubLevels(minecraft, camera, legacyModelView, legacyProjection, false);
		*/ //?}
    }

    //? iris {
    /**
     * Builds the pipeline the renderer draws with while a shader pack is loaded, once: the pack's programs take the
     * place of its shaders, and it holds Iris's terrain format so they find every attribute they read.
     */
    private static void setUpShaderPack() {
        if (shaderPackSetUp) {
            return;
        }
        shaderPackSetUp = true;
        //? >=26.3 {
        // Compiled by the game first, from the mod's own shaders, and handed to Iris, which draws it with the pack's
        // terrain cutout program instead while the level is drawn. It holds Iris's terrain format, so that program finds
        // every attribute it reads, and draws into vanilla's pass, whose one colour target it has to match.
        shaderPackPipeline = RenderPipeline.builder(
            RenderPipelines.BLOCK_SNIPPET
        )
            .withLocation(
                Identifier.fromNamespaceAndPath(
                    AllThingsFoliage.MOD_ID,
                    "pipeline/foliage_shader_pack"
                )
            )
            .withVertexShader(
                Identifier.fromNamespaceAndPath(
                    AllThingsFoliage.MOD_ID,
                    "core/foliage"
                )
            )
            .withFragmentShader(
                Identifier.fromNamespaceAndPath(
                    AllThingsFoliage.MOD_ID,
                    "core/foliage"
                )
            )
            .withVertexBinding(0, IrisCompat.shaderPackFormat())
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withBindGroupLayout(SWAY_SETTINGS)
            .withBindGroupLayout(GpuFoliageInteraction.LAYOUT)
            .withShaderDefine("ALPHA_CUTOUT", 0.5F)
            .build();
        IrisCompat.setUp(
            shaderPackPipeline,
            null,
            List.of(SWAY_SETTINGS, GpuFoliageInteraction.LAYOUT),
            GpuFoliageRenderer::drawShadow
        );
        // The same without the alpha test, which Iris draws with the pack's solid terrain program: for leaves drawn solid.
        shaderPackOpaquePipeline = RenderPipeline.builder(
            RenderPipelines.BLOCK_SNIPPET
        )
            .withLocation(
                Identifier.fromNamespaceAndPath(
                    AllThingsFoliage.MOD_ID,
                    "pipeline/foliage_shader_pack_opaque"
                )
            )
            .withVertexShader(
                Identifier.fromNamespaceAndPath(
                    AllThingsFoliage.MOD_ID,
                    "core/foliage"
                )
            )
            .withFragmentShader(
                Identifier.fromNamespaceAndPath(
                    AllThingsFoliage.MOD_ID,
                    "core/foliage"
                )
            )
            .withVertexBinding(0, IrisCompat.shaderPackFormat())
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withBindGroupLayout(SWAY_SETTINGS)
            .withBindGroupLayout(GpuFoliageInteraction.LAYOUT)
            .build();
        IrisCompat.setUpOpaque(shaderPackOpaquePipeline);
        //?} elif >=26.2 {
        /*shaderPackPipeline = RenderPipeline.builder(RenderPipelines.BLOCK_SNIPPET)
				.withLocation(Identifier.fromNamespaceAndPath(AllThingsFoliage.MOD_ID, "pipeline/foliage_shader_pack"))
				.withVertexShader(Identifier.fromNamespaceAndPath(AllThingsFoliage.MOD_ID, "core/foliage"))
				.withFragmentShader(Identifier.fromNamespaceAndPath(AllThingsFoliage.MOD_ID, "core/foliage"))
				.withVertexBinding(0, IrisCompat.shaderPackFormat())
				.withBindGroupLayout(SWAY_SETTINGS)
				.withBindGroupLayout(GpuFoliageInteraction.LAYOUT)
				.withShaderDefine("ALPHA_CUTOUT", 0.5F)
				.build();
		// Minecraft tests depth reversed, and a shader pack's shadow map does not. Iris turns the test round in its shadow
		// pass only for the pipelines it knows, so the one drawn into the shadow map has it turned round already: the
		// block pipelines' test the way Iris turns it. With theirs, plants passed only behind what was already in the
		// shadow map and wrote their depth over it, wiping the shadows around them.
		shaderPackShadowPipeline = RenderPipeline.builder(RenderPipelines.BLOCK_SNIPPET)
				.withLocation(Identifier.fromNamespaceAndPath(AllThingsFoliage.MOD_ID, "pipeline/foliage_shader_pack_shadow"))
				.withVertexShader(Identifier.fromNamespaceAndPath(AllThingsFoliage.MOD_ID, "core/foliage"))
				.withFragmentShader(Identifier.fromNamespaceAndPath(AllThingsFoliage.MOD_ID, "core/foliage"))
				.withVertexBinding(0, IrisCompat.shaderPackFormat())
				.withBindGroupLayout(SWAY_SETTINGS)
				.withBindGroupLayout(GpuFoliageInteraction.LAYOUT)
				.withShaderDefine("ALPHA_CUTOUT", 0.5F)
				.withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
				.build();
		IrisCompat.setUp(shaderPackPipeline, shaderPackShadowPipeline,
				List.of(SWAY_SETTINGS, GpuFoliageInteraction.LAYOUT), GpuFoliageRenderer::drawShadow);
		// The same two without the alpha test, drawn with the mod's copies of the pack's solid programs: for leaves drawn
		// solid.
		shaderPackOpaquePipeline = RenderPipeline.builder(RenderPipelines.BLOCK_SNIPPET)
				.withLocation(Identifier.fromNamespaceAndPath(AllThingsFoliage.MOD_ID, "pipeline/foliage_shader_pack_opaque"))
				.withVertexShader(Identifier.fromNamespaceAndPath(AllThingsFoliage.MOD_ID, "core/foliage"))
				.withFragmentShader(Identifier.fromNamespaceAndPath(AllThingsFoliage.MOD_ID, "core/foliage"))
				.withVertexBinding(0, IrisCompat.shaderPackFormat())
				.withBindGroupLayout(SWAY_SETTINGS)
				.withBindGroupLayout(GpuFoliageInteraction.LAYOUT)
				.build();
		shaderPackShadowOpaquePipeline = RenderPipeline.builder(RenderPipelines.BLOCK_SNIPPET)
				.withLocation(Identifier.fromNamespaceAndPath(AllThingsFoliage.MOD_ID, "pipeline/foliage_shader_pack_shadow_opaque"))
				.withVertexShader(Identifier.fromNamespaceAndPath(AllThingsFoliage.MOD_ID, "core/foliage"))
				.withFragmentShader(Identifier.fromNamespaceAndPath(AllThingsFoliage.MOD_ID, "core/foliage"))
				.withVertexBinding(0, IrisCompat.shaderPackFormat())
				.withBindGroupLayout(SWAY_SETTINGS)
				.withBindGroupLayout(GpuFoliageInteraction.LAYOUT)
				.withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
				.build();
		IrisCompat.setUpOpaque(shaderPackOpaquePipeline, shaderPackShadowOpaquePipeline);
		*/ //?} elif >=1.21.11 {
        /*// Before 26.2 depth is not reversed, so the shadow map takes the same test as everything else. The shadow pipeline is
		// a pipeline of its own only so the pack's shadow program is the one it draws with.
		shaderPackPipeline = blockPipeline("pipeline/foliage_shader_pack", IrisCompat.shaderPackFormat());
		shaderPackShadowPipeline = blockPipeline("pipeline/foliage_shader_pack_shadow", IrisCompat.shaderPackFormat());
		IrisCompat.setUp(shaderPackPipeline, shaderPackShadowPipeline,
				List.of(new RenderPipeline.UniformDescription(SWAY_SETTINGS_UNIFORM, UniformType.UNIFORM_BUFFER),
						new RenderPipeline.UniformDescription(GpuFoliageInteraction.UNIFORM, UniformType.UNIFORM_BUFFER)),
				GpuFoliageRenderer::drawShadow);
		// The same two without the alpha test, drawn with the mod's copies of the pack's solid programs: for leaves drawn
		// solid.
		shaderPackOpaquePipeline = blockPipeline("pipeline/foliage_shader_pack_opaque", IrisCompat.shaderPackFormat(), false);
		shaderPackShadowOpaquePipeline = blockPipeline("pipeline/foliage_shader_pack_shadow_opaque",
				IrisCompat.shaderPackFormat(), false);
		IrisCompat.setUpOpaque(shaderPackOpaquePipeline, shaderPackShadowOpaquePipeline);
		*/ //?} else {
        /*// Before 1.21.11 the renderer draws with the pack's programs themselves, so only the shadow draw is handed over.
		IrisCompat.setUp(GpuFoliageRenderer::drawShadow);
		*/ //?}
    }

    //?}

    /** Holds the sun's projection for drawing into a shadow map: a shader pack's, or Polytone's. */
    //? >=26.1.2 {
    private static net.minecraft.client.renderer.ProjectionMatrixBuffer shadowProjectionUniform;

    private static net.minecraft.client.renderer.ProjectionMatrixBuffer shadowProjectionBuffer() {
        if (shadowProjectionUniform == null) {
            shadowProjectionUniform =
                new net.minecraft.client.renderer.ProjectionMatrixBuffer(
                    "MC2 foliage shadow"
                );
        }
        return shadowProjectionUniform;
    }

    //?} elif >=1.21.11 {
    /*// Before 26.1.2 the buffer that takes any matrix is the one named for perspective projections.
	private static net.minecraft.client.renderer.PerspectiveProjectionMatrixBuffer shadowProjectionUniform;

	private static net.minecraft.client.renderer.PerspectiveProjectionMatrixBuffer shadowProjectionBuffer() {
		if (shadowProjectionUniform == null) {
			shadowProjectionUniform = new net.minecraft.client.renderer.PerspectiveProjectionMatrixBuffer("MC2 foliage shadow");
		}
		return shadowProjectionUniform;
	}
	*/ //?}

    /**
     * Draws the foliage into Polytone's shadow map, called once Polytone has drawn the terrain into it, so plants cast
     * shadows that sway with them onto the post effects that read it. Polytone draws the terrain with the chunks' own
     * shaders, so the foliage is drawn with the shaders it is drawn with on screen, only seen from the sun: shadowMatrix
     * is Polytone's, its projection times its view, and the targets are its shadow map's. While a shader pack is loaded
     * the foliage is meshed for the pack's programs, and the pack casts its own shadows.
     * <p>
     * Every section near the player the chunk mesh left its foliage out of is drawn, as for a shader pack's shadow map:
     * the camera's frustum does not say what casts a shadow into view.
     */
    //? >=1.21.11 {
    public static void drawPolytoneShadow(
        Matrix4f shadowMatrix,
        GpuTextureView color,
        GpuTextureView depth
    ) {
        //?} else {
        /*// Before 1.21.11 Polytone leaves its shadow map bound while it draws into it, so the foliage goes wherever it is.
	public static void drawPolytoneShadow(Matrix4f shadowMatrix) {
	*/ //?}
        Minecraft minecraft = Minecraft.getInstance();
        if (!active || meshedForShaderPack || minecraft.level == null) {
            return;
        }
        //? >=26.2 {
        Vec3 camera = minecraft.gameRenderer.mainCamera().position();
        //?} elif >=1.21.11 {
        /*Vec3 camera = minecraft.gameRenderer.getMainCamera().position();
         */ //?} else {
        /*Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
         */ //?}
        if (collectDraws(null, null)) {
            // The whole of the sun's transform is in the projection, so the view is left as none at all.
            //? >=1.21.11 {
            submitDraws(
                minecraft,
                camera,
                new Matrix4f(),
                shadowMatrix,
                color,
                depth
            );
            //?} else {
            /*drawLegacy(minecraft, camera, DRAWN, new Matrix4f(), shadowMatrix);
             */ //?}
        }
    }

    /** Set while the foliage is drawn into Beryl's shadow map, which only Beryl's own shadow shaders may draw into. */
    private static boolean berylShadowPass;

    /**
     * Draws the foliage into Beryl's shadow map, called once Beryl has drawn its terrain into it and before it draws the
     * entities, so plants cast shadows that sway with them. Beryl draws its shadow map from its own chunk meshes, which
     * the foliage was left out of. The targets are the ones Beryl has the game draw the entities' shadows into, and the
     * pipeline is Beryl's own for the shadow map, put under the mod's by {@link BerylBridge}: it places each vertex by
     * the light's matrix, which Beryl hands it, so no matrix is handed over here.
     * <p>
     * Every section near the player the chunk mesh left its foliage out of is drawn, as for a shader pack's shadow map:
     * the camera's frustum does not say what casts a shadow into view.
     */
    public static void drawBerylShadow() {
        //? >=1.21.11 && <26.2 {
        /*Minecraft minecraft = Minecraft.getInstance();
		GpuTextureView color = RenderSystem.outputColorTextureOverride;
		GpuTextureView depth = RenderSystem.outputDepthTextureOverride;
		if (!active || meshedForShaderPack || minecraft.level == null || color == null || depth == null) {
			return;
		}
		Vec3 camera = minecraft.gameRenderer.getMainCamera().position();
		if (collectDraws(null, null)) {
			berylShadowPass = true;
			try {
				submitDraws(minecraft, camera, new Matrix4f(), new Matrix4f(), color, depth);
			} finally {
				berylShadowPass = false;
			}
		}
		*/
        //?} elif >=1.21.1 && <1.21.11 {
        /*// Before 1.21.11 the foliage is drawn through VulkanMod itself, into whatever Beryl has bound: its shadow map.
		Minecraft minecraft = Minecraft.getInstance();
		if (!active || meshedForShaderPack || minecraft.level == null || !VulkanBridge.drawsThroughItself()) {
			return;
		}
		Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
		if (collectDraws(null, null)) {
			drawLegacyThroughVulkanMod(minecraft, camera, DRAWN, null, null);
		}
		*/
        //?}
    }

    //? iris {
    /**
     * Draws the foliage into a shader pack's shadow map, called by Iris in the middle of its shadow pass, so plants cast
     * shadows that sway with them. Every section near the player the chunk mesh left its foliage out of is drawn: the
     * shadow's view is not the camera's, so neither the camera's frustum nor Sodium's view of what is visible applies.
     */
    private static void drawShadow(
        Matrix4f modelView,
        Matrix4f projection,
        double cameraX,
        double cameraY,
        double cameraZ
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (
            !active ||
            !meshedForShaderPack ||
            !IrisCompat.hasShadowProgram() ||
            minecraft.level == null
        ) {
            return;
        }
        boolean anything = collectDraws(null, null);
        //? >=26.3 {
        if (anything) {
            drawIntoShadowMap(
                minecraft,
                new Vec3(cameraX, cameraY, cameraZ),
                modelView,
                projection
            );
        }
        //?} elif >=1.21.11 {
        /*if (anything) {
			submitDraws(minecraft, new Vec3(cameraX, cameraY, cameraZ), modelView, projection, null, null);
		}
		*/ //?} else {
        /*Vec3 camera = new Vec3(cameraX, cameraY, cameraZ);
		IrisCompat.inTerrainPhase(() -> {
			if (anything) {
				drawLegacy(minecraft, camera, DRAWN, modelView, projection);
			}
			// A sub-level's foliage casts its shadow from where the sub-level is, as it is drawn there.
			drawSubLevels(minecraft, camera, modelView, projection, true);
		});
		*/ //?}
    }

    //?}

    /**
     * Works out this frame's draw calls into {@link #DRAWN} and {@link #draws}: the sections the chunk mesh left their
     * foliage out of, inside the frustum and, with Sodium, among what it sees. Returns whether there is anything to draw.
     */
    private static boolean collectDraws(Frustum frustum, Object sodium) {
        return collectDraws(frustum, sodium, null);
    }

    /**
     * The same for the regions of one sub-level's plot, or with none for the world's own: a plot's are drawn apart,
     * each sub-level's where that sub-level is, so they are never among the world's.
     */
    private static boolean collectDraws(
        Frustum frustum,
        Object sodium,
        Object subLevel
    ) {
        List<Region> drawn = DRAWN;
        drawn.clear();
        drawsSize = 0;
        opaqueDrawsSize = 0;
        for (Region region : REGIONS.values()) {
            if (region.subLevel != subLevel) {
                continue;
            }
            if (frustum != null && !frustum.isVisible(region.bounds)) {
                continue;
            }
            // A region wholly inside the frustum needs no per-section frustum test. Its unpadded box is
            // used, so a section whose padding pokes outside is simply drawn -- never wrongly dropped.
            //? >=1.21.11 {
            Frustum sectionFrustum =
                frustum != null &&
                frustum.cubeInFrustum(region.blockBounds) ==
                    FrustumIntersection.INSIDE
                    ? null
                    : frustum;
            //?} else {
            /*// Vanilla's frustum can only be asked about boxes before 1.21.11, so every section is tested.
			Frustum sectionFrustum = frustum;
			*/ //?}

            // Walk the sections in buffer order, extending a run while they stay visible, so a region
            // that is fully in view costs one draw call however many sections it holds.
            // The solid leaves are a stretch of their own after the cut out part, walked alongside it as a run of its own.
            int regionIndex = drawn.size();
            int drawsBefore = drawsSize;
            int opaqueDrawsBefore = opaqueDrawsSize;
            int runStart = -1;
            int runEnd = -1;
            int opaqueStart = -1;
            int opaqueEnd = -1;
            for (int i = 0; i < region.occupiedCount; i++) {
                int slot = region.occupied[i];
                // Drawn as uploaded: a section rebuilt since is not in the buffer yet.
                Section section = region.uploadedSections[slot];
                if (!isVisible(section, sectionFrustum, sodium)) {
                    if (runStart >= 0) {
                        addDraw(regionIndex, runStart, runEnd - runStart);
                        runStart = -1;
                    }
                    if (opaqueStart >= 0) {
                        addOpaqueDraw(
                            regionIndex,
                            opaqueStart,
                            opaqueEnd - opaqueStart
                        );
                        opaqueStart = -1;
                    }
                    continue;
                }
                if (section.animatedSprites != null) {
                    SodiumBridge.markActive(section.animatedSprites);
                }
                if (runStart < 0) {
                    runStart = region.firstVertex[slot];
                }
                runEnd = region.firstVertex[slot] + section.cutoutVertexCount();
                if (opaqueStart < 0) {
                    opaqueStart = region.firstOpaqueVertex[slot];
                }
                opaqueEnd =
                    region.firstOpaqueVertex[slot] + section.opaqueVertexCount;
            }
            if (runStart >= 0) {
                addDraw(regionIndex, runStart, runEnd - runStart);
            }
            if (opaqueStart >= 0) {
                addOpaqueDraw(
                    regionIndex,
                    opaqueStart,
                    opaqueEnd - opaqueStart
                );
            }
            if (
                drawsSize > drawsBefore || opaqueDrawsSize > opaqueDrawsBefore
            ) {
                drawn.add(region);
            }
        }

        return !drawn.isEmpty();
    }

    //? >=1.21.11 {
    //? >=26.3 {
    /** The pipelines foliage is drawn into the level with, all of one colour format. */
    private record LevelPipelines(
        RenderPipeline own,
        RenderPipeline ownOpaque,
        RenderPipeline sodium,
        RenderPipeline sodiumOpaque,
        RenderPipeline terrain,
        RenderPipeline terrainOpaque
    ) {}

    private static final LevelPipelines LEVEL_PIPELINES = new LevelPipelines(
        PIPELINE,
        OPAQUE_PIPELINE,
        SODIUM_PIPELINE,
        SODIUM_OPAQUE_PIPELINE,
        TERRAIN_PIPELINE,
        TERRAIN_OPAQUE_PIPELINE
    );
    private static final Map<GpuFormat, LevelPipelines> LEVEL_PIPELINES_FOR =
        new java.util.EnumMap<>(GpuFormat.class);
    private static boolean pipelineRefused;

    /**
     * The pipelines for the format the level is drawn in. A pass refuses a pipeline whose colour format is not its
     * target's, and the main target is not always vanilla's eight bits: cbbg allocates it with sixteen, as floats,
     * while no shader pack is loaded. It gives the pipelines the game compiled a twin of that format as they are set,
     * but the copies of Sodium's shaders and the terrain's are compiled by the mod and it never sees them; so the mod
     * builds its own for whatever the main target holds.
     */
    private static LevelPipelines levelPipelines(Minecraft minecraft) {
        com.mojang.renderpearl.api.textures.GpuTexture color =
            minecraft.gameRenderer.mainRenderTarget().getColorTexture();
        GpuFormat format =
            color == null ? GpuFormat.RGBA8_UNORM : color.getFormat();
        if (format == GpuFormat.RGBA8_UNORM) {
            return LEVEL_PIPELINES;
        }
        return LEVEL_PIPELINES_FOR.computeIfAbsent(format, wanted ->
            new LevelPipelines(
                modernPipeline("pipeline/foliage", true, wanted),
                modernPipeline("pipeline/foliage_opaque", false, wanted),
                sodiumPipeline("pipeline/foliage_sodium", wanted),
                sodiumPipeline("pipeline/foliage_sodium_opaque", wanted),
                terrainPipeline("pipeline/foliage_terrain", true, wanted),
                terrainPipeline(
                    "pipeline/foliage_terrain_opaque",
                    false,
                    wanted
                )
            )
        );
    }

    /**
     * Prepares what {@link #collectDraws} worked out, for {@link #drawIntoLevelPass} to draw.
     * <p>
     * 26.3 draws the opaque world inside one render pass vanilla opens itself, and while it is open no buffer may be
     * written to, no other pass opened and no pipeline compiled. So the foliage is drawn in two halves: everything that
     * writes happens here, as the level's extraction ends and before anything is drawn, and what is left -- binding and
     * drawing -- waits for vanilla's pass. Only the screen is drawn on 26.3 so far; the shadow arguments are not used.
     */
    private static void submitDraws(
        Minecraft minecraft,
        Vec3 camera,
        Matrix4f shadowModelView,
        Matrix4f shadowProjection,
        GpuTextureView shadowColor,
        GpuTextureView shadowDepth
    ) {
        // With the shaders the chunks are drawn with, so the foliage is lit and coloured exactly as the terrain around it:
        // a copy of Sodium's while Sodium draws them, the terrain's own -- a resource pack's included -- otherwise. Where
        // those do not compile, the mod's own. While Sodium draws the chunks the terrain's shaders are not what the chunks
        // around the foliage are drawn with, so the mod keeps to its own rather than follow them.
        // While a shader pack is loaded, with the pack's own program, which Iris puts in place of the pipeline's as the
        // level is drawn; the pipeline is compiled here all the same, since Iris builds on what the game compiled.
        boolean shaderPack = meshedForShaderPack;
        boolean sodium = !shaderPack && SodiumBridge.drawsChunks();
        LevelPipelines level = levelPipelines(minecraft);
        CompiledRenderPipeline sodiumPipeline = sodium
            ? SodiumFoliageShader.compiled(level.sodium(), true)
            : null;
        CompiledRenderPipeline terrainPipeline =
            shaderPack || sodium
                ? null
                : TerrainFoliageShader.compiled(level.terrain());
        boolean sodiumShaders = sodiumPipeline != null;
        boolean terrainShaders = terrainPipeline != null;
        CompiledRenderPipeline pipeline = shaderPack
            ? compiledPipeline(shaderPackPipeline)
            : sodiumShaders
              ? sodiumPipeline
              : terrainShaders
                ? terrainPipeline
                : compiledPipeline(level.own());
        if (pipeline == null) {
            return;
        }
        // Leaves drawn solid, as the game's Fast leaves are, go through the same shaders keeping every pixel, as vanilla's
        // solid terrain does and Sodium's solid pass; through a shader pack, with the pack's solid terrain program, in a
        // pipeline of its own that is compiled here as the cutout one is. Where that copy does not compile they are drawn
        // cut out rather than not at all.
        CompiledRenderPipeline solid =
            opaqueDrawsSize == 0
                ? null
                : shaderPack
                  ? compiledPipeline(shaderPackOpaquePipeline)
                  : sodiumShaders
                    ? SodiumFoliageShader.compiled(level.sodiumOpaque(), false)
                    : terrainShaders
                      ? TerrainFoliageShader.compiled(level.terrainOpaque())
                      : compiledPipeline(level.ownOpaque());
        CompiledRenderPipeline opaquePipeline =
            solid != null ? solid : pipeline;
        Region[] regions = DRAWN.toArray(new Region[0]);
        AbstractTexture atlas = minecraft
            .getTextureManager()
            .getTexture(TextureAtlas.LOCATION_BLOCKS);
        // Where each region is. The terrain's shaders read it from the section's block, and the terrain's own block holds
        // the view and the atlas's size, which their fragment shader samples the atlas by. The others read it from a
        // transform: every region's offset from the camera, in one mapping of vanilla's transform buffer, which stays
        // valid until the frame ends. Both hold the view, so both are written as the level starts to be drawn, once the
        // view is final -- see writeLevelUniforms -- and read by the draw from these.
        GpuBufferSlice[] regionBlocks = new GpuBufferSlice[regions.length];
        GpuBufferSlice[] terrainBlock = new GpuBufferSlice[1];
        pendingLevelUniforms = () -> {
            if (terrainShaders) {
                terrainBlock[0] =
                    RenderSystem.getDynamicUniforms().writeTerrainTransform(
                        LEVEL_MODEL_VIEW,
                        atlas.getTextureView().getWidth(0),
                        atlas.getTextureView().getHeight(0)
                    );
                for (int i = 0; i < regions.length; i++) {
                    regionBlocks[i] = regions[i].chunkSection().slice();
                }
                return;
            }
            Vector4f noModulation = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
            Matrix4f noTextureTransform = new Matrix4f();
            DynamicGpuData.Transform[] transforms =
                new DynamicGpuData.Transform[regions.length];
            for (int i = 0; i < regions.length; i++) {
                BlockPos origin = regions[i].origin;
                transforms[i] = new DynamicGpuData.Transform(
                    LEVEL_MODEL_VIEW,
                    noModulation,
                    new Vector3f(
                        (float) (origin.getX() - camera.x),
                        (float) (origin.getY() - camera.y),
                        (float) (origin.getZ() - camera.z)
                    ),
                    noTextureTransform
                );
            }
            System.arraycopy(
                RenderSystem.getDynamicUniforms().writeTransforms(transforms),
                0,
                regionBlocks,
                0,
                regions.length
            );
        };
        String regionUniform = terrainShaders
            ? "ChunkSection"
            : "DynamicTransforms";
        int[] drawList = Arrays.copyOf(draws, drawsSize);
        int[] opaqueList = Arrays.copyOf(opaqueDraws, opaqueDrawsSize);

        // The shared index buffer is replaced, the old one closed, whenever it has to grow, so it may not grow inside the
        // pass the terrain was just drawn from it in. It is asked for here, and vanilla grows every shared index buffer once,
        // before its pass opens.
        int longestDraw = 0;
        for (int i = 2; i < drawList.length; i += 3) {
            longestDraw = Math.max(longestDraw, drawList[i]);
        }
        for (int i = 2; i < opaqueList.length; i += 3) {
            longestDraw = Math.max(longestDraw, opaqueList[i]);
        }
        int indicesNeeded = indexCountFor(longestDraw);
        RenderSystem.AutoStorageIndexBuffer indices =
            RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        indices.requestIndexCount(indicesNeeded);
        GpuBuffer settings = swaySettings(camera);
        GpuBuffer interaction = GpuFoliageInteraction.upload(camera);

        java.util.function.Consumer<RenderPass> draw = pass -> {
            GpuBuffer indexBuffer = indices.getBuffer();
            if (
                indexBuffer == null ||
                !indices.hasStorage(indicesNeeded) ||
                (regions.length > 0 && regionBlocks[0] == null)
            ) {
                // Not drawn where the level's uniforms were not written this frame.
                return;
            }
            // The solid leaves first, as vanilla draws its solid terrain before its cut out terrain, then the rest; each
            // with everything bound again for its pipeline.
            for (int kind = 0; kind < 2; kind++) {
                int[] list = kind == 0 ? opaqueList : drawList;
                if (list.length == 0) {
                    continue;
                }
                // Asked for again here, as the level is drawn, which is when Iris hands back the pack's program in its
                // place; the mod's swaying copy of that program then takes its place in turn.
                boolean drawnSolid = kind == 0 && solid != null;
                try {
                    if (shaderPack) {
                        IrisCompat.setTerrainPhase(drawnSolid);
                        pass.setPipeline(
                            IrisCompat.withFoliageProgram(
                                RenderSystem.getCompiledPipeline(
                                    drawnSolid
                                        ? shaderPackOpaquePipeline
                                        : shaderPackPipeline
                                ),
                                drawnSolid
                            )
                        );
                    } else {
                        pass.setPipeline(kind == 0 ? opaquePipeline : pipeline);
                    }
                } catch (IllegalStateException e) {
                    // The pass holds targets the pipeline was not built for -- another mod's, or a shader pack's in the
                    // frame it is switched in. The foliage is left out of the frame rather than the game ended.
                    if (!pipelineRefused) {
                        pipelineRefused = true;
                        AllThingsFoliage.LOGGER.error(
                            "The level's pass refused the GPU foliage pipeline; the foliage it " +
                                "holds is not drawn while it does",
                            e
                        );
                    }
                    return;
                }
                // Projection, fog and the globals are bound by vanilla as it opens the pass. The chunk shaders pick mip
                // levels themselves, so they sample the atlas as the chunk mesh does: smoothly, between mip levels. The
                // mod's own shader reads it as the atlas is set to be read.
                if (terrainBlock[0] != null) {
                    pass.setUniform("TerrainUniform", terrainBlock[0]);
                }
                pass.setUniform(
                    sodiumShaders
                        ? SodiumFoliageShader.BLOCK_TEXTURE
                        : "Sampler0",
                    atlas.getTextureView(),
                    sodiumShaders || terrainShaders
                        ? RenderSystem.getSamplerCache().getClampToEdge(
                              FilterMode.LINEAR,
                              true
                          )
                        : atlas.getSampler()
                );
                pass.setUniform(
                    sodiumShaders
                        ? SodiumFoliageShader.LIGHT_TEXTURE
                        : "Sampler2",
                    minecraft.gameRenderer.lightmap(),
                    RenderSystem.getSamplerCache().getClampToEdge(
                        FilterMode.LINEAR
                    )
                );
                pass.setUniform(SWAY_SETTINGS_UNIFORM, settings);
                pass.setUniform(GpuFoliageInteraction.UNIFORM, interaction);
                pass.setIndexBuffer(indexBuffer, indices.type());
                if (shaderPack) {
                    // The pack's program binds only the blocks Iris knows, so the sway's are bound where the copy reads them.
                    IrisCompat.bindSwayBlocks(settings, interaction);
                }
                int boundRegion = -1;
                for (int i = 0; i < list.length; i += 3) {
                    int regionIndex = list[i];
                    if (regionIndex != boundRegion) {
                        pass.setUniform(
                            regionUniform,
                            regionBlocks[regionIndex]
                        );
                        pass.setVertexBuffer(
                            0,
                            regions[regionIndex].vertices.slice()
                        );
                        boundRegion = regionIndex;
                    }
                    // The sequential index buffer counts from zero, and the base vertex moves it to where this run of
                    // sections starts in the region's buffer.
                    pass.drawIndexed(
                        indexCountFor(list[i + 2]),
                        1,
                        0,
                        list[i + 1],
                        0
                    );
                }
            }
        };
        // Drawn as the pack's cutout terrain, which some packs read to tell what they are drawing apart.
        pendingDraw = shaderPack
            ? pass -> IrisCompat.inTerrainPhase(() -> draw.accept(pass))
            : draw;
    }

    /** This frame's foliage, prepared and waiting for vanilla's pass, or null when there is nothing to draw. */
    private static java.util.function.Consumer<RenderPass> pendingDraw;
    /** Writes this frame's uniforms that hold the view, once it is final; see {@link #writeLevelUniforms}. */
    private static Runnable pendingLevelUniforms;

    /**
     * Writes the uniforms of this frame's foliage that hold the view -- each region's transform, or the terrain's own block
     * -- called by LevelPassMixin as vanilla starts to draw the level, before any pass is open. The view is final only
     * then: while a shader pack is loaded Iris moves the camera's bobbing out of the projection and into the view just
     * before the level is drawn, and foliage drawn with the view as it was extracted slid about with every step.
     */
    public static void writeLevelUniforms(
        Matrix4f view,
        GpuBufferSlice terrainFog
    ) {
        levelFog = terrainFog;
        Runnable write = pendingLevelUniforms;
        pendingLevelUniforms = null;
        if (write != null) {
            LEVEL_MODEL_VIEW.set(view);
            write.run();
        }
    }

    /**
     * The fog vanilla draws this frame's terrain with. It sets it only inside its main pass, so the shadow draw, which
     * comes before, binds it itself; see drawIntoShadowMap.
     */
    private static GpuBufferSlice levelFog;
    /** The matrix vanilla draws the level with this frame: the camera's rotation, which it pushes as it starts. */
    private static final Matrix4f LEVEL_MODEL_VIEW = new Matrix4f();
    /** Vanilla's own terrain pipeline as compiled when each of the mod's last failed to; see {@link #compiledPipeline}. */
    private static final Map<
        RenderPipeline,
        CompiledRenderPipeline
    > PIPELINES_FAILED_WITH = new java.util.IdentityHashMap<>();

    /**
     * Draws the foliage prepared this frame into vanilla's pass, called by LevelPassMixin once the opaque terrain is in
     * it. Each frame's foliage is drawn once, and not at all where nothing was prepared.
     */
    public static void drawIntoLevelPass(RenderPass pass) {
        java.util.function.Consumer<RenderPass> draw = pendingDraw;
        pendingDraw = null;
        if (draw != null) {
            draw.accept(pass);
        }
    }

    //? iris {
    /**
     * Draws the foliage {@link #collectDraws} found into a shader pack's shadow map, all at once and in a pass of the
     * mod's own: Iris runs its shadow callbacks with no pass open, before vanilla opens its main one. Iris turns the
     * depth test round, turns culling off and sizes the viewport for every pipeline drawn while it draws shadows, and
     * the pack's shadow program binds the map as it is set up, so the pass is opened on the main target as Iris opens
     * its own. What was prepared for vanilla's pass is left alone: it holds copies of its own.
     */
    private static void drawIntoShadowMap(
        Minecraft minecraft,
        Vec3 camera,
        Matrix4f modelView,
        Matrix4f projection
    ) {
        // The pushes are the ones written for the screen this frame: writing them again would move the springs twice.
        GpuBuffer interaction = GpuFoliageInteraction.uploaded();
        GpuBufferSlice fog = levelFog;
        if (
            interaction == null ||
            fog == null ||
            compiledPipeline(shaderPackPipeline) == null
        ) {
            return;
        }
        Region[] regions = DRAWN.toArray(new Region[0]);
        for (Region region : regions) {
            if (region.vertices == null) {
                // Let go of since the frame was prepared; the next shadow pass draws from what is there.
                return;
            }
        }
        // Every region's offset from the camera, seen from the sun.
        Vector4f noModulation = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
        Matrix4f noTextureTransform = new Matrix4f();
        DynamicGpuData.Transform[] transforms =
            new DynamicGpuData.Transform[regions.length];
        for (int i = 0; i < regions.length; i++) {
            BlockPos origin = regions[i].origin;
            transforms[i] = new DynamicGpuData.Transform(
                modelView,
                noModulation,
                new Vector3f(
                    (float) (origin.getX() - camera.x),
                    (float) (origin.getY() - camera.y),
                    (float) (origin.getZ() - camera.z)
                ),
                noTextureTransform
            );
        }
        GpuBufferSlice[] regionBlocks =
            RenderSystem.getDynamicUniforms().writeTransforms(transforms);
        int[] drawList = Arrays.copyOf(draws, drawsSize);
        int[] opaqueList = Arrays.copyOf(opaqueDraws, opaqueDrawsSize);
        // Solid leaves cast a solid shadow, through the pack's solid shadow program; cut out where there is none.
        boolean solidShadow =
            opaqueList.length > 0 &&
            IrisCompat.hasOpaquePrograms() &&
            compiledPipeline(shaderPackOpaquePipeline) != null;
        int longestDraw = 0;
        for (int i = 2; i < drawList.length; i += 3) {
            longestDraw = Math.max(longestDraw, drawList[i]);
        }
        for (int i = 2; i < opaqueList.length; i += 3) {
            longestDraw = Math.max(longestDraw, opaqueList[i]);
        }
        // No pass is open, so the shared index buffer may grow here; vanilla's pass asks for it again as it draws.
        RenderSystem.AutoStorageIndexBuffer indices =
            RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        GpuBuffer indexBuffer = indices.getBuffer(indexCountFor(longestDraw));
        GpuBuffer settings = swaySettings(camera);
        // The sun's projection, as Iris hands it over, as on 26.2.
        GpuBufferSlice sunProjection = shadowProjectionBuffer().getBuffer(
            projection
        );
        AbstractTexture atlas = minecraft
            .getTextureManager()
            .getTexture(TextureAtlas.LOCATION_BLOCKS);
        RenderTarget target = minecraft.gameRenderer.mainRenderTarget();
        try (
            RenderPass pass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(
                    () -> "MC2 foliage shadow",
                    target.getColorTextureView(),
                    Optional.empty(),
                    target.getDepthTextureView(),
                    OptionalDouble.empty()
                )
        ) {
            // As vanilla binds them as it opens its main pass; the projection is the sun's, and the fog -- set by vanilla
            // only inside that pass -- this frame's terrain fog.
            RenderSystem.bindDefaultUniforms(pass);
            // The solid leaves, then the rest, each with everything bound again for its pipeline.
            for (int kind = 0; kind < 2; kind++) {
                int[] list = kind == 0 ? opaqueList : drawList;
                if (list.length == 0) {
                    continue;
                }
                boolean drawnSolid = kind == 0 && solidShadow;
                // Iris hands back the pack's shadow program in the pipeline here, and the mod's swaying copy takes its
                // place.
                pass.setPipeline(
                    IrisCompat.withFoliageProgram(
                        RenderSystem.getCompiledPipeline(
                            drawnSolid
                                ? shaderPackOpaquePipeline
                                : shaderPackPipeline
                        ),
                        drawnSolid
                    )
                );
                pass.setUniform("Projection", sunProjection);
                pass.setUniform("Fog", fog);
                pass.setUniform(
                    "Sampler0",
                    atlas.getTextureView(),
                    atlas.getSampler()
                );
                pass.setUniform(
                    "Sampler2",
                    minecraft.gameRenderer.lightmap(),
                    RenderSystem.getSamplerCache().getClampToEdge(
                        FilterMode.LINEAR
                    )
                );
                pass.setUniform(SWAY_SETTINGS_UNIFORM, settings);
                pass.setUniform(GpuFoliageInteraction.UNIFORM, interaction);
                pass.setIndexBuffer(indexBuffer, indices.type());
                IrisCompat.bindSwayBlocks(settings, interaction);
                int boundRegion = -1;
                for (int i = 0; i < list.length; i += 3) {
                    int regionIndex = list[i];
                    if (regionIndex != boundRegion) {
                        pass.setUniform(
                            "DynamicTransforms",
                            regionBlocks[regionIndex]
                        );
                        pass.setVertexBuffer(
                            0,
                            regions[regionIndex].vertices.slice()
                        );
                        boundRegion = regionIndex;
                    }
                    pass.drawIndexed(
                        indexCountFor(list[i + 2]),
                        1,
                        0,
                        list[i + 1],
                        0
                    );
                }
            }
        }
    }

    //?}

    /**
     * The mod's pipeline as the game has compiled it, or null where it could not be.
     * <p>
     * The game compiles a pipeline the first time it is asked for, which is why this is asked while preparing, with no
     * pass open. It does not remember a pipeline that failed, and would compile it again on every request, so a failure
     * is remembered here until the game reloads its shaders -- which shows as its own terrain pipeline coming back as a
     * different compiled one, since a reload compiles everything afresh and closes what it had.
     */
    private static CompiledRenderPipeline compiledPipeline(
        RenderPipeline requested
    ) {
        CompiledRenderPipeline terrain =
            RenderSystem.getCompiledPipelineNullable(
                RenderPipelines.SOLID_TERRAIN
            );
        if (
            PIPELINES_FAILED_WITH.containsKey(requested) &&
            PIPELINES_FAILED_WITH.get(requested) == terrain
        ) {
            return null;
        }
        CompiledRenderPipeline compiled;
        try {
            compiled = RenderSystem.getCompiledPipelineNullable(requested);
        } catch (RuntimeException e) {
            // A shader that does not compile is thrown rather than handed back as null.
            AllThingsFoliage.LOGGER.error(
                "The GPU foliage pipeline did not compile; the foliage it holds is not drawn",
                e
            );
            compiled = null;
        }
        if (compiled == null) {
            PIPELINES_FAILED_WITH.put(requested, terrain);
        }
        return compiled;
    }

    //?} else {
    /*/^*
	 * The pipeline foliage is drawn with while no shader pack is loaded: the shaders the chunks are drawn with --
	 * Sodium's while Sodium draws them, the terrain's otherwise -- where they compile, and the mod's own where they don't.
	 ^/
	private static RenderPipeline ownPipeline() {
		if (VulkanBridge.drawsChunks()) {
			// VulkanMod draws the chunks with shaders of its own, which are not the terrain's.
			return PIPELINE;
		}
		if (SodiumBridge.drawsChunks()) {
			// The terrain's own shaders are not what the chunks are drawn with then, so where Sodium's cannot be read the
			// mod keeps to its own rather than following shaders the terrain around the foliage does not.
			return SodiumFoliageShader.usable(SODIUM_PIPELINE) ? SODIUM_PIPELINE : PIPELINE;
		}
		return TerrainFoliageShader.usable(TERRAIN_PIPELINE) ? TERRAIN_PIPELINE : PIPELINE;
	}

	/^*
	 * The pipeline leaves drawn solid go through, as the game's Fast leaves are: the same shaders keeping every pixel, as
	 * vanilla's solid terrain differs from its cutout terrain. Where there is none yet -- Sodium's copy, a shader pack's
	 * program -- or it does not compile, the pipeline itself, so they are drawn cut out rather than not at all.
	 ^/
	private static RenderPipeline opaqueOf(RenderPipeline pipeline) {
		//? iris {
		// Through a shader pack, the pipelines the mod's copies of its solid programs are drawn with.
		if (pipeline == shaderPackPipeline) {
			return IrisCompat.hasOpaquePrograms() ? shaderPackOpaquePipeline : pipeline;
		}
		if (pipeline == shaderPackShadowPipeline) {
			return IrisCompat.hasOpaquePrograms() ? shaderPackShadowOpaquePipeline : pipeline;
		}
		//?}
		if (pipeline == SODIUM_PIPELINE) {
			return SodiumFoliageShader.usable(SODIUM_OPAQUE_PIPELINE) ? SODIUM_OPAQUE_PIPELINE : pipeline;
		}
		if (pipeline == TERRAIN_PIPELINE) {
			return TerrainFoliageShader.usable(TERRAIN_OPAQUE_PIPELINE) ? TERRAIN_OPAQUE_PIPELINE : pipeline;
		}
		return pipeline == PIPELINE ? OPAQUE_PIPELINE : pipeline;
	}

	//? <26.1.2 {
	/^/^¹* Sodium 0.8's per-chunk fade-in times, all -1: no chunk the mod draws is fading in. Made once. ¹^/
	private static GpuBuffer sodiumChunkData;

	private static GpuBuffer sodiumChunkData() {
		if (sodiumChunkData == null) {
			// 64 ivec4s, as Sodium's shader declares them.
			ByteBuffer fades = MemoryUtil.memAlloc(64 * 4 * Integer.BYTES);
			try {
				while (fades.hasRemaining()) {
					fades.putInt(-1);
				}
				fades.flip();
				sodiumChunkData = RenderSystem.getDevice().createBuffer(() -> "MC2 foliage chunk fades",
						GpuBuffer.USAGE_UNIFORM, fades);
			} finally {
				MemoryUtil.memFree(fades);
			}
		}
		return sodiumChunkData;
	}
	^///?}

	/^*
	 * Draws what {@link #collectDraws} worked out: through the mod's own pipeline, or through a shader pack's programs
	 * while one is loaded -- into its shadow map if this is its shadow pass. Iris binds the framebuffer a pack's program
	 * writes to itself, so the pass is opened on the main target then. With no shader pack a shadow pass is Polytone's,
	 * drawn with the pipeline the screen is, into the targets it hands over.
	 ^/
	private static void submitDraws(Minecraft minecraft, Vec3 camera, Matrix4f shadowModelView, Matrix4f shadowProjection,
			GpuTextureView shadowColor, GpuTextureView shadowDepth) {
		List<Region> drawn = DRAWN;
		boolean shadowPass = shadowModelView != null;
		//? iris {
		RenderPipeline pipeline = !meshedForShaderPack ? ownPipeline()
				: shadowPass ? shaderPackShadowPipeline : shaderPackPipeline;
		//?} else {
		/^RenderPipeline pipeline = ownPipeline();
		^///?}
		// The terrain's shaders read where each region is from the chunk section block, the mod's own from the transform.
		boolean terrainShaders = pipeline == TERRAIN_PIPELINE;
		// Every region's offset is written in one mapping of the uniform ring buffer. The singular
		// writeTransform maps and unmaps it per call, which costs a GPU round trip for each one. In a shadow pass the
		// model view is the sun's, as Iris hands it over.
		//? >=26.2 {
		Matrix4f modelView = shadowPass ? shadowModelView : RenderSystem.getModelViewMatrixCopy();
		//?} else {
		/^Matrix4f modelView = shadowPass ? shadowModelView : RenderSystem.getModelViewMatrix();
		^///?}
		AbstractTexture atlas = minecraft.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
		GpuBufferSlice[] offsets;
		if (terrainShaders) {
			// As the chunk mesh fills it in: the region's corner in the world, fully faded in, and the atlas's size, which
			// the terrain's fragment shader samples the atlas by.
			int atlasWidth = atlas.getTextureView().getWidth(0);
			int atlasHeight = atlas.getTextureView().getHeight(0);
			DynamicUniforms.ChunkSectionInfo[] sections = new DynamicUniforms.ChunkSectionInfo[drawn.size()];
			for (int i = 0; i < sections.length; i++) {
				BlockPos origin = drawn.get(i).origin;
				sections[i] = new DynamicUniforms.ChunkSectionInfo(modelView, origin.getX(), origin.getY(), origin.getZ(),
						1.0F, atlasWidth, atlasHeight);
			}
			offsets = RenderSystem.getDynamicUniforms().writeChunkSections(sections);
		} else {
			Vector4f noModulation = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
			Matrix4f noTextureTransform = new Matrix4f();
			DynamicUniforms.Transform[] transforms = new DynamicUniforms.Transform[drawn.size()];
			for (int i = 0; i < transforms.length; i++) {
				BlockPos origin = drawn.get(i).origin;
				transforms[i] = new DynamicUniforms.Transform(
						modelView,
						noModulation,
						new Vector3f(
								(float) (origin.getX() - camera.x),
								(float) (origin.getY() - camera.y),
								(float) (origin.getZ() - camera.z)),
						noTextureTransform);
			}
			offsets = RenderSystem.getDynamicUniforms().writeTransforms(transforms);
		}
		String regionUniform = terrainShaders ? "ChunkSection" : "DynamicTransforms";

		int longestDraw = 0;
		for (int i = 2; i < drawsSize; i += 3) {
			longestDraw = Math.max(longestDraw, draws[i]);
		}
		for (int i = 2; i < opaqueDrawsSize; i += 3) {
			longestDraw = Math.max(longestDraw, opaqueDraws[i]);
		}
		// Asked for before the pass opens, as it may be compiled on the way.
		RenderPipeline opaquePipeline = opaqueDrawsSize > 0 ? opaqueOf(pipeline) : pipeline;
		//? >=26.2 {
		RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
		//?} else {
		/^RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
		^///?}
		GpuBuffer indexBuffer = indices.getBuffer(indexCountFor(longestDraw));
		//? >=26.2 {
		RenderTarget target = minecraft.gameRenderer.mainRenderTarget();
		//?} else {
		/^RenderTarget target = minecraft.getMainRenderTarget();
		^///?}
		// Written before the pass opens: a buffer cannot be written to while a render pass is open.
		GpuBuffer settings = swaySettings(camera);
		GpuBuffer interaction = GpuFoliageInteraction.upload(camera);
		// While Beryl draws the terrain with its shaders, the mod's pipelines are lit by them: they read the sway's blocks
		// and the lightmap by other names then.
		// While it draws its shadow map they are the ones it draws the terrain into it with.
		//? <26.2 {
		/^boolean berylLit = pipeline == PIPELINE && BerylBridge.light(FOLIAGE_FORMAT,
				FOLIAGE_FORMAT.getElementAttributeNames().indexOf("Normal"),
				FOLIAGE_FORMAT.getElementAttributeNames().indexOf("SwayCell"), (int) settings.size(),
				(int) interaction.size(), pipeline, opaquePipeline);
		^///?} else {
		// There is no VulkanMod from 26.2 on, and so no Beryl.
		boolean berylLit = false;
		//?}
		if (berylShadowPass && !berylLit) {
			// Beryl's shadow map takes nothing but what is drawn with its own shadow shaders.
			return;
		}
		// The sun's projection, as Iris or Polytone hands it over: what the game holds as the projection by the time Iris
		// hands over its shadow pass is not reliably the shadow map's, and plants projected any other way land at the wrong
		// depth in it.
		GpuBufferSlice projection = shadowPass ? shadowProjectionBuffer().getBuffer(shadowProjection) : null;
		GpuTextureView color = shadowColor != null ? shadowColor : target.getColorTextureView();
		GpuTextureView depth = shadowDepth != null ? shadowDepth : target.getDepthTextureView();

		Runnable draw = () -> {
		try (RenderPass pass = RenderSystem.getDevice()
				.createCommandEncoder()
				.createRenderPass(
						() -> shadowPass ? "MC2 foliage shadow" : "MC2 foliage",
						color,
						//? >=26.2 {
						Optional.empty(),
						//?} else {
						/^OptionalInt.empty(),
						^///?}
						depth,
						OptionalDouble.empty())) {
			// The solid leaves first, as vanilla draws its solid terrain before its cut out terrain, then the rest; each
			// with everything bound again for its pipeline.
			for (int kind = 0; kind < 2; kind++) {
				int[] list = kind == 0 ? opaqueDraws : draws;
				int listSize = kind == 0 ? opaqueDrawsSize : drawsSize;
				if (listSize == 0) {
					continue;
				}
				pass.setPipeline(kind == 0 ? opaquePipeline : pipeline);
				//? iris {
				if (meshedForShaderPack) {
					// Marked for the pack as the chunk mesh's own layers are: solid terrain, then cutout terrain.
					IrisCompat.setTerrainPhase(kind == 0);
				}
				//?}
				RenderSystem.bindDefaultUniforms(pass);
				if (projection != null) {
					pass.setUniform("Projection", projection);
				}
				// The chunk shaders pick mip levels themselves, so they sample the atlas the way the chunk mesh does: smoothly,
				// between mip levels. The mod's own shader reads it as the atlas is set to be read.
				boolean sodiumShaders = pipeline == SODIUM_PIPELINE;
				pass.bindTexture(sodiumShaders ? SodiumFoliageShader.BLOCK_TEXTURE : "Sampler0", atlas.getTextureView(),
						sodiumShaders || terrainShaders
								? RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR, true) : atlas.getSampler());
				pass.bindTexture(sodiumShaders ? SodiumFoliageShader.LIGHT_TEXTURE
								: berylLit ? BerylBridge.lightTexture() : "Sampler2",
						//? >=26.1.2 {
						minecraft.gameRenderer.lightmap(),
						//?} else {
						/^minecraft.gameRenderer.lightTexture().getTextureView(),
						^///?}
						RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
				//? <26.1.2 {
				/^if (sodiumShaders) {
					pass.setUniform(SodiumFoliageShader.CHUNK_DATA, sodiumChunkData());
				}
				^///?}
				pass.setIndexBuffer(indexBuffer, indices.type());
				pass.setUniform(SWAY_SETTINGS_UNIFORM, settings);
				pass.setUniform(GpuFoliageInteraction.UNIFORM, interaction);
				if (berylLit) {
					pass.setUniform(BerylBridge.sway(), settings);
					pass.setUniform(BerylBridge.interaction(), interaction);
					pass.setUniform(BerylBridge.globals(), RenderSystem.getGlobalSettingsUniform());
				}

				int boundRegion = -1;
				for (int i = 0; i < listSize; i += 3) {
					int regionIndex = list[i];
					if (regionIndex != boundRegion) {
						Region region = drawn.get(regionIndex);
						pass.setUniform(regionUniform, offsets[regionIndex]);
						if (berylLit) {
							pass.setUniform(BerylBridge.REGION, offsets[regionIndex]);
						}
						//? >=26.2 {
						pass.setVertexBuffer(0, region.vertices.slice());
						//?} else {
						/^pass.setVertexBuffer(0, region.vertices);
						^///?}
						boundRegion = regionIndex;
					}
					// The sequential index buffer counts from zero, and the base vertex moves it to where
					// this run of sections starts in the region's buffers.
					//? >=26.2 {
					pass.drawIndexed(indexCountFor(list[i + 2]), 1, 0, list[i + 1], 0);
					//?} else {
					/^pass.drawIndexed(list[i + 1], 0, indexCountFor(list[i + 2]), 1);
					^///?}
				}
			}
		}
		};
		//? iris {
		if (meshedForShaderPack) {
			IrisCompat.inTerrainPhase(draw);
			return;
		}
		//?}
		draw.run();
	}
	*/ //?}
    //?}

    //? <1.21.11 {
    /*/^* Where the foliage shader reads the plant pushes from. Nothing in vanilla binds uniform buffers here. ^/
	static final int INTERACTION_BINDING = 12;

	/^* The foliage shader, as the game last loaded it, or null before it has been. ^/
	private static ShaderInstance legacyShader;
	private static Matrix4f legacyModelView;
	private static Matrix4f legacyProjection;

	/^* The vertex format the foliage shader is loaded for: the block's, with the mod's own two values. ^/
	public static VertexFormat legacyVertexFormat() {
		return FOLIAGE_FORMAT;
	}

	/^*
	 * Called whenever the game has loaded the foliage shader, which it does again on every resource reload. The
	 * push data's uniform block is tied to its binding here, once per program, as the shader's JSON has no way
	 * to say it.
	 ^/
	public static void onLegacyShaderLoaded(ShaderInstance shader) {
		legacyShader = shader;
		//? >=1.20.1 {
		// The resources just changed, so the cutout shaders foliage follows may have too.
		LegacyTerrainShader.invalidate();
		//?}
		if (VulkanBridge.drawsChunks()) {
			// No OpenGL under VulkanMod, which draws the foliage with a pipeline of its own; see VulkanLegacyDraw.
			return;
		}
		int index = GL31.glGetUniformBlockIndex(shader.getId(), GpuFoliageInteraction.UNIFORM);
		if (index != GL31.GL_INVALID_INDEX) {
			GL31.glUniformBlockBinding(shader.getId(), index, INTERACTION_BINDING);
		}
	}

	/^* Set while a sub-level's foliage is drawn: from a camera in its plot, with no edge to the wind. ^/
	private static boolean subLevelPass;
	/^* While that is set, whether it is drawn into a shader pack's shadow map rather than onto the screen. ^/
	private static boolean subLevelInShadow;

	/^* Whether Sable shades its sub-levels by where their faces look, as last asked; see SableBridge.shadesByFacing. ^/
	private static boolean subLevelShading;
	/^* Set while a section of a sub-level is meshed with no shade in it. ^/
	private static boolean unshaded;
	private static final Vector4f NO_WIND_EDGE = new Vector4f(-1.0E6F, -1.0E6F, 1.0E6F, 1.0E6F);
	private static final org.joml.Vector3d POSE_POSITION = new org.joml.Vector3d();
	private static final org.joml.Quaterniond POSE_ORIENTATION = new org.joml.Quaterniond();
	private static final org.joml.Vector3d POSE_ROTATION_POINT = new org.joml.Vector3d();
	private static long subLevelLoggedAt;

	/^* How far the tip of a plant lying on its side hangs, in blocks. ^/
	private static final float SUB_LEVEL_DROOP = 1.0F;
	/^*
	 * How a sub-level's foliage follows it. Each sub-level tows a weight on a spring, kept where the world is rather
	 * than where the plot is: the weight is pulled toward the sub-level and slowed by the air, so it runs on past a
	 * sub-level that stops -- a tree hitting the ground -- and swings back until it settles. How far the weight is
	 * from the sub-level is how far its plants' tips are thrown, on top of hanging down.
	 * <p>
	 * A swing every second and a half or so, losing about half of each to the next; one that stops dead throws the
	 * weight about its speed over the root of the stiffness. Settled by eye with the player, as the two below were.
	 ^/
	private static final float SWING_STIFFNESS = 20.0F;
	private static final float SWING_DAMPING = 2.0F;
	/^* How far a block of the weight's lag throws the tips, in the same measure as hanging straight down is 1. ^/
	private static final float SWING_THROW = 2.5F;
	/^*
	 * How far a sub-level's own speed throws the tips back, in that measure for each block a second: the weight alone
	 * trails a moving sub-level by too little to see, and foliage is seen to stream behind what carries it.
	 ^/
	private static final float SWING_TRAIL = 0.35F;
	/^* The most the tips are thrown and hang together, in that measure. ^/
	private static final float SWING_MOST = 5.0F;
	/^* A sub-level that moves further than this in one frame was put there, not carried: nothing swings for it. ^/
	private static final double SWING_JUMP = 8.0D;

	/^* One sub-level's weight: where it is and how fast it moves, in the world. ^/
	private static final class Swing {
		final org.joml.Vector3d weight = new org.joml.Vector3d();
		final org.joml.Vector3d speed = new org.joml.Vector3d();
		/^* Where the sub-level was last frame, and how fast it has been moving, eased over a few frames. ^/
		final org.joml.Vector3d lastPosition = new org.joml.Vector3d();
		final org.joml.Vector3d moving = new org.joml.Vector3d();
		/^* Where the tips hang to this frame, as the world sees it: down, and thrown by the weight's lag. ^/
		final org.joml.Vector3f hang = new org.joml.Vector3f(0.0F, -1.0F, 0.0F);
		boolean placed;
	}

	private static final Map<Object, Swing> SWINGS = new java.util.IdentityHashMap<>();
	private static long swungNanos;
	/^* Where the tips hang to for the sub-level being drawn, as its plot sees it, in blocks. ^/
	private static final org.joml.Vector3f SUB_LEVEL_HANG = new org.joml.Vector3f();

	/^*
	 * Moves every sub-level's weight on by the time since the last frame, toward where the sub-level is drawn now.
	 * Nothing is asked of Sable but the pose: the weight's own lag says how the sub-level has been moving.
	 ^/
	private static void swingSubLevels() {
		long now = System.nanoTime();
		float delta = swungNanos == 0L ? 0.0F : Math.min((now - swungNanos) / 1.0E9F, 0.1F);
		swungNanos = now;
		SWINGS.keySet().retainAll(SUB_LEVELS.keySet());
		for (Object subLevel : SUB_LEVELS.keySet()) {
			if (!SableBridge.pose(subLevel, POSE_POSITION, POSE_ORIENTATION, POSE_ROTATION_POINT)) {
				continue;
			}
			Swing swing = SWINGS.computeIfAbsent(subLevel, found -> new Swing());
			if (!swing.placed || swing.weight.distance(POSE_POSITION) > SWING_JUMP) {
				swing.placed = true;
				swing.weight.set(POSE_POSITION);
				swing.speed.zero();
				swing.lastPosition.set(POSE_POSITION);
				swing.moving.zero();
			}
			if (delta > 0.0F) {
				// The pose moves in the game's ticks, a step at a time: its speed is eased, or it would jump with them.
				double ease = Math.min(1.0D, delta / 0.15D);
				swing.moving.x += ((POSE_POSITION.x - swing.lastPosition.x) / delta - swing.moving.x) * ease;
				swing.moving.y += ((POSE_POSITION.y - swing.lastPosition.y) / delta - swing.moving.y) * ease;
				swing.moving.z += ((POSE_POSITION.z - swing.lastPosition.z) / delta - swing.moving.z) * ease;
				swing.lastPosition.set(POSE_POSITION);
			}
			// In steps short enough for the spring to stay steady at any frame time.
			int steps = Math.max(1, Mth.ceil(delta / 0.01F));
			double step = delta / steps;
			for (int i = 0; i < steps; i++) {
				swing.speed.x += ((POSE_POSITION.x - swing.weight.x) * SWING_STIFFNESS - swing.speed.x * SWING_DAMPING) * step;
				swing.speed.y += ((POSE_POSITION.y - swing.weight.y) * SWING_STIFFNESS - swing.speed.y * SWING_DAMPING) * step;
				swing.speed.z += ((POSE_POSITION.z - swing.weight.z) * SWING_STIFFNESS - swing.speed.z * SWING_DAMPING) * step;
				swing.weight.add(swing.speed.x * step, swing.speed.y * step, swing.speed.z * step);
			}
			swing.hang.set(
					(float) ((swing.weight.x - POSE_POSITION.x) * SWING_THROW - swing.moving.x * SWING_TRAIL),
					(float) ((swing.weight.y - POSE_POSITION.y) * SWING_THROW - swing.moving.y * SWING_TRAIL) - 1.0F,
					(float) ((swing.weight.z - POSE_POSITION.z) * SWING_THROW - swing.moving.z * SWING_TRAIL));
			float length = swing.hang.length();
			if (length > SWING_MOST) {
				swing.hang.mul(SWING_MOST / length);
			}
		}
	}

	/^*
	 * Takes up the sections of a plot a chunk build has just met foliage in, and lets go of the sub-levels that are
	 * no more. A sub-level is first heard of through that build -- Sable building its mesh, with the foliage still in
	 * it -- and from there on its sections change hands as the world's do: meshed, uploaded, and Sable's mesh asked
	 * for again without the foliage, by the request Sable answers for its own sections.
	 ^/
	private static void followSubLevels(Minecraft minecraft) {
		if (!SableBridge.LOADED) {
			return;
		}
		boolean shading = SableBridge.shadesByFacing();
		if (shading != subLevelShading) {
			// Sable builds its own meshes again as the setting changes, and so are the renderer's.
			subLevelShading = shading;
			DIRTY.addAll(PLOT_OWNERS.keySet());
		}
		GpuFoliageSplit.forEachPlotFound(key -> {
			if (PLOT_OWNERS.containsKey(key)) {
				return;
			}
			Object subLevel = SableBridge.subLevelAt(SectionPos.x(key), SectionPos.z(key));
			if (subLevel == null) {
				return;
			}
			PLOT_OWNERS.put(key, subLevel);
			SUB_LEVELS.computeIfAbsent(subLevel, found -> new LongOpenHashSet()).add(key);
			GpuFoliageSplit.addPlotColumn(SectionPos.x(key), SectionPos.z(key));
			DIRTY.add(key);
			if (SableBridge.LOG) {
				AllThingsFoliage.LOGGER.info("[Sable] section {} {} {} of sub-level {} holds foliage: queued",
						SectionPos.x(key), SectionPos.y(key), SectionPos.z(key), System.identityHashCode(subLevel));
			}
		});
		var followed = SUB_LEVELS.entrySet().iterator();
		while (followed.hasNext()) {
			var entry = followed.next();
			long any = entry.getValue().iterator().nextLong();
			if (SableBridge.subLevelAt(SectionPos.x(any), SectionPos.z(any)) == entry.getKey()) {
				continue;
			}
			for (long key : entry.getValue()) {
				DIRTY.remove(key);
				removeSection(key);
				GpuFoliageSplit.forgetSection(key);
				GpuFoliageSplit.removePlotColumn(SectionPos.x(key), SectionPos.z(key));
				PLOT_OWNERS.remove(key);
			}
			if (SableBridge.LOG) {
				AllThingsFoliage.LOGGER.info("[Sable] sub-level {} is gone: {} sections let go",
						System.identityHashCode(entry.getKey()), entry.getValue().size());
			}
			followed.remove();
		}
		pushSubLevelPlants(minecraft);
		swingSubLevels();
	}

	/^*
	 * Has the entities near the player push the plants of the sub-levels they are on, as they push the world's: each
	 * entity's box is put where it is inside the plot of every sub-level it is near, and Sway is asked from there.
	 * The box is turned with the plot, so on a sub-level lying on its side the entity lies across the plants, and
	 * pushes them walking past as it would standing ones; the box that holds the turned one is what Sway is given.
	 ^/
	private static void pushSubLevelPlants(Minecraft minecraft) {
		GpuFoliageInteraction.beginPlotForces();
		if (SUB_LEVELS.isEmpty() || minecraft.player == null || !SwayConfig.INSTANCE.enabled) {
			return;
		}
		List<net.minecraft.world.entity.Entity> entities = minecraft.level.getEntities((net.minecraft.world.entity.Entity) null,
				minecraft.player.getBoundingBox().inflate(SwayConfig.INSTANCE.maxDistance),
				entity -> !entity.isSpectator() && entity.isAlive());
		org.joml.Vector3d corner = new org.joml.Vector3d();
		org.joml.Vector3d centre = new org.joml.Vector3d();
		for (var subLevel : SUB_LEVELS.entrySet()) {
			if (!SableBridge.pose(subLevel.getKey(), POSE_POSITION, POSE_ORIENTATION, POSE_ROTATION_POINT)) {
				continue;
			}
			LongOpenHashSet sections = subLevel.getValue();
			for (net.minecraft.world.entity.Entity entity : entities) {
				AABB box = entity.getBoundingBox();
				double minX = Double.MAX_VALUE;
				double minY = Double.MAX_VALUE;
				double minZ = Double.MAX_VALUE;
				double maxX = -Double.MAX_VALUE;
				double maxY = -Double.MAX_VALUE;
				double maxZ = -Double.MAX_VALUE;
				for (int i = 0; i < 8; i++) {
					corner.set((i & 1) == 0 ? box.minX : box.maxX, (i & 2) == 0 ? box.minY : box.maxY,
							(i & 4) == 0 ? box.minZ : box.maxZ).sub(POSE_POSITION);
					POSE_ORIENTATION.transformInverse(corner).add(POSE_ROTATION_POINT);
					minX = Math.min(minX, corner.x);
					minY = Math.min(minY, corner.y);
					minZ = Math.min(minZ, corner.z);
					maxX = Math.max(maxX, corner.x);
					maxY = Math.max(maxY, corner.y);
					maxZ = Math.max(maxZ, corner.z);
				}
				centre.set((minX + maxX) * 0.5D, (minY + maxY) * 0.5D, (minZ + maxZ) * 0.5D);
				if (nearSection(sections, centre)) {
					GpuFoliageInteraction.pushPlot(minecraft.level, new AABB(minX, minY, minZ, maxX, maxY, maxZ));
				}
			}
		}
	}

	/^* Whether a place in a plot is in or beside one of the sections held there: within a few blocks of it. ^/
	private static boolean nearSection(LongOpenHashSet sections, org.joml.Vector3d place) {
		for (int dx = -4; dx <= 4; dx += 8) {
			for (int dz = -4; dz <= 4; dz += 8) {
				for (int dy = -4; dy <= 4; dy += 8) {
					if (sections.contains(SectionPos.asLong(
							SectionPos.blockToSectionCoord(Mth.floor(place.x + dx)),
							SectionPos.blockToSectionCoord(Mth.floor(place.y + dy)),
							SectionPos.blockToSectionCoord(Mth.floor(place.z + dz))))) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/^*
	 * Draws the foliage of each sub-level where the sub-level is. Sable draws a plot's sections turned about a point
	 * and moved to where the sub-level is; seen from the camera, that is the plot seen from where the camera would be
	 * inside it, turned. So each sub-level's regions are drawn as the world's are, from that camera and with the view
	 * turned as the sub-level is: the offsets, the sway and the fog all stay what they are for the world.
	 * <p>
	 * The shade of a face is the one thing that does not: the game works it out as a block is meshed, from where the
	 * face looks then, and a felled tree's faces look elsewhere. Sable meshes its sub-levels with no shade and works
	 * it out in the shader, from the normal turned as the sub-level is, where its dynamic shading is on; the renderer
	 * does the same then, so the leaves match the trunk they hang on, and a plant the block it grows on.
	 ^/
	private static void drawSubLevels(Minecraft minecraft, Vec3 camera, Matrix4f view, Matrix4f projection,
			boolean shadowMap) {
		if (SUB_LEVELS.isEmpty() || view == null || projection == null) {
			return;
		}
		boolean log = !shadowMap && SableBridge.LOG && System.currentTimeMillis() - subLevelLoggedAt > 2000L;
		for (Object subLevel : SUB_LEVELS.keySet()) {
			if (!SableBridge.pose(subLevel, POSE_POSITION, POSE_ORIENTATION, POSE_ROTATION_POINT)) {
				continue;
			}
			// Where its plants' tips hang to, turned into the plot and given its length in blocks.
			Swing swing = SWINGS.get(subLevel);
			if (swing == null) {
				SUB_LEVEL_HANG.set(0.0F, -1.0F, 0.0F);
			} else {
				SUB_LEVEL_HANG.set(swing.hang);
			}
			new org.joml.Quaternionf(POSE_ORIENTATION).transformInverse(SUB_LEVEL_HANG).mul(SUB_LEVEL_DROOP);
			boolean anything = collectDraws(null, null, subLevel);
			// Where the camera is in the plot: the pose undone.
			org.joml.Vector3d inPlot = new org.joml.Vector3d(camera.x, camera.y, camera.z).sub(POSE_POSITION);
			POSE_ORIENTATION.transformInverse(inPlot).add(POSE_ROTATION_POINT);
			if (log) {
				AllThingsFoliage.LOGGER.info("[Sable] sub-level {}: {} sections followed, {} regions drawn ({} cut out runs, "
								+ "{} solid), at {}, turned {}, about {}, camera {} and in its plot {}, shader pack {}",
						System.identityHashCode(subLevel), SUB_LEVELS.get(subLevel).size(), DRAWN.size(), drawsSize / 3,
						opaqueDrawsSize / 3, POSE_POSITION, POSE_ORIENTATION, POSE_ROTATION_POINT, camera, inPlot,
						meshedForShaderPack);
			}
			if (!anything) {
				continue;
			}
			// A shader pack's program works its other matrices out from this one as it is applied.
			Matrix4f modelView = new Matrix4f(view).rotate(new org.joml.Quaternionf(POSE_ORIENTATION));
			subLevelPass = true;
			subLevelInShadow = shadowMap;
			try {
				drawLegacy(minecraft, new Vec3(inPlot.x, inPlot.y, inPlot.z), DRAWN, modelView, projection);
			} finally {
				subLevelPass = false;
				subLevelInShadow = false;
			}
		}
		if (log) {
			subLevelLoggedAt = System.currentTimeMillis();
		}
	}

	/^*
	 * Draws the queued runs the way vanilla draws its chunk layers before 1.21.11: the leaves drawn solid first, with the
	 * solid layer's render state and shaders that keep every pixel, then everything else with the cutout layer's.
	 ^/
	private static void drawLegacy(Minecraft minecraft, Vec3 camera, List<Region> drawn, Matrix4f shadowModelView,
			Matrix4f shadowProjection) {
		// In a shader pack's shadow pass the matrices are the sun's, as Iris hands them over. A sub-level is drawn
		// with matrices of its own in either pass, and which of the two it is is said apart.
		boolean given = shadowModelView != null;
		boolean shadowPass = subLevelPass ? subLevelInShadow : given;
		Matrix4f modelView = given ? shadowModelView : legacyModelView;
		Matrix4f projection = given ? shadowProjection : legacyProjection;
		if (modelView == null || projection == null) {
			return;
		}
		if (VulkanBridge.drawsThroughItself()) {
			if (!shadowPass) {
				drawLegacyThroughVulkanMod(minecraft, camera, drawn, modelView, projection);
			}
			return;
		}
		boolean uploaded = false;
		for (int kind = 0; kind < 2; kind++) {
			int[] list = kind == 0 ? opaqueDraws : draws;
			int listSize = kind == 0 ? opaqueDrawsSize : drawsSize;
			if (listSize == 0) {
				continue;
			}
			boolean solid = kind == 0;
			// The mod's own shader draws either kind: what it discards is a uniform.
			ShaderInstance shader = legacyShader;
			boolean shaderPack = false;
			//? iris {
			if (meshedForShaderPack) {
				// The pack's own program for the pass, built for the foliage; see IrisFoliageShaders. Leaves meshed solid
				// under a pack whose solid programs are gone since are drawn cut out.
				shader = IrisCompat.program(shadowPass, solid);
				if (shader == null && solid) {
					solid = false;
					shader = IrisCompat.program(shadowPass, false);
				}
				shaderPack = true;
			}
			//?}
			//? >=1.20.1 {
			// The shaders the chunk mesh draws that layer with, where they compile with the sway spliced in. Sodium draws
			// its chunks with shaders of its own, which a resource pack's core shaders never reach, so the mod keeps to its
			// own shader there rather than following a pack the terrain does not.
			if (!shaderPack && legacyShader != null && !SodiumBridge.drawsChunks()) {
				ShaderInstance terrain = LegacyTerrainShader.get(FOLIAGE_FORMAT, INTERACTION_BINDING, solid);
				if (terrain != null) {
					shader = terrain;
				}
			}
			//?}
			if (shader == null) {
				continue;
			}
			if (!uploaded) {
				// For a sub-level the camera is the one inside its plot, and so are the pushes: those on its own plants.
				GpuFoliageInteraction.uploadLegacy(camera, INTERACTION_BINDING);
				uploaded = true;
			}
			drawLegacyRuns(minecraft, camera, drawn, shader, solid, modelView, projection, list, listSize);
		}
	}

	/^*
	 * Draws the queued runs through VulkanMod's own pipelines, where it stands in for OpenGL call by call and the draw
	 * below, which is OpenGL's, cannot be made: the same two kinds in the same order, each with its layer's render
	 * state, which VulkanMod follows, and one pipeline for both, told what to discard. See VulkanLegacyDraw.
	 ^/
	private static void drawLegacyThroughVulkanMod(Minecraft minecraft, Vec3 camera, List<Region> drawn,
			Matrix4f modelView, Matrix4f projection) {
		// Into Beryl's shadow map there are no matrices to hand over, and no render state to set: see begin. The pushes
		// are the ones the frame before was drawn with, which are worked out once a frame.
		boolean shadowMap = modelView == null;
		if (shadowMap ? !VulkanLegacyDraw.built()
				: !VulkanLegacyDraw.ready(FOLIAGE_FORMAT, FOLIAGE_FORMAT.getElementAttributeNames().indexOf("Normal"),
						FOLIAGE_FORMAT.getElementAttributeNames().indexOf("SwayCell"),
						GpuFoliageInteraction.legacyCells(camera))) {
			return;
		}
		Vector4f edge = windEdgeFrom(camera);
		Vector4f weather = weather();
		BlockPos cameraBlock = BlockPos.containing(camera);
		if (!VulkanLegacyDraw.begin(modelView, projection, cameraBlock.getX(), cameraBlock.getY(), cameraBlock.getZ(),
				(float) (cameraBlock.getX() - camera.x), (float) (cameraBlock.getY() - camera.y),
				(float) (cameraBlock.getZ() - camera.z), RenderSystem.getShaderGameTime(), swayIntensity(), calmSway(),
				edge.x, edge.y, edge.z, edge.w,
				weather.x, weather.y, weather.z, weather.w)) {
			return;
		}
		int longestRun = 0;
		for (int i = 2; i < drawsSize; i += 3) {
			longestRun = Math.max(longestRun, draws[i]);
		}
		for (int i = 2; i < opaqueDrawsSize; i += 3) {
			longestRun = Math.max(longestRun, opaqueDraws[i]);
		}
		for (int kind = 0; kind < 2; kind++) {
			int[] list = kind == 0 ? opaqueDraws : draws;
			int listSize = kind == 0 ? opaqueDrawsSize : drawsSize;
			if (listSize == 0) {
				continue;
			}
			boolean solid = kind == 0;
			RenderType renderType = solid ? RenderType.solid() : RenderType.cutout();
			if (!shadowMap) {
				renderType.setupRenderState();
			}
			// What vanilla's cutout layer discards below, or nothing.
			VulkanLegacyDraw.cutout(solid ? 0.0F : 0.1F);
			int boundRegion = -1;
			for (int i = 0; i < listSize; i += 3) {
				int regionIndex = list[i];
				if (regionIndex != boundRegion) {
					Region region = drawn.get(regionIndex);
					VulkanLegacyDraw.region(region.vertices,
							(float) (region.origin.getX() - camera.x),
							(float) (region.origin.getY() - camera.y),
							(float) (region.origin.getZ() - camera.z), longestRun);
					boundRegion = regionIndex;
				}
				VulkanLegacyDraw.run(list[i + 1], indexCountFor(list[i + 2]));
			}
			if (!shadowMap) {
				renderType.clearRenderState();
			}
		}
		if (!shadowMap) {
			VulkanLegacyDraw.end(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix());
		}
	}

	/^*
	 * Draws one kind's runs: the layer's render state, the shader's default uniforms, then one offset and one vertex
	 * buffer per region. Only the draw call differs from vanilla's: a vertex buffer can only draw the whole of itself,
	 * and a region is drawn in runs of visible sections, so each run is drawn from where it starts in the buffer.
	 ^/
	private static void drawLegacyRuns(Minecraft minecraft, Vec3 camera, List<Region> drawn, ShaderInstance shader,
			boolean solid, Matrix4f modelView, Matrix4f projection, int[] list, int listSize) {
		//? iris {
		if (meshedForShaderPack) {
			IrisCompat.setTerrainPhase(solid);
		}
		//?}
		RenderType renderType = solid ? RenderType.solid() : RenderType.cutout();
		renderType.setupRenderState();
		ShaderInstance program = shader;
		RenderSystem.setShader(() -> program);
		//? >=1.21.1 {
		shader.setDefaultUniforms(VertexFormat.Mode.QUADS, modelView, projection, minecraft.getWindow());
		//?} else {
		/^// Before 1.21.1 nothing sets the uniforms every shader shares in one call: they are set the way the terrain's
		// own layers set them.
		for (int sampler = 0; sampler < 12; sampler++) {
			shader.setSampler("Sampler" + sampler, RenderSystem.getShaderTexture(sampler));
		}
		if (shader.MODEL_VIEW_MATRIX != null) {
			shader.MODEL_VIEW_MATRIX.set(modelView);
		}
		if (shader.PROJECTION_MATRIX != null) {
			shader.PROJECTION_MATRIX.set(projection);
		}
		if (shader.COLOR_MODULATOR != null) {
			shader.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
		}
		if (shader.FOG_START != null) {
			shader.FOG_START.set(RenderSystem.getShaderFogStart());
		}
		if (shader.FOG_END != null) {
			shader.FOG_END.set(RenderSystem.getShaderFogEnd());
		}
		if (shader.FOG_COLOR != null) {
			shader.FOG_COLOR.set(RenderSystem.getShaderFogColor());
		}
		if (shader.FOG_SHAPE != null) {
			shader.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
		}
		if (shader.GAME_TIME != null) {
			shader.GAME_TIME.set(RenderSystem.getShaderGameTime());
		}
		// The two the chunk layers set that nothing above covers; a resource pack's shader may read either.
		if (shader.GLINT_ALPHA != null) {
			shader.GLINT_ALPHA.set(RenderSystem.getShaderGlintAlpha());
		}
		if (shader.TEXTURE_MATRIX != null) {
			shader.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
		}
		RenderSystem.setupShaderLights(shader);
		^///?}
		shader.safeGetUniform("SwayIntensity").set(swayIntensity());
		shader.safeGetUniform("CalmSway").set(calmSway());
		// The wind eases off at the edge of the area around the player, which a sub-level's plot is nowhere near.
		Vector4f edge = subLevelPass ? NO_WIND_EDGE : windEdgeFrom(camera);
		shader.safeGetUniform("SwayEdge").set(edge.x, edge.y, edge.z, edge.w);
		BlockPos cameraBlock = BlockPos.containing(camera);
		shader.safeGetUniform("CameraBlockPos").set(cameraBlock.getX(), cameraBlock.getY(), cameraBlock.getZ());
		shader.safeGetUniform("CameraOffset").set(
				(float) (cameraBlock.getX() - camera.x),
				(float) (cameraBlock.getY() - camera.y),
				(float) (cameraBlock.getZ() - camera.z));
		Vector4f weather = weather();
		shader.safeGetUniform("Weather").set(weather.x, weather.y, weather.z, weather.w);
		// What the mod's own fragment shader discards below: vanilla's cutout layer's 0.1, or nothing.
		shader.safeGetUniform("AlphaCutout").set(solid ? 0.0F : 0.1F);
		// The same values under the names sway.glsl reads them by, for the shader built from the cutout shaders.
		shader.safeGetUniform("mc2_SwayIntensity").set(swayIntensity());
		shader.safeGetUniform("mc2_CalmSway").set(calmSway());
		shader.safeGetUniform("mc2_SwayEdge").set(edge.x, edge.y, edge.z, edge.w);
		shader.safeGetUniform("mc2_CameraBlockPos").set(cameraBlock.getX(), cameraBlock.getY(), cameraBlock.getZ());
		shader.safeGetUniform("mc2_CameraOffset").set(
				(float) (cameraBlock.getX() - camera.x),
				(float) (cameraBlock.getY() - camera.y),
				(float) (cameraBlock.getZ() - camera.z));
		shader.safeGetUniform("mc2_Weather").set(weather.x, weather.y, weather.z, weather.w);
		shader.safeGetUniform("mc2_GameTime").set(RenderSystem.getShaderGameTime());
		// The world's down and its west as a sub-level's plot sees them, for its plants to hang by and its wind to blow
		// by; nothing of either for the world's own foliage. See sublevel.glsl.
		org.joml.Vector3f down = new org.joml.Vector3f();
		org.joml.Vector3f west = new org.joml.Vector3f(-1.0F, 0.0F, 0.0F);
		if (subLevelPass) {
			org.joml.Quaternionf turn = new org.joml.Quaternionf(POSE_ORIENTATION);
			turn.transformInverse(down.set(0.0F, -1.0F, 0.0F));
			turn.transformInverse(west);
		}
		org.joml.Vector3f hang = subLevelPass ? SUB_LEVEL_HANG : down;
		shader.safeGetUniform("mc2_SubLevelDown").set(down.x, down.y, down.z);
		shader.safeGetUniform("mc2_SubLevelWind").set(west.x, west.z);
		shader.safeGetUniform("mc2_SubLevelHang").set(hang.x, hang.y, hang.z);
		if (subLevelPass && subLevelShading) {
			shader.safeGetUniform("mc2_SubLevelTurn").set((float) POSE_ORIENTATION.x, (float) POSE_ORIENTATION.y,
					(float) POSE_ORIENTATION.z, (float) POSE_ORIENTATION.w);
			shader.safeGetUniform("mc2_SubLevelShade").set(
					minecraft.level.getShade(Direction.DOWN, true), minecraft.level.getShade(Direction.UP, true),
					minecraft.level.getShade(Direction.NORTH, true), minecraft.level.getShade(Direction.EAST, true));
		} else {
			shader.safeGetUniform("mc2_SubLevelTurn").set(0.0F, 0.0F, 0.0F, 0.0F);
		}
		shader.apply();
		// The pushes' block is tied to its binding again on the program now in use. A mod may build a shader's program
		// anew as the game runs -- Veil, which Sable brings, does as the shader is applied -- and a new program reads
		// every uniform block from binding 0, where the pushes are not: the plants swayed and nothing pushed them.
		int interactionBlock = GL31.glGetUniformBlockIndex(shader.getId(), GpuFoliageInteraction.UNIFORM);
		if (interactionBlock != GL31.GL_INVALID_INDEX) {
			GL31.glUniformBlockBinding(shader.getId(), interactionBlock, INTERACTION_BINDING);
		}
		//? iris {
		if (meshedForShaderPack) {
			// A pack's program knows only the uniforms Iris lists, so the sway's are set on it directly once it is in use.
			IrisCompat.setSwayUniforms(shader, swayIntensity(), calmSway(),
					edge, cameraBlock.getX(), cameraBlock.getY(), cameraBlock.getZ(),
					(float) (cameraBlock.getX() - camera.x), (float) (cameraBlock.getY() - camera.y),
					(float) (cameraBlock.getZ() - camera.z), RenderSystem.getShaderGameTime(), weather, down, west.x, west.z,
					hang);
		}
		//?}

		Uniform regionOffset = shader.CHUNK_OFFSET;
		int boundRegion = -1;
		int indexType = 0;
		for (int i = 0; i < listSize; i += 3) {
			int regionIndex = list[i];
			if (regionIndex != boundRegion) {
				Region region = drawn.get(regionIndex);
				if (regionOffset != null) {
					regionOffset.set(
							(float) (region.origin.getX() - camera.x),
							(float) (region.origin.getY() - camera.y),
							(float) (region.origin.getZ() - camera.z));
					regionOffset.upload();
				}
				region.vertices.bind();
				// Read after binding: binding is what grows vanilla's shared quad indices to fit the region, and
				// growing them can widen the index type.
				indexType = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS).type().asGLType;
				boundRegion = regionIndex;
			}
			GL32.glDrawElementsBaseVertex(GL11.GL_TRIANGLES, indexCountFor(list[i + 2]), indexType, 0L, list[i + 1]);
		}

		if (regionOffset != null) {
			regionOffset.set(0.0F, 0.0F, 0.0F);
		}
		shader.clear();
		VertexBuffer.unbind();
		renderType.clearRenderState();
	}
	*/ //?}

    //? >=1.21.11 {
    /**
     * The sway settings buffer, with the current intensity and whether the calm sway is on, the edge the wind eases off
     * at, and the weather. It is written only when a value changed, so moving the slider is seen live and
     * standing still costs nothing.
     */
    private static GpuBuffer swaySettings(Vec3 camera) {
        if (swaySettings == null) {
            swaySettings = RenderSystem.getDevice().createBuffer(
                () -> "MC2 foliage sway settings",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                SWAY_SETTINGS_SIZE
            );
            uploadedIntensity = Float.NaN;
        }
        float intensity = swayIntensity();
        float calm = calmSway();
        Vector4f edge = windEdgeFrom(camera);
        Vector4f weather = weather();
        if (
            intensity != uploadedIntensity ||
            calm != uploadedCalmSway ||
            !edge.equals(UPLOADED_EDGE) ||
            !weather.equals(UPLOADED_WEATHER)
        ) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                ByteBuffer data = Std140Builder.onStack(
                    stack,
                    SWAY_SETTINGS_SIZE
                )
                    .putFloat(intensity)
                    .putFloat(calm)
                    .putVec4(edge)
                    .putVec4(weather)
                    .get();
                RenderSystem.getDevice()
                    .createCommandEncoder()
                    .writeToBuffer(swaySettings.slice(), data);
            }
            uploadedIntensity = intensity;
            uploadedCalmSway = calm;
            UPLOADED_EDGE.set(edge);
            UPLOADED_WEATHER.set(weather);
        }
        return swaySettings;
    }

    //?}

    /**
     * The weather the shader turns the sway into wind by: the level's rain and thunder, each from 0 to 1 and eased in and
     * out by the game, taken between ticks so they change smoothly, then how far from the player the wind blows and over
     * how many blocks it eases off there. None of it while the weather wind is switched off.
     */
    private static Vector4f weather() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return new Vector4f();
        }
        //? >=1.21.11 {
        float partialTick = minecraft
            .getDeltaTracker()
            .getGameTimeDeltaPartialTick(false);
        //?} elif >=1.21.1 {
        /*float partialTick = minecraft.getTimer().getGameTimeDeltaPartialTick(false);
         */ //?} else {
        /*float partialTick = minecraft.getFrameTime();
         */ //?}
        if (!FoliageSettings.weatherWind()) {
            return new Vector4f();
        }
        // Past the reach the wind eases off, so the shader is told both.
        return new Vector4f(
            minecraft.level.getRainLevel(partialTick),
            minecraft.level.getThunderLevel(partialTick),
            weatherWindReach(),
            WEATHER_WIND_FADE_BLOCKS
        );
    }

    /**
     * How strongly plants sway: the waving foliage slider while the calm sway is on, and a fixed strength otherwise, so
     * the weather wind blows just as well with the calm sway switched off.
     */
    private static float swayIntensity() {
        return FoliageSettings.wavingFoliage()
            ? FoliageSettings.wavingIntensity()
            : FoliageSettings.DEFAULT_WAVING_INTENSITY;
    }

    /** 1 while plants sway in calm weather, 0 while they stand still until the weather wind moves them. */
    private static float calmSway() {
        return FoliageSettings.wavingFoliage() ? 1.0F : 0.0F;
    }

    /**
     * Moves the wind's edge towards the near area's, at {@link #WIND_EDGE_SPEED}, or straight onto it when it is
     * far off or there was none yet.
     */
    private static void followWindEdge() {
        long now = System.nanoTime();
        // A long pause, like a paused game, counts as a short one, so the edge never skips a band of plants at once.
        double step =
            WIND_EDGE_SPEED *
            Math.min((now - windEdgeMovedNanos) / 1.0E9D, 0.1D);
        windEdgeMovedNanos = now;
        if (!GpuFoliageSplit.hasArea()) {
            Arrays.fill(WIND_EDGE, Double.NaN);
            return;
        }
        int radius = GpuFoliageSplit.radius();
        double[] target = {
            SectionPos.sectionToBlockCoord(GpuFoliageSplit.centreX() - radius),
            SectionPos.sectionToBlockCoord(GpuFoliageSplit.centreZ() - radius),
            SectionPos.sectionToBlockCoord(
                GpuFoliageSplit.centreX() + radius + 1
            ),
            SectionPos.sectionToBlockCoord(
                GpuFoliageSplit.centreZ() + radius + 1
            ),
        };
        boolean jump = false;
        for (int i = 0; i < target.length; i++) {
            // NaN fails every comparison, so a missing edge jumps too.
            jump |= !(Math.abs(target[i] - WIND_EDGE[i]) <= WIND_EDGE_JUMP);
        }
        for (int i = 0; i < target.length; i++) {
            double gap = target[i] - WIND_EDGE[i];
            WIND_EDGE[i] =
                jump || Math.abs(gap) <= step
                    ? target[i]
                    : WIND_EDGE[i] + Math.signum(gap) * step;
        }
    }

    /** The wind's edge relative to the camera, as the shader reads it. */
    private static Vector4f windEdgeFrom(Vec3 camera) {
        return new Vector4f(
            (float) (WIND_EDGE[0] - camera.x),
            (float) (WIND_EDGE[1] - camera.z),
            (float) (WIND_EDGE[2] - camera.x),
            (float) (WIND_EDGE[3] - camera.z)
        );
    }

    private static final LongOpenHashSet REGIONS_WITH_QUEUED_SECTIONS =
        new LongOpenHashSet();

    // ---- Animated textures, where Sodium has to be told which are on screen; see SodiumBridge.markActive.
    /** The animated textures met while meshing the section in hand. */
    private static final it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet<net.minecraft.client.renderer.texture.TextureAtlasSprite> SECTION_SPRITES =
        new it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet<>();
    /** Whether each texture met so far is animated, asked of Sodium once. */
    private static final it.unimi.dsi.fastutil.objects.Reference2BooleanOpenHashMap<net.minecraft.client.renderer.texture.TextureAtlasSprite> SPRITE_ANIMATES =
        new it.unimi.dsi.fastutil.objects.Reference2BooleanOpenHashMap<>();
    /** Before 26.1.2, where a block's quads are not handed over as it is meshed: each block state's, found once. */
    private static final java.util.Map<
        Object,
        net.minecraft.client.renderer.texture.TextureAtlasSprite[]
    > STATE_SPRITES = new java.util.IdentityHashMap<>();
    private static final net.minecraft.client.renderer.texture.TextureAtlasSprite[] NO_SPRITES =
        new net.minecraft.client.renderer.texture.TextureAtlasSprite[0];
    private static net.minecraft.client.renderer.texture.TextureAtlasSprite lastSprite;

    /** Remembers a texture a quad of the section in hand is drawn with, if it is animated. */
    private static void noteSprite(
        net.minecraft.client.renderer.texture.TextureAtlasSprite sprite
    ) {
        // A block's quads mostly share one texture.
        if (sprite == lastSprite || !SodiumBridge.tracksSprites()) {
            return;
        }
        lastSprite = sprite;
        if (animates(sprite)) {
            SECTION_SPRITES.add(sprite);
        }
    }

    private static boolean animates(
        net.minecraft.client.renderer.texture.TextureAtlasSprite sprite
    ) {
        if (!SPRITE_ANIMATES.containsKey(sprite)) {
            SPRITE_ANIMATES.put(sprite, SodiumBridge.animates(sprite));
        }
        return SPRITE_ANIMATES.getBoolean(sprite);
    }

    //? >=1.21.11 && <26.1.2 {
    /*/^* Remembers the animated textures of a block state's model, read from the parts it is meshed from. ^/
	private static void noteSprites(BlockState state, List<BlockModelPart> parts) {
		if (!SodiumBridge.tracksSprites()) {
			return;
		}
		net.minecraft.client.renderer.texture.TextureAtlasSprite[] sprites = STATE_SPRITES.get(state);
		if (sprites == null) {
			java.util.Set<net.minecraft.client.renderer.texture.TextureAtlasSprite> found = new java.util.LinkedHashSet<>();
			for (BlockModelPart part : parts) {
				for (int side = -1; side < SIDES.length; side++) {
					for (net.minecraft.client.renderer.block.model.BakedQuad quad : part.getQuads(side < 0 ? null : SIDES[side])) {
						if (animates(quad.sprite())) {
							found.add(quad.sprite());
						}
					}
				}
			}
			sprites = found.isEmpty() ? NO_SPRITES : found.toArray(NO_SPRITES);
			STATE_SPRITES.put(state, sprites);
		}
		for (net.minecraft.client.renderer.texture.TextureAtlasSprite sprite : sprites) {
			SECTION_SPRITES.add(sprite);
		}
	}
	*/ //?} elif <1.21.11 {
    /*/^*
	 * Remembers the animated textures of a model, read from the model a block is meshed from. By model rather than by
	 * block state: a block may be meshed from more than one, see emissiveLayerOf.
	 ^/
	private static void noteSprites(BlockState state, BakedModel model, RandomSource random, long seed) {
		if (!SodiumBridge.tracksSprites()) {
			return;
		}
		net.minecraft.client.renderer.texture.TextureAtlasSprite[] sprites = STATE_SPRITES.get(model);
		if (sprites == null) {
			java.util.Set<net.minecraft.client.renderer.texture.TextureAtlasSprite> found = new java.util.LinkedHashSet<>();
			for (int side = -1; side < SIDES.length; side++) {
				random.setSeed(seed);
				for (net.minecraft.client.renderer.block.model.BakedQuad quad
						: model.getQuads(state, side < 0 ? null : SIDES[side], random)) {
					if (animates(quad.getSprite())) {
						found.add(quad.getSprite());
					}
				}
			}
			sprites = found.isEmpty() ? NO_SPRITES : found.toArray(NO_SPRITES);
			STATE_SPRITES.put(model, sprites);
		}
		for (net.minecraft.client.renderer.texture.TextureAtlasSprite sprite : sprites) {
			SECTION_SPRITES.add(sprite);
		}
	}

	private static final java.util.Map<BakedModel, Object> EMISSIVE_LAYERS = new java.util.IdentityHashMap<>();
	private static final Object NO_EMISSIVE_LAYER = new Object();

	/^*
	 * The glowing layer a mod draws over a block's model, or null where there is none. Before 1.21.4 a model cannot say
	 * that a part of it glows, so a mod that backports a block which does -- VanillaBackport's firefly bush and open
	 * eyeblossom -- wraps the block's model and draws a second one over it at full light, through Fabric's rendering
	 * API alone. The renderer meshes through vanilla's path, which only ever sees the first.
	 * <p>
	 * Found by the wrapper's shape rather than its name: among the models wrapped one inside another, the one kept in a
	 * field called emissiveModel. Looked for once per model.
	 ^/
	private static BakedModel emissiveLayerOf(BakedModel model) {
		//? fabric {
		Object found = EMISSIVE_LAYERS.get(model);
		if (found == null) {
			found = NO_EMISSIVE_LAYER;
			try {
				Object current = model;
				for (int depth = 0; current != null && depth < 8 && found == NO_EMISSIVE_LAYER; depth++) {
					Object next = null;
					for (Class<?> type = current.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
						for (java.lang.reflect.Field field : type.getDeclaredFields()) {
							if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
									|| !BakedModel.class.isAssignableFrom(field.getType())) {
								continue;
							}
							field.setAccessible(true);
							Object value = field.get(current);
							if (value == null || value == current) {
								continue;
							}
							if (field.getName().equals("emissiveModel")) {
								found = value;
							} else if (next == null) {
								next = value;
							}
						}
					}
					current = next;
				}
			} catch (ReflectiveOperationException | RuntimeException e) {
				found = NO_EMISSIVE_LAYER;
			}
			EMISSIVE_LAYERS.put(model, found);
		}
		return found == NO_EMISSIVE_LAYER ? null : (BakedModel) found;
		//?} else {
		/^return null;
		^///?}
	}
	*/ //?}
    /**
     * The queued sections that are on screen already and show a change the player just made or saw: a block placed or
     * broken, the light of a torch. They are rebuilt before anything else, all of them on the frame they are queued
     * where their budget allows, and their regions uploaded on that frame too: the chunk mesh shows the
     * same change within a frame or two, and foliage that follows it several frames later reads as light arriving late.
     * A section meshed for the first time is not urgent: nothing of it is on screen yet.
     */
    private static final LongOpenHashSet URGENT = new LongOpenHashSet();
    /** When each section's light last changed, and how soon after that another change counts as a light on the move. */
    private static final it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap LIGHT_CHANGED_AT =
        new it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap();
    private static final long LIGHT_STREAM_MILLIS = 3_000L;
    /** The regions the urgent sections rebuilt this frame are in. */
    private static final LongOpenHashSet URGENT_REGIONS = new LongOpenHashSet();

    /** Queues a section that is drawn already to be rebuilt ahead of the rest; returns whether it was not hurried yet. */
    private static boolean queueUrgent(long key) {
        Region region = REGIONS.get(regionKeyOf(key));
        if (region == null || region.sections[slotOf(key)] == null) {
            return false;
        }
        // Queued already as a rule: the route every request takes has seen it first, see onSectionAsked.
        DIRTY.add(key);
        return URGENT.add(key);
    }

    /**
     * Uploads the regions the urgent sections rebuilt this frame are in, on this frame, where they could not be written
     * over their own place: a section that changed size, which is a block placed or broken. Not once the rest of the
     * region's urgent sections are done, as it was: a torch set down on a bush changes the light of thirty sections
     * around it, and the bush stayed on screen, inside the torch, until the last of them was rebuilt -- half a second
     * with the ones out of view taking their turn.
     */
    private static void uploadUrgentRegions() {
        if (URGENT_REGIONS.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        var regions = URGENT_REGIONS.iterator();
        while (regions.hasNext()) {
            Region region = REGIONS.get(regions.nextLong());
            regions.remove();
            if (region != null && DIRTY_REGIONS.remove(region)) {
                uploadMeasured(region, now);
            }
        }
    }

    /**
     * Works out how long this frame's rebuilds may take: a quarter of the frame before it, never less than
     * {@link #REBUILD_BUDGET_NANOS} nor more than {@link #REBUILD_BUDGET_MAX_NANOS}. A slow frame is given more, so a
     * burst of rebuilds reaches the screen in about the same time whatever the frame rate, at the cost of the same share
     * of each frame while it lasts.
     */
    private static void measureFrame() {
        long now = System.nanoTime();
        long frame = lastFrameNanos == 0L ? 0L : now - lastFrameNanos;
        lastFrameNanos = now;
        frameBudgetNanos = Math.max(
            REBUILD_BUDGET_NANOS,
            Math.min(
                frame / REBUILD_BUDGET_FRAME_SHARE,
                REBUILD_BUDGET_MAX_NANOS
            )
        );
    }

    // ---- Measuring, written to the debug log only: how long a burst of rebuilds takes to reach the screen, from the
    // first section queued until nothing is left queued or waiting to be uploaded.
    /**
     * Whether the bursts are written to the ordinary log rather than the debug one, which not every loader keeps for a
     * mod: while a file of this name stands in the game's folder. For a report: make the file, play, send the log.
     */
    private static final boolean MEASURE_TO_LOG = java.nio.file.Files.exists(
        java.nio.file.Path.of("mc2-foliage-timing")
    );
    private static long burstStartNanos;
    private static int burstFrames;
    private static int burstLightQueued;
    private static int burstLightStreamed;
    private static int burstAsked;
    private static int burstPatched;
    private static int burstRebuiltHidden;
    private static long burstAnchorNanos;
    private static long burstWeightNanos;
    private static long burstPatchNanos;
    private static int burstPeakQueue;
    private static int burstRebuilt;
    private static long burstMeshNanos;
    private static long burstMeshMaxNanos;
    private static int burstUploads;
    private static long burstUploadNanos;
    private static long burstUploadMaxNanos;
    private static long burstWaitMaxMillis;

    /** Called once a frame, after the rebuilds and the upload. */
    private static void measureBurst() {
        if (!MEASURE_TO_LOG && !AllThingsFoliage.LOGGER.isDebugEnabled()) {
            return;
        }
        if (!DIRTY.isEmpty() || !DIRTY_REGIONS.isEmpty()) {
            if (burstStartNanos == 0L) {
                burstStartNanos = System.nanoTime();
            }
            burstFrames++;
            burstPeakQueue = Math.max(burstPeakQueue, DIRTY.size());
            return;
        }
        if (burstRebuilt > 0) {
            if (burstStartNanos == 0L) {
                // Queued, rebuilt and uploaded within one frame.
                burstStartNanos = System.nanoTime();
                burstFrames = 1;
            }
            String report = String.format(
                java.util.Locale.ROOT,
                "Foliage burst: %.0f ms over %d frames; %d sections rebuilt (%d queued by light, %d of them by a light " +
                    "on the move, %d asked for by another route, peak queue %d), " +
                    "meshing %.1f ms in all, %.1f ms the slowest; %d region uploads, %.1f ms in all, %.1f ms the " +
                    "slowest, longest wait before upload %d ms; %d sections written over their own, %.1f ms in all; %d of the sections rebuilt were out of view; of the meshing, " +
                    "%.1f ms setting each block up and %.1f ms on the vertices' weights",
                (System.nanoTime() - burstStartNanos) / 1.0E6D,
                burstFrames,
                burstRebuilt,
                burstLightQueued,
                burstLightStreamed,
                burstAsked,
                burstPeakQueue,
                burstMeshNanos / 1.0E6D,
                burstMeshMaxNanos / 1.0E6D,
                burstUploads,
                burstUploadNanos / 1.0E6D,
                burstUploadMaxNanos / 1.0E6D,
                burstWaitMaxMillis,
                burstPatched,
                burstPatchNanos / 1.0E6D,
                burstRebuiltHidden,
                burstAnchorNanos / 1.0E6D,
                burstWeightNanos / 1.0E6D
            );
            if (MEASURE_TO_LOG) {
                AllThingsFoliage.LOGGER.info(report);
            } else {
                AllThingsFoliage.LOGGER.debug(report);
            }
        }
        burstStartNanos = 0L;
        burstFrames = 0;
        burstLightQueued = 0;
        burstLightStreamed = 0;
        burstAsked = 0;
        burstPatched = 0;
        burstRebuiltHidden = 0;
        burstAnchorNanos = 0L;
        burstWeightNanos = 0L;
        burstPatchNanos = 0L;
        burstPeakQueue = 0;
        burstRebuilt = 0;
        burstMeshNanos = 0L;
        burstMeshMaxNanos = 0L;
        burstUploads = 0;
        burstUploadNanos = 0L;
        burstUploadMaxNanos = 0L;
        burstWaitMaxMillis = 0L;
    }

    private static void uploadMeasured(Region region, long now) {
        long start = System.nanoTime();
        burstWaitMaxMillis = Math.max(
            burstWaitMaxMillis,
            now - region.pendingSince
        );
        region.upload();
        long taken = System.nanoTime() - start;
        burstUploads++;
        burstUploadNanos += taken;
        burstUploadMaxNanos = Math.max(burstUploadMaxNanos, taken);
    }

    /**
     * Uploads the regions that are due, one at least and more only while they are quick: the one nearest the camera
     * first among those with nothing left in the queue, or that have waited too long. A region upload can be tens of
     * megabytes with a detailed resource pack or waving leaves, and uploading a region for every section built into it,
     * every frame, was most of what the renderer cost.
     * <p>
     * Nearest first, as sections are built, so the foliage at your feet is not held up behind a region further out; one
     * left waiting past {@link #REGION_UPLOAD_STARVE_MILLIS} goes first, so walking on never keeps the far ones off screen.
     */
    private static void uploadDueRegion(Vec3 camera) {
        uploadUrgentRegions();
        // One at least, and more while they are quick: a heavy one ends the frame's uploads.
        long budgetEnd =
            System.nanoTime() + frameBudgetNanos / UPLOAD_BUDGET_SHARE;
        while (uploadOneDueRegion(camera) && System.nanoTime() < budgetEnd) {
            // Each call uploads the next.
        }
    }

    /** Uploads the region that is due first, if any is, and returns whether one was. */
    private static boolean uploadOneDueRegion(Vec3 camera) {
        if (DIRTY_REGIONS.isEmpty()) {
            return false;
        }
        long now = System.currentTimeMillis();
        // The set keeps the order regions started waiting in, so the first is the one waiting longest.
        Region oldest = DIRTY_REGIONS.iterator().next();
        if (now - oldest.pendingSince >= REGION_UPLOAD_STARVE_MILLIS) {
            DIRTY_REGIONS.remove(oldest);
            uploadMeasured(oldest, now);
            return true;
        }
        REGIONS_WITH_QUEUED_SECTIONS.clear();
        for (long key : DIRTY) {
            REGIONS_WITH_QUEUED_SECTIONS.add(regionKeyOf(key));
        }
        Region nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Region region : DIRTY_REGIONS) {
            if (
                now - region.pendingSince < REGION_UPLOAD_MAX_WAIT_MILLIS &&
                REGIONS_WITH_QUEUED_SECTIONS.contains(region.key)
            ) {
                continue;
            }
            double distance = region.distanceSqTo(camera);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = region;
            }
        }
        if (nearest != null) {
            DIRTY_REGIONS.remove(nearest);
            uploadMeasured(nearest, now);
            return true;
        }
        return false;
    }

    private static boolean isVisible(
        Section section,
        Frustum frustum,
        Object sodium
    ) {
        // Only the sections whose chunk mesh on screen went without their foliage are drawn here; every
        // other one still has it in the chunk mesh.
        if (!GpuFoliageSplit.chunkMeshLeftFoliage(section.key)) {
            return false;
        }
        if (frustum != null && !frustum.isVisible(section.bounds)) {
            return false;
        }
        if (sodium == null) {
            return true;
        }
        // The box is kept inside the section so Sodium answers for this section and not its neighbours.
        BlockPos origin = section.origin;
        return SodiumBridge.isVisible(
            sodium,
            origin.getX() + 1,
            origin.getY() + 1,
            origin.getZ() + 1,
            origin.getX() + SECTION_SIZE - 1,
            origin.getY() + SECTION_SIZE - 1,
            origin.getZ() + SECTION_SIZE - 1
        );
    }

    /**
     * The most vertices one draw takes: what sixteen-bit indices can count. VulkanMod has the game's shared index buffer
     * always made of sixteen-bit indices, however many vertices it is asked for, so a longer draw counted past the end
     * and the foliage went missing -- all of it, while a region wholly in view was drawn as one run. A longer run is
     * drawn in pieces this long, each from where the last stopped; a whole number of quads.
     */
    private static final int MOST_VERTICES_A_DRAW = 65536;

    private static void addDraw(
        int regionIndex,
        int firstVertex,
        int vertexCount
    ) {
        // A run of sections holding only solid leaves has nothing cut out to draw, and the other way round.
        while (vertexCount > 0) {
            int piece = Math.min(vertexCount, MOST_VERTICES_A_DRAW);
            if (drawsSize + 3 > draws.length) {
                draws = Arrays.copyOf(draws, draws.length * 2);
            }
            draws[drawsSize++] = regionIndex;
            draws[drawsSize++] = firstVertex;
            draws[drawsSize++] = piece;
            firstVertex += piece;
            vertexCount -= piece;
        }
    }

    private static void addOpaqueDraw(
        int regionIndex,
        int firstVertex,
        int vertexCount
    ) {
        while (vertexCount > 0) {
            int piece = Math.min(vertexCount, MOST_VERTICES_A_DRAW);
            if (opaqueDrawsSize + 3 > opaqueDraws.length) {
                opaqueDraws = Arrays.copyOf(
                    opaqueDraws,
                    opaqueDraws.length * 2
                );
            }
            opaqueDraws[opaqueDrawsSize++] = regionIndex;
            opaqueDraws[opaqueDrawsSize++] = firstVertex;
            opaqueDraws[opaqueDrawsSize++] = piece;
            firstVertex += piece;
            vertexCount -= piece;
        }
    }

    private static int indexCountFor(int vertexCount) {
        return (vertexCount / 4) * 6;
    }

    /**
     * How far out, in chunks, the renderer draws the foliage for a render distance, as the config screen's
     * setting asks. It is read every frame, so changing either takes effect at once.
     */
    static int nearRadius(int renderDistance) {
        int radius = switch (FoliageSettings.gpuDistance()) {
            case PERFORMANCE -> pushRadius();
            case ADAPTIVE -> adaptiveRadius(renderDistance);
            case HALF -> renderDistance / 2;
            case FULL -> renderDistance;
        };
        return Math.max(MIN_NEAR_RADIUS, radius);
    }

    /**
     * Every chunk a plant pushed by an entity can be in. The player can stand anywhere in their own chunk, and a
     * ring of whole chunks around it reaches at least 16 blocks each way per chunk.
     */
    private static int pushRadius() {
        return (int) Math.ceil(
            (SwayConfig.INSTANCE.maxDistance + PUSH_REACH_PAST_RADIUS) /
                SECTION_SIZE
        );
    }

    /**
     * Half of the render distance up to 8 chunks, as it always was; past that a share shrinking steadily to a
     * quarter, which keeps the near area close to 5 chunks all the way from 10 to 20 and lets it reach no further
     * than 8 at 32.
     */
    private static int adaptiveRadius(int renderDistance) {
        if (renderDistance <= FULL_NEAR_SHARE_UP_TO) {
            return renderDistance / 2;
        }
        float share = Math.max(
            MIN_NEAR_SHARE,
            FULL_NEAR_SHARE -
                (renderDistance - FULL_NEAR_SHARE_UP_TO) *
                    NEAR_SHARE_DROP_PER_CHUNK
        );
        return Math.round(renderDistance * share);
    }

    /**
     * Keeps the near area on the player. When it moves or changes size, every section with foliage that crossed
     * its edge is meshed again, which is what hands that foliage over between the chunk mesh and this
     * renderer; sections that came inside are also queued here, so their geometry is built in time.
     * <p>
     * Only the old and the new area are walked, so even a teleport costs two squares of chunks.
     */
    private static void updateNearArea(Minecraft minecraft) {
        if (minecraft.player == null) {
            return;
        }
        int centreX = SectionPos.blockToSectionCoord(
            minecraft.player.getBlockX()
        );
        int centreZ = SectionPos.blockToSectionCoord(
            minecraft.player.getBlockZ()
        );
        int radius = nearRadius(minecraft.options.renderDistance().get());
        boolean hadArea = GpuFoliageSplit.hasArea();
        int oldX = GpuFoliageSplit.centreX();
        int oldZ = GpuFoliageSplit.centreZ();
        int oldRadius = GpuFoliageSplit.radius();
        if (
            hadArea && centreX == oldX && centreZ == oldZ && radius == oldRadius
        ) {
            return;
        }
        GpuFoliageSplit.setArea(centreX, centreZ, radius);
        if (hadArea) {
            for (
                int chunkX = oldX - oldRadius;
                chunkX <= oldX + oldRadius;
                chunkX++
            ) {
                for (
                    int chunkZ = oldZ - oldRadius;
                    chunkZ <= oldZ + oldRadius;
                    chunkZ++
                ) {
                    if (!GpuFoliageSplit.isNear(chunkX, chunkZ)) {
                        remeshColumn(minecraft, chunkX, chunkZ, false);
                    }
                }
            }
        }
        for (
            int chunkX = centreX - radius;
            chunkX <= centreX + radius;
            chunkX++
        ) {
            for (
                int chunkZ = centreZ - radius;
                chunkZ <= centreZ + radius;
                chunkZ++
            ) {
                boolean wasNear =
                    hadArea &&
                    Math.max(
                        Math.abs(chunkX - oldX),
                        Math.abs(chunkZ - oldZ)
                    ) <= oldRadius;
                if (!wasNear) {
                    remeshColumn(minecraft, chunkX, chunkZ, true);
                }
            }
        }
    }

    /**
     * The GPU renderer was switched off: the chunk mesh takes back the near foliage, and everything the renderer
     * held is let go. Switching it on again goes through {@link #updateNearArea} as on first use.
     */
    private static void deactivate(Minecraft minecraft) {
        active = false;
        // Sable's meshes take back the foliage of its sub-levels, as the chunk mesh does the world's below.
        GpuFoliageSplit.clearPlots();
        for (long key : PLOT_OWNERS.keySet()) {
            rebuildChunkMesh(
                minecraft,
                SectionPos.x(key),
                SectionPos.y(key),
                SectionPos.z(key)
            );
        }
        PLOT_OWNERS.clear();
        SUB_LEVELS.clear();
        if (GpuFoliageSplit.hasArea()) {
            int centreX = GpuFoliageSplit.centreX();
            int centreZ = GpuFoliageSplit.centreZ();
            int radius = GpuFoliageSplit.radius();
            // Cleared before any column is queued, so none of their builds can still find itself near.
            GpuFoliageSplit.clearArea();
            for (
                int chunkX = centreX - radius;
                chunkX <= centreX + radius;
                chunkX++
            ) {
                for (
                    int chunkZ = centreZ - radius;
                    chunkZ <= centreZ + radius;
                    chunkZ++
                ) {
                    remeshColumn(minecraft, chunkX, chunkZ, false);
                }
            }
        }
        discardAll();
    }

    /**
     * A whitelist group was switched on or off: its blocks change hands, the chunk mesh's or the renderer's, in the near
     * sections only -- further out the chunk mesh draws them either way, and nothing there has to be built again.
     * <p>
     * A section the renderer draws has its chunk mesh built again first, and is meshed again by the renderer as soon as
     * that mesh is on screen. A section it does not draw yet -- leaves and nothing else -- is queued like any new one,
     * and taken over the usual way once meshed.
     */
    public static void foliageListChanged() {
        GpuFoliageSplit.foliageChanged();
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (!active || level == null || !GpuFoliageSplit.hasArea()) {
            return;
        }
        int centreX = GpuFoliageSplit.centreX();
        int centreZ = GpuFoliageSplit.centreZ();
        int radius = GpuFoliageSplit.radius();
        // Switched on, the renderer takes the blocks over; switched off, it hands them back.
        boolean takingOver = WavingWhitelist.leavesWave();
        for (
            int chunkX = centreX - radius;
            chunkX <= centreX + radius;
            chunkX++
        ) {
            for (
                int chunkZ = centreZ - radius;
                chunkZ <= centreZ + radius;
                chunkZ++
            ) {
                LevelChunk chunk = level
                    .getChunkSource()
                    .getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                if (chunk == null || !GpuFoliageSplit.isNear(chunkX, chunkZ)) {
                    continue;
                }
                LevelChunkSection[] sections = chunk.getSections();
                for (int index = 0; index < sections.length; index++) {
                    int sectionY = level.getSectionYFromSectionIndex(index);
                    long key = SectionPos.asLong(chunkX, sectionY, chunkZ);
                    // Snow that may lie on the leaves changes hands with them, as the leaves do: between the chunk mesh and
                    // the renderer.
                    if (
                        !sections[index].maybeHas(
                            state ->
                                WavingWhitelist.listed(state.getBlock()) ||
                                WavingWhitelist.restsOnLeaves(state.getBlock())
                        )
                    ) {
                        // Blocks that follow the leaves, and hanging plants, stay the renderer's either way, only waving
                        // differently.
                        if (
                            sections[index].maybeHas(
                                state ->
                                    WavingWhitelist.followsLeaves(
                                        state.getBlock()
                                    ) ||
                                    HangingVineMultiblockBehavior.isHangingVine(
                                        state
                                    )
                            )
                        ) {
                            markIfHeld(key);
                        }
                        continue;
                    }
                    if (GpuFoliageSplit.isGpuReady(key)) {
                        if (takingOver) {
                            // The renderer goes first: the chunk mesh keeps the blocks until it holds them.
                            GpuFoliageSplit.awaitGroup(key);
                            DIRTY.add(key);
                        } else {
                            rebuildChunkMesh(
                                minecraft,
                                chunkX,
                                sectionY,
                                chunkZ
                            );
                            GpuFoliageSplit.remeshOnSwap(key);
                        }
                    } else {
                        DIRTY.add(key);
                    }
                }
            }
        }
    }

    /** Has the chunk mesher mesh again every section of a column that could hold foliage. */
    /**
     * Asks for a section's chunk mesh to be built again, by whichever route is in charge: vanilla's own, or
     * Sodium's, which does not follow vanilla's. Asking both costs nothing when only one is listening.
     */
    private static void rebuildChunkMesh(
        Minecraft minecraft,
        int sectionX,
        int sectionY,
        int sectionZ
    ) {
        //? >=26.2 {
        minecraft.levelExtractor.setSectionDirty(sectionX, sectionY, sectionZ);
        //?} else {
        /*minecraft.levelRenderer.setSectionDirty(sectionX, sectionY, sectionZ);
         */ //?}
        SodiumBridge.scheduleRebuild(sectionX, sectionY, sectionZ);
    }

    private static void remeshColumn(
        Minecraft minecraft,
        int chunkX,
        int chunkZ,
        boolean nowNear
    ) {
        ClientLevel level = minecraft.level;
        LevelChunk chunk = level
            .getChunkSource()
            .getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
        if (chunk == null) {
            return;
        }
        LevelChunkSection[] sections = chunk.getSections();
        for (int index = 0; index < sections.length; index++) {
            if (!mayHoldFoliage(sections[index])) {
                continue;
            }
            int sectionY = level.getSectionYFromSectionIndex(index);
            long key = SectionPos.asLong(chunkX, sectionY, chunkZ);
            if (!nowNear || GpuFoliageSplit.isGpuReady(key)) {
                // Leaving: the chunk mesh takes the foliage back. Entering, already on the GPU: it lets it go.
                rebuildChunkMesh(minecraft, chunkX, sectionY, chunkZ);
            } else {
                // Entering, not built yet: the chunk mesh keeps the foliage until it is, and is asked to let go then.
                DIRTY.add(key);
            }
        }
    }

    /** How far from the player sections are built and kept, in chunks. */
    private static int buildRadius() {
        return GpuFoliageSplit.hasArea()
            ? GpuFoliageSplit.radius() + BUILD_MARGIN
            : 0;
    }

    /**
     * Rebuilds the queued sections nearest the camera. Nearest first matters because a chunk load
     * storm can leave thousands of entries queued, and foliage appearing late at your feet is far
     * more noticeable than foliage appearing late further out.
     */
    private static void buildNearest(
        Minecraft minecraft,
        Vec3 camera,
        Frustum frustum
    ) {
        long[] nearest = new long[REBUILD_BUDGET];
        double[] distances = new double[REBUILD_BUDGET];
        Arrays.fill(distances, Double.MAX_VALUE);
        int found = 0;
        Object sodium = SodiumBridge.renderer();

        var pending = DIRTY.iterator();
        while (pending.hasNext()) {
            long key = pending.next();
            if (!withinRange(minecraft, key)) {
                pending.remove();
                URGENT.remove(key);
                continue;
            }
            // What is in view goes first, nearest first; what is not -- behind the player, or hidden as Sodium sees it --
            // only after all of that, with whatever the frame has left. A light carried through a forest asks for every
            // section around the player every tick, and half of them are behind: rebuilt in the order they came, they
            // held up the ones being looked at.
            // A sub-level's section is far away only by where its blocks are kept: it is among the first.
            boolean plot = PLOT_OWNERS.containsKey(key);
            double distance = plot ? 0.0D : distanceSqTo(key, camera);
            if (!plot && !inView(key, frustum, sodium)) {
                distance += OUT_OF_VIEW;
            }
            for (int slot = 0; slot < REBUILD_BUDGET; slot++) {
                if (distance < distances[slot]) {
                    int tail = REBUILD_BUDGET - slot - 1;
                    System.arraycopy(
                        distances,
                        slot,
                        distances,
                        slot + 1,
                        tail
                    );
                    System.arraycopy(nearest, slot, nearest, slot + 1, tail);
                    distances[slot] = distance;
                    nearest[slot] = key;
                    found = Math.min(found + 1, REBUILD_BUDGET);
                    break;
                }
            }
        }

        long begin = System.nanoTime();
        int hiddenBuilt = 0;
        long budgetEnd = begin + frameBudgetNanos;
        // A change the player just made or saw, in view, may take longer; see URGENT.
        long urgentEnd =
            begin +
            Math.max(
                URGENT_BUDGET_MIN_NANOS,
                Math.min(frameBudgetNanos * 2L, URGENT_BUDGET_MAX_NANOS)
            );
        for (int i = 0; i < found; i++) {
            long key = nearest[i];
            boolean urgent = distances[i] < OUT_OF_VIEW && URGENT.contains(key);
            // However few sections that is: a heavy resource pack makes a single one cost several milliseconds.
            long now = System.nanoTime();
            boolean hidden = distances[i] >= OUT_OF_VIEW;
            // One a frame of what is out of view, so it is not left behind for good, and no more: a carried light asks for
            // the sections behind the player again every tick, and rebuilding them as fast as they come is work nobody sees.
            if (hidden && hiddenBuilt > 0) {
                break;
            }
            if (i > 0 && now >= (urgent ? urgentEnd : budgetEnd)) {
                if (now >= urgentEnd) {
                    break;
                }
                continue;
            }
            DIRTY.remove(key);
            if (URGENT.remove(key)) {
                URGENT_REGIONS.add(regionKeyOf(key));
            }
            long start = System.nanoTime();
            rebuild(minecraft, minecraft.level, key);
            long taken = System.nanoTime() - start;
            burstRebuilt++;
            if (hidden) {
                hiddenBuilt++;
                burstRebuiltHidden++;
            }
            burstMeshNanos += taken;
            burstMeshMaxNanos = Math.max(burstMeshMaxNanos, taken);
        }
    }

    /** Added to the distance of a queued section that is out of view, so every one in view is rebuilt before it. */
    private static final double OUT_OF_VIEW = 1.0E12D;

    /** Whether a section is in the camera's view, and among what Sodium sees where Sodium draws the chunks. */
    private static boolean inView(long key, Frustum frustum, Object sodium) {
        int x = SectionPos.sectionToBlockCoord(SectionPos.x(key));
        int y = SectionPos.sectionToBlockCoord(SectionPos.y(key));
        int z = SectionPos.sectionToBlockCoord(SectionPos.z(key));
        if (
            frustum != null &&
            !frustum.isVisible(
                new AABB(
                    x - SWAY_MARGIN,
                    y - SWAY_MARGIN,
                    z - SWAY_MARGIN,
                    x + SECTION_SIZE + SWAY_MARGIN,
                    y + SECTION_SIZE + SWAY_MARGIN,
                    z + SECTION_SIZE + SWAY_MARGIN
                )
            )
        ) {
            return false;
        }
        return (
            sodium == null ||
            SodiumBridge.isVisible(
                sodium,
                x + 1,
                y + 1,
                z + 1,
                x + SECTION_SIZE - 1,
                y + SECTION_SIZE - 1,
                z + SECTION_SIZE - 1
            )
        );
    }

    private static double distanceSqTo(long key, Vec3 camera) {
        double x =
            SectionPos.sectionToBlockCoord(SectionPos.x(key)) + 8.0D - camera.x;
        double y =
            SectionPos.sectionToBlockCoord(SectionPos.y(key)) + 8.0D - camera.y;
        double z =
            SectionPos.sectionToBlockCoord(SectionPos.z(key)) + 8.0D - camera.z;
        return x * x + y * y + z * z;
    }

    /** Sections beyond the build radius are neither built nor kept. */
    private static boolean withinRange(Minecraft minecraft, long key) {
        if (minecraft.player == null) {
            return false;
        }
        // A sub-level's sections are kept for as long as it is followed, wherever its plot is.
        if (PLOT_OWNERS.containsKey(key)) {
            return true;
        }
        int limit = buildRadius();
        int dx =
            SectionPos.x(key) -
            SectionPos.blockToSectionCoord(minecraft.player.getBlockX());
        int dz =
            SectionPos.z(key) -
            SectionPos.blockToSectionCoord(minecraft.player.getBlockZ());
        int dy =
            SectionPos.y(key) -
            SectionPos.blockToSectionCoord(minecraft.player.getBlockY());
        return (
            Math.abs(dx) <= limit &&
            Math.abs(dz) <= limit &&
            Math.abs(dy) <= limit
        );
    }

    /** A region is kept while its section nearest the player is still within range. */
    private static boolean regionWithinRange(
        Minecraft minecraft,
        long regionKey
    ) {
        if (minecraft.player == null) {
            return false;
        }
        Region held = REGIONS.get(regionKey);
        if (held != null && held.subLevel != null) {
            return true;
        }
        int minX = SectionPos.x(regionKey) << REGION_WIDTH_SHIFT;
        int minY = SectionPos.y(regionKey) << REGION_HEIGHT_SHIFT;
        int minZ = SectionPos.z(regionKey) << REGION_WIDTH_SHIFT;
        return withinRange(
            minecraft,
            SectionPos.asLong(
                Mth.clamp(
                    SectionPos.blockToSectionCoord(
                        minecraft.player.getBlockX()
                    ),
                    minX,
                    minX + REGION_WIDTH - 1
                ),
                Mth.clamp(
                    SectionPos.blockToSectionCoord(
                        minecraft.player.getBlockY()
                    ),
                    minY,
                    minY + REGION_HEIGHT - 1
                ),
                Mth.clamp(
                    SectionPos.blockToSectionCoord(
                        minecraft.player.getBlockZ()
                    ),
                    minZ,
                    minZ + REGION_WIDTH - 1
                )
            )
        );
    }

    //? >=1.21.1 && <26.1.2 {
    /*/^*
	 * Appends the mod's per-vertex data as vanilla writes each vertex, for the versions where a block is
	 * tesselated into a vertex consumer rather than handed over one quad at a time.
	 * <p>
	 * Everything is passed straight through to the buffer, untouched. Only {@code addVertex} is of
	 * interest: it is where a vertex's final position is known, and one set of weights is written for it.
	 * The position arrives relative to the region, so the block's own corner is taken off again to get the
	 * height within the plant that a sway weight is measured against.
	 ^/
	private static final class FoliageVertexWriter implements VertexConsumer {

		private final VertexConsumer delegate;
		private final SwayAnchor anchor;
		private final BlockPos regionOrigin;
		private float blockY;
		/^* Set while a layer that glows is written: every vertex at full light, whatever the light where it stands. ^/
		boolean fullBright;
		/^* What goes above each vertex's light: the block's id for Beryl, where it is installed. See BerylFoliageShader. ^/
		int lightBits;
		/^* Whether Beryl turns the colours written now into plain light, which its shaders do again; see BerylBridge. ^/
		private final boolean straightened = BerylBridge.straightensColours();

		FoliageVertexWriter(VertexConsumer delegate, SwayAnchor anchor, BlockPos regionOrigin) {
			this.delegate = delegate;
			this.anchor = anchor;
			this.regionOrigin = regionOrigin;
		}

		/^* The height of the block about to be tesselated, relative to the region. ^/
		void beginBlock(float blockY) {
			this.blockY = blockY;
		}

		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			delegate.addVertex(x, y, z);
			long weightStart = MEASURE_TO_LOG ? System.nanoTime() : 0L;
			float localY = y - blockY;
			reserveWeights(WEIGHT_BYTES);
			weightScratch.putFloat(packCell(anchor.cellX - regionOrigin.getX(),
					anchor.cellY - regionOrigin.getY(),
					anchor.cellZ - regionOrigin.getZ()));
			weightScratch.putFloat(packWeights(anchor.waveAt(regionOrigin.getY() + y)
						* anchor.pinAt(regionOrigin.getX() + x, regionOrigin.getY() + y, regionOrigin.getZ() + z),
					anchor.pushAt(localY, regionOrigin.getY() + y), anchor.still, anchor.steady, anchor.exposureAt(regionOrigin.getX() + x,
							regionOrigin.getY() + y, regionOrigin.getZ() + z), anchor.leafHeldAt(regionOrigin.getX() + x,
							regionOrigin.getY() + y, regionOrigin.getZ() + z)));
			if (MEASURE_TO_LOG) {
				burstWeightNanos += System.nanoTime() - weightStart;
			}
			return this;
		}

		@Override
		public VertexConsumer setColor(int red, int green, int blue, int alpha) {
			delegate.setColor(red, green, blue, alpha);
			return this;
		}

		@Override
		public VertexConsumer setColor(int packed) {
			// VulkanMod draws a block for any vertex consumer but its own with red and blue changed over.
			if (VulkanBridge.swapsColours()) {
				packed = packed & 0xFF00FF00 | (packed & 0x00FF0000) >> 16 | (packed & 0x000000FF) << 16;
			}
			if (straightened) {
				packed = BerylBridge.beforeStraightening(packed);
			}
			delegate.setColor(packed);
			return this;
		}

		@Override
		public VertexConsumer setUv(float u, float v) {
			delegate.setUv(u, v);
			return this;
		}

		@Override
		public VertexConsumer setUv1(int u, int v) {
			delegate.setUv1(u, v);
			return this;
		}

		@Override
		public VertexConsumer setUv2(int u, int v) {
			if (fullBright) {
				delegate.setUv2(240 | lightBits, 240);
				return this;
			}
			delegate.setUv2(u | lightBits, v);
			return this;
		}

		@Override
		public VertexConsumer setNormal(float x, float y, float z) {
			if (normalsDropped) {
				delegate.setNormal(0.0F, 1.0F, 0.0F);
			} else {
				delegate.setNormal(x, y, z);
			}
			return this;
		}

		//? >=1.21.11 {
		@Override
		public VertexConsumer setLineWidth(float width) {
			delegate.setLineWidth(width);
			return this;
		}
		//?}
	}
	*/ //?} elif <1.21.1 {
    /*/^*
	 * Appends the mod's per-vertex data as vanilla writes each vertex, as the newer writer does, against the vertex
	 * consumer of before 1.21.1: a vertex is either written whole in one call, which is how a block's quads arrive,
	 * or element by element and then ended, and its weights are written as it is finished either way.
	 ^/
	private static final class FoliageVertexWriter implements VertexConsumer {

		private final VertexConsumer delegate;
		private final SwayAnchor anchor;
		private final BlockPos regionOrigin;
		private float blockY;
		/^* Set while a layer that glows is written: every vertex at full light, whatever the light where it stands. ^/
		boolean fullBright;
		/^* The position of a vertex being written element by element, kept until it is ended. ^/
		private float pendingX;
		private float pendingY;
		private float pendingZ;

		FoliageVertexWriter(VertexConsumer delegate, SwayAnchor anchor, BlockPos regionOrigin) {
			this.delegate = delegate;
			this.anchor = anchor;
			this.regionOrigin = regionOrigin;
		}

		/^* The block's id for Beryl, which is not there before 1.21.1 and so is never written; see the writer above. ^/
		int lightBits;

		/^* The height of the block about to be tesselated, relative to the region. ^/
		void beginBlock(float blockY) {
			this.blockY = blockY;
		}

		private void writeWeights(float x, float y, float z) {
			float localY = y - blockY;
			reserveWeights(WEIGHT_BYTES);
			weightScratch.putFloat(packCell(anchor.cellX - regionOrigin.getX(),
					anchor.cellY - regionOrigin.getY(),
					anchor.cellZ - regionOrigin.getZ()));
			weightScratch.putFloat(packWeights(anchor.waveAt(regionOrigin.getY() + y)
						* anchor.pinAt(regionOrigin.getX() + x, regionOrigin.getY() + y, regionOrigin.getZ() + z),
					anchor.pushAt(localY, regionOrigin.getY() + y), anchor.still, anchor.steady, anchor.exposureAt(regionOrigin.getX() + x,
							regionOrigin.getY() + y, regionOrigin.getZ() + z), anchor.leafHeldAt(regionOrigin.getX() + x,
							regionOrigin.getY() + y, regionOrigin.getZ() + z)));
		}

		@Override
		public void vertex(float x, float y, float z, float red, float green, float blue, float alpha,
				float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
			delegate.vertex(x, y, z, red, green, blue, alpha, u, v, overlay, light, normalX, normalY, normalZ);
			writeWeights(x, y, z);
		}

		@Override
		public VertexConsumer vertex(double x, double y, double z) {
			delegate.vertex(x, y, z);
			pendingX = (float) x;
			pendingY = (float) y;
			pendingZ = (float) z;
			return this;
		}

		@Override
		public VertexConsumer color(int red, int green, int blue, int alpha) {
			delegate.color(red, green, blue, alpha);
			return this;
		}

		@Override
		public VertexConsumer uv(float u, float v) {
			delegate.uv(u, v);
			return this;
		}

		@Override
		public VertexConsumer overlayCoords(int u, int v) {
			delegate.overlayCoords(u, v);
			return this;
		}

		@Override
		public VertexConsumer uv2(int u, int v) {
			if (fullBright) {
				delegate.uv2(240, 240);
				return this;
			}
			delegate.uv2(u, v);
			return this;
		}

		@Override
		public VertexConsumer normal(float x, float y, float z) {
			delegate.normal(x, y, z);
			return this;
		}

		@Override
		public void endVertex() {
			delegate.endVertex();
			writeWeights(pendingX, pendingY, pendingZ);
		}

		@Override
		public void defaultColor(int red, int green, int blue, int alpha) {
			delegate.defaultColor(red, green, blue, alpha);
		}

		@Override
		public void unsetDefaultColor() {
			delegate.unsetDefaultColor();
		}
	}
	*/ //?}

    private static boolean warnedForeignVertexFormat;

    /**
     * Staging memory for a region upload, reused between them and grown when a region needs more. Render
     * thread only, and only live for the length of one upload: nothing holds on to it afterwards.
     */
    private static ByteBuffer uploadStaging;

    private static ByteBuffer uploadScratch(int bytes) {
        if (uploadStaging == null || uploadStaging.capacity() < bytes) {
            if (uploadStaging != null) {
                MemoryUtil.memFree(uploadStaging);
            }
            uploadStaging = MemoryUtil.memAlloc(Math.max(bytes, 64 * 1024));
        }
        return uploadStaging.clear().limit(bytes);
    }

    //? >=1.21.1 && <1.21.11 {
    /*/^* The same, for the versions that upload a region as a mesh. It is left empty by every upload. ^/
	private static ByteBufferBuilder uploadBuilder;

	private static ByteBufferBuilder uploadBuilder() {
		if (uploadBuilder == null) {
			uploadBuilder = new ByteBufferBuilder(64 * 1024);
		}
		return uploadBuilder;
	}
	*/ //?} elif <1.21.1 {
    /*/^* The same, as a builder: before 1.21.1 a mesh only comes out of one. It is left empty by every upload. ^/
	private static BufferBuilder uploadBuilder;

	private static BufferBuilder uploadBuilder() {
		if (uploadBuilder == null) {
			uploadBuilder = new BufferBuilder(64 * 1024);
		}
		return uploadBuilder;
	}
	*/ //?}

    private static boolean warnedWeightCountMismatch;

    private static void warnWeightCountMismatch(int vertices, int weights) {
        if (warnedWeightCountMismatch) {
            return;
        }
        warnedWeightCountMismatch = true;
        AllThingsFoliage.LOGGER.warn(
            "Foliage was meshed with {} vertices but {} sway weights, so the GPU renderer " +
                "left it to the chunk mesh. Another mod is writing vertices the mod cannot see.",
            vertices,
            weights
        );
    }

    private static void warnForeignVertexFormat(Object format) {
        if (warnedForeignVertexFormat) {
            return;
        }
        warnedForeignVertexFormat = true;
        AllThingsFoliage.LOGGER.warn(
            "Foliage was meshed in {} rather than the block format, so the GPU renderer " +
                "left it to the chunk mesh. Another mod is widening the mod's vertex buffer.",
            format
        );
    }

    private static void reserveWeights(int bytes) {
        if (weightScratch.remaining() >= bytes) {
            return;
        }
        ByteBuffer grown = MemoryUtil.memAlloc(
            Math.max(
                weightScratch.capacity() * 2,
                weightScratch.position() + bytes
            )
        );
        weightScratch.flip();
        grown.put(weightScratch);
        MemoryUtil.memFree(weightScratch);
        weightScratch = grown;
    }

    /**
     * Meshes a section. Iris widens any buffer asked for the block format while a shader pack is loaded: that is left
     * to happen when the renderer draws through the pack, whose programs read that wider format, and is kept from
     * happening otherwise, when these vertices are written for a pipeline of the mod's own.
     */
    private static void rebuild(
        Minecraft minecraft,
        ClientLevel level,
        long key
    ) {
        boolean wasSkipping = meshedForShaderPack
            ? IrisVertexExtension.allow()
            : IrisVertexExtension.begin();
        try {
            meshSection(minecraft, level, key);
        } finally {
            IrisVertexExtension.end(wasSkipping);
        }
    }

    private static void meshSection(
        Minecraft minecraft,
        ClientLevel level,
        long key
    ) {
        if (!mayHoldFoliage(level, key)) {
            removeSection(key);
            return;
        }
        normalsDropped = false;
        //? <1.21.11 {
        /*// A sub-level's faces are shaded as it is drawn, where Sable shades its own so; see drawSubLevels.
		unshaded = subLevelShading && PLOT_OWNERS.containsKey(key);
		*/ //?}
        if (modelRenderer == null) {
            // Same arguments the section compiler passes, so the geometry matches it exactly.
            //? >=26.1.2 {
            // The second argument is whether faces against a neighbour hiding them are culled, as the section compiler
            // always has them.
            modelRenderer = new ModelBlockRenderer(
                minecraft.options.ambientOcclusion().get(),
                true,
                minecraft.getBlockColors()
            );
            uncullingRenderer = new ModelBlockRenderer(
                minecraft.options.ambientOcclusion().get(),
                false,
                minecraft.getBlockColors()
            );
            //?} else {
            /*modelRenderer = new ModelBlockRenderer(minecraft.getBlockColors());
             */ //?}
        }
        if (vertexScratch == null) {
            //? >=1.21.1 {
            vertexScratch = new ByteBufferBuilder(
                VERTEX_BYTES * 4 * INITIAL_SCRATCH_QUADS
            );
            //?} else {
            /*vertexScratch = new BufferBuilder(VERTEX_BYTES * 4 * INITIAL_SCRATCH_QUADS);
             */ //?}
            weightScratch = MemoryUtil.memAlloc(
                WEIGHT_BYTES * 4 * INITIAL_SCRATCH_QUADS
            );
        }
        vertexScratch.clear();
        weightScratch.clear();

        BlockPos origin = new BlockPos(
            SectionPos.sectionToBlockCoord(SectionPos.x(key)),
            SectionPos.sectionToBlockCoord(SectionPos.y(key)),
            SectionPos.sectionToBlockCoord(SectionPos.z(key))
        );
        BlockPos regionOrigin = regionOrigin(regionKeyOf(key));
        WindShelter shelter =
            shelterActive &&
            minecraft.player != null &&
            withinReach(
                origin,
                minecraft.player.getX(),
                minecraft.player.getZ(),
                weatherWindReach()
            )
                ? new WindShelter(level, origin)
                : null;
        // Vertices are written relative to the region, so every section in it can share its buffers.
        int offsetX = origin.getX() - regionOrigin.getX();
        int offsetY = origin.getY() - regionOrigin.getY();
        int offsetZ = origin.getZ() - regionOrigin.getZ();

        //? >=26.2 {
        BufferBuilder builder = new BufferBuilder(
            vertexScratch,
            PrimitiveTopology.QUADS,
            DefaultVertexFormat.BLOCK
        );
        //?} elif >=1.21.1 {
        /*BufferBuilder builder = new BufferBuilder(vertexScratch, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
         */ //?} else {
        /*BufferBuilder builder = vertexScratch;
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
		*/ //?}
        // Vanilla writes the standard attributes; alongside each quad we append the sway weight of its
        // four vertices, so both bindings stay in step without touching its output.
        SwayAnchor anchor = new SwayAnchor();
        SECTION_SPRITES.clear();
        lastSprite = null;
        // A tree's leaves are sheltered corner by corner, on their own terms; see WindShelter.cornerExposure.
        anchor.fieldShelter =
            shelter == null ? null : new WindShelter(level, origin, true);
        //? >=26.1.2 {
        BlockQuadOutput output = (x, y, z, quad, instance) -> {
            builder.putBlockBakedQuad(x, y, z, quad, instance);
            noteSprite(quad.materialInfo().sprite());
            reserveWeights(WEIGHT_BYTES * 4);
            float cell = packCell(
                anchor.cellX - regionOrigin.getX(),
                anchor.cellY - regionOrigin.getY(),
                anchor.cellZ - regionOrigin.getZ()
            );
            for (int vertex = 0; vertex < 4; vertex++) {
                float localY = quad.position(vertex).y();
                float worldY = regionOrigin.getY() + y + localY;
                weightScratch.putFloat(cell);
                float worldX =
                    regionOrigin.getX() + x + quad.position(vertex).x();
                float worldZ =
                    regionOrigin.getZ() + z + quad.position(vertex).z();
                weightScratch.putFloat(
                    packWeights(
                        anchor.waveAt(worldY) *
                            anchor.pinAt(worldX, worldY, worldZ),
                        anchor.pushAt(localY, worldY),
                        anchor.still,
                        anchor.steady,
                        anchor.exposureAt(worldX, worldY, worldZ),
                        anchor.leafHeldAt(worldX, worldY, worldZ)
                    )
                );
            }
        };

        BlockStateModelSet models = minecraft
            .getModelManager()
            .getBlockStateModelSet();
        //?} else {
        /*// Before 26.1.2 a block is tesselated into a vertex consumer rather than handed over quad by quad,
		// so the weights are appended as each vertex is written instead of once per quad.
		FoliageVertexWriter output = new FoliageVertexWriter(builder, anchor, regionOrigin);
		PoseStack poseStack = new PoseStack();
		RandomSource random = RandomSource.create();
		BlockModelShaper models = minecraft.getModelManager().getBlockModelShaper();
		*/ //?}
        // Blocks are read from the section itself: going through the level would look the chunk up again
        // for every one of its 4096 blocks.
        LevelChunkSection blocks = level
            .getChunk(SectionPos.x(key), SectionPos.z(key))
            .getSection(level.getSectionIndexFromSectionY(SectionPos.y(key)));
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos neighbour = new BlockPos.MutableBlockPos();
        // Leaves drawn solid, as the game's Fast leaves are, go last, in a second walk over the section, so they are one
        // stretch at the end of its vertices that a pipeline keeping every pixel draws. The game picks them as its section
        // compiler does (ModelBlockRenderer.forceOpaque): every LeavesBlock. Their faces against one another are hidden by
        // the culling renderer as the chunk mesh's are, since LeavesBlock hides them while its leaves are not cut out.
        boolean opaqueLeaves = WavingWhitelist.opaqueLeaves();
        int cutoutVertices = 0;
        for (int walk = 0; walk < 2; walk++) {
            if (walk == 1) {
                cutoutVertices = weightScratch.position() / WEIGHT_BYTES;
                if (!opaqueLeaves) {
                    break;
                }
            }
            for (int dx = 0; dx < SECTION_SIZE; dx++) {
                for (int dy = 0; dy < SECTION_SIZE; dy++) {
                    for (int dz = 0; dz < SECTION_SIZE; dz++) {
                        BlockState state = blocks.getBlockState(dx, dy, dz);
                        // Snow! Real Magic's snow holding a plant: the plant is meshed, the snow stays with the chunk mesh.
                        float raised = 0.0F;
                        boolean inSnow = false;
                        pos.set(
                            origin.getX() + dx,
                            origin.getY() + dy,
                            origin.getZ() + dz
                        );
                        // Snow lying on a leaf, while the leaves wave, is meshed itself: it moves with the leaf, as a field.
                        if (
                            !GpuFoliageSplit.isFoliage(state) &&
                            !GpuFoliageSplit.restsOnWavingLeaves(
                                level,
                                pos,
                                state
                            )
                        ) {
                            if (!SnowRealMagicCompat.isSnowBlock(state)) {
                                continue;
                            }
                            BlockState plant = SnowRealMagicCompat.plantIn(
                                level,
                                pos,
                                state
                            );
                            if (
                                plant == null ||
                                !GpuFoliageSplit.isFoliage(plant)
                            ) {
                                continue;
                            }
                            state = plant;
                            raised = SnowRealMagicCompat.liftOf(plant);
                            inSnow = true;
                        }
                        if (
                            opaqueLeaves &&
                            state.getBlock() instanceof LeavesBlock !=
                                (walk == 1)
                        ) {
                            continue;
                        }
                        pos.set(
                            origin.getX() + dx,
                            origin.getY() + dy,
                            origin.getZ() + dz
                        );
                        long anchorStart = MEASURE_TO_LOG
                            ? System.nanoTime()
                            : 0L;
                        anchor.prepare(state, pos, level);
                        // Measured at the anchor, so every block of a tall plant bends as one piece; plants stand in a column,
                        // so the anchor shares the block's.
                        anchor.exposure =
                            shelter == null
                                ? 1.0F
                                : shelter.exposureAt(
                                      pos.getX(),
                                      anchor.cellY,
                                      pos.getZ(),
                                      anchor.length
                                  );
                        anchor.still = false;
                        // A plant under Snow! Real Magic's snow -- held in it, or the top half of one that is -- is weighed
                        // down. Either still bends as far as any other when pushed.
                        boolean laden =
                            inSnow ||
                            SnowRealMagicCompat.standsOnSnow(level, pos, state);
                        anchor.waving = laden ? SNOW_LADEN_SWAY : 1.0F;
                        // A block of the waving whitelist waves as hard as its group says, and leaves as a field, which the wind
                        // reaches wherever it is so that neighbouring blocks never part.
                        anchor.waving *= WavingWhitelist.intensityOf(
                            state.getBlock()
                        );
                        anchor.field = WavingWhitelist.wavesAsField(
                            state.getBlock()
                        );
                        anchor.restY = WavingWhitelist.restsOnLeaves(
                            state.getBlock()
                        )
                            ? pos.getY()
                            : SwayAnchor.NOT_RESTING;
                        anchor.steady = anchor.field;
                        // Weighed down in calm weather only: the weather's wind moves it nearly as far as any other plant. A
                        // field under snow keeps the one weight for both, as its neighbours it must not part from.
                        if (laden && !anchor.field) {
                            anchor.waving *= SNOW_LADEN_WIND / SNOW_LADEN_SWAY;
                            anchor.still = true;
                        }
                        // A field's reach is worked out at each corner instead, the same for every block sharing it.
                        if (anchor.field) {
                            anchor.exposure = 1.0F;
                        }
                        // A vine goes block by block: with the leaves behind it, still against anything else, and hanging free
                        // below both; see SwayAnchor.hangVine. Any other hanging plant moves with the leaves while it literally
                        // hangs from leaves that wave -- marked still and steady at once, which nothing else is, the wind reaching
                        // it as far as it reaches the leaves at the same corner (see exposureAt) -- and is left a plant as any
                        // other otherwise.
                        anchor.hangsOnLeaves = false;
                        anchor.pushless = false;
                        anchor.vineFree = false;
                        anchor.vineOnLeaves = false;
                        if (
                            anchor.hanging &&
                            WavingWhitelist.followsLeaves(state.getBlock())
                        ) {
                            anchor.hangVine(level, pos, state);
                        } else if (
                            anchor.hanging &&
                            WavingWhitelist.leavesWave() &&
                            anchor.hangOnLeaves(level)
                        ) {
                            anchor.hangsOnLeaves = true;
                            anchor.still = true;
                            anchor.steady = true;
                            anchor.exposure = 1.0F;
                        }
                        if (MEASURE_TO_LOG) {
                            burstAnchorNanos += System.nanoTime() - anchorStart;
                        }
                        // Another mod may draw the plant lower than its block, as the chunk mesh shows it.
                        float lift =
                            TerrainSlabsCompat.offsetY(level, pos, state) +
                            raised;
                        //? iris {
                        if (meshedForShaderPack) {
                            // The pack reads which block each vertex belongs to, and where its centre is, as it does on the
                            // chunk mesh; Iris writes them as the vertices go in.
                            IrisCompat.beginBlock(
                                builder,
                                state,
                                offsetX + dx,
                                offsetY + dy,
                                offsetZ + dz
                            );
                        }
                        //?}
                        //? >=26.1.2 {
                        (anchor.field &&
                        partsFromNeighbour(
                            level,
                            blocks,
                            dx,
                            dy,
                            dz,
                            pos,
                            neighbour
                        )
                            ? uncullingRenderer
                            : modelRenderer
                        ).tesselateBlock(
                            output,
                            offsetX + dx,
                            offsetY + dy + lift,
                            offsetZ + dz,
                            level,
                            pos,
                            state,
                            SnowRealMagicCompat.snowyVariant(
                                GpuFoliageSplit.modelFor(
                                    state,
                                    models.get(state)
                                ),
                                state,
                                level,
                                pos,
                                inSnow
                            ),
                            state.getSeed(pos)
                        );
                        //?} else {
                        /*//? >=1.21.11 {
						random.setSeed(state.getSeed(pos));
						List<BlockModelPart> parts = GpuFoliageSplit
								.modelFor(state, models.getBlockModel(state))
								.collectParts(random);
						noteSprites(state, parts);
						// The pose carries what the newer call took as arguments: where the block sits in
						// its region. The writer subtracts it again to recover each vertex's height in
						// its own block, which is what a sway weight is measured against.
						poseStack.pushPose();
						poseStack.translate(offsetX + dx, offsetY + dy + lift, offsetZ + dz);
						output.beginBlock(offsetY + dy + lift);
						output.lightBits = BerylBridge.materialBits(state);
						modelRenderer.tesselateBlock(level, parts, state, pos, poseStack, output,
								!(anchor.field && partsFromNeighbour(level, blocks, dx, dy, dz, pos, neighbour)),
								OverlayTexture.NO_OVERLAY);
						poseStack.popPose();
						//?} else {
						/^poseStack.pushPose();
						poseStack.translate(offsetX + dx, offsetY + dy + lift, offsetZ + dz);
						output.beginBlock(offsetY + dy + lift);
						output.lightBits = BerylBridge.materialBits(state);
						tesselate(level, SnowRealMagicCompat.snowyVariant(GpuFoliageSplit.modelFor(state,
								models.getBlockModel(state)), state, level, pos, inSnow), state, pos, poseStack, output, random,
								!(anchor.field && partsFromNeighbour(level, blocks, dx, dy, dz, pos, neighbour)));
						poseStack.popPose();
						// A glowing layer a mod draws over the block's own model, at full light, as that mod draws it.
						BakedModel emissive = emissiveLayerOf(models.getBlockModel(state));
						if (emissive != null) {
							poseStack.pushPose();
							poseStack.translate(offsetX + dx, offsetY + dy + lift, offsetZ + dz);
							output.beginBlock(offsetY + dy + lift);
							output.fullBright = true;
							tesselate(level, emissive, state, pos, poseStack, output, random, true);
							output.fullBright = false;
							poseStack.popPose();
						}
						^///?}
						*/ //?}
                    }
                }
            }
        }

        //? iris {
        if (meshedForShaderPack) {
            IrisCompat.endBlock(builder);
        }
        //?}
        //? >=1.21.1 {
        MeshData mesh = builder.build();
        //?} else {
        /*BufferBuilder.RenderedBuffer mesh = builder.endOrDiscardIfEmpty();
         */ //?}
        if (mesh == null) {
            removeSection(key);
            return;
        }
        //? >=1.21.1 {
        try (mesh) {
            //?} else {
            /*// Before 1.21.1 the mesh is not closeable: it hands its memory back to the builder when released.
		try {
		*/ //?}
            // The mesh says how many vertices it holds and in what format, rather than dividing its size by
            // the block format's stride: another mod may widen the buffer behind our back -- Iris does, for
            // shader packs -- and the vertices would then be read at a stride they were not written at. A
            // section that comes back in another format is left to the chunk mesh instead of drawn as noise.
            //? iris {
            VertexFormat expected = meshedForShaderPack
                ? IrisCompat.shaderPackMeshFormat()
                : DefaultVertexFormat.BLOCK;
            //?} else {
            /*VertexFormat expected = DefaultVertexFormat.BLOCK;
             */ //?}
            if (!expected.equals(mesh.drawState().format())) {
                warnForeignVertexFormat(mesh.drawState().format());
                removeSection(key);
                return;
            }
            int vertexBytes = expected.getVertexSize();
            ByteBuffer meshVertices = mesh.vertexBuffer();
            int vertexCount = mesh.drawState().vertexCount();
            weightScratch.flip();
            // One set of weights was written for each vertex as it was written. If the two ever disagree,
            // every vertex past the first difference would be read from the wrong place, so the section is
            // left to the chunk mesh rather than drawn wrongly.
            if (weightScratch.remaining() != vertexCount * WEIGHT_BYTES) {
                warnWeightCountMismatch(
                    vertexCount,
                    weightScratch.remaining() / WEIGHT_BYTES
                );
                removeSection(key);
                return;
            }
            // Interleaved here, once, rather than each time the section's region is uploaded.
            int stride = vertexBytes + WEIGHT_BYTES;
            int meshBase = meshVertices.position();
            ByteBuffer data = MemoryUtil.memAlloc(vertexCount * stride);
            for (int vertex = 0; vertex < vertexCount; vertex++) {
                data.put(
                    vertex * stride,
                    meshVertices,
                    meshBase + vertex * vertexBytes,
                    vertexBytes
                );
                data.put(
                    vertex * stride + vertexBytes,
                    weightScratch,
                    vertex * WEIGHT_BYTES,
                    WEIGHT_BYTES
                );
            }
            Section section = new Section(
                key,
                origin,
                data,
                vertexCount,
                vertexCount - cutoutVertices,
                stride
            );
            section.animatedSprites = SECTION_SPRITES.isEmpty()
                ? null
                : SECTION_SPRITES.toArray(
                      new net.minecraft.client.renderer.texture.TextureAtlasSprite[0]
                  );
            section.sheltered = shelter != null;
            section.listGeneration = GpuFoliageSplit.listGeneration();
            putSection(key, section);
            //? >=1.21.1 {
        }
        //?} else {
        /*} finally {
			mesh.release();
		}
		*/ //?}
    }

    //? <1.21.11 {
    /*/^*
	 * Vanilla's {@code tesselateBlock}, step for step: the same choice of smooth or flat lighting, and the same
	 * random offset for plants that have one.
	 * <p>
	 * It is not called itself because, before 1.21.11, both Fabric's renderer and Sodium take it over for any
	 * model that is not a plain vanilla one, and send it through the rendering API instead -- where Sway would
	 * bake its own sway into geometry the shader is about to move again. On NeoForge, Sway hooks it too. The two methods it picks between are
	 * left alone by both, and read the model's plain quads, which Sway's wrapper hands over untouched.
	 ^/
	private static void tesselate(ClientLevel level, BakedModel model, BlockState state, BlockPos pos,
			PoseStack poseStack, VertexConsumer output, RandomSource random, boolean cull) {
		//? neoforge {
		/^// NeoForge lets a model decide, and only falls back to the block's light where it does not.
		boolean smoothLighting = Minecraft.useAmbientOcclusion()
				&& switch (model.useAmbientOcclusion(state, net.neoforged.neoforge.client.model.data.ModelData.EMPTY, null)) {
					case TRUE -> true;
					case DEFAULT -> state.getLightEmission(level, pos) == 0;
					case FALSE -> false;
				};
		^///?} elif forge {
		/^// Forge's rule, as its own tesselateBlock applies it: the block's light where it is placed, and the model asked
		// with no layer in particular, as a block outside a chunk is.
		boolean smoothLighting = Minecraft.useAmbientOcclusion() && state.getLightEmission(level, pos) == 0
				&& model.useAmbientOcclusion(state, null);
		^///?} else {
		boolean smoothLighting = Minecraft.useAmbientOcclusion() && state.getLightEmission() == 0
				&& model.useAmbientOcclusion();
		//?}
		Vec3 offset = state.getOffset(level, pos);
		poseStack.translate(offset.x, offset.y, offset.z);
		long seed = state.getSeed(pos);
		noteSprites(state, model, random, seed);
		// A sub-level's section is meshed with no shade in it; see UnshadedLevel.
		net.minecraft.world.level.BlockAndTintGetter lit = unshaded ? new UnshadedLevel(level) : level;
		if (smoothLighting) {
			modelRenderer.tesselateWithAO(lit, model, state, pos, poseStack, output, cull, random, seed,
					OverlayTexture.NO_OVERLAY);
		} else {
			modelRenderer.tesselateWithoutAO(lit, model, state, pos, poseStack, output, cull, random, seed,
					OverlayTexture.NO_OVERLAY);
		}
		normalsDropped = false;
	}

	/^*
	 * The level as a block of a sub-level is meshed from it where Sable shades its sub-levels as they are drawn: every
	 * face at full shade, as Sable's own meshes have them. The block renderer asks for a quad's shade just before it
	 * writes the quad, saying whether the quad takes any, which is noted for the vertex writer: one that takes none
	 * is written looking up.
	 ^/
	private static final class UnshadedLevel implements net.minecraft.world.level.BlockAndTintGetter {

		private final ClientLevel level;

		UnshadedLevel(ClientLevel level) {
			this.level = level;
		}

		@Override
		public float getShade(Direction direction, boolean shade) {
			normalsDropped = !shade;
			return 1.0F;
		}

		@Override
		public net.minecraft.world.level.lighting.LevelLightEngine getLightEngine() {
			return level.getLightEngine();
		}

		@Override
		public int getBlockTint(BlockPos pos, net.minecraft.world.level.ColorResolver resolver) {
			return level.getBlockTint(pos, resolver);
		}

		@Override
		public net.minecraft.world.level.block.entity.BlockEntity getBlockEntity(BlockPos pos) {
			return level.getBlockEntity(pos);
		}

		@Override
		public BlockState getBlockState(BlockPos pos) {
			return level.getBlockState(pos);
		}

		@Override
		public net.minecraft.world.level.material.FluidState getFluidState(BlockPos pos) {
			return level.getFluidState(pos);
		}

		@Override
		public int getHeight() {
			return level.getHeight();
		}

		@Override
		public int getMinBuildHeight() {
			return level.getMinBuildHeight();
		}
	}
	*/ //?}
}
//?}
