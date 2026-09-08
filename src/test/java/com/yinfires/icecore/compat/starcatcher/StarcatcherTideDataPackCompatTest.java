package com.yinfires.icecore.compat.starcatcher;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarcatcherTideDataPackCompatTest {
    private static final String RESOURCE =
            "/resourcepacks/starcatcher_tide_compat/data/tide/starcatcher/fish/shooting_starfish.json";

    @Test
    void shootingStarfishUsesCurrentBaitArraySchema() throws IOException {
        try (var stream = getClass().getResourceAsStream(RESOURCE)) {
            assertNotNull(stream, "compatibility data pack resource is missing");
            JsonObject fish = JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
            JsonObject baitRestriction = fish.getAsJsonArray("restrictions").asList().stream()
                    .map(element -> element.getAsJsonObject())
                    .filter(restriction -> "starcatcher:bait".equals(restriction.get("type").getAsString()))
                    .findFirst()
                    .orElseThrow();

            assertTrue(baitRestriction.get("baits").isJsonArray());
            JsonArray baits = baitRestriction.getAsJsonArray("baits");
            assertEquals(1, baits.size());
            assertEquals("starcatcher:legendary_bait",
                    baits.get(0).getAsJsonObject().get("id").getAsString());
            assertEquals(50, baits.get(0).getAsJsonObject().get("extra_chance").getAsInt());
        }
    }

    @Test
    void packIsRegisteredBelowStarcatcherBuiltInPacks() throws IOException {
        Path source = Path.of("src/main/java/com/yinfires/icecore/compat/starcatcher/StarcatcherTideDataPackCompat.java");
        String text = Files.readString(source, StandardCharsets.UTF_8);
        assertTrue(text.contains("Pack.Position.TOP"));
        assertTrue(text.contains("\"mod/starcatcher:built_in_datapacks/tide_compat\""),
                "the replacement must use Starcatcher's built-in pack id");
    }
}
