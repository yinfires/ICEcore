package com.yinfires.icecore.spoilage;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ShelfLifeTest {
    @Test void fridgeAndCreativePauseResumeAndRestartPreserveRemainingDays() {
        var state = new ShelfLifeState(7, 10, false).advance(12, true);
        assertEquals(new ShelfLifeState(5, 12, true), state);
        assertEquals(new ShelfLifeState(5, 12, true), state.advance(100, true));
        var thawed = state.advance(100, false);
        assertEquals(new ShelfLifeState(5, 100, false), thawed);
        assertEquals(4, thawed.advance(101, false).days());
        assertEquals(5, thawed.advance(90, false).days());
        assertEquals(100, thawed.advance(90, false).day());
        assertEquals(0, thawed.advance(110, false).days());
    }
    @Test void calendarBoundariesAndForwardJumpsNeverRestoreFreshness() {
        assertEquals(7, ShelfLifeMath.remaining(7, 10, 10));
        assertEquals(6, ShelfLifeMath.remaining(7, 10, 11));
        assertEquals(2, ShelfLifeMath.remaining(7, 10, 15));
        assertEquals(0, ShelfLifeMath.remaining(7, 10, 17));
        assertEquals(0, ShelfLifeMath.remaining(7, 10, Long.MAX_VALUE));
        assertEquals(3, ShelfLifeMath.remaining(3, 15, 10));
    }
    @Test void manualTransferConservesPartialSourceAndHonoursRightClickAndSlotLimit() {
        assertEquals(4, ShelfLifeMath.moved(20, 60, 64, false));
        assertEquals(1, ShelfLifeMath.moved(20, 60, 64, true));
        assertEquals(0, ShelfLifeMath.moved(20, 64, 64, false));
        assertEquals(1, ShelfLifeMath.moved(20, 0, 1, false));
        assertEquals(0, ShelfLifeMath.moved(0, 0, 64, false));
    }
    @Test void onlyExplicitGroupsApplyAndWaterAlwaysRemainsExcluded() {
        var rules = ShelfLifeRules.parse(JsonParser.parseString("""
                {"groups":{"7":["minecraft:carrot","minecraft:water_bucket"],"1":["minecraft:bread"]},
                 "excluded":["minecraft:bread"]}
                """));
        assertEquals(7, rules.items().get("minecraft:carrot"));
        assertFalse(rules.items().containsKey("minecraft:water_bucket"));
        assertFalse(rules.items().containsKey("minecraft:bread"));
        assertFalse(rules.items().containsKey("minecraft:apple"));
        assertThrows(IllegalArgumentException.class, () -> ShelfLifeRules.parse(JsonParser.parseString("{\"groups\":{\"0\":[\"minecraft:carrot\"]}}")));
        assertThrows(IllegalArgumentException.class, () -> ShelfLifeRules.parse(JsonParser.parseString("{\"groups\":{\"1\":[\"minecraft:carrot\"],\"7\":[\"minecraft:carrot\"]}}")));
    }
}
