package com.yinfires.icecore.journal.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Minimal auto-hiding scrollbar for one scrollable region. Draws only a rounded thumb at the
 * region's right edge, no track. It fades in while scrolling or while the pointer is over the
 * region and fades out otherwise, and supports wheel scrolling plus thumb dragging.
 *
 * <p>The owner supplies the region rectangle and the total content height each frame; this class
 * owns the scroll offset, visibility alpha and drag state. Alpha is time-based (nanoTime) so it is
 * frame-rate independent; it never enters the 0..3 quantized range that Minecraft's fill/font paths
 * would force opaque — it is clamped away from that when emitting the color.
 */
public final class Scrollbar {
    private static final long FADE_MILLIS = 200L;
    private static final long IDLE_HIDE_MILLIS = 700L;

    private double scroll;
    private long lastActivityNanos = Long.MIN_VALUE;
    private boolean dragging;
    private double dragOffset;

    // Region + content, refreshed each frame by the owner before querying.
    private int x;
    private int y;
    private int width;
    private int height;
    private int contentHeight;

    public void setRegion(int x, int y, int width, int height, int contentHeight) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.contentHeight = contentHeight;
        clampScroll();
    }

    public int scrollOffset() {
        return (int) Math.round(scroll);
    }

    public boolean scrollable() {
        return contentHeight > height;
    }

    private int maxScroll() {
        return Math.max(0, contentHeight - height);
    }

    private void clampScroll() {
        scroll = Mth.clamp(scroll, 0.0D, maxScroll());
    }

    private void poke() {
        lastActivityNanos = System.nanoTime();
    }

    /** Wheel scroll while the pointer is inside the region. Returns true if it consumed the event. */
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!scrollable() || !inRegion(mouseX, mouseY)) {
            return false;
        }
        scroll -= delta * 18.0D;
        clampScroll();
        poke();
        return true;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !scrollable()) {
            return false;
        }
        int[] thumb = thumbBounds();
        if (mouseX >= thumbX() && mouseX <= thumbX() + JournalTheme.SCROLLBAR_WIDTH
                && mouseY >= thumb[0] && mouseY <= thumb[0] + thumb[1]) {
            dragging = true;
            dragOffset = mouseY - thumb[0];
            poke();
            return true;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY) {
        if (!dragging) {
            return false;
        }
        int[] thumb = thumbBounds();
        double travel = height - thumb[1];
        if (travel > 0) {
            double ratio = Mth.clamp((mouseY - dragOffset - y) / travel, 0.0D, 1.0D);
            scroll = ratio * maxScroll();
        }
        poke();
        return true;
    }

    public void mouseReleased() {
        dragging = false;
    }

    /** Marks the pointer position each frame so hover keeps the bar visible. */
    public void updateHover(double mouseX, double mouseY) {
        if (scrollable() && inRegion(mouseX, mouseY)) {
            poke();
        }
    }

    public void render(GuiGraphics graphics, double mouseX, double mouseY) {
        if (!scrollable()) {
            return;
        }
        float alpha = visibility();
        if (alpha <= 0.0F) {
            return;
        }
        int[] thumb = thumbBounds();
        boolean hover = dragging || (mouseX >= thumbX() - 2 && mouseX <= thumbX() + JournalTheme.SCROLLBAR_WIDTH + 2
                && mouseY >= thumb[0] && mouseY <= thumb[0] + thumb[1]);
        int base = hover ? JournalTheme.SCROLLBAR_THUMB_HOVER : JournalTheme.SCROLLBAR_THUMB;
        int a = Mth.clamp((int) ((base >>> 24) * alpha), 4, 255);
        int color = (a << 24) | (base & 0x00FFFFFF);
        int tx = thumbX();
        // Rounded look with a plain fill body and 1px inset caps; no track behind it.
        graphics.fill(tx, thumb[0] + 1, tx + JournalTheme.SCROLLBAR_WIDTH, thumb[0] + thumb[1] - 1, color);
        graphics.fill(tx + 1, thumb[0], tx + JournalTheme.SCROLLBAR_WIDTH - 1, thumb[0] + 1, color);
        graphics.fill(tx + 1, thumb[0] + thumb[1] - 1, tx + JournalTheme.SCROLLBAR_WIDTH - 1, thumb[0] + thumb[1], color);
    }

    private int thumbX() {
        return x + width - JournalTheme.SCROLLBAR_WIDTH;
    }

    /** Returns {thumbTop, thumbHeight}. */
    private int[] thumbBounds() {
        int minThumb = 16;
        int thumbHeight = Math.max(minThumb, (int) ((long) height * height / Math.max(1, contentHeight)));
        thumbHeight = Math.min(thumbHeight, height);
        int travel = height - thumbHeight;
        int max = maxScroll();
        int top = y + (max == 0 ? 0 : (int) (scroll / max * travel));
        return new int[]{top, thumbHeight};
    }

    private boolean inRegion(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private float visibility() {
        if (dragging) {
            return 1.0F;
        }
        if (lastActivityNanos == Long.MIN_VALUE) {
            return 0.0F;
        }
        long elapsed = (System.nanoTime() - lastActivityNanos) / 1_000_000L;
        if (elapsed <= IDLE_HIDE_MILLIS) {
            return 1.0F;
        }
        long fade = elapsed - IDLE_HIDE_MILLIS;
        if (fade >= FADE_MILLIS) {
            return 0.0F;
        }
        return 1.0F - (float) fade / FADE_MILLIS;
    }
}
