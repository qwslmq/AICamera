attribute vec2 vPosition;
attribute vec4 vTexCoord;
uniform mat4 uTexRotateMatrix;
varying vec2 texCoord;
void main() {
      texCoord = vTexCoord.xy;
      gl_Position = uTexRotateMatrix *  vec4 ( vPosition.x, vPosition.y, 0.0, 1.0 );
}