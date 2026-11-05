// How the GPU renderer moves a plant: the wind, the pushes from entities and the ease off at the edge of its area.
//
// Shared word for word by the mod's own vertex shader and, while a shader pack is loaded, by the copy of the pack's
// vertex shader the mod draws foliage with, so the two can never drift apart. Everything is prefixed so it cannot
// clash with a shader pack's own names; it reads the two uniform blocks below, which each shader declares itself.
//
// mc2_FoliageSway:       float mc2_SwayIntensity; float mc2_CalmSway; vec4 mc2_SwayEdge; vec4 mc2_Weather;
// mc2_FoliageInteraction: int mc2_CellCount; vec4 mc2_CellBoundsMin; vec4 mc2_CellBoundsMax;
//                         vec4 mc2_CellPosition[MC2_MAX_CELLS]; vec4 mc2_CellForce[MC2_MAX_CELLS];
//
// SwayIntensity multiplies MC2_SWAY_STRENGTH, from 0.5 to 2.0, for the calm sway and the weather wind alike; CalmSway
// is 1 while plants sway in calm weather and 0 while they stand still until the weather wind moves them. SwayEdge is the edge of the area the renderer draws,
// relative to the camera: x and y its lowest x and z, z and w its highest. Weather is the level's rain in x and its
// thunder in y, each from 0 to 1 and eased in and out by the game; z how far from the camera it blows, in blocks,
// and w over how many blocks it eases off there. The cells are Sway's pushes on the plants near the player this frame, one per plant by its anchor block, already followed through a spring by the mod so
// plants lean in smoothly and rock back when let go: xyz the anchor block's corner relative to the camera, and for
// the force xy the push along x and z and z the most the plant may be pushed.

// GameTime is the fraction of a Minecraft day, so it advances from 0 to 1 over 24000 ticks -- twenty real minutes.
// Scaling by two pi times a whole number of cycles gives a visible rhythm (one sway every three seconds) and keeps
// the motion seamless when it wraps back to zero.
const float MC2_SWAY_SPEED = 2513.2741;

// The phase follows world position, with the camera's block wrapped to 4096 so the numbers stay precise far from
// the origin. The scale fits a whole number of cycles into that span -- 230, and 299 for the second axis, which
// runs 1.3 times faster -- so the wrap never shows as a seam.
const int MC2_PHASE_WRAP = 4095;
const float MC2_SWAY_SCALE = 6.2831853 * 230.0 / 4096.0;
const float MC2_SWAY_STRENGTH = 0.33;
// How hard a tree's leaves wave, which a block hanging on them moves with: WavingWhitelist's LEAVES intensity.
const float MC2_LEAVES_WAVE = 0.20;

// ---- Rain's wind -------------------------------------------------------------------------------------------------
// While it rains the calm sway gives way to a wind blowing west, the way the clouds drift. Every amount below is in
// units of the calm sway's reach -- the player's intensity times MC2_SWAY_STRENGTH, on a plant's freest vertex -- so
// the slider scales the wind as it does the sway. Raising the lean or the storm makes plants reach further out of their
// sections; GpuFoliageRenderer.SWAY_MARGIN has to grow with them, or plants at the edge of the screen get culled.
const vec2 MC2_WIND_DIRECTION = vec2(-1.0, 0.0);
// How far a plant leans downwind, and how much of that lean comes and goes with the gusts: at 0.4 a plant leans 60%
// of the way between gusts and all the way under one.
const float MC2_WIND_LEAN = 2.5;
const float MC2_WIND_GUST = 0.4;
// How much further a storm blows everything: the lean, the gusts, the flutter and the tug.
const float MC2_STORM_STRENGTH = 2.0;
// A plant flutters across the wind and tugs along it, each to its own rhythm, as if the wind were about to tear it
// out. The tug should stay below the lean between gusts, MC2_WIND_LEAN * (1 - MC2_WIND_GUST), or plants swing back
// upwind.
const float MC2_WIND_FLUTTER = 0.2;
const float MC2_WIND_TUG = 0.2;
// Speeds are two pi times how many times a day each repeats, a whole number so nothing jumps when the day wraps:
// 400 is one cycle every three seconds. Gusts roll over the ground downwind once every four seconds, a little under
// eighteen blocks apart.
const float MC2_GUST_SPEED = 6.2831853 * 300.0;
const float MC2_FLUTTER_SPEED = 6.2831853 * 1600.0;
const float MC2_TUG_SPEED = 6.2831853 * 2100.0;

