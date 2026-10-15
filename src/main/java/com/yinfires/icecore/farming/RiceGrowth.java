package com.yinfires.icecore.farming;

/** Registry-independent rice lifecycle: three root steps, heading, three panicle steps. */
public final class RiceGrowth {
    public static final int MATURE = 7;
    private RiceGrowth() {}

    public static int stage(int rootAge, int upperAge) {
        return upperAge < 0 ? Math.min(3, Math.max(0, rootAge)) : 4 + Math.min(3, Math.max(0, upperAge));
    }

    public static int next(int stage, int firstStep) {
        return Math.min(MATURE, stage == 0 ? Math.min(3, Math.max(1, firstStep)) : stage + 1);
    }

}
