// What foliage does on one of Sable's sub-levels alone -- a felled tree, a ship -- on top of sway.glsl, which it comes
// after: a sub-level turns and moves, and the world's down, its wind and where its faces look are no longer the
// plot's own. Everything here does nothing in the world itself, where mc2_SubLevelDown is all zero. The renderer sets
// the uniforms as it draws a sub-level (GpuFoliageRenderer.drawSubLevels); the shader declares what sway.glsl reads.
//
// mc2_SubLevelDown: the world's down as the plot sees it, a unit vector; all zero while the world itself is drawn.
// mc2_SubLevelWind: the way rain's wind blows as the plot sees it, along its x and z -- west, turned as the sub-level
// is. It is shorter the more the wind blows along the plants rather than across them, which bends them less.
// mc2_SubLevelHang: where a plant's tip hangs to, in blocks, as the plot sees it: toward the world's down, and behind
// the sub-level as it moves, followed through a spring by the mod so the foliage lags, swings past and settles.
uniform vec3 mc2_SubLevelDown;
uniform vec2 mc2_SubLevelWind;
uniform vec3 mc2_SubLevelHang;

// sway.glsl's own direction for the wind gives way to this one in the copy of it a sub-level's shader is given: west
// in the world itself, as it has there.
vec2 mc2_subLevelWindDirection() {
    return mc2_SubLevelDown == vec3(0.0) ? vec2(-1.0, 0.0) : mc2_SubLevelWind;
}

// Keeps the sway and the wind level. sway.glsl moves a vertex along the plot's x and z, which on a sub-level lying on
// its side are not both level any more: one of them is the world's up and down, and a plant swaying along it bobbed
// like a spring. What of the offset runs along the world's up is taken out, so a plant only ever sways sideways as
// the world sees it. A sub-level standing nearly straight keeps most of its sway both ways, and from a third of the
// way over all of that part is gone: so nothing changes at once as a sub-level starts to lean. The copy of sway.glsl
// a sub-level's shader is given moves its vertices through this; a push is left as it is, since whoever walks over a
// plant lying down does press it toward the ground.
vec2 mc2_subLevelLevel(vec2 offset) {
    vec2 up = -mc2_SubLevelDown.xz;
    float leaning = dot(up, up);
    if (leaning == 0.0) {
        return offset;
    }
    return offset - up * (dot(offset, up) / max(leaning, 0.25));
}

// How hard rain's wind shakes a plant it blows straight down onto, in units of the calm sway's reach.
const float MC2_SUB_LEVEL_BUFFET = 0.5;

// How far what nothing pushes hangs -- a tree's leaves, and every block that only waves -- in units of how far the
// tip of a plant that is pushed does: the same, so a sub-level's foliage hangs, lags and swings back as one.
const float MC2_SUB_LEVEL_LOOSE = 0.2;

// What a sub-level adds to a vertex sway.glsl has moved.
//
// It hangs: by how far a push moves it, which is Sway's own curve along the plant, so a plant droops the way it bends
// when pushed. What nothing pushes has no such curve, and hangs whole wherever it sways at all: a tree's leaves move
// together, corners neighbouring leaves share alike, and stay where a block holds them still -- against the trunk --
// so the canopy leans from its wood rather than leaving it. A block hanging on leaves hangs as the leaf beside it.
// Only the part of the hang across the plant bends it: standing still it is all along the stalk and nothing moves,
// and upside down the same.
//
// And where rain's wind meets the plant head on -- blowing along the stalk, with nothing left of it across to lean
// the plant by -- the plant shakes a little all the same, to its own rhythm, so that the wind is seen to reach it.
vec3 mc2_subLevelBend(vec3 pos, float swayCell, float swayWeights, float gameTime) {
    if (mc2_SubLevelDown == vec3(0.0)) {
        return pos;
    }
    vec4 weights = mc2_unpackWeights(swayWeights);
    float headOn = mc2_Weather.x * weights.z * (1.0 - min(length(mc2_SubLevelWind), 1.0));
    if (headOn > 0.0) {
        float reach = weights.x * MC2_SWAY_STRENGTH * mc2_SwayIntensity * mix(1.0, MC2_STORM_STRENGTH, mc2_Weather.y);
        // Every vertex of a plant carries the same cell, so the plant shakes as one, and no two alike. A tree's
        // leaves and what hangs on them go by where the vertex is instead, as they sway: the corners neighbouring
        // leaves share move together, and the leaves never part.
        vec3 world = pos + vec3(mc2_CameraBlockPos & MC2_PHASE_WRAP) - mc2_CameraOffset;
        float phase = mc2_unpackSteady(swayWeights) > 0.5
                ? (world.x + world.y + world.z) * MC2_SWAY_SCALE * 4.0 : swayCell * 0.37;
        vec2 shake = vec2(sin(phase + gameTime * MC2_FLUTTER_SPEED), sin(phase * 1.3 + gameTime * MC2_TUG_SPEED));
        pos.xz += mc2_subLevelLevel(shake) * (MC2_SUB_LEVEL_BUFFET * reach * headOn);
    }
    // It bends rather than slides, as under a push: the further the tip hangs, the nearer the stalk it sits. What
    // nothing pushes has no stalk to bend, and only slides.
    float hung = length(mc2_SubLevelHang.xz * weights.y);
    pos.y -= min(hung * hung, hung) * 0.5;
    float loose = weights.y * 255.0 < 0.5 && weights.x * 1023.0 >= 0.5 ? 1.0 : 0.0;
    if (mc2_unpackSteady(swayWeights) > 0.5 && weights.w < 0.5) {
        // Hanging on leaves: the lowest of the wind's bits says whether the leaf beside it sways; see mc2_sway.
        float bits = floor(weights.z * 15.0 + 0.5);
        loose = bits - floor(bits / 2.0) * 2.0;
    }
    pos.xz += mc2_SubLevelHang.xz * max(weights.y, loose * MC2_SUB_LEVEL_LOOSE);
    return pos;
}

// ---- Only for a shader that reads a normal and shades by it; left out of the others as the shader is put together.
#ifdef MC2_SUB_LEVEL_SHADE
// A sub-level's faces are shaded by where they look once it has turned: its quaternion, all zero for the world itself,
// and the shade of a face looking down, up, along z and along x. A quad that takes no shade -- a plant's -- is meshed
// looking up, so it is shaded as the top of the block it grows on.
uniform vec4 mc2_SubLevelTurn;
uniform vec4 mc2_SubLevelShade;

float mc2_subLevelShade(vec3 normal) {
    if (mc2_SubLevelTurn == vec4(0.0)) {
        return 1.0;
    }
    vec3 axis = mc2_SubLevelTurn.xyz;
    vec3 turned = normal + 2.0 * cross(axis, cross(axis, normal) + mc2_SubLevelTurn.w * normal);
    return pow(clamp(-turned.y, 0.0, 1.0), 3.0) * mc2_SubLevelShade.x
            + pow(clamp(turned.y, 0.0, 1.0), 3.0) * mc2_SubLevelShade.y
            + turned.z * turned.z * mc2_SubLevelShade.z + turned.x * turned.x * mc2_SubLevelShade.w;
}
#endif
