#version 150

uniform sampler2D InSampler;
uniform sampler2D WallpaperSampler;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 mapColor = texture(InSampler, texCoord);
    // Replace opaque black pixels (unexplored areas) with the wallpaper.
    // Xaero tiles use pure black (r=g=b=0, a=1) for unexplored sub-areas.
    if (mapColor.r < 0.01 && mapColor.g < 0.01 && mapColor.b < 0.01 && mapColor.a > 0.99) {
        fragColor = vec4(texture(WallpaperSampler, texCoord).rgb, 1.0);
    } else {
        fragColor = vec4(mapColor.rgb, 1.0);
    }
}
