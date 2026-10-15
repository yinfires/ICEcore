package com.yinfires.icecore.farming;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SeedBagGrowthContractTest {
    private static String source(String file) throws Exception {
        return Files.readString(Path.of("src/main/java/com/yinfires/icecore", file), StandardCharsets.UTF_8);
    }

    @Test void randomTickSelectorCoversEveryRegisteredPartAndTimeCompensation() throws Exception {
        String hook = source("mixin/SeedBagCompatDewRandomTickMixin.java");
        assertTrue(hook.contains("m_222972_(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"));
        assertTrue(hook.contains("SeedBagCrops.find("));
        assertTrue(hook.contains("callback.cancel()"));
        String crops = source("farming/SeedBagCrops.java");
        assertTrue(crops.contains("for (SeedBagDefinition definition : ModItems.SEED_BAG_DEFINITIONS)"));
        assertTrue(crops.contains("map.put(root, crop)"));
        assertTrue(crops.contains("map.put(upper, crop)"));
        assertTrue(source("mixin/ServerLevelRandomTickMixin.java").contains("state.randomTick(level, pos, random)"));
    }

    @Test void optionalMixinGatesAndProductionBridgesDoNotLoadTargets() throws Exception {
        String plugin = source("mixin/ICECoreMixinPlugin.java");
        assertTrue(plugin.contains("if (mixinClassName.contains(\"SeedBagCompatDewRandomTick\")) return present(\"cool.bot.dewdropfarmland.Config\")"));
        assertFalse(plugin.contains("Class.forName"));
        assertFalse(plugin.contains("loadClass("));
        String config = Files.readString(Path.of("src/main/resources/icecore.mixins.json"));
        for (String name : List.of("SeedBagCompatDewRandomTickMixin", "SeedBagCompatDewGrowthMixin",
                "SeedBagCompatDoubleGrowthMixin", "SeedBagCompatClimbingGrowthMixin")) {
            assertTrue(config.contains("\"" + name + "\""));
        }
        assertTrue(source("mixin/SeedBagCompatDoubleGrowthMixin.java").contains("@Invoker(value = \"setGrowth\", remap = false)"));
        assertTrue(source("mixin/SeedBagCompatClimbingGrowthMixin.java").contains("@Invoker(value = \"postGrowth\", remap = false)"));
        assertTrue(source("farming/DailyClimbingCrop.java").startsWith("package com.yinfires.icecore.farming;"));
        assertTrue(source("farming/DailyDoubleCrop.java").startsWith("package com.yinfires.icecore.farming;"));
    }

    @Test void dailyGrowthKeepsStructureEventsAndBoundaries() throws Exception {
        String growth = source("compat/dewdrop/SeedBagDailyGrowth.java");
        for (String boundary : List.of("SturdyFarmlandBlockTags.NOT_DAILY", "!root.equals(pos)",
                "i < 64", "boolean hadNext", "if (!hadNext) break", "state.setValue(crop.age()",
                "vine.icecore$postGrowth", "doubleCrop.icecore$setGrowth", "isValidBonemealTarget",
                "ForgeHooks.onCropsGrowPre", "ForgeHooks.onCropsGrowPost", "stage == RiceGrowth.MATURE",
                "!upper.isAir()", "canSurvive(level, top)")) assertTrue(growth.contains(boundary), boundary);
        String hook = source("mixin/SeedBagCompatDewGrowthMixin.java");
        assertTrue(hook.contains("handleCropGrowth(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;)V"));
        assertTrue(hook.contains("SeedBagDailyGrowth.grow(crop, level, pos, soil, random)"));
        assertFalse(growth.contains(".randomTick("));
        assertFalse(growth.contains("Class.forName"));
    }

    @Test void riceTooltipUsesSameRootAndLifecycleOnBothParts() throws Exception {
        String jade = source("compat/jade/ICECoreJadePlugin.java");
        for (String term : List.of("registerBlockComponent(RiceProgressProvider.INSTANCE", "SeedBagCrops.rootOf",
                "RiceGrowth.stage", "Identifiers.MC_CROP_PROGRESS", "tooltip.jade.crop_growth",
                "tooltip.jade.crop_mature", "IThemeHelper.get().info", "IThemeHelper.get().success",
                "String.format(\"%.0f%%\"")) assertTrue(jade.contains(term));
        assertFalse(jade.contains("RiceGrowth.remaining"));
        for (String locale : List.of("en_us", "zh_cn")) {
            String lang = Files.readString(Path.of("src/main/resources/assets/icecore/lang/" + locale + ".json"));
            assertFalse(lang.contains("tooltip.icecore.rice_progress"));
            assertFalse(lang.contains("tooltip.icecore.rice_remaining"));
        }
    }
}
