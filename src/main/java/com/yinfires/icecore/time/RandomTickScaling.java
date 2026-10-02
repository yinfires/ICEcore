package com.yinfires.icecore.time;

/** Pure scaling rules for matching random-tick density to the configured day-time rate. */
public final class RandomTickScaling {
    public static final int MAX_COMPENSATION_PER_CHUNK_TICK = 10_000;

    private RandomTickScaling() {}

    /** Returns the extra random-tick attempts, in bounded batches, after vanilla's attempts. */
    public static int compensationAttempts(int vanillaAttempts, double dayDurationMultiplier) {
        if (vanillaAttempts <= 0 || !Double.isFinite(dayDurationMultiplier) || dayDurationMultiplier <= 0.0D
                || dayDurationMultiplier == 1.0D) return 0;

        double extraFactor = dayDurationMultiplier > 1.0D
                ? dayDurationMultiplier - 1.0D
                : (1.0D / dayDurationMultiplier) - 1.0D;
        double attempts = Math.ceil(vanillaAttempts * extraFactor);
        return (int) Math.min(attempts, MAX_COMPENSATION_PER_CHUNK_TICK);
    }
}
