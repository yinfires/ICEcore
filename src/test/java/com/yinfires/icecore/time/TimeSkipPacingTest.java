package com.yinfires.icecore.time;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TimeSkipPacingTest {
    @Test
    void defaultFastForwardUsesOneRealSecondPerGameHour() {
        assertEquals(20, TimeVoteManager.fastForwardDurationTicks(0L, 1_000L, 20));
        assertEquals(240, TimeVoteManager.fastForwardDurationTicks(12_000L, 24_000L, 20));
        assertEquals(480, TimeVoteManager.fastForwardDurationTicks(0L, 24_000L, 20));
    }

    @Test
    void partialHoursKeepExactProportionRoundedUp() {
        assertEquals(10, TimeVoteManager.fastForwardDurationTicks(23_500L, 24_000L, 20));
        assertEquals(1, TimeVoteManager.fastForwardDurationTicks(23_999L, 24_000L, 20));
    }
}
