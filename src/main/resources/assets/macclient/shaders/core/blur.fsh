#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 BlurDir;
uniform float Radius;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 texelSize = 1.0 / vec2(textureSize(DiffuseSampler, 0));
    vec4 color = vec4(0.0);
    float total = 0.0;
    int iRadius = int(Radius);

    for (int i = -64; i <= 64; i++) {
        if (i < -iRadius || i > iRadius) continue;
        float offset = float(i);
        float weight = 1.0 - abs(offset) / (Radius + 1.0);
        vec2 samplePos = texCoord + BlurDir * offset * texelSize;
        color += texture(DiffuseSampler, samplePos) * weight;
        total += weight;
    }

    fragColor = color / max(total, 0.0001);
}