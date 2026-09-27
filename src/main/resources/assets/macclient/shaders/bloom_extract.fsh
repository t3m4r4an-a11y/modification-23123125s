#version 150

uniform sampler2D DiffuseSampler;
uniform float Threshold;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 c = texture(DiffuseSampler, texCoord);
    float brightness = dot(c.rgb, vec3(0.2126, 0.7152, 0.0722));
    float soft = max(brightness - Threshold, 0.0) / max(1.0 - Threshold, 0.0001);
    fragColor = vec4(c.rgb * soft, 1.0);
}