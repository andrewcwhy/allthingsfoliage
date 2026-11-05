#version 330
#extension GL_ARB_separate_shader_objects : require

// Vanilla's core/block vertex shader as 26.3 writes it, with a sway added before projection: the modern
// counterpart of foliage.vsh, for the shading language that includes rather than imports and names a
// location for every input and output.
//
// The displacement is driven by the weights the mod computes per vertex from Sway's anchor for that
// plant: 0 where it is held and 1 at its free end. The sway itself lives in sway.glsl, shared with the
// other shader sets.

#include <minecraft:globals.glsl>
#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>
#include <minecraft:sample_lightmap.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
layout(location = 3) in ivec2 UV2;
// The mod's two per-vertex values; see sway.glsl.
layout(location = 4) in float SwayCell;
layout(location = 5) in float SwayWeights;

#include <mc2_interactivefoliage:foliage_uniforms.glsl>
#include <mc2_interactivefoliage:sway.glsl>

uniform sampler2D Sampler2;

layout(location = 0) out float sphericalVertexDistance;
layout(location = 1) out float cylindricalVertexDistance;
layout(location = 2) out vec4 vertexColor;
layout(location = 3) out vec2 texCoord0;

void main() {
    // Relative to the camera: ModelOffset is the region's corner relative to it.
    vec3 pos = mc2_sway(Position + ModelOffset, ModelOffset, CameraBlockPos, CameraOffset, GameTime,
            SwayCell, SwayWeights);

    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    sphericalVertexDistance = fog_spherical_distance(pos);
    cylindricalVertexDistance = fog_cylindrical_distance(pos);
    vertexColor = Color * sample_lightmap(Sampler2, UV2);
    texCoord0 = UV0;
}
