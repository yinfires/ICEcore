package com.yinfires.icecore.workstation;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class TrashCanEntryTest {
    @Test void entryStopsAfterNormalLandingWithoutChangingStorageOrOtherEntities() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/yinfires/icecore/mixin/WorkstationTrashEnterMixin.java"));
        assertTrue(source.contains("shift = At.Shift.AFTER"));
        assertTrue(source.contains("HorizontalDirectionalBlock;m_142072_"));
        assertTrue(source.contains("if (entity instanceof Player) ci.cancel()"));
        assertFalse(source.contains("startRiding"));
        assertFalse(source.contains("entityInside"));
        assertFalse(source.contains("getStorage"));
        assertTrue(Files.readString(Path.of("src/main/resources/icecore.mixins.json")).contains("WorkstationTrashEnterMixin"));
    }
}
