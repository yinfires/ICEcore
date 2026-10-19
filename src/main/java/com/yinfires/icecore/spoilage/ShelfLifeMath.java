package com.yinfires.icecore.spoilage;

/** Integer calendar arithmetic, independent of Minecraft bootstrap. */
public final class ShelfLifeMath {
    private ShelfLifeMath() {}
    public static int remaining(int days, long anchor, long now) {
        if (now <= anchor) return days;
        long elapsed = now - anchor;
        return elapsed >= days || elapsed < 0 ? 0 : days - (int) elapsed;
    }
    public static int moved(int source, int target, int limit, boolean secondary) {
        return Math.max(0, Math.min(secondary ? Math.min(1, source) : source, limit - target));
    }
}
