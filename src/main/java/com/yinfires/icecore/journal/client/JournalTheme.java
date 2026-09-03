package com.yinfires.icecore.journal.client;

/**
 * Shared visual constants for the quest/tutorial UI and HUD. Translucent black system, minimal
 * decoration. Colors are ARGB. Kept here so screen, detail panel, tracker HUD and scrollbar stay
 * consistent and can be retuned in one place; textures (when added) layer on top of these fallbacks.
 */
public final class JournalTheme {
    private JournalTheme() {
    }

    // Panels / backgrounds
    public static final int PANEL_BG = 0xB0000000;
    public static final int PANE_BG = 0x66000000;
    public static final int DIVIDER = 0x66FFFFFF;
    public static final int BORDER = 0x40FFFFFF;

    // Text
    public static final int TITLE_COLOR = 0xFFFFFFFF;
    public static final int CONTENT_COLOR = 0xFFC9CDD2;
    public static final int MUTED_COLOR = 0xFF8B9096;

    // List entries
    public static final int ENTRY_HOVER = 0x33FFFFFF;
    public static final int ENTRY_SELECTED = 0x4DFFFFFF;
    public static final int BADGE_NEW = 0xFFE8563F;

    // Scrollbar (slider only, no track)
    public static final int SCROLLBAR_THUMB = 0x99FFFFFF;
    public static final int SCROLLBAR_THUMB_HOVER = 0xCCFFFFFF;
    public static final int SCROLLBAR_WIDTH = 3;

    // Layout
    public static final int TITLE_SCALE_NUM = 7; // title drawn at 1.4x via scale(1.4)
    public static final float TITLE_SCALE = 1.4F;
    public static final int PADDING = 8;
}
