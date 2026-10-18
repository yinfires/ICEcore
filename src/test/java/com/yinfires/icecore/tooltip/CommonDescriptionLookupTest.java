package com.yinfires.icecore.tooltip;

import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class CommonDescriptionLookupTest {
    @TempDir Path directory;
    @Test void explicitAssociationOverridesAutomaticAndFailedReloadRetainsBoth() throws Exception {
        var descriptions = new ItemDescriptions();
        var file = directory.resolve("item_descriptions.json");
        var stick = ResourceLocation.parse("minecraft:stick");
        var stone = ResourceLocation.parse("minecraft:stone");
        Files.writeString(file, """
            {"items":{"minecraft:stone":{"common":["automatic"]},
            "minecraft:stick":{"common":["explicit"],"blocks":["minecraft:stone"]}}}
            """);
        descriptions.reload(file);
        assertEquals("explicit", descriptions.common(stone, stone).get(0).getString());
        assertEquals("automatic", descriptions.common(ResourceLocation.parse("minecraft:dirt"), stone).get(0).getString());
        Files.writeString(file, "{\"items\":{\"minecraft:stick\":{\"blocks\":[\"bad\"]}}}");
        descriptions.reload(file);
        assertEquals("explicit", descriptions.common(stone, stone).get(0).getString());
        assertEquals("explicit", descriptions.get(stick).get(0).getString());
        Files.writeString(file, "{\"items\":{}}");
        descriptions.reload(file);
        assertTrue(descriptions.common(stone, stone).isEmpty());
        assertNull(descriptions.get(stick));
    }
}