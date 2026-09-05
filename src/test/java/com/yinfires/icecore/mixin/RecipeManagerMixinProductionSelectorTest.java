package com.yinfires.icecore.mixin;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards {@code RecipeManagerMixin} against reintroducing development names: the no-refmap project
 * requires production SRG names and full descriptors for every Minecraft selector.
 */
final class RecipeManagerMixinProductionSelectorTest {
    private static String source() throws IOException {
        return Files.readString(
                Path.of("src/main/java/com/yinfires/icecore/mixin/RecipeManagerMixin.java"),
                StandardCharsets.UTF_8);
    }

    @Test
    void recipeManagerSelectorsUseProductionNamesAndDescriptors() throws IOException {
        String source = source();
        // Only byType + byKey are filtered (the craft path). getRecipes is intentionally NOT filtered.
        assertTrue(source.contains("m_44054_(Lnet/minecraft/world/item/crafting/RecipeType;)Ljava/util/Map;"));
        assertTrue(source.contains("m_44043_(Lnet/minecraft/resources/ResourceLocation;)Ljava/util/Optional;"));
        assertFalse(source.contains("m_44051_"), "getRecipes must not be filtered (breaks JEI + server self-lookup)");
        // No official / development method names.
        assertFalse(source.contains("\"byType\"") || source.contains("\"byKey\"") || source.contains("\"getRecipes\""));
    }
}
