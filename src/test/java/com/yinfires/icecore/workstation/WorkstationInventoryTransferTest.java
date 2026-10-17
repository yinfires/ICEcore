package com.yinfires.icecore.workstation;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class WorkstationInventoryTransferTest {
    private static final Path ROOT = Path.of("src/main/java/com/yinfires/icecore");
    @Test void ordinaryInventoryAndSingleItemAreUsedBeforeConsumingContents() throws Exception {
        String transfer = Files.readString(ROOT.resolve("workstation/WorkstationInventoryTransfer.java"));
        assertTrue(transfer.contains("PlayerMainInvWrapper(player.getInventory())"));
        assertTrue(transfer.contains("stack.copyWithCount(1)"));
        assertTrue(transfer.indexOf("insert(player, single, true)") < transfer.indexOf("insert(player, single, false)"));
        assertFalse(transfer.contains("selected"));
        assertFalse(transfer.contains("setItemInHand"));
        assertFalse(transfer.contains("placeItemBackInInventory"));
        String service = Files.readString(ROOT.resolve("workstation/WorkstationInteractionService.java"));
        assertFalse(service.contains("setItemInHand"));
        assertTrue(service.indexOf("if (!give(player, hand, held, output") < service.indexOf("adapter.takeOutput()"));
        assertTrue(service.indexOf("if (!give(player, hand, held, item") < service.indexOf("remainder.shrink(1)"));
    }
    @Test void ovenAndMixingUseSharedReceptionWithNoDirectHandReplacement() throws Exception {
        for (String name : new String[]{"oven/OvenBlock.java", "mixing/MixingBowlBlock.java"}) {
            String block = Files.readString(ROOT.resolve(name));
            assertTrue(block.contains("WorkstationInventoryTransfer.giveOne"));
            assertFalse(block.contains("setItemInHand"));
        }
        for (String name : new String[]{"oven/OvenBlockEntity.java", "mixing/MixingBowlBlockEntity.java"}) {
            String entity = Files.readString(ROOT.resolve(name));
            assertTrue(entity.contains("receive.test("));
            assertTrue(entity.contains("copyWithCount(1)"));
            assertTrue(entity.contains("shrink(1)"));
        }
    }
}
