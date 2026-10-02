package com.yinfires.icecore.item;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SeedBagRenderingContractTest {
    private static final Path JAVA_ROOT = Path.of("src/main/java/com/yinfires/icecore/item");
    private static final Path MODEL_ROOT = Path.of("src/main/resources/assets/icecore/models/item");
    private static final Path TEXTURE_ROOT = Path.of("src/main/resources/assets/icecore/textures/item");

    @Test
    void itemModelsTemporarilyUseTheirProduceTextures() throws IOException {
        assertGeneratedModel("wheat_seed_bag.json", "minecraft:item/wheat");
        assertGeneratedModel("carrot_seed_bag.json", "minecraft:item/carrot");
        assertFalse(Files.exists(TEXTURE_ROOT.resolve("seed_bag.png")));
    }

    @Test
    void customCompositeRenderingHasBeenRemoved() throws IOException {
        assertFalse(Files.exists(JAVA_ROOT.resolve("SeedBagBakedModel.java")));
        assertFalse(Files.exists(JAVA_ROOT.resolve("SeedBagClientModels.java")));
        assertFalse(Files.exists(JAVA_ROOT.resolve("SeedBagRenderer.java")));

        String itemSource = read(JAVA_ROOT.resolve("SeedBagItem.java"));
        assertFalse(itemSource.contains("initializeClient"));
        assertFalse(itemSource.contains("IClientItemExtensions"));

        String definitionSource = read(JAVA_ROOT.resolve("SeedBagDefinition.java"));
        assertFalse(definitionSource.contains("labelX"));
        assertFalse(definitionSource.contains("labelY"));
        assertFalse(definitionSource.contains("labelScale"));
    }

    private static void assertGeneratedModel(String fileName, String texture) throws IOException {
        String json = read(MODEL_ROOT.resolve(fileName));
        assertTrue(json.contains("\"parent\":\"minecraft:item/generated\""));
        assertTrue(json.contains("\"layer0\":\"" + texture + "\""));
        assertFalse(json.contains("icecore:item/seed_bag"));
        assertFalse(json.contains("builtin/entity"));
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
