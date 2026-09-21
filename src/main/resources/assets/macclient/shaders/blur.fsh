#version 150

uniform sampler2D DiffuseSampler;

uniform vec2 BlurDir;
uniform float Radius;

in vec2 texCoord;

out vec4 fragColor;

const int MAX_RADIUS = 32;

void main() {
    vec2 texelSize =
        1.0 / vec2(textureSize(DiffuseSampler, 0));

    float radius = clamp(
        Radius,
        0.0,
        float(MAX_RADIUS)
    );

    /*
     * Gaussian sigma.
     *
     * Larger radius produces a wider, softer blur.
     */
    float sigma = max(
        radius / 3.0,
        0.5
    );

    float twoSigmaSq =
        2.0 * sigma * sigma;

    vec4 sum = vec4(0.0);
    float totalWeight = 0.0;

    /*
     * Fixed loop bound is much friendlier to GLSL 1.50
     * drivers than a loop whose condition depends directly
     * on a uniform.
     */
    for (int i = -MAX_RADIUS; i <= MAX_RADIUS; i++) {

        float fi = float(i);

        if (abs(fi) > radius) {
            continue;
        }

        float weight =
            exp(
                -(fi * fi) / twoSigmaSq
            );

        vec2 offset =
            BlurDir *
            texelSize *
            fi;

        sum += texture(
            DiffuseSampler,
            texCoord + offset
        ) * weight;

        totalWeight += weight;
    }

    fragColor =
        sum / max(
            totalWeight,
            0.0001
        );
}