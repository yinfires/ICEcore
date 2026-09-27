package com.yinfires.icecore.food;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FoodCompatibilityRulesTest {
    public static void main(String[] args) throws Exception {
        Path source = Path.of("src/main/java/com/yinfires/icecore/food/FoodConsumptionRules.java");
        String text = Files.readString(source, StandardCharsets.UTF_8);
        require(text.contains("raw_skewers"));
        require(text.contains("grilled_skewers"));
        require(text.contains("TeacupItem"));
        require(text.contains("DrinkBlockItem"));
        require(text.contains("CocktailBlockItem"));
        require(text.contains("JuiceBucketItem"));
        require(!text.contains("sakura_fubuki"));
        require(!text.contains("apple_juice"));
        require(!text.contains("sakura_wine"));
        require(!text.contains("raw_pork_belly_skewer3"));
    }

    private static void require(boolean condition) {
        if (!condition) throw new AssertionError("food compatibility contract failed");
    }
}
