#version 150

uniform vec2  QuadSize;     // full quad size (rect + padding) in px
uniform vec2  RectSize;     // inner rounded rect size in px
uniform float Radius;       // corner radius in px
uniform float GlowSize;     // how far the glow extends outward, px
uniform vec4  GlowColor;    // rgb + alpha

in vec2 texCoord;
out vec4 fragColor;

// Signed distance to rounded rect, centred at (0,0)
float sdRoundRect(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + vec2(r);
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

void main() {
    // Local coords, origin at quad centre
    vec2 p = texCoord * QuadSize - QuadSize * 0.5;

    float d = sdRoundRect(p, RectSize * 0.5, Radius);

    // Glow only outside the rect
    float glow = 0.0;
    if (d > 0.0 && d < GlowSize) {
        float t = d / GlowSize;
        // Soft quartic falloff — richer than linear, cheaper than gaussian
        float k = 1.0 - t;
        glow = k * k * k;
    }

    fragColor = vec4(GlowColor.rgb, GlowColor.a * glow);
}