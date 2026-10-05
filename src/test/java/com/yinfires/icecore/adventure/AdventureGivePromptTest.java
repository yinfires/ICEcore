package com.yinfires.icecore.adventure;

import com.yinfires.icecore.compat.smartkeyprompts.AdventureGivePrompt;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class AdventureGivePromptTest {
    @Test
    void cachedPromptSurvivesBindingChangesWithoutMutatingQueuedEntry() throws Exception {
        Class<?> hud = Class.forName("com.mafuyu404.smartkeyprompts.init.HUD");
        Set<?> cache = (Set<?>) hud.getField("KeyPromptCache").get(null);
        cache.clear();
        try {
            AdventureGivePrompt.submit("key.keyboard.q");
            Object original = cache.iterator().next();
            AdventureGivePrompt.submit("key.keyboard.q");
            assertEquals(1, cache.size());
            assertSame(original, cache.iterator().next());
            AdventureGivePrompt.submit("key.keyboard.g");
            assertEquals("key.keyboard.q", original.getClass().getField("key").get(original));
            assertEquals(2, cache.size());
            assertEquals("icecore_adventure_give", original.getClass().getField("group").get(original));
            assertEquals("icecore.adventure_give.hud", original.getClass().getField("desc").get(original));
            assertEquals(true, original.getClass().getField("isCustom").get(original));
            cache.clear(); // SmartKeyPrompts consumes and clears this set every START.
            AdventureGivePrompt.submit("key.keyboard.g");
            assertEquals(1, cache.size());
        } finally {
            cache.clear();
        }
    }

    @Test
    void optionalClientEntryUsesEndPhaseAndNeverDrawsItsOwnHud() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/yinfires/icecore/adventure/AdventureItemEvents.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("event.phase != TickEvent.Phase.END || !SMART_KEY_PROMPTS"));
        assertTrue(source.contains("isLoaded(\"smartkeyprompts\")"));
        assertTrue(source.contains("minecraft.screen != null"));
        assertTrue(source.contains("minecraft.player.getMainHandItem().isEmpty()"));
        assertTrue(source.contains("target == minecraft.player"));
        assertTrue(source.contains("minecraft.options.keyDrop.getKey().getName()"));
        assertFalse(source.contains("RenderGuiEvent"));
        assertFalse(source.contains("com.mafuyu404"));
    }
}
