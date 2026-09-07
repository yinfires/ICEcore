package com.yinfires.icecore.time;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

public final class TimeSavedData extends SavedData {
    public static final String FILE_ID = "icecore_time_state";
    private long trackedDay;
    private long currentIncome;
    private long previousIncome;
    private long dewDropProcessedDay = Long.MIN_VALUE;

    public static TimeSavedData load(CompoundTag tag) {
        TimeSavedData data = new TimeSavedData();
        data.trackedDay = tag.getLong("TrackedDay");
        data.currentIncome = tag.getLong("CurrentIncome");
        data.previousIncome = tag.getLong("PreviousIncome");
        data.dewDropProcessedDay = tag.contains("DewDropProcessedDay")
                ? tag.getLong("DewDropProcessedDay") : Long.MIN_VALUE;
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        tag.putLong("TrackedDay", trackedDay);
        tag.putLong("CurrentIncome", currentIncome);
        tag.putLong("PreviousIncome", previousIncome);
        tag.putLong("DewDropProcessedDay", dewDropProcessedDay);
        return tag;
    }
    public long trackedDay() { return trackedDay; }
    public long previousIncome() { return previousIncome; }
    public long currentIncome() { return currentIncome; }
    public long dewDropProcessedDay() { return dewDropProcessedDay; }
    public void setDewDropProcessedDay(long day) { if (dewDropProcessedDay != day) { dewDropProcessedDay = day; setDirty(); } }
    public void clearDewDropProcessedDay() { if (dewDropProcessedDay != Long.MIN_VALUE) { dewDropProcessedDay = Long.MIN_VALUE; setDirty(); } }
    public void initialize(long day) { if (trackedDay == 0L) { trackedDay = day; setDirty(); } }
    public boolean observeDay(long day) {
        if (trackedDay == 0L) trackedDay = day;
        if (day != trackedDay) { previousIncome = currentIncome; currentIncome = 0L; trackedDay = day; setDirty(); return true; }
        return false;
    }
    public void addIncome(long delta) {
        try { currentIncome = Math.addExact(currentIncome, delta); }
        catch (ArithmeticException exception) { currentIncome = delta < 0 ? Long.MIN_VALUE : Long.MAX_VALUE; }
        setDirty();
    }
}
