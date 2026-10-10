#version 150

in vec2 localCoord;

uniform vec2 uSize;         // pixel size (w, h)
uniform vec4 uRadius;       // corner radii: (tl, tr, br, bl)
uniform vec4 uColor;        // color 1 (primary / top)
uniform vec4 uColor2;       // color 2 (secondary / bottom for gradient)
uniform int  uMode;         // 0 = fill, 1 = border, 2 = shadow/glow
uniform float uBorder;      // border thickness (mode 1)
uniform float uShadowBlur;  // shadow/glow blur radius (mode 2)

out vec4 outColor;

// Signed distance field of a box with independent corner radii
// p: relative to center
// b: half dimensions (w/2, h/2)
// r: corner radii (br, tr, bl, tl)
float roundedBoxSDF(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x  = (p.y > 0.0) ? r.x  : r.y;
    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

void main() {
    vec2 center = uSize * 0.5;
    vec2 p = localCoord - center;

    // Clamp radii so they never exceed half of width or height
    float maxR = max(0.0, min(center.x, center.y));
    vec4 clampedR = min(uRadius, vec4(maxR));

    // Map (tl, tr, br, bl) to (br, tr, bl, tl) for roundedBoxSDF
    vec4 r = vec4(clampedR.z, clampedR.y, clampedR.w, clampedR.x);

    float dist = roundedBoxSDF(p, center, r);
    float edge = max(fwidth(dist), 0.7);

    if (uMode == 0) {
        // Mode 0: Fill (Solid or Gradient) with subpixel anti-aliasing
        float alpha = clamp(0.5 - dist / edge, 0.0, 1.0);
        if (alpha <= 0.001) discard;

        float t = clamp(localCoord.y / max(uSize.y, 1.0), 0.0, 1.0);
        vec4 col = mix(uColor, uColor2, t);
        outColor = vec4(col.rgb, col.a * alpha);

    } else if (uMode == 1) {
        // Mode 1: Inset border outline with subpixel anti-aliasing
        float borderDist = abs(dist + uBorder * 0.5) - uBorder * 0.5;
        float alpha = clamp(0.5 - borderDist / edge, 0.0, 1.0);
        if (alpha <= 0.001) discard;

        float t = clamp(localCoord.y / max(uSize.y, 1.0), 0.0, 1.0);
        vec4 col = mix(uColor, uColor2, t);
        outColor = vec4(col.rgb, col.a * alpha);

    } else if (uMode == 2) {
        // Mode 2: Gaussian soft drop shadow or neon bloom glow
        if (uShadowBlur <= 0.1) discard;
        float sDist = max(0.0, dist);
        float factor = clamp(1.0 - (sDist / uShadowBlur), 0.0, 1.0);
        factor = factor * factor * (3.0 - 2.0 * factor); // smooth hermite falloff

        float alpha = uColor.a * factor;
        if (alpha <= 0.002) discard;

        outColor = vec4(uColor.rgb, alpha);
    }
}
