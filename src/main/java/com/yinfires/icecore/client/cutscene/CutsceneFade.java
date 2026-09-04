package com.yinfires.icecore.client.cutscene;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Shared full-screen fade helper for every scripted cutscene. Owns the two pieces
 * that are identical wherever a cutscene blacks the screen out: the smoothstep
 * easing curve and a trapezoid fade envelope (fade in → hold opaque → fade out),
 * plus a full-screen colour fill that skips near-transparent frames. Timing stays
 * with each consumer — only the shape of a single fade segment is shared here.
 *
 * <p>A cutscene that fades to black to hide a camera cut computes its cover
 * fraction with {@link #envelope} and paints it with {@link #fill}; the time skip
 * and the island entrance both drive their black screens this way so the look is
 * the same. Client render thread only; pure math, no state.
 */
public final class CutsceneFade {
    public static final int BLACK = 0x000000;
    public static final int WHITE = 0xFFFFFF;

    private CutsceneFade() {
    }

    /** Smoothstep easing for [0,1]; clamps out-of-range inputs. */
    public static float ease(float x) {
        x = Math.max(0F, Math.min(1F, x));
        return x * x * (3F - 2F * x);
    }

    /**
     * Cover fraction in [0,1] of one trapezoid fade at time {@code t}: eased 0→1 over
     * {@code [fadeInStart, fadeInStart+fadeInTicks]}, held at 1 until {@code fadeOutStart},
     * then eased 1→0 over {@code [fadeOutStart, fadeOutStart+fadeOutTicks]}. Durations of
     * zero collapse that edge to an instant step. Returns 0 outside the envelope.
     */
    public static float envelope(double t, double fadeInStart, double fadeInTicks,
                                 double fadeOutStart, double fadeOutTicks) {
        if (t < fadeInStart) {
            return 0F;
        }
        if (fadeInTicks > 0D && t < fadeInStart + fadeInTicks) {
            return ease((float) ((t - fadeInStart) / fadeInTicks));
        }
        if (t < fadeOutStart) {
            return 1F;
        }
        if (fadeOutTicks > 0D && t < fadeOutStart + fadeOutTicks) {
            return 1F - ease((float) ((t - fadeOutStart) / fadeOutTicks));
        }
        return t < fadeOutStart ? 1F : 0F;
    }

    /**
     * Fills the whole GUI with {@code rgb} at the given [0,1] alpha. Frames below a
     * ~1.5% alpha are skipped so a fully-faded screen costs nothing.
     */
    public static void fill(GuiGraphics graphics, float alpha, int rgb) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255F)));
        if (a < 4) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        graphics.fill(0, 0, w, h, (a << 24) | (rgb & 0xFFFFFF));
    }
}
