package com.yinfires.icecore.time;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TimeSavedDataTest {
    @Test void oldStateHasNoProcessedDewDropDay() {
        TimeSavedData data = TimeSavedData.load(new CompoundTag());
        assertEquals(Long.MIN_VALUE, data.dewDropProcessedDay());
    }

    @Test void dewDropProcessedDayRoundTrips() {
        TimeSavedData data = new TimeSavedData();
        data.setDewDropProcessedDay(42L);
        CompoundTag tag = data.save(new CompoundTag());
        assertEquals(42L, TimeSavedData.load(tag).dewDropProcessedDay());
    }

    @Test void processedDayCanBeClearedForBackwardTimeEpoch() {
        TimeSavedData data = new TimeSavedData();
        data.setDewDropProcessedDay(42L);
        data.clearDewDropProcessedDay();
        assertEquals(Long.MIN_VALUE, data.dewDropProcessedDay());
    }
}
