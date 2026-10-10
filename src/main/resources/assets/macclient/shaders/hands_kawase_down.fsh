#version 150

uniform sampler2D MaskTex;
uniform vec2 uOffset;
uniform vec2 uHalfPixel;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 uv = texCoord;
    vec2 hp = uHalfPixel * uOffset;

    vec4 sum = texture(MaskTex, uv) * 4.0;
    sum += texture(MaskTex, uv - hp);
    sum += texture(MaskTex, uv + hp);
    sum += texture(MaskTex, uv + vec2( hp.x, -hp.y));
    sum += texture(MaskTex, uv + vec2(-hp.x,  hp.y));

    fragColor = sum / 8.0;
}
