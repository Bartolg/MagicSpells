#version 310 es
precision mediump float;

in vec2 vUv;
out vec4 fragColor;

uniform int uPalette;

vec3 paletteNeon(float t) {
    if (uPalette == 0) {
        return mix(vec3(0.05, 0.15, 0.45), vec3(1.2, 0.4, 0.1), t);
    }
    return vec3(
        0.6 + 0.4 * sin(6.2831 * (t + 0.0)),
        0.6 + 0.4 * sin(6.2831 * (t + 0.33)),
        0.6 + 0.4 * sin(6.2831 * (t + 0.66)));
}

void main() {
    float r = length(vUv - 0.5);
    float t = clamp(1.0 - r * 1.5, 0.0, 1.0);
    vec3 color = paletteNeon(t);
    fragColor = vec4(color, 1.0);
}
