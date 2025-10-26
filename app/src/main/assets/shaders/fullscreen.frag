#version 310 es
precision mediump float;

in vec2 vUv;
out vec4 fragColor;

uniform int uPalette;
uniform sampler2D uDensity;
uniform float uAspect;
uniform int uHasDensity;

vec3 paletteWarm(float t) {
    return mix(vec3(0.05, 0.15, 0.45), vec3(1.2, 0.4, 0.1), clamp(t, 0.0, 1.0));
}

vec3 paletteRainbow(float t) {
    float a = clamp(t, 0.0, 1.0);
    return vec3(
        0.6 + 0.4 * sin(6.2831 * (a + 0.0)),
        0.6 + 0.4 * sin(6.2831 * (a + 0.33)),
        0.6 + 0.4 * sin(6.2831 * (a + 0.66)));
}

void main() {
    if (uHasDensity == 0) {
        vec2 centered = (vUv - 0.5) * vec2(uAspect, 1.0);
        float r = length(centered);
        float t = clamp(1.0 - r * 1.5, 0.0, 1.0);
        fragColor = vec4(paletteWarm(t), 1.0);
        return;
    }
    vec4 sample = texture(uDensity, vUv);
    vec3 dye = sample.xyz;
    float strength = clamp(length(dye), 0.0, 1.5);
    vec3 paletteColor = (uPalette == 0) ? paletteWarm(strength) : paletteRainbow(strength);
    vec3 normalized = normalize(dye + vec3(1e-4));
    vec3 color = mix(paletteColor, normalized * strength, 0.35) + dye * 0.65;
    color = clamp(color, 0.0, 2.0);
    fragColor = vec4(color, 1.0);
}
