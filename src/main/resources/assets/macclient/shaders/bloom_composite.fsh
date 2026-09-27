#version 150

uniform sampler2D SceneSampler;
uniform sampler2D BloomSampler;
uniform float Intensity;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 scene = texture(SceneSampler, texCoord);
    vec4 bloom = texture(BloomSampler, texCoord);
    fragColor = vec4(scene.rgb + bloom.rgb * Intensity, scene.a);
}