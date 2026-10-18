package com.yinfires.icecore.workstation;

/** Native PUT lasts 0.5833s (12 ticks rounded up); WITHDRAW lasts 0.5s (10 ticks). */
public final class TrashCanAnimationTiming {
    private TrashCanAnimationTiming() {}
    public static int remainingTicks(long previousSequence, long sequence, long start, long now, boolean taking) {
        if (sequence <= previousSequence || sequence <= 0 || now < start) return 0;
        long age = now - start;
        int duration = taking ? 10 : 12;
        return age < duration ? duration - (int) age : 0;
    }
}
