#version 150

uniform sampler2D MaskTex;
uniform vec2 texelSize;
uniform vec3 color;
uniform vec3 color2;
uniform float time;
uniform float speed;
uniform float scale;
uniform float outline;
uniform float glowStrength;
uniform float fill;
uniform float alpha;
uniform float shaderType;
uniform float shaderIntensity;

in vec2 texCoord;
out vec4 fragColor;

// === Noise utils ===
float hash(vec2 p) {
    p = fract(p * vec2(123.34, 345.45));
    p += dot(p, p + 34.345);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0; float a = 0.5;
    for (int i = 0; i < 5; i++) { v += noise(p) * a; p = p * 2.02 + vec2(8.4, 5.7); a *= 0.5; }
    return v;
}

float ridged(vec2 p) {
    float v = 0.0; float a = 0.55;
    for (int i = 0; i < 4; i++) {
        float r = 1.0 - abs(noise(p) * 2.0 - 1.0);
        v += r * a; p = p * 2.18 + vec2(3.1, 9.2); a *= 0.52;
    }
    return v;
}

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0/3.0, 1.0/3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

// Edge detection on mask
float getEdge(vec2 uv, float s) {
    float a0  = texture(MaskTex, uv).a;
    float ax1 = texture(MaskTex, uv + vec2( texelSize.x * s, 0.0)).a;
    float ax2 = texture(MaskTex, uv - vec2( texelSize.x * s, 0.0)).a;
    float ay1 = texture(MaskTex, uv + vec2(0.0,  texelSize.y * s)).a;
    float ay2 = texture(MaskTex, uv - vec2(0.0,  texelSize.y * s)).a;
    float edge = abs(a0-ax1) + abs(a0-ax2) + abs(a0-ay1) + abs(a0-ay2);
    float d1 = texture(MaskTex, uv + vec2( texelSize.x * s,  texelSize.y * s)).a;
    float d2 = texture(MaskTex, uv + vec2(-texelSize.x * s,  texelSize.y * s)).a;
    float d3 = texture(MaskTex, uv + vec2( texelSize.x * s, -texelSize.y * s)).a;
    float d4 = texture(MaskTex, uv + vec2(-texelSize.x * s, -texelSize.y * s)).a;
    edge += 0.7 * (abs(a0-d1) + abs(a0-d2) + abs(a0-d3) + abs(a0-d4));
    return clamp(edge * 1.9, 0.0, 1.0);
}