// The wind eases off over this many blocks before the edge, so it does not stop dead where the chunk mesh takes
// over.
const float MC2_EDGE_EASE_BLOCKS = 6.0;

// A last say over how hard entities push, on top of what Sway already decides. At 1.0 a plant bends exactly as far
// as it would in the chunk mesh; the mod already sends a stronger push while it draws the foliage.
const float MC2_INTERACT_STRENGTH = 1.0;
// How much of its sway a plant keeps while an entity pushes it.
const float MC2_PUSHED_SWAY = 0.25;
// How hard a push has to be before a plant keeps only MC2_PUSHED_SWAY; lighter pushes calm it partly, so the sway
// eases down as a plant is pushed and back up as the push fades.
const float MC2_PUSH_FOR_CALM = 0.2;
// How much of its calm sway a plant weighed down by snow keeps, of the weight it is meshed with, which is what the
// weather's wind moves it by: GpuFoliageRenderer's SNOW_LADEN_SWAY over its SNOW_LADEN_WIND. Such a plant is marked
// still without steady.
const float MC2_LADEN_CALM = 0.25;

// How much of the wind a plant keeps, by how far its anchor block sits inside the edge. It rises along a curve that
// starts and ends gently, so the plants by the edge barely stir and no one plant moves much more than the next; and
// continuously, so the edge following the player changes each plant's sway smoothly rather than in steps. Measured
// from the anchor, so the whole plant eases off together; outside the edge, while the chunk mesh takes a plant back,
// it keeps none.
float mc2_edgeEase(vec3 cell) {
    vec2 centre = cell.xz + 0.5;
    float inside = min(min(centre.x - mc2_SwayEdge.x, mc2_SwayEdge.z - centre.x),
                       min(centre.y - mc2_SwayEdge.y, mc2_SwayEdge.w - centre.y));
    return smoothstep(0.0, MC2_EDGE_EASE_BLOCKS, inside);
}

// The cells are read through these two, so a shader whose renderer takes no arrays in a uniform block can lay them out
// another way and say how they are read itself: it defines MC2_NO_UNIFORM_ARRAYS, and both functions, before this file.
#ifdef MC2_NO_UNIFORM_ARRAYS
#else
vec4 mc2_cellPosition(int i) {
    return mc2_CellPosition[i];
}

vec4 mc2_cellForce(int i) {
    return mc2_CellForce[i];
}
#endif

// Sway's push on the plant whose anchor block is at this cell, or none. Every vertex of a plant reads the same push,
// so the whole plant leans as one piece.
vec2 mc2_cellPush(vec3 cell) {
    if (mc2_CellCount == 0
            || any(lessThan(cell, mc2_CellBoundsMin.xyz - 0.5))
            || any(greaterThan(cell, mc2_CellBoundsMax.xyz + 0.5))) {
        return vec2(0.0);
    }
    for (int i = 0; i < mc2_CellCount; i++) {
        if (all(lessThan(abs(mc2_cellPosition(i).xyz - cell), vec3(0.5)))) {
            vec4 pushed = mc2_cellForce(i);
            vec2 force = pushed.xy;
            float limit = pushed.z;
            float amount = length(force);
            return amount > limit ? force * (limit / amount) : force;
        }
    }
    return vec2(0.0);
}

// The mod's two per-vertex values each travel packed whole into one float, because a vertex attribute is read for
// every vertex of every frame and eight bytes beat twenty. A float holds whole numbers up to 2^24 exactly, which is
// three bytes' worth, and both values fit inside that.
//
// SwayCell: the plant's anchor block relative to the region, a byte per axis, biased so an anchor just outside its
// region still fits. Every vertex of a plant carries the same one.
vec3 mc2_unpackCell(float bits) {
    float x = floor(bits / 65536.0);
    float rest = bits - x * 65536.0;
    float y = floor(rest / 256.0);
    return vec3(x, y, rest - y * 256.0) - 64.0;
}

