package com.yinfires.icecore.food;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FoodConsumptionRulesTest {
    @Test
    void rejectedUseOnlySuppressesFeedbackAfterExternalInteraction() throws Exception {
        String feedback = Files.readString(Path.of("src/main/java/com/yinfires/icecore/mixin/FoodUseFeedbackMixin.java"));
        String local = Files.readString(Path.of("src/main/java/com/yinfires/icecore/mixin/FoodUseLocalPlayerMixin.java"));
        assertTrue(source("FoodConsumptionEvents.java").contains("BlockedFoodUse.record(player, event.getItem())"));
        assertTrue(feedback.contains("BlockedFoodUse.begin()"));
        assertTrue(feedback.contains("BlockedFoodUse.clear()"));
        assertTrue(feedback.contains("m_109320_(Lnet/minecraft/world/InteractionHand;)V"));
        assertTrue(feedback.contains("renderer.itemUsed(hand)"));
        assertFalse(feedback.contains("cancellable = true"));
        assertFalse(feedback.contains("FoodConsumptionRules"));
        assertTrue(local.contains("@At(\"RETURN\")"));
        assertTrue(local.contains("player.getUseItem().isEmpty()"));
        assertTrue(local.contains("player.stopUsingItem()"));
        assertTrue(source("BlockedFoodUse.java").contains("stack == source.getItemInHand(hand)"));
    }

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
        assertFalse(events.contains("player.stopUsingItem()"));
        assertFalse(events.contains("isKaleidoscopeSkewerThreading"));
        String input = source("FoodConsumptionClientInputEvents.java");
        assertFalse(input.contains("event.setCanceled(true)"));
        assertFalse(input.contains("event.setSwingHand(false)"));
        assertFalse(input.contains("stopUsingItem"));

        String gameModeMixin = Files.readString(
                Path.of("src/main/java/com/yinfires/icecore/mixin/MultiPlayerGameModeMixin.java"),
                StandardCharsets.UTF_8);
        assertFalse(gameModeMixin.contains("m_233721_"));
        assertFalse(gameModeMixin.contains("FoodConsumptionRules"));
        assertTrue(gameModeMixin.contains("icecore$blockWrenchVanillaUse"));
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
    void clientInputDoesNotPreemptExternalFoodInteractions() throws Exception {
        String input = source("FoodConsumptionClientInputEvents.java");
        assertFalse(input.contains("InteractionKeyMappingTriggered"));
        assertFalse(input.contains("EventPriority.HIGHEST"));
        assertFalse(input.contains("event.setCanceled(true)"));
        assertFalse(input.contains("event.setSwingHand(false)"));
        assertFalse(input.contains("stopUsingItem"));
        String rules = source("FoodConsumptionRules.java");
        assertTrue(rules.contains("kaleidoscope_grilling"));
        assertFalse(rules.contains("Items.STICK"));
        assertFalse(rules.contains("getOffhandItem"));
        assertFalse(rules.contains("getMainHandItem"));
    }
}
