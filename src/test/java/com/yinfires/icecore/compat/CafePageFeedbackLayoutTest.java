package com.yinfires.icecore.compat;

import com.yinfires.icecore.compat.cozycafe.client.CafePageFeedbackLayout;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CafePageFeedbackLayoutTest {
    @Test void eachLineCentersAboveMovedOpenButton() {
        assertEquals(68, CafePageFeedbackLayout.left(88, 40));
        assertEquals(268, CafePageFeedbackLayout.left(288, 40));
        assertEquals(168, CafePageFeedbackLayout.top(192, 2, 9));
        assertEquals(159, CafePageFeedbackLayout.top(192, 3, 9));
    }
    @Test void expiresAndFadesWithoutDependingOnContainerTicks() {
        assertEquals(1.0F, CafePageFeedbackLayout.alpha(0));
        assertEquals(1.0F, CafePageFeedbackLayout.alpha(5_000_000_000L));
        assertEquals(0.5F, CafePageFeedbackLayout.alpha(5_500_000_000L));
        assertEquals(0.0F, CafePageFeedbackLayout.alpha(6_000_000_000L));
        assertEquals(0.0F, CafePageFeedbackLayout.alpha(20_000_000_000L));
    }
}
