package com.yinfires.icecore.spoilage;

/** One transition covers calendar progress, both pause reasons and time reversal. */
public record ShelfLifeState(int days, long day, boolean paused) {
    public ShelfLifeState advance(long now, boolean pause) {
        int next = paused ? days : ShelfLifeMath.remaining(days, day, now);
        long anchor = paused != pause || !pause && now > day ? Math.max(now, day) : day;
        return new ShelfLifeState(next, anchor, pause);
    }
}
