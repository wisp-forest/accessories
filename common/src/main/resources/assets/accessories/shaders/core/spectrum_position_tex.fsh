#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float GameTime;

in vec2 texCoord0;
in vec4 vertexColor;
in vec2 screenPos;

out vec4 fragColor;

vec3 hsv2rgb(vec3 hsv) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(hsv.xxx + K.xyz) * 6.0 - K.www);
    return hsv.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), hsv.y);
}

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor;

    if (color.a < 0.1) {
        discard;
    }

    vec3 modifiedHSV = vec3(
        mod((((screenPos.x + screenPos.y) * 1000) + (GameTime * 24000)) / 2.0, 100.0) / 100.0,
        1.0,
        1.0
    );

    fragColor = vec4(
        hsv2rgb(modifiedHSV).xyz,
        0.75
    ) * ColorModulator;
}
