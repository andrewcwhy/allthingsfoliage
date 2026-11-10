#ifndef ATF_FOLIAGE_UNIFORMS_GLSL
#define ATF_FOLIAGE_UNIFORMS_GLSL

// Settings the player changes in game, and the pushes on the plants near the player, written by the mod into
// buffers so they apply on the next frame. See sway.glsl for what each holds.
//
// Shared by the modern shaders the mod draws foliage with: its own, and the copy of Sodium's chunk shader it builds
// while Sodium draws the chunks.
layout(std140) uniform FoliageSway {
    float atf_SwayIntensity;
    float atf_CalmSway;
    vec4 atf_SwayEdge;
    vec4 atf_Weather;
};

#define ATF_MAX_CELLS 128
layout(std140) uniform FoliageInteraction {
    int atf_CellCount;
    vec4 atf_CellBoundsMin;
    vec4 atf_CellBoundsMax;
    vec4 atf_CellPosition[ATF_MAX_CELLS];
    vec4 atf_CellForce[ATF_MAX_CELLS];
};

#endif