// SwayWeights: how freely this vertex sways, ten bits, how far a push moves it, eight, whether the plant stands still in
// calm weather, one, whether a push leaves its sway as it is, one, and how much of rain's wind reaches the plant, four.
// The first is the wind's own curve; the second is Sway's, so a pushed plant bends the same whoever draws it; the third is
// set alone for a plant weighed down by snow, which keeps MC2_LADEN_CALM of its calm sway and all of its wind; the fourth for a tree's leaves,
// which wave together, and for what hangs on them; the fifth is 1 in the open and falls under roofs, in caves and behind walls upwind, as the
// mod works out when it meshes the plant. A block hanging on leaves keeps three of those four bits for the wind and gives
// the lowest to whether the leaves wave where the vertex is; mc2_sway takes z apart for it. x: the wind weight, y: the
// push, z: the wind's reach, w: how much of the calm
// sway the plant has, 1 or 0. Whether a push leaves the block swaying is read apart, by mc2_unpackSteady: no function
// here hands a value back through a parameter, which not every renderer's shader converter keeps (VulkanMod's drops
// the qualifier, and the value with it).
vec4 mc2_unpackWeights(float bits) {
    float exposure = bits - floor(bits / 16.0) * 16.0;
    float rest = (bits - exposure) / 16.0;
    float steady = rest - floor(rest / 2.0) * 2.0;
    rest = (rest - steady) / 2.0;
    float still = rest - floor(rest / 2.0) * 2.0;
    float weights = (rest - still) / 2.0;
    float wave = floor(weights / 256.0);
    return vec4(wave / 1023.0, (weights - wave * 256.0) / 255.0, exposure / 15.0, 1.0 - still);
}

// SwayWeights' fifth bit: 1 for a block a push leaves swaying, 0 for a plant.
float mc2_unpackSteady(float bits) {
    float rest = floor(bits / 16.0);
    return rest - floor(rest / 2.0) * 2.0;
}

// How far rain's wind moves a vertex that may sway this far, along x and z: the plant leans downwind, gusts rolling
// over the ground push it further, and it flutters across the wind and tugs along it. The tug never outweighs the lean,
// so a plant shakes in place rather than swinging back upwind. An entity pushing the plant adds to the lean rather than
// taking its place, and only calms the shaking, by shake, as it calms the sway.
vec2 mc2_wind(vec3 world, float phase, float gameTime, float reach, float shake) {
    float strength = reach * mix(1.0, MC2_STORM_STRENGTH, mc2_Weather.y);
    // Measured against the wind's direction, so the gusts travel the way it blows.
    float along = -dot(world.xz, MC2_WIND_DIRECTION);
    float gust = 0.5 + 0.5 * sin(along * MC2_SWAY_SCALE + gameTime * MC2_GUST_SPEED);
    vec2 across = vec2(-MC2_WIND_DIRECTION.y, MC2_WIND_DIRECTION.x);
    float flutter = sin(phase + gameTime * MC2_FLUTTER_SPEED) * MC2_WIND_FLUTTER * shake;
    // The second axis's phase, so the tug keeps to whole cycles across the wrapped span too.
    float tug = sin(phase * 1.3 + gameTime * MC2_TUG_SPEED) * MC2_WIND_TUG * shake;
    float lean = MC2_WIND_LEAN * (1.0 - MC2_WIND_GUST + MC2_WIND_GUST * gust);
    return (MC2_WIND_DIRECTION * (lean + tug) + across * flutter) * strength;
}

// The calm sway and rain's wind of a vertex that may sway this far, as far as the wind reaches it (exposure) and the
// calm sway is its to have (calmShare); rain's reach eases off by where easeAt is. Returns the offset along x and z
// in xy, and the wind's own part of it in zw.
vec4 mc2_motion(vec3 world, float phase, float gameTime, float reach, float shake, float calmShare, float exposure,
                vec3 easeAt) {
    vec2 sway = vec2(sin(phase), cos(phase * 1.3) * 0.7) * reach * shake * mc2_CalmSway * calmShare;
    // The game eases rain in and out over a few seconds, and the sway turns into the wind along with it -- as far as
    // the wind reaches the plant: under a roof or behind a wall it keeps its calm sway.
    float rain = mc2_Weather.x * exposure
            * (1.0 - smoothstep(mc2_Weather.z - mc2_Weather.w, mc2_Weather.z, length(easeAt.xz + 0.5)));
    vec2 wind = rain > 0.0 ? mc2_wind(world, phase, gameTime, reach, shake) * rain : vec2(0.0);
    return vec4(mix(sway, vec2(0.0), rain) + wind, wind);
}

