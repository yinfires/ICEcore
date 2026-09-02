package com.yinfires.icecore.npc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class NpcRotationTest {
    @Test void snapsAllEightDirectionsAndWraps() {
        assertEquals(0.0F, NpcRotation.snapToEightDirections(1.0F));
        assertEquals(45.0F, NpcRotation.snapToEightDirections(44.0F));
        assertEquals(90.0F, NpcRotation.snapToEightDirections(91.0F));
        assertEquals(135.0F, NpcRotation.snapToEightDirections(134.0F));
        assertEquals(-180.0F, NpcRotation.snapToEightDirections(181.0F));
        assertEquals(-135.0F, NpcRotation.snapToEightDirections(226.0F));
        assertEquals(-90.0F, NpcRotation.snapToEightDirections(271.0F));
        assertEquals(-45.0F, NpcRotation.snapToEightDirections(314.0F));
        assertEquals(0.0F, NpcRotation.snapToEightDirections(720.0F));
    }

    @Test void midpointUsesJavaRoundTieRule() {
        assertEquals(45.0F, NpcRotation.snapToEightDirections(22.5F));
        assertEquals(0.0F, NpcRotation.snapToEightDirections(-22.5F));
        assertEquals(-45.0F, NpcRotation.snapToEightDirections(-22.51F));
    }
}
