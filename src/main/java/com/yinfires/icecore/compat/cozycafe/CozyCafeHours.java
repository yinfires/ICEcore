package com.yinfires.icecore.compat.cozycafe;

/** Main-world clock: tick zero is 06:00, tick 12000 is 18:00. */
public final class CozyCafeHours {
    private CozyCafeHours() {}

    public static boolean isClosed(long time) {
        return Math.floorMod(time, 24000L) >= 12000L;
    }

    public static boolean crossedClosing(long previous, long current) {
        if (current <= previous) return false;
        long previousBoundary = Math.floorDiv(previous, 24000L) + (isClosed(previous) ? 1L : 0L);
        long currentBoundary = Math.floorDiv(current, 24000L) + (isClosed(current) ? 1L : 0L);
        return currentBoundary > previousBoundary;
    }
}
