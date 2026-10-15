package com.yinfires.icecore.farming;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RiceGrowthTest {
    @Test void ordinarySoilTakesSevenSuccessfulDaysAndThenStops() {
        int stage = RiceGrowth.stage(0, -1);
        for (int day = 0; day < 7; day++) {
            stage = RiceGrowth.next(stage, 1);
            assertEquals(day + 1, stage);
        }
        assertEquals(7, RiceGrowth.next(stage, 1));
    }

    @Test void headingIsSeparateAndPanicleAgeCountsTowardWholePlant() {
        assertEquals(3, RiceGrowth.stage(3, -1));
        assertEquals(4, RiceGrowth.stage(3, 0));
        assertEquals(6, RiceGrowth.stage(3, 2));
        assertEquals(7, RiceGrowth.stage(3, 3));
        // Hoe harvest removes the top and resets root age.
        assertEquals(0, RiceGrowth.stage(0, -1));
    }

    @Test void fertilizerOnlyChangesInitialRootStepAndNeverSkipsHeading() {
        for (int fertilizer = 1; fertilizer <= 4; fertilizer++) {
            int stage = 0;
            int days = 8 - Math.min(3, fertilizer);
            for (int day = 0; day < days; day++) stage = RiceGrowth.next(stage, fertilizer);
            assertEquals(7, stage);
            assertEquals(7 - Math.min(3, fertilizer) + 1, days);
            assertEquals(4, RiceGrowth.next(3, fertilizer));
            assertEquals(5, RiceGrowth.next(4, fertilizer));
        }
    }
}
