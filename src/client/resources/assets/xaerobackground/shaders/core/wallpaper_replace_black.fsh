#version 150

uniform sampler2D InSampler;
uniform sampler2D WallpaperSampler;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 mapColor = texture(InSampler, texCoord);

    // Replace any pixel that is very dark (pure black = unexplored, or dark hover
    // highlight blended over unexplored black) with the wallpaper.
    // Real terrain colors are never this dark because Xaero tints them from actual
    // block colors, which always have at least some saturation or brightness.
    float maxChannel = max(mapColor.r, max(mapColor.g, mapColor.b));
    if (maxChannel < 0.1 && mapColor.a > 0.5) {
        fragColor = vec4(texture(WallpaperSampler, texCoord).rgb, 1.0);
    } else {
        fragColor = vec4(mapColor.rgb, 1.0);
    }
}
