package com.yinfires.icecore.food;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FoodConsumptionRulesTest {
    private static String source(String name) throws Exception {
        return Files.readString(Path.of("src/main/java/com/yinfires/icecore/food", name),
                StandardCharsets.UTF_8);
    }

    @Test
    void recognizesFoodPropertiesAndModTagsWithoutExampleIdHardcoding() throws Exception {
        String rules = source("FoodConsumptionRules.java");
        assertTrue(rules.contains("stack.getFoodProperties(entity) != null"));
        assertFalse(rules.contains("UseAnim"));
        assertTrue(rules.contains("TagKey"));
        assertTrue(rules.contains("raw_skewers"));
        assertTrue(rules.contains("grilled_skewers"));
        assertTrue(rules.contains("MilkBucketItem"));
        assertTrue(rules.contains("PotionItem"));
        assertFalse(rules.contains("sakura_fubuki"));
        assertFalse(rules.contains("apple_juice"));
        assertFalse(rules.contains("sakura_wine"));
        assertFalse(rules.contains("raw_pork_belly_skewer3"));
    }

    @Test
    void onlyCancelsTheStartedUseOfPlayerFood() throws Exception {
        String events = source("FoodConsumptionEvents.java");
        assertTrue(events.contains("LivingEntityUseItemEvent.Start"));
        assertTrue(events.contains("event.getEntity() instanceof Player"));
        assertTrue(events.contains("FoodConsumptionRules.isFood(event.getItem(), player)"));
        assertTrue(events.contains("event.setCanceled(true)"));
        assertFalse(events.contains("PlayerInteractEvent"));
        String input = source("FoodConsumptionClientInputEvents.java");
        assertTrue(input.contains("BlockHitResult"));
        assertTrue(input.contains("EntityHitResult"));
        assertTrue(input.contains("normal block interaction pipeline"));

        String gameModeMixin = Files.readString(
                Path.of("src/main/java/com/yinfires/icecore/mixin/MultiPlayerGameModeMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(gameModeMixin.contains("m_233721_"));
        assertTrue(gameModeMixin.contains("FoodConsumptionRules.isFood(player.getItemInHand(hand), player)"));
        assertTrue(gameModeMixin.contains("callback.setReturnValue(InteractionResult.PASS)"));
    }

    @Test
    void dedicatedServerValueIsSynchronizedToClients() throws Exception {
        String events = source("FoodConsumptionEvents.java");
        String clientState = source("FoodConsumptionClientState.java");
        assertTrue(events.contains("PlayerLoggedInEvent"));
        assertTrue(events.contains("ClientBoundFoodConsumptionConfigPacket"));
        assertTrue(events.contains("player.level().isClientSide"));
        assertTrue(clientState.contains("synchronizedFromServer ? disabled"));
    }

    @Test
    void clientInputBlocksUseBeforeCustomRightClickAudio() throws Exception {
        String input = source("FoodConsumptionClientInputEvents.java");
        assertTrue(input.contains("InteractionKeyMappingTriggered"));
        assertTrue(input.contains("EventPriority.HIGHEST"));
        assertTrue(input.contains("event.setCanceled(true)"));
        assertTrue(input.contains("event.setSwingHand(false)"));
        assertTrue(input.contains("stopUsingItem"));
    }
}
