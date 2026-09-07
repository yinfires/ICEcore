package com.yinfires.icecore.time;

/** Pure day-time window crossing calculations shared by optional mod compatibility. */
public final class DailyTimeWindow {
    public static final long NONE = Long.MIN_VALUE;

    private DailyTimeWindow() {}

    public static long latestEnteredBoundary(long previous, long current, int start, int end) {
        if (current <= previous) return NONE;
        long boundary = start + Math.floorDiv(current - start, TimeCalendar.DAY_TICKS) * TimeCalendar.DAY_TICKS;
        long windowEnd = boundary + (end - start);
        return previous < windowEnd ? boundary : NONE;
    }
}
