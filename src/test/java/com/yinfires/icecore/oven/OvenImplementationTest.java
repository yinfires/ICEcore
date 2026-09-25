package com.yinfires.icecore.oven;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class OvenImplementationTest {
    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }

    @Test void sharedContainerIsNineSingleItemSlotsWithoutFluidTank() throws IOException {
        String entity = read("src/main/java/com/yinfires/icecore/oven/OvenBlockEntity.java");
        assertTrue(entity.contains("CAPACITY=9"));
        assertTrue(entity.contains("copyWithCount(1)"));
        assertFalse(entity.contains("FluidTank"));
        assertFalse(entity.contains("s.grow"));
    }

    @Test void heatSourceAndContainerMetadataAreUnifiedAndPersisted() throws IOException {
        String heat = read("src/main/java/com/yinfires/icecore/oven/OvenHeatSourceCompat.java");
        String block = read("src/main/java/com/yinfires/icecore/oven/OvenBlock.java");
        String entity = read("src/main/java/com/yinfires/icecore/oven/OvenBlockEntity.java");
        assertTrue(heat.contains("farmersdelight") && heat.contains("heat_sources"));
        assertTrue(heat.contains("HEAT_SOURCES"));
        assertFalse(block.contains("minecraft:campfire"));
        assertFalse(block.contains("minecraft:fire"));
        assertFalse(block.contains("farmersdelight:stove"));
        assertTrue(block.contains("FluidContainerMode.STORED_AS_ITEM"));
        assertTrue(entity.contains("t.put(\"Containers\""));
        assertTrue(entity.contains("containers.get(i).isEmpty()"));
        assertTrue(entity.contains("!OvenHeatSourceCompat.isHeatSource"));
        assertTrue(entity.contains("items.set(i,ItemStack.EMPTY);containers.set(i,ItemStack.EMPTY)"));
        assertTrue(entity.contains("Arrays.fill(sequence,0)"));
        assertTrue(entity.contains("onDataPacket"));
        assertTrue(entity.contains("handleUpdateTag"));
    }

    @Test void rendererUsesFixedVerticalStackAndJeiUsesNineByNineDisplay() throws IOException {
        String renderer = read("src/main/java/com/yinfires/icecore/oven/OvenRenderer.java");
        String jei = read("src/main/java/com/yinfires/icecore/oven/client/OvenJeiPlugin.java");
        assertTrue(renderer.contains("translate(.5F,.145F+i*.035F,.5F)"));
        assertFalse(renderer.contains("i%2"));
        assertTrue(renderer.contains("scale(.43F,.43F,.43F)"));
        assertTrue(jei.contains("(i%3)*18"));
        assertTrue(jei.contains("(i/3)*18"));
        assertTrue(jei.contains("0xFFFFFFFF"));
        assertFalse(jei.contains("getRecipeArrow"));
        assertTrue(jei.contains("translate(0,0,200)"));
        assertTrue(jei.contains("sx+3,sy+1"));
        assertTrue(jei.contains("ChatFormatting.GRAY"));
        assertTrue(jei.contains("getBackground"));
        assertTrue(jei.contains("textures/gui/oven.png"));
        assertFalse(jei.contains("addText"));
        assertFalse(jei.contains("extradelight\",\"textures/gui/oven.png"));
    }

    @Test void recipeSeparatesSingleUnorderedAndOptionalBatchModes() throws IOException {
        String recipe = read("src/main/java/com/yinfires/icecore/oven/OvenRecipe.java");
        assertTrue(recipe.contains("processing"));
        assertTrue(recipe.contains("mode"));
        assertTrue(recipe.contains("max_batches"));
        assertTrue(recipe.contains("batch ? Math.min(limit, maxBatches) : 1"));
    }

    @Test void testRecipeLivesOnlyInClientKubeJs() throws IOException {
        assertFalse(Files.exists(Path.of("src/main/resources/data/icecore/recipes/oven_juicer_bread.json")));
        Path script = Path.of("D:/Minecraft/PCL启动器/.minecraft/versions/异次元餐厅/kubejs/server_scripts/icecore_oven_test.js");
        assertTrue(Files.exists(script));
        String source = Files.readString(script, StandardCharsets.UTF_8);
        assertTrue(source.contains("kaleidoscope_fragrantorchard:juicer"));
        assertTrue(source.contains("minecraft:bread"));
        String[] roots = {"src/main/java", "src/main/resources", "build/resources/main"};
        for (String root : roots) {
            if (!Files.exists(Path.of(root))) continue;
            try (var paths = Files.walk(Path.of(root))) {
                paths.filter(Files::isRegularFile).forEach(path -> {
                    try {
                        String text = Files.readString(path, StandardCharsets.UTF_8);
                        assertFalse(text.contains("kaleidoscope_fragrantorchard:juicer"), "test recipe leaked into " + path);
                    } catch (IOException e) { throw new RuntimeException(e); }
                });
            }
        }
    }
}