// Moves one vertex. pos is relative to the camera; modelOffset is its region's corner relative to the camera, which
// SwayCell is measured from; the camera's block, that block's corner minus the camera and the game time are the
// values vanilla hands every shader. Returns the moved position, still relative to the camera.
vec3 mc2_sway(vec3 pos, vec3 modelOffset, ivec3 cameraBlockPos, vec3 cameraOffset, float gameTime,
              float swayCell, float swayWeights) {
    // Adding the camera back gives a world position, so each plant keeps its own rhythm while the player moves
    // instead of the pattern sliding along with them.
    vec3 world = pos + vec3(cameraBlockPos & MC2_PHASE_WRAP) - cameraOffset;

    // Phase varies with world position so neighbouring plants never move in lockstep.
    float phase = (world.x + world.z) * MC2_SWAY_SCALE + gameTime * MC2_SWAY_SPEED;
    vec3 cell = mc2_unpackCell(swayCell) + modelOffset;
    vec2 force = mc2_cellPush(cell);
    float steady = mc2_unpackSteady(swayWeights);
    vec4 weights = mc2_unpackWeights(swayWeights);
    // A block hanging on leaves -- a vine while they wave -- is marked still and steady at once, which nothing else is.
    // It sways as the plant it is, all of its calm sway, and moves as the leaves do on top of that, all over, at the
    // leaves' own weight; the wind reaches both as far as it reaches the leaves at the same corner.
    bool hanging = steady > 0.5 && weights.w < 0.5;
    float calmShare = hanging ? 1.0 : mix(MC2_LADEN_CALM, 1.0, weights.w);
    float exposure = weights.z;
    // It keeps three of the wind's bits, and the last says whether the leaves wave where this vertex is or stand still
    // against a block holding them, as the leaf beside it was told: so it moves exactly as that leaf.
    float leafWaves = 1.0;
    if (hanging) {
        float bits = floor(weights.z * 15.0 + 0.5);
        leafWaves = bits - floor(bits / 2.0) * 2.0;
        exposure = floor(bits / 2.0) / 7.0;
    }
    // Where the edge's ease and rain's reach are measured: at a plant's anchor, so the whole plant eases together, or at
    // the vertex itself for the leaves, so the corners neighbouring leaves share move as one and they never part.
    vec3 easeAt = steady > 0.5 && !hanging ? pos - 0.5 : cell;
    // Every vertex of a plant reads the same force, so the whole plant calms together.
    float calm = smoothstep(0.0, MC2_PUSH_FOR_CALM, length(force));
    // How far this vertex may sway at all, and how much of its shaking a push leaves it.
    float reach = weights.x * MC2_SWAY_STRENGTH * mc2_SwayIntensity * mc2_edgeEase(easeAt);
    // A steady block never calms: the blocks hanging on the leaves are pushed, and calming them alone would part them
    // from the leaves, which nothing pushes.
    float shake = steady > 0.5 ? 1.0 : mix(1.0, MC2_PUSHED_SWAY, calm);
    vec2 push = force * weights.y * MC2_INTERACT_STRENGTH;
    vec4 motion = mc2_motion(world, phase, gameTime, reach, shake, calmShare, exposure, easeAt);
    vec2 offset = motion.xy;
    vec2 wind = motion.zw;
    if (hanging) {
        // Where it hangs its own sway is nothing, and it moves exactly as the leaf beside it.
        vec3 atVertex = pos - 0.5;
        float leafReach = MC2_LEAVES_WAVE * leafWaves * MC2_SWAY_STRENGTH * mc2_SwayIntensity * mc2_edgeEase(atVertex);
        vec4 leafMotion = mc2_motion(world, phase, gameTime, leafReach, 1.0, 1.0, exposure, atVertex);
        offset += leafMotion.xy;
        wind += leafMotion.zw;
    }
    pos.xz += offset + push;
    // A plant bends rather than slides: the further its tip is pushed, the lower it sits. Up to a block of push the
    // drop grows with its square, as a bending stalk would; past that it only grows in step, so a strong push leans a
    // plant over instead of sinking it into the ground. The wind leans plants over the same way.
    float pushed = length(push + wind);
    pos.y -= min(pushed * pushed, pushed) * 0.5;
    return pos;
}
