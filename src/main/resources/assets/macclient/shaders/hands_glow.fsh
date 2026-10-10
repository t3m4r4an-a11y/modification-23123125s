#version 150

uniform sampler2D BlurredTex;
uniform sampler2D MaskTex;
uniform vec3 color;
uniform vec3 color2;
uniform float exposure;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 uv = texCoord;
    vec4 bloom = texture(BlurredTex, uv);
    vec4 mask  = texture(MaskTex,    uv);

    // Subtle bloom halo around the hand and held weapon
    float outer = bloom.a * (1.0 - mask.a * 0.7);
    vec3 grad = mix(color, color2, uv.y);
    float intensity = clamp(outer * exposure * 0.45, 0.0, 0.75);

    if (intensity <= 0.003) discard;
    fragColor = vec4(grad, intensity);
}
