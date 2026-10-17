package com.yinfires.icecore.mixin;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class TeaSteamerPickupContractTest {
    private static final Path ROOT = Path.of("src/main/java/com/yinfires/icecore/mixin");
    @Test void cupsReceiveBeforeStateReductionAndKeepNativeHeldInteractions() throws Exception {
        String cup = Files.readString(ROOT.resolve("WorkstationCupPickupMixin.java"));
        assertTrue(cup.contains("!player.getMainHandItem().isEmpty()) return"));
        assertTrue(cup.contains("WorkstationInventoryTransfer.giveOne"));
        assertTrue(cup.indexOf("WorkstationInventoryTransfer.giveOne") < cup.indexOf("level.setBlockAndUpdate"));
        assertTrue(cup.contains("boolean empty = cups > tea"));
        assertTrue(cup.contains("empty ? tea : tea - 1"));
        assertFalse(cup.contains("setItemInHand"));
        assertFalse(cup.contains("PlayerFeedback"));
        String ingredient = Files.readString(ROOT.resolve("WorkstationTeapotIngredientPickupMixin.java"));
        assertTrue(ingredient.indexOf("WorkstationInventoryTransfer.giveOne") < ingredient.indexOf("self.getInput().shrink(1)"));
    }
    @Test void steamerHeldFallbackIsBlockedAndFullInventoryPreservesContents() throws Exception {
        String block = Files.readString(ROOT.resolve("WorkstationSteamerBlockMixin.java"));
        assertTrue(block.contains("ISteamer;takeFood"));
        assertTrue(block.contains("hand != InteractionHand.MAIN_HAND || !player.getItemInHand(hand).isEmpty()"));
        String entity = Files.readString(ROOT.resolve("WorkstationSteamerMixin.java"));
        assertTrue(entity.contains("!player.getMainHandItem().isEmpty() || level.isClientSide"));
        assertTrue(entity.contains("stored.getCount() - remainder.getCount()"));
        assertTrue(entity.contains("if (moved <= 0) continue"));
        assertTrue(entity.indexOf("WorkstationInventoryTransfer.giveOne") < entity.indexOf("level.setBlockAndUpdate"));
        assertFalse(entity.contains("getItemToLivingEntity"));
    }
}
