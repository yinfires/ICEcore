package com.yinfires.icecore.time;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

public final class TimeSavedData extends SavedData {
    public static final String FILE_ID = "icecore_time_state";
    private long trackedDay;
    private long currentIncome;
    private long previousIncome;

    public static TimeSavedData load(CompoundTag tag) {
        TimeSavedData data = new TimeSavedData();
        data.trackedDay = tag.getLong("TrackedDay");
        data.currentIncome = tag.getLong("CurrentIncome");
        data.previousIncome = tag.getLong("PreviousIncome");
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        tag.putLong("TrackedDay", trackedDay);
        tag.putLong("CurrentIncome", currentIncome);
        tag.putLong("PreviousIncome", previousIncome);
        return tag;
    }
    public long trackedDay() { return trackedDay; }
    public long previousIncome() { return previousIncome; }
    public long currentIncome() { return currentIncome; }
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
