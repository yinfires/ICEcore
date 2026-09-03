package com.yinfires.icecore.npc;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The built-in "fire" NPC (definition, dialogue, texture) was moved out of the mod into the
 * 咖啡馆 client datapack; the mod ships only the NPC system, not specific NPCs. This test now
 * guards only that the mod's own language files keep identical key sets across locales.
 */
final class NpcResourcesTest {
    private static JsonObject read(String path) throws IOException {
        return JsonParser.parseString(Files.readString(Path.of(path), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test void languageKeySetsStayIdentical() throws IOException {
        Set<String> chinese = read("src/main/resources/assets/icecore/lang/zh_cn.json").keySet();
        Set<String> english = read("src/main/resources/assets/icecore/lang/en_us.json").keySet();
        assertEquals(chinese, english);
    }
}
