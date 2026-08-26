package com.yinfires.icecore.time;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TimeCalendarTest {
    @Test void originAndWeekday() {
        assertEquals(1L, TimeCalendar.day(0L));
        assertEquals(0, TimeCalendar.weekday(0L));
        assertEquals(6, TimeCalendar.hour(0L));
        assertEquals(0, TimeCalendar.minute(0L));
    }
    @Test void dayAndClockBoundaries() {
        assertEquals(1L, TimeCalendar.day(23_999L));
        assertEquals(2L, TimeCalendar.day(24_000L));
        assertEquals(5, TimeCalendar.hour(23_999L));
        assertEquals(59, TimeCalendar.minute(23_999L));
        assertEquals(6, TimeCalendar.hour(24_000L));
    }
    @Test void strictNextSix() {
        assertEquals(24_000L, TimeCalendar.nextDaySix(0L));
        assertEquals(24_000L, TimeCalendar.nextDaySix(23_999L));
        assertEquals(48_000L, TimeCalendar.nextDaySix(24_000L));
    }
    @Test void negativeTimeUsesFloorArithmetic() {
        assertEquals(0L, TimeCalendar.day(-1L));
        assertEquals(5, TimeCalendar.hour(-1L));
        assertEquals(59, TimeCalendar.minute(-1L));
    }
}
