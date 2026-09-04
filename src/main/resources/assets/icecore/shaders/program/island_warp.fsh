#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float Strength;   // 0..1 overall build: shake amplitude + distortion, accelerating
uniform float White;      // 0..1 white fill covering the screen at the peak
uniform float Age;        // seconds since load — MONOTONIC clock (no vanilla Time reset)

in vec2 texCoord;
out vec4 fragColor;

const vec2 CENTER = vec2(0.5, 0.5);

// Cheap value noise for an organic, non-repeating shake path.
float n1(float x) { return fract(sin(x * 127.1 + 3.7) * 43758.5453); }

// Gathering teleport surge: the whole screen shakes harder and harder, then warps slightly
// inward the instant before it whites out. All motion is driven by Strength and the
// MONOTONIC Age clock, so it only ever intensifies and never snaps back on screen.
void main() {
    float aspect = InSize.x / max(InSize.y, 1.0);

    // Screen shake: a jittery whole-frame UV offset whose amplitude grows (Strength²) so it
    // starts as a faint tremor and builds to a violent judder. Two fast, mismatched
    // frequencies + noise so the path never looks like a clean sine loop.
    float amp = Strength * Strength * 0.045;
    float ph = Age * 46.0;
    vec2 shake = vec2(
        sin(ph * 1.00) * 0.6 + (n1(floor(ph * 3.0)) - 0.5) * 1.4,
        cos(ph * 1.17) * 0.6 + (n1(floor(ph * 3.0) + 9.0) - 0.5) * 1.4
    ) * amp;

    vec2 uv = texCoord + shake;

    // Slight inward warp at the very end (barrel pinch toward the centre), easing the shaken
    // world into the white-out so the cut to white feels like a pull, not a hard flash.
    vec2 toC = uv - CENTER;
    float warp = Strength * Strength * 0.12;
    uv = CENTER + toC * (1.0 - warp * (1.0 - length(vec2(toC.x * aspect, toC.y))));

    uv = clamp(uv, 0.0, 1.0);
    vec3 col = texture(DiffuseSampler, uv).rgb;

    // White-out: fills the whole screen at the peak, the clean hand-off to the GUI white hold.
    col = mix(col, vec3(1.0), clamp(White, 0.0, 1.0));

    fragColor = vec4(col, 1.0);
}
