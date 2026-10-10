#version 150

uniform sampler2D BeforeTex;
uniform sampler2D DepthBefore;
uniform sampler2D DepthAfter;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 uv = texCoord;
    float depthAfter = texture(DepthAfter, uv).r;

    // In Minecraft first person pass, depth is cleared to 1.0 before hands render.
    // Therefore, any pixel where depthAfter < 0.9999 belongs to the rendered hands/items.
    float mask = (depthAfter < 0.9999) ? 1.0 : 0.0;
    fragColor = vec4(mask, mask, mask, mask);
}
