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
    void itemModelsUseTheirDedicatedSeedBagTextures() throws IOException {
        assertGeneratedModel("wheat_seed_bag.json", "icecore:item/wheat_seed_bag");
        assertGeneratedModel("carrot_seed_bag.json", "icecore:item/carrot_seed_bag");
        assertGeneratedModel("potato_seed_bag.json", "icecore:item/potato_seed_bag");
        assertGeneratedModel("beetroot_seed_bag.json", "icecore:item/beetroot_seed_bag");
        assertGeneratedModel("soybean_seed_bag.json", "icecore:item/soybean_seed_bag");
        assertGeneratedModel("redbean_seed_bag.json", "icecore:item/redbean_seed_bag");
        assertGeneratedModel("strawberry_seed_bag.json", "icecore:item/strawberry_seed_bag");
        assertGeneratedModel("corn_seed_bag.json", "icecore:item/corn_seed_bag");
        assertGeneratedModel("sweet_potato_seed_bag.json", "icecore:item/sweet_potato_seed_bag");
        assertGeneratedModel("houttuynia_seed_bag.json", "icecore:item/houttuynia_seed_bag");
        assertGeneratedModel("tomato_seed_bag.json", "icecore:item/tomato_seed_bag");
        assertGeneratedModel("chili_seed_bag.json", "icecore:item/chili_seed_bag");
        assertGeneratedModel("eggplant_seed_bag.json", "icecore:item/eggplant_seed_bag");
        assertGeneratedModel("onion_seed_bag.json", "icecore:item/onion_seed_bag");
        assertGeneratedModel("cabbage_seed_bag.json", "icecore:item/cabbage_seed_bag");
        assertGeneratedModel("cucumber_seed_bag.json", "icecore:item/cucumber_seed_bag");
        assertGeneratedModel("rice_seed_bag.json", "icecore:item/rice_seed_bag");
        assertGeneratedModel("tea_seed_bag.json", "icecore:item/tea_seed_bag");
        assertGeneratedModel("lime_seed_bag.json", "icecore:item/lime_seed_bag");
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
        assertTrue(Files.exists(TEXTURE_ROOT.resolve(fileName.replace(".json", ".png"))));
        assertFalse(json.contains("builtin/entity"));
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
