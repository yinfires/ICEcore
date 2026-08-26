package com.yinfires.icecore.time;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class TimeService {
    private static MinecraftServer server;
    private static TimeSavedData saved;
    private static double fraction;
    private static long lastObserved;
    private static int syncTicks;
    private static boolean lastCycle;
    private static boolean applyingInternal;
    private static boolean advancedNaturally;
    private static long summarySequence;
    private TimeService() {}
    public static void start(MinecraftServer value) {
        server = value;
        saved = value.overworld().getDataStorage().computeIfAbsent(TimeSavedData::load, TimeSavedData::new, TimeSavedData.FILE_ID);
        lastObserved = value.overworld().getDayTime();
        saved.initialize(TimeCalendar.day(lastObserved));
        fraction = 0.0D;
        advancedNaturally = false;
        summarySequence = 0L;
        lastCycle = value.overworld().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DAYLIGHT);
    }
    public static void stop() { server = null; saved = null; fraction = 0.0D; advancedNaturally = false; }
    public static void resetFraction() { fraction = 0.0D; advancedNaturally = false; }
    public static long previousIncome() { return saved == null ? 0L : saved.previousIncome(); }
    public static long currentIncome() { return saved == null ? 0L : saved.currentIncome(); }
    public static void recordIncome(long delta) { if (saved != null) saved.addIncome(delta); }

    /** Called instead of vanilla's isolated dayTime + 1 operation. */
    public static void advanceNatural(ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD) return;
        if (TimeVoteManager.isRunning()) return;
        double increment = 1.0D / TimeConfigManager.get().data().dayDurationMultiplier();
        fraction += increment;
        long whole = (long) Math.floor(fraction);
        if (whole > 0L) {
            fraction -= whole;
            advancedNaturally = true;
            applyingInternal = true;
            try { level.setDayTime(level.getDayTime() + whole); }
            finally { applyingInternal = false; }
        }
    }
    public static void setDayTimeInternal(ServerLevel level, long value) {
        applyingInternal = true;
        try { level.setDayTime(value); }
        finally { applyingInternal = false; }
    }
    public static void beforeDayTimeSet() {
        if (!applyingInternal) {
            fraction = 0.0D;
            advancedNaturally = false;
            if (TimeVoteManager.isRunning()) TimeVoteManager.abort();
        }
    }
    public static void tick() {
        if (server == null || saved == null) return;
        long current = server.overworld().getDayTime();
        boolean cycle = server.overworld().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DAYLIGHT);
        if (current != lastObserved && !TimeVoteManager.isFastForwarding()) {
            // Natural advancement is expected; arbitrary jumps also settle through the same
            // calendar rule and discard fractional carry.
            if (Math.abs(current - lastObserved) > 100L) fraction = 0.0D;
        }
        long previous = lastObserved;
        boolean settled = saved.observeDay(TimeCalendar.day(current));
        if (shouldPlayNaturalSummary(previous, current, settled, advancedNaturally,
                TimeConfigManager.get().data().naturalDaySummaryEnabled(), TimeVoteManager.isRunning())) {
            TimeNetworking.broadcastDaySummary(++summarySequence, saved.previousIncome(), TimeConfigManager.get().data().timings());
        }
        advancedNaturally = false;
        lastObserved = current;
        if (++syncTicks >= 20 || cycle != lastCycle) { syncTicks = 0; lastCycle = cycle; TimeNetworking.broadcastSnapshot(); }
    }

    static boolean shouldPlayNaturalSummary(long previous, long current, boolean settled,
                                            boolean naturalAdvance, boolean enabled, boolean cutsceneRunning) {
        return settled && naturalAdvance && enabled && !cutsceneRunning && current > previous
                && TimeCalendar.day(current) > TimeCalendar.day(previous);
    }
}
