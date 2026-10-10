#version 150

in vec2 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec2 uPos;
uniform vec2 uSize;
uniform float uMargin;

out vec2 localCoord;

void main() {
    localCoord = (Position * (uSize + 2.0 * uMargin)) - vec2(uMargin);
    vec2 pixelPos = uPos + localCoord;
    vec4 pos = ProjMat * ModelViewMat * vec4(pixelPos, 0.0, 1.0);
    // Force NDC z = 0.0 to guarantee it is never clipped by orthographic near/far planes
    gl_Position = vec4(pos.xy, 0.0, 1.0);
}