void main() {
    vec2 uv = texCoord;
    float mask = texture(MaskTex, uv).a;
    if (mask <= 0.0) discard;

    float edge = getEdge(uv, outline);
    float edgeBand = smoothstep(0.02, 0.42, edge);

    float t  = time * max(speed, 0.001);
    float sc = clamp(scale, 0.2, 5.0);

    vec3 shaderColor = mix(color, color2, 0.5);
    float patternIntensity = 0.5;

    int mode = int(shaderType + 0.5);

    // MODE 0 — WAVES
    if (mode == 0) {
        vec2 flow = uv * mix(1.2, 3.2, clamp(sc / 3.0, 0.0, 1.0));
        vec2 drift = vec2(t * 0.20, -t * 0.15);
        vec2 warp = vec2(fbm(flow * 0.90 + drift * 0.75 + vec2(0.0, 4.1)),
                         fbm(flow * 0.78 - drift * 0.48 + vec2(3.7, 1.8)));
        vec2 q = flow + (warp - 0.5) * 1.8;
        float mist = fbm(q * 0.72 - drift * 0.24 + vec2(4.2, 8.1));
        float veins = ridged(q * 1.85 + vec2(mist * 2.5, mist * 1.6) - drift * 0.55);
        veins = pow(clamp(veins, 0.0, 1.0), 2.4);
        float stripeA = 1.0 - abs(sin((q.x * 1.08 + q.y * 0.42) * 1.7 + t * 0.85 + mist * 4.3));
        float stripeB = 1.0 - abs(sin((q.x * -0.58 + q.y * 1.12) * 1.45 - t * 0.65 - mist * 2.9));
        stripeA = pow(clamp(stripeA, 0.0, 1.0), 4.8);
        stripeB = pow(clamp(stripeB, 0.0, 1.0), 5.4);
        float energy = clamp(mist * 0.22 + veins * 0.88 + stripeA * 0.55 + stripeB * 0.32, 0.0, 1.0);
        float core = smoothstep(0.18, 0.98, energy);
        float accent = pow(clamp(max(veins, stripeA), 0.0, 1.0), 1.25);
        shaderColor = mix(color, color2, clamp(core * 0.75 + stripeB * 0.25, 0.0, 1.0));
        patternIntensity = 0.26 + core * 0.82 + accent * 0.28;
    }
    // MODE 1 — PLASMA
    else if (mode == 1) {
        vec2 uvS = uv * sc * 4.0;
        float v = sin(uvS.x * 2.5 + t * 1.2)
                + sin((uvS.y * 2.5 + t) * 0.5)
                + sin((uvS.x * 2.0 + uvS.y * 2.0 + t * 1.4) * 0.5);
        vec2 c = uvS - vec2(2.0 + sin(t * 0.6) * 1.5, 2.0 + cos(t * 0.8) * 1.5);
        v += sin(sqrt(c.x * c.x + c.y * c.y + 1.0) + t * 1.8);
        float plas = (v * 0.25 + 0.5);
        float pulse = 0.5 + 0.5 * sin(plas * 3.14159 * 2.0);
        shaderColor = mix(color, color2, clamp(plas, 0.0, 1.0));
        shaderColor += mix(vec3(1.0), color, 0.3) * pow(pulse, 3.0) * 0.8;
        patternIntensity = 0.4 + plas * 0.7 + pulse * 0.3;
    }
    // MODE 2 — CYBERPUNK
    else if (mode == 2) {
        float scan = sin(uv.y * 360.0 + t * 12.0) * 0.18;
        vec2 gridUv = fract(uv * 32.0 * sc);
        float gridLine = max(smoothstep(0.92, 0.98, gridUv.x), smoothstep(0.92, 0.98, gridUv.y));
        float glitchRow = floor(uv.y * 45.0);
        float glitchActive = step(0.96, hash(vec2(glitchRow, floor(t * 6.0))));
        float hexPattern = sin(uv.x * 40.0 * sc + glitchActive * hash(vec2(glitchRow, floor(t * 12.0))) * 4.0)
                         * cos(uv.y * 40.0 * sc);
        float cyber = clamp(gridLine * 0.85 + abs(hexPattern) * 0.4 + scan + glitchActive * 0.4, 0.0, 1.0);
        shaderColor = mix(color, color2, clamp(sin(uv.x * 8.0 + t) * 0.5 + 0.5, 0.0, 1.0));
        shaderColor += vec3(gridLine * 0.6);
        patternIntensity = 0.35 + cyber * 0.85;
    }
    // MODE 3 — FIRE
    else if (mode == 3) {
        vec2 fireUv = vec2(uv.x * sc * 2.0, uv.y * sc * 2.0 - t * 1.6);
        float n1 = noise(fireUv * 2.0);
        float n2 = noise(fireUv * 4.0 + vec2(n1 * 1.5, 0.0));
        float flame = ridged(fireUv * 3.0 + vec2(0.0, n2 * 1.2));
        flame = pow(clamp(flame, 0.0, 1.0), 1.8);
        vec3 heatColor = mix(color * 0.4, color, clamp(flame * 1.5, 0.0, 1.0));
        heatColor = mix(heatColor, color2, clamp((flame - 0.45) * 2.0, 0.0, 1.0));
        heatColor += vec3(1.0, 0.95, 0.8) * pow(clamp((flame - 0.7) * 3.3, 0.0, 1.0), 2.0);
        shaderColor = heatColor;
        patternIntensity = 0.3 + flame * 1.1;
    }
    // MODE 4 — LIGHTNING
    else if (mode == 4) {
        vec2 elecUv = uv * sc * 3.0;
        float spark1 = abs(sin(elecUv.x * 6.0 + fbm(elecUv * 4.0 + vec2(t * 7.0, t * 4.0)) * 6.0));
        float spark2 = abs(cos(elecUv.y * 6.0 + fbm(elecUv * 4.0 + vec2(-t * 5.0, t * 6.0)) * 6.0));
        float bolt = clamp(1.0/(spark1 * 32.0 + 1.0) + 1.0/(spark2 * 32.0 + 1.0), 0.0, 2.0);
        shaderColor = mix(color, color2, clamp(sin(t * 3.0) * 0.5 + 0.5, 0.0, 1.0));
        shaderColor += vec3(1.0) * pow(clamp(bolt * 0.6, 0.0, 1.0), 2.0);
        patternIntensity = 0.25 + bolt * 1.1;
    }
    // MODE 5 — RAINBOW
    else if (mode == 5) {
        float hue = fract((uv.x * 0.7 + uv.y * 0.7) * sc + t * 0.25);
        vec3 rainbowRgb = hsv2rgb(vec3(hue, 0.85, 1.0));
        float wave = 0.5 + 0.5 * sin((uv.x - uv.y) * 12.0 * sc + t * 2.0);
        shaderColor = mix(rainbowRgb, mix(color, color2, wave), 0.25);
        patternIntensity = 0.6 + wave * 0.4;
    }
    // MODE 6 — AURORA
    else if (mode == 6) {
        vec2 auUv = uv * sc * 2.5;
        float a1 = sin(auUv.x * 3.0 + sin(auUv.y * 2.5 + t * 1.1));
        float a2 = cos(auUv.y * 3.0 + sin(auUv.x * 2.5 - t * 0.9));
        float aurora = smoothstep(0.1, 0.9, 0.5 + 0.25 * (a1 + a2));
        float caustic = pow(0.5 + 0.5 * sin(auUv.x * 8.0 + auUv.y * 8.0 + t * 2.0), 4.0) * 0.6;
        shaderColor = mix(color, color2, aurora) + vec3(caustic);
        patternIntensity = 0.35 + aurora * 0.75 + caustic * 0.3;
    }
    // MODE 7 — WETNESS (CS2-style glossy wet skin & animated dripping water droplets)
    else if (mode == 7) {
        vec2 dropUv = vec2(uv.x * 24.0 * sc, (uv.y - t * 0.35) * 16.0 * sc);
        vec2 dropCell = floor(dropUv);
        vec2 dropFract = fract(dropUv) - 0.5;
        float rnd = hash(dropCell);
        float trailWobble = sin(uv.y * 28.0 + rnd * 6.28) * 0.12;
        float dX = dropFract.x + trailWobble;
        float dY = dropFract.y * 1.5;
        float dist = sqrt(dX * dX + dY * dY);
        float drop = smoothstep(0.26, 0.04, dist) * step(0.65, rnd);

        // Specular glint on droplet crest
        float glint = smoothstep(0.10, 0.02, length(vec2(dX - 0.06, dY - 0.06))) * step(0.65, rnd);

        // Wet dripping trail behind sliding droplets
        float trail = smoothstep(0.06, 0.0, abs(dropFract.x + trailWobble)) * step(0.0, dropFract.y) * 0.25 * step(0.65, rnd);

        // Overall glistening wet gloss sheen across the surface
        float gloss = pow(clamp(edgeBand * 0.6 + noise(uv * 12.0 + t * 0.1) * 0.45, 0.0, 1.0), 2.2);

        vec3 waterTint = mix(color, vec3(0.85, 0.95, 1.0), 0.70);
        shaderColor = mix(color, waterTint, drop * 0.8 + glint * 0.9);
        shaderColor += vec3(glint * 1.5) + vec3(gloss * 0.65);
        patternIntensity = 0.30 + (drop + trail) * 0.85 + glint * 0.7 + gloss * 0.5;
    }

    vec3 outlineColor = mix(color, color2, 0.35);
    float innerMask   = clamp(mask - edgeBand * 0.58, 0.0, 1.0);
    float fillStrength = fill * innerMask * patternIntensity * max(0.01, shaderIntensity);
    float edgeStrength = edgeBand * (0.34 + glowStrength * 0.12);
    vec3 rgb = shaderColor * fillStrength + outlineColor * edgeStrength;
    float outAlpha = clamp(alpha * (fillStrength * 0.92 + edgeBand * 0.48) * mask, 0.0, 1.0);

    if (outAlpha <= 0.001) discard;
    fragColor = vec4(rgb, outAlpha);
}
