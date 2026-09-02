package com.yinfires.icecore.npc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class NpcResourcesTest {
    private static JsonObject read(String path) throws IOException {
        return JsonParser.parseString(Files.readString(Path.of(path), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test void fireDefinitionIsValidAndSlim() throws IOException {
        JsonObject json = read("src/main/resources/data/icecore/icecore/npcs/fire.json");
        NpcDefinition definition = NpcDefinitionManager.parse(ResourceLocation.parse("icecore:fire"), json);
        assertEquals("npc.icecore.fire", definition.nameKey());
        assertTrue(definition.slim());
        assertEquals(ResourceLocation.parse("icecore:npc/fire"), definition.dialogue());
        assertEquals("greeting", definition.dialogueGroup());
    }

    @Test void dialogueLoopsAndGoodbyeClosesPastLastNode() throws IOException {
        JsonObject json = read("src/main/resources/data/icecore/chatbox/dialogues/npc/fire.json");
        JsonArray nodes = json.getAsJsonObject("dialogues").getAsJsonArray("greeting");
        assertEquals(2, nodes.size());
        assertEquals("1", nodes.get(0).getAsJsonObject().getAsJsonArray("options").get(0).getAsJsonObject().get("next").getAsString());
        assertEquals("0", nodes.get(1).getAsJsonObject().getAsJsonArray("options").get(0).getAsJsonObject().get("next").getAsString());
        for (var node : nodes) assertEquals("2", node.getAsJsonObject().getAsJsonArray("options").get(1).getAsJsonObject().get("next").getAsString());
    }

    @Test void languageKeySetsStayIdentical() throws IOException {
        Set<String> chinese = read("src/main/resources/assets/icecore/lang/zh_cn.json").keySet();
        Set<String> english = read("src/main/resources/assets/icecore/lang/en_us.json").keySet();
        assertEquals(chinese, english);
    }
}
