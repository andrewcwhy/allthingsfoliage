#ifndef MC2_FOLIAGE_UNIFORMS_GLSL
#define MC2_FOLIAGE_UNIFORMS_GLSL

// Settings the player changes in game, and the pushes on the plants near the player, written by the mod into
// buffers so they apply on the next frame. See sway.glsl for what each holds.
//
// Shared by the modern shaders the mod draws foliage with: its own, and the copy of Sodium's chunk shader it builds
// while Sodium draws the chunks.
layout(std140) uniform FoliageSway {
    float mc2_SwayIntensity;
    float mc2_CalmSway;
    vec4 mc2_SwayEdge;
    vec4 mc2_Weather;
};

#define MC2_MAX_CELLS 128
layout(std140) uniform FoliageInteraction {
    int mc2_CellCount;
    vec4 mc2_CellBoundsMin;
    vec4 mc2_CellBoundsMax;
    vec4 mc2_CellPosition[MC2_MAX_CELLS];
    vec4 mc2_CellForce[MC2_MAX_CELLS];
};

#endif
