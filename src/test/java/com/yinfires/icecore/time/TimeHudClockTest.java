package com.yinfires.icecore.time;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TimeHudClockTest {
    @Test
    void ordinaryMinuteOnlyRollsMinuteOnes() {
        assertEquals("06:05", TimeHud.clockText(6 * 60 + 5));
        assertFalse(TimeHud.slotChanges(6 * 60 + 5, 0));
        assertFalse(TimeHud.slotChanges(6 * 60 + 5, 1));
        assertFalse(TimeHud.slotChanges(6 * 60 + 5, 2));
        assertFalse(TimeHud.slotChanges(6 * 60 + 5, 3));
        assertTrue(TimeHud.slotChanges(6 * 60 + 5, 4));
    }

    @Test
    void minuteCarryOnlyRollsAffectedSlots() {
        int minute = 6 * 60 + 9;
        assertTrue(TimeHud.slotChanges(minute, 3));
        assertTrue(TimeHud.slotChanges(minute, 4));
        assertFalse(TimeHud.slotChanges(minute, 0));
        assertFalse(TimeHud.slotChanges(minute, 1));
        assertFalse(TimeHud.slotChanges(minute, 2));
    }

    @Test
    void midnightCarryKeepsColonStill() {
        int minute = 23 * 60 + 59;
        assertEquals("23:59", TimeHud.clockText(minute));
        assertEquals("00:00", TimeHud.clockText(minute + 1));
        assertTrue(TimeHud.slotChanges(minute, 0));
        assertTrue(TimeHud.slotChanges(minute, 1));
        assertFalse(TimeHud.slotChanges(minute, 2));
        assertTrue(TimeHud.slotChanges(minute, 3));
        assertTrue(TimeHud.slotChanges(minute, 4));
    }

    @Test
    void rollingFinishesThenPausesAtNormalSpeed() {
        assertEquals(250.0D, TimeHud.rollDurationMillis(1.0D));
        assertTrue(TimeHud.rollDurationMillis(1.0D) < 50_000.0D / 60.0D);
        assertTrue(TimeHud.rollDurationMillis(0.01D) < (50_000.0D / 60.0D) * 0.01D);
    }
}
