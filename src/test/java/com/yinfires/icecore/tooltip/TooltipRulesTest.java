package com.yinfires.icecore.tooltip;

import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TooltipRulesTest {
    @TempDir Path directory;

    @Test void allSwitchCombinationsKeepLaterRecipeAndCafeDetails() {
        Component name = Component.literal("name");
        Component flavor = Component.literal("flavor");
        Component effect = Component.translatable("effect.minecraft.speed");
        List<Component> context = List.of(Component.translatable("recipe.icecore.oven.batch"),
                Component.translatable("recipe.icecore.oven.time", 20),
                Component.translatable("gui.cozycafe.menu_selector.price", 100));
        for (boolean hideOriginal : new boolean[]{false, true}) {
            for (boolean hideEffects : new boolean[]{false, true}) {
                var lines = new ArrayList<>(List.of(name, flavor, effect));
                TooltipRules.applyOriginal(lines, null, hideOriginal, hideEffects);
                assertEquals(!hideOriginal, lines.contains(flavor));
                assertEquals(!hideOriginal && !hideEffects, lines.contains(effect));
                lines.addAll(context); // Forge/JEI add these after original filtering.
                if (hideEffects) TooltipRules.removeFoodEffects(lines);
                assertTrue(lines.containsAll(context));
                List<Component> override = List.of(Component.literal("ICEcore description"));
                TooltipRules.applyOriginal(lines, override, hideOriginal, hideEffects);
                assertEquals(List.of(name, override.get(0)), lines);
            }
        }
    }

    @Test void preservesNameIdentityAndReplacesEveryOriginalLine() {
        Component name = Component.literal("Custom name").withStyle(ChatFormatting.GOLD);
        var lines = new ArrayList<>(List.of(name, Component.literal("mod name"), Component.literal("armor")));
        TooltipRules.replaceBody(lines, List.of(Component.translatable("description.icecore.test")));
        assertSame(name, lines.get(0));
        assertEquals(2, lines.size());
        TooltipRules.replaceBody(lines, List.of());
        assertEquals(List.of(name), lines);
        TooltipRules.replaceBody(lines, null);
        assertEquals(List.of(name), lines);
    }

    @Test void removesEffectsAndOnlyTheirAttributeSectionInAnyLanguage() {
        var name = Component.literal("food");
        var flavor = Component.literal("flavor");
        var equipment = Component.translatable("item.modifiers.mainhand");
        var modifier = Component.translatable("attribute.modifier.plus.0", 3, "attack");
        var lines = new ArrayList<Component>(List.of(name, flavor,
                Component.empty().append(Component.translatable("potion.withDuration",
                        Component.translatable("effect.minecraft.speed"), "1:00")),
                Component.translatable("potion.whenDrank"), modifier, equipment, modifier));
        TooltipRules.removeFoodEffects(lines);
        assertEquals(List.of(name, flavor, equipment, modifier), lines);
    }

    @Test void reloadIsAtomicAndSupportsDeletionEmptyOverridesAndComponents() throws Exception {
        Path file = directory.resolve("descriptions.json");
        ItemDescriptions descriptions = new ItemDescriptions();
        ResourceLocation apple = ResourceLocation.parse("minecraft:apple");
        descriptions.reload(file);
        assertNull(descriptions.get(apple));
        Files.writeString(file, """
                {"items":{"minecraft:apple":["第一行",{"translate":"test.description","color":"gray"}],
                          "minecraft:stick":[]}}
                """);
        descriptions.reload(file);
        assertEquals("第一行", descriptions.get(apple).get(0).getString());
        assertEquals(ChatFormatting.GRAY.getColor(), descriptions.get(apple).get(1).getStyle().getColor().getValue());
        assertEquals(List.of(), descriptions.get(ResourceLocation.parse("minecraft:stick")));
        for (String bad : List.of("{", "{\"items\":{\"apple\":[]}}", "{\"items\":{\"minecraft:apple\":[null]}}",
                "{\"items\":{\"minecraft:apple\":[true]}}")) {
            Files.writeString(file, bad);
            descriptions.reload(file);
            assertEquals(2, descriptions.get(apple).size());
        }
        Files.writeString(file, "{\"items\":{}}");
        descriptions.reload(file);
        assertNull(descriptions.get(apple));
        Files.delete(file);
        descriptions.reload(file);
        assertNull(descriptions.get(apple));
    }

    @Test void rejectsInvalidSchema() {
        for (String bad : List.of("[]", "{}", "{\"items\":null}", "{\"items\":{\"INVALID:id\":[]}}")) {
            assertThrows(IllegalArgumentException.class, () -> ItemDescriptions.parse(JsonParser.parseString(bad)));
        }
    }
}
