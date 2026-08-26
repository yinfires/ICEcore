package com.yinfires.icecore.time;

public final class TimeCalendar {
    private TimeCalendar() {}
    public static long day(long dayTime) { return Math.floorDiv(dayTime, 24_000L) + 1L; }
    public static int weekday(long dayTime) { return (int) Math.floorMod(day(dayTime) - 1L, 7L); }
    public static int minuteOfDay(long dayTime) {
        return (int) (Math.floorMod(dayTime, 24_000L) * 1440L / 24_000L) + 360;
    }
    public static int hour(long dayTime) { return Math.floorMod(minuteOfDay(dayTime), 1440) / 60; }
    public static int minute(long dayTime) { return Math.floorMod(minuteOfDay(dayTime), 60); }
    public static long nextDaySix(long dayTime) { return Math.multiplyExact(Math.floorDiv(dayTime, 24_000L) + 1L, 24_000L); }
}
