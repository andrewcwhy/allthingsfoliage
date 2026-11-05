package io.github.andrewcwhy.allthingsfoliage.gpu;

//? >=1.20.1 {

import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Puts what foliage does on one of Sable's sub-levels alone into a vertex shader that sways it: sublevel.glsl, after
 * sway.glsl, which it builds on. A sub-level turns and moves, so the world's down, its wind and where its faces look
 * are not the plot's own; see that file, and {@link SableBridge} for what a sub-level is.
 * <p>
 * No preprocessor line is left in what comes out: some of the shaders it goes into are parsed before any compiler
 * sees them, by a parser that takes none.
 * <p>
 * sway.glsl is every version's and is not written for it: the two things of its own that have to give way -- the
 * wind's direction, and the line that moves a vertex by its sway -- are changed in the copy handed over here, where
 * they are found worded as they are today. Where one is not, the wind keeps blowing the plot's west, or the sway
 * keeps to the plot's own axes, and the rest works the same.
 */
final class SubLevelFoliageShader {

    /** sway.glsl's direction for the wind, as it is worded there, and the name it goes by. */
    private static final String WIND_DIRECTION =
        "const vec2 MC2_WIND_DIRECTION = vec2(-1.0, 0.0);";
    private static final String WIND_DIRECTION_NAME = "MC2_WIND_DIRECTION";
    /** Where sway.glsl moves a vertex by its sway, its wind and its push, and the same with the first two kept level. */
    private static final String SWAY_MOVE = "pos.xz += offset + push;";
    private static final String LEVEL_MOVE =
        "pos.xz += mc2_subLevelLevel(offset) + push;";
    /** What marks off the part of sublevel.glsl that shades a vertex by its normal. */
    private static final String SHADE_BEGIN = "#ifdef MC2_SUB_LEVEL_SHADE\n";
    private static final String SHADE_END = "#endif\n";
    /** Around the call that sways a vertex: lets it hang toward the world's down after it. */
    static final String BEND_OPEN = "mc2_subLevelBend(";
    static final String BEND_CLOSE = ", SwayCell, SwayWeights, mc2_GameTime)";
    /** What sway.glsl's copy calls of sublevel.glsl, which comes after it. */
    private static final String DECLARED_AHEAD =
        "vec2 mc2_subLevelWindDirection();\nvec2 mc2_subLevelLevel(vec2 offset);\n";

    private SubLevelFoliageShader() {}

    /**
     * sway.glsl and sublevel.glsl after it, for a shader that declares the sway's own inputs itself.
     *
     * @param shaded whether the shader also shades a vertex by its normal, which needs one to read
     */
    static String withSway(String sway, boolean shaded) {
        String unixSway = sway.replace("\r\n", "\n");
        if (unixSway.contains(WIND_DIRECTION)) {
            unixSway = unixSway
                .replace(WIND_DIRECTION, "")
                .replace(WIND_DIRECTION_NAME, "mc2_subLevelWindDirection()");
        }
        // The sway and the wind are kept level on their way to the vertex; a push is not.
        unixSway = unixSway.replace(SWAY_MOVE, LEVEL_MOVE);
        // The part that shades by a normal is marked off in the file, and kept or dropped here.
        String source = read();
        int from = source.indexOf(SHADE_BEGIN);
        int to = source.indexOf(SHADE_END, Math.max(from, 0));
        if (from >= 0 && to >= 0) {
            source =
                source.substring(0, from) +
                (shaded
                    ? source.substring(from + SHADE_BEGIN.length(), to)
                    : "") +
                source.substring(to + SHADE_END.length());
        }
        return DECLARED_AHEAD + unixSway + '\n' + source;
    }

    private static String read() {
        String path =
            "/assets/" +
            AllThingsFoliage.MOD_ID +
            "/shaders/include/sublevel.glsl";
        try (
            InputStream in = SubLevelFoliageShader.class.getResourceAsStream(
                path
            )
        ) {
            if (in == null) {
                throw new IOException("Missing " + path);
            }
            return new String(
                in.readAllBytes(),
                StandardCharsets.UTF_8
            ).replace("\r\n", "\n");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
//?}
