#version 150

uniform sampler2D MaskTex;
uniform vec2 uOffset;
uniform vec2 uHalfPixel;
uniform vec3 glowColor;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 uv = texCoord;
    vec2 hp = uHalfPixel * uOffset;

    vec4 sum = texture(MaskTex, uv + vec2(-hp.x * 2.0, 0.0));
    sum += texture(MaskTex, uv + vec2(-hp.x,  hp.y)) * 2.0;
    sum += texture(MaskTex, uv + vec2( 0.0,   hp.y * 2.0));
    sum += texture(MaskTex, uv + vec2( hp.x,  hp.y)) * 2.0;
    sum += texture(MaskTex, uv + vec2( hp.x * 2.0, 0.0));
    sum += texture(MaskTex, uv + vec2( hp.x, -hp.y)) * 2.0;
    sum += texture(MaskTex, uv + vec2( 0.0,  -hp.y * 2.0));
    sum += texture(MaskTex, uv + vec2(-hp.x, -hp.y)) * 2.0;

    vec4 result = sum / 12.0;
    fragColor = vec4(result.rgb * glowColor, result.a);
}
