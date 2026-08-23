package com.yinfires.icecore.currency;

public final class CurrencyClientState {
    private static final long ROLL_MILLIS = 600L;
    private static final long CHANGE_MILLIS = 3_000L;

    private static long from;
    private static long target;
    private static long delta;
    private static long animationStarted;
    private static long changeStarted;
    private static boolean hudEnabled = true;
    private static boolean initialized;

    private CurrencyClientState() {
    }

    public static void accept(long previous, long current, long change, boolean enabled, boolean animate) {
        long now = System.currentTimeMillis();
        hudEnabled = enabled;
        if (!initialized || !animate) {
            from = current;
            target = current;
            delta = 0L;
            animationStarted = now - ROLL_MILLIS;
            changeStarted = 0L;
            initialized = true;
            return;
        }
        from = displayedValue(now);
        target = current;
        delta = change;
        animationStarted = now;
        changeStarted = now;
    }

    public static long displayedValue(long now) {
        if (!initialized || now - animationStarted >= ROLL_MILLIS) return target;
        double t = Math.max(0.0D, Math.min(1.0D, (now - animationStarted) / (double) ROLL_MILLIS));
        long eased = Math.round((1.0D - Math.pow(1.0D - t, 4.0D)) * 10_000.0D);
        if (target >= from) {
            return from + scaled(target - from, eased);
        }
        return from - scaled(from - target, eased);
    }

    public static double rollProgress(long now) {
        return Math.max(0.0D, Math.min(1.0D, (now - animationStarted) / (double) ROLL_MILLIS));
    }

    public static long from() { return from; }
    public static long target() { return target; }
    public static long delta() { return delta; }
    public static long changeAge(long now) { return changeStarted == 0L ? Long.MAX_VALUE : now - changeStarted; }
    public static boolean hudEnabled() { return hudEnabled; }
    public static boolean initialized() { return initialized; }

    public static void reset() {
        initialized = false;
        from = target = delta = 0L;
        changeStarted = 0L;
    }

    private static long scaled(long distance, long progress) {
        // Split before multiplying so interpolation stays exact across the full non-negative
        // long range without overflowing or converting the balance distance to floating point.
        return (distance / 10_000L) * progress + (distance % 10_000L) * progress / 10_000L;
    }
}
