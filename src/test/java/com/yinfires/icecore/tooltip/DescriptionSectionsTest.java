package com.yinfires.icecore.tooltip;

import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DescriptionSectionsTest {
    @Test void sectionsKeepOrderColorsAndActionMarkerAfterCopy() {
        var snapshot = ItemDescriptions.parseSnapshot(JsonParser.parseString("""
            {"items":{"minecraft:stick":{"description":["ordinary"],"common":["shared"],
            "placement":["厨房中","灶台上"],"actions":[{"keys":["shift","右键"],"text":"倒茶"}],
            "blocks":["minecraft:stone"]}}}
            """));
        var entry = snapshot.items().get(ResourceLocation.parse("minecraft:stick"));
        assertEquals(4, entry.lines().size());
        assertEquals("ordinary", entry.lines().get(0).getString());
        assertEquals("shared", entry.lines().get(1).getString());
        assertEquals(0xAAAAAA, entry.lines().get(0).getStyle().getColor().getValue());
        assertEquals(0xAAAAAA, entry.lines().get(1).getStyle().getColor().getValue());
        assertEquals(0x55FF55, entry.lines().get(2).getStyle().getColor().getValue());
        var action = assertInstanceOf(DescriptionAction.class, entry.lines().get(3).copy().getContents());
        assertEquals(0xFFFFAA, action.text().getStyle().getColor().getValue());
        assertEquals(entry, snapshot.blocks().get(ResourceLocation.parse("minecraft:stone")));
    }
    @Test void emptySectionsAndExplicitStyles() {
        var empty = ItemDescriptions.parse(JsonParser.parseString("""
            {"items":{"minecraft:stick":{},"minecraft:stone":{"description":[],"common":[],"placement":[],"actions":[]}}}
            """));
        assertTrue(empty.values().stream().allMatch(java.util.List::isEmpty));
        var styled = ItemDescriptions.parse(JsonParser.parseString("""
            {"items":{"minecraft:stick":{"common":[{"text":"red","color":"red"}]}}}
            """));
        assertEquals(0xFF5555, styled.get(ResourceLocation.parse("minecraft:stick")).get(0).getStyle().getColor().getValue());
    }
    @Test void conflictingAssociationsAndMalformedSectionsAreRejected() {
        for (String json : java.util.List.of(
            "{\"items\":{\"minecraft:stick\":{\"blocks\":[\"minecraft:stone\"]},\"minecraft:apple\":{\"blocks\":[\"minecraft:stone\"]}}}",
            "{\"items\":{\"minecraft:stick\":{\"common\":null}}}",
            "{\"items\":{\"minecraft:stick\":{\"actions\":[{\"keys\":[],\"text\":\"x\"}]}}}"))
            assertThrows(IllegalArgumentException.class, () -> ItemDescriptions.parse(JsonParser.parseString(json)));
    }
    @Test void fixedKeyAliasesIgnoreCaseAndKeepUnknownNames() {
        for (String key : java.util.List.of("shift", "SHIFT", "Shift"))
            assertEquals("key.keyboard.left.shift", FixedDescriptionKeys.canonical(key));
        for (String key : java.util.List.of("ctrl", "CTRL", "control"))
            assertEquals("key.keyboard.left.control", FixedDescriptionKeys.canonical(key));
        assertEquals("key.keyboard.right.shift", FixedDescriptionKeys.canonical("rshift"));
        assertEquals("key.mouse.right", FixedDescriptionKeys.canonical("右键"));
        assertEquals("key.keyboard.a", FixedDescriptionKeys.canonical("A"));
        assertEquals("自定义", FixedDescriptionKeys.canonical("自定义"));
    }
}