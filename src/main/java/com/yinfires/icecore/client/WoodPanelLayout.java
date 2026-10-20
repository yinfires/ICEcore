package com.yinfires.icecore.client;

/** Geometry shared by the native tooltip adapters. All coordinates are GUI units. */
public final class WoodPanelLayout {
    public static final int BORDER = 6;

    public static int outerStart(int contentStart) {
        return contentStart - BORDER;
    }

    public static int outerSize(int contentSize) {
        return contentSize + 2 * BORDER;
    }

    public static int constrain(int contentStart, int contentSize, int screenSize) {
        // Oversized content keeps its native scrolling extent; never shrink it to the screen.
        return Math.max(BORDER, Math.min(contentStart, screenSize - contentSize - BORDER));
    }

    private WoodPanelLayout() {}
}
