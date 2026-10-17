package com.yinfires.icecore.compat;

import com.yinfires.icecore.compat.cozycafe.CozyCafeHours;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CozyCafeHoursTest {
    @Test void openingWindowUsesSixAmDayBoundary() {
        assertFalse(CozyCafeHours.isClosed(0));
        assertFalse(CozyCafeHours.isClosed(11999));
        assertTrue(CozyCafeHours.isClosed(12000));
        assertTrue(CozyCafeHours.isClosed(18000));
        assertTrue(CozyCafeHours.isClosed(23999));
        assertFalse(CozyCafeHours.isClosed(24000));
        assertTrue(CozyCafeHours.isClosed(-1));
    }
    @Test void detectsAcceleratedAndMultiDayJumpsWithoutBackwardTriggers() {
        assertTrue(CozyCafeHours.crossedClosing(11999, 12000));
        assertTrue(CozyCafeHours.crossedClosing(11950, 12050));
        assertTrue(CozyCafeHours.crossedClosing(1000, 25000));
        assertTrue(CozyCafeHours.crossedClosing(1000, 100000));
        assertFalse(CozyCafeHours.crossedClosing(1000, 11999));
        assertFalse(CozyCafeHours.crossedClosing(12000, 12000));
        assertFalse(CozyCafeHours.crossedClosing(13000, 1000));
        assertFalse(CozyCafeHours.crossedClosing(13000, 25000));
        assertTrue(CozyCafeHours.crossedClosing(13000, 36000));
        assertTrue(CozyCafeHours.crossedClosing(Long.MIN_VALUE, Long.MAX_VALUE));
    }
}
