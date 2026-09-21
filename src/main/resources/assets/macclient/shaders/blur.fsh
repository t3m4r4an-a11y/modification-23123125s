#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 BlurDir;
uniform float Radius;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 texelSize = 1.0 / vec2(textureSize(DiffuseSampler, 0));
    float sigma = max(Radius / 3.0, 0.5);
    float twoSigmaSq = 2.0 * sigma * sigma;

    vec4 sum = vec4(0.0);
    float total = 0.0;
    int r = int(Radius);

    for (int i = -r; i <= r; i++) {
        float w = exp(-float(i * i) / twoSigmaSq);
        vec2 offset = BlurDir * texelSize * float(i);
        sum += texture(DiffuseSampler, texCoord + offset) * w;
        total += w;
    }

    fragColor = sum / max(total, 0.0001);
}