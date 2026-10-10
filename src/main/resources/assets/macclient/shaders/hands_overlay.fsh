#version 150

uniform sampler2D MaskTex;
uniform vec3 color;
uniform float fillAmount;
uniform float alpha;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 uv = texCoord;
    float center = texture(MaskTex, uv).a;
    float inside  = smoothstep(0.05, 0.95, center);
    float a = clamp(alpha * inside * fillAmount, 0.0, 1.0);
    if (a <= 0.001) discard;
    fragColor = vec4(color * a, a);
}
