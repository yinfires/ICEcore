package com.yinfires.icecore.farming;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Source/resource contract for seed-bag farming. Normal JUnit cannot bootstrap the registries, so
 * the definitions are read from source and cross-checked against models, loot tables, lang and mixins.
 */
final class SeedBagFarmingContractTest {
    private static final Path SRC = Path.of("src/main/java/com/yinfires/icecore");
    private static final Path RES = Path.of("src/main/resources");
    private static final Pattern DEFINITION = Pattern.compile(
            "(?:single\\(|new SeedBagDefinition\\()\"([a-z_]+)\",\\s*(?:id\\()?\"([a-z_]+:[a-z_]+)\"");

    /** Seed-bag id, crop block, loot-table block providing the seed bag. */
    private static final List<String[]> EXPECTED = List.of(
            new String[]{"wheat", "minecraft:wheat"}, new String[]{"carrot", "minecraft:carrots"},
            new String[]{"potato", "minecraft:potatoes"}, new String[]{"beetroot", "minecraft:beetroots"},
            new String[]{"soybean", "youkaisfeasts:soybean"}, new String[]{"redbean", "youkaisfeasts:redbean"},
            new String[]{"strawberry", "farm_and_charm:strawberry_crop"}, new String[]{"corn", "farm_and_charm:corn_crop"},
            new String[]{"sweet_potato", "kaleidoscope_grilling:sweet_potato_crop"},
            new String[]{"houttuynia", "kaleidoscope_grilling:houttuynia_crop"},
            new String[]{"tomato", "kaleidoscope_cookery:tomato_crop"}, new String[]{"chili", "kaleidoscope_cookery:chili_crop"},
            new String[]{"eggplant", "kaleidoscope_chinesefood:eggplant_crop"}, new String[]{"onion", "farmersdelight:onions"},
            new String[]{"cucumber", "youkaisfeasts:cucumber"}, new String[]{"rice", "farmersdelight:rice"},
            new String[]{"tea", "youkaisfeasts:tea"}, new String[]{"lime", "collectorsreap:lime_bush"});

    @Test
    void everyDefinitionHasModelLangAndSeedBagLootTable() throws IOException {
        String items = read(SRC.resolve("item/ModItems.java"));
        Matcher matcher = DEFINITION.matcher(items);
        int count = 0;
        while (matcher.find()) {
            String[] expected = EXPECTED.get(count++);
            assertEquals(expected[0], matcher.group(1));
            assertEquals(expected[1], matcher.group(2));
            String bag = "icecore:" + expected[0] + "_seed_bag";
            assertTrue(Files.exists(RES.resolve("assets/icecore/models/item/" + expected[0] + "_seed_bag.json")));
            assertTrue(read(RES.resolve("assets/icecore/lang/zh_cn.json")).contains("\"item.icecore." + expected[0] + "_seed_bag\""));
            assertTrue(read(RES.resolve("assets/icecore/lang/en_us.json")).contains("\"item.icecore." + expected[0] + "_seed_bag\""));
            String[] crop = expected[1].split(":");
            String loot = read(RES.resolve("data/" + crop[0] + "/loot_tables/blocks/" + crop[1] + ".json"));
            assertTrue(loot.contains("\"name\":\"" + bag + "\""), expected[1]);
        }
        assertEquals(EXPECTED.size(), count);
    }

    @Test
    void lootTablesNeverDropOriginalSeedsAndChiliIsEvenSplit() throws IOException {
        String chili = read(RES.resolve("data/kaleidoscope_cookery/loot_tables/blocks/chili_crop.json"));
        assertTrue(chili.contains("{\"type\":\"minecraft:item\",\"name\":\"kaleidoscope_cookery:red_chili\",\"weight\":1}"));
        assertTrue(chili.contains("{\"type\":\"minecraft:item\",\"name\":\"kaleidoscope_cookery:green_chili\",\"weight\":1}"));
        assertFalse(chili.contains("chili_seed\""));
        assertFalse(read(RES.resolve("data/youkaisfeasts/loot_tables/blocks/tea.json")).contains("tea_seeds"));
        assertFalse(read(RES.resolve("data/collectorsreap/loot_tables/blocks/lime_bush.json")).contains("lime_seeds"));
        String panicles = read(RES.resolve("data/farmersdelight/loot_tables/blocks/rice_panicles.json"));
        assertTrue(panicles.contains("\"name\":\"farmersdelight:rice_panicle\""));
        assertFalse(panicles.contains("\"name\":\"farmersdelight:rice\""));
    }

    @Test
    void farmingMixinsAreRegisteredAndOptionalOnesAreGated() throws IOException {
        String mixins = read(RES.resolve("icecore.mixins.json"));
        for (String name : List.of("BushBlockSeedBagFarmlandMixin", "FarmBlockUntilledDirtMixin",
                "BlockStateSoilRemovedMixin", "SeedBagCompatDewFarmlandMixin", "SeedBagCompatDewGrowthMixin",
                "SeedBagCompatRiceMixin", "SeedBagCompatTeaMixin", "SeedBagCompatEggplantMixin")) {
            assertTrue(mixins.contains("\"" + name + "\""), name);
            assertTrue(Files.exists(SRC.resolve("mixin/" + name + ".java")), name);
        }
        assertTrue(read(SRC.resolve("mixin/ICECoreMixinPlugin.java"))
                .contains("mixinClassName.contains(\"SeedBagCompat\")) return present(targetClassName)"));
    }

    @Test
    void untilledDirtReusesVanillaDirtAndIsTillable() throws IOException {
        assertTrue(read(RES.resolve("assets/icecore/blockstates/untilled_dirt.json")).contains("minecraft:block/dirt"));
        assertTrue(read(RES.resolve("assets/icecore/models/item/untilled_dirt.json")).contains("minecraft:block/dirt"));
        assertTrue(read(RES.resolve("data/minecraft/tags/blocks/mineable/shovel.json")).contains("icecore:untilled_dirt"));
        String block = read(SRC.resolve("farming/UntilledDirtBlock.java"));
        assertTrue(block.contains("ToolActions.HOE_TILL"));
        assertFalse(Files.exists(RES.resolve("data/minecraft/tags/blocks/dirt.json")));
    }

    @Test
    void jadeShowsSeedBagIconForEveryCropPart() throws IOException {
        String plugin = read(SRC.resolve("compat/jade/ICECoreJadePlugin.java"));
        assertTrue(plugin.contains("@WailaPlugin(ICECore.MOD_ID)"));
        assertTrue(plugin.contains("registerBlockIcon(SeedBagIconProvider.INSTANCE, BushBlock.class)"));
        assertTrue(plugin.contains("SeedBagCrops.find(accessor.getBlock())"));
        assertTrue(plugin.contains("crop.seedBag()"));
        assertTrue(read(Path.of("build.gradle")).contains("compileOnly fg.deobf(files('libs/Jade-1.20.1-Forge-11.13.3.jar'))"));
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
