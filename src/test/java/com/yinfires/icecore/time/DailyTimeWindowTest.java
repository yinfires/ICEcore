package com.yinfires.icecore.time;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DailyTimeWindowTest {
    @Test void entersDefaultWindowNaturally() {
        assertEquals(23_990L, DailyTimeWindow.latestEnteredBoundary(23_989L, 23_990L, 23_990, 24_000));
    }

    @Test void catchesJumpAcrossEntireWindow() {
        assertEquals(23_990L, DailyTimeWindow.latestEnteredBoundary(23_980L, 24_020L, 23_990, 24_000));
    }

    @Test void catchesFirstMovementWhenLoadedInsideWindow() {
        assertEquals(23_990L, DailyTimeWindow.latestEnteredBoundary(23_995L, 23_996L, 23_990, 24_000));
    }

    @Test void ignoresBackwardMovement() {
        assertEquals(DailyTimeWindow.NONE, DailyTimeWindow.latestEnteredBoundary(24_000L, 23_990L, 23_990, 24_000));
    }

    @Test void largeJumpTriggersOnlyLatestDayAndNextDayStillTriggers() {
        assertEquals(71_990L, DailyTimeWindow.latestEnteredBoundary(1L, 80_000L, 23_990, 24_000));
        assertEquals(95_990L, DailyTimeWindow.latestEnteredBoundary(80_000L, 96_000L, 23_990, 24_000));
    }

    @Test void doesNotTriggerAfterWindowWhenItWasNeverCrossed() {
        assertEquals(DailyTimeWindow.NONE, DailyTimeWindow.latestEnteredBoundary(24_000L, 24_001L, 23_990, 24_000));
    }
}
