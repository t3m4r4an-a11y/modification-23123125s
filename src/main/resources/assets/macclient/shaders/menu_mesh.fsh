#version 150

uniform vec2 resolution;
uniform float time;
uniform vec3 accent;
uniform float alpha;

in vec2 texCoord;
out vec4 fragColor;

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

void main() {
    vec2 uv = texCoord;
    vec2 p = (gl_FragCoord.xy * 2.0 - resolution) / min(resolution.x, resolution.y);

    float t = time * 0.18;

    // Deep luxury dark background: obsidian #07090E
    vec3 baseCol = vec3(0.030, 0.040, 0.065);

    // Ethereal chromatic undulating wave ribbons
    float wave1 = sin(p.x * 1.8 + p.y * 1.2 + t * 1.6 + sin(p.y * 2.0 + t));
    float wave2 = cos(p.x * 2.2 - p.y * 1.5 - t * 1.2 + cos(p.x * 1.7 - t));
    float waves = (wave1 + wave2) * 0.5;

    // Glowing energy flow
    float ribbon = 1.0 / (abs(p.y + sin(p.x * 1.5 + t) * 0.35 + waves * 0.25) * 18.0 + 1.0);
    float ribbon2 = 1.0 / (abs(p.y - cos(p.x * 1.2 - t * 0.8) * 0.45 - waves * 0.2) * 22.0 + 1.0);

    // Dynamic accent & secondary harmonic color (cyber violet)
    vec3 col1 = accent;
    vec3 col2 = vec3(col1.b, col1.r * 0.6 + 0.4, col1.g * 0.8 + 0.2);

    vec3 energy = col1 * (ribbon * 0.75) + col2 * (ribbon2 * 0.55);

    // Soft animated nebula cloud
    float neb = noise(p * 1.2 + vec2(t * 0.3, -t * 0.2)) * 0.5 + noise(p * 2.4 - vec2(t * 0.2, t * 0.4)) * 0.25;
    vec3 nebColor = mix(baseCol, col1 * 0.18, neb);

    // Floating micro-particles / dust embers
    vec2 dustUv = uv * vec2(40.0, 24.0) + vec2(0.0, t * 1.5);
    vec2 dustCell = floor(dustUv);
    float dustRnd = hash(dustCell);
    float dustDist = length(fract(dustUv) - 0.5);
    float dust = smoothstep(0.18, 0.02, dustDist) * step(0.92, dustRnd);

    vec3 finalRgb = nebColor + energy + (col1 + vec3(0.6)) * dust * 0.4;

    // Subtle edge vignette
    float vig = 1.0 - smoothstep(0.4, 1.4, length(p * 0.65));
    finalRgb *= vig;

    // Final alpha blending with UI transition
    float outAlpha = clamp(alpha * 0.78, 0.0, 1.0);
    fragColor = vec4(finalRgb, outAlpha);
}
