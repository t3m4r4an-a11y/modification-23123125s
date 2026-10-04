#version 150

in vec2 Position;
in vec2 UV0;

uniform mat4 Transform;

out vec2 texCoord;

void main() {
    gl_Position = Transform * vec4(Position, 0.0, 1.0);
    texCoord = UV0;
}