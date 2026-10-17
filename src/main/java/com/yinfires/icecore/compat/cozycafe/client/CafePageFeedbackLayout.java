package com.yinfires.icecore.compat.cozycafe.client;

/** Coordinates and monotonic lifetime for the computer's own error message. */
public final class CafePageFeedbackLayout {
    private CafePageFeedbackLayout() {}
    public static int left(int buttonCenter, int lineWidth) { return buttonCenter - lineWidth / 2; }
    public static int top(int buttonY, int lines, int lineHeight) { return buttonY - 6 - lines * lineHeight; }
    public static float alpha(long elapsedNanos) {
        if (elapsedNanos >= 6_000_000_000L) return 0.0F;
        if (elapsedNanos <= 5_000_000_000L) return 1.0F;
        return (6_000_000_000L - elapsedNanos) / 1_000_000_000.0F;
    }
}
