package com.yinfires.icecore.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.*;

class ItemTooltipMixinTest {
    private static ClassNode upstream(String jarPath, String className) throws Exception {
        ClassNode node = new ClassNode();
        try (JarFile jar = new JarFile(jarPath); var input = jar.getInputStream(jar.getJarEntry(className + ".class"))) {
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    @Test void qualityFoodAddsGraphicalEffectsAfterTextAndIsBlockedAtItsOwnEntry() throws Exception {
        var type = upstream("libs/quality_food-1.20.1-2.4.3-all.jar", "de/cadentem/quality_food/events/ForgeEvents");
        var method = type.methods.stream().filter(m -> m.name.equals("addEffectTooltip")).findFirst().orElseThrow();
        String source = Files.readString(Path.of("src/main/java/com/yinfires/icecore/mixin/QualityFoodTooltipMixin.java"));
        assertTrue(source.contains(method.name + method.desc));
        assertTrue(source.contains("@At(\"HEAD\")"));
        assertTrue(source.contains("if (ICECoreClientConfig.hideFoodEffects()) ci.cancel();"));
        int graphicalEffects = 0;
        for (var instruction : method.instructions) {
            if (instruction instanceof TypeInsnNode allocation && allocation.desc.equals("de/cadentem/quality_food/core/EffectComponent")) {
                graphicalEffects++;
            }
        }
        assertTrue(graphicalEffects >= 2, "Must cover direct and dynamic food effect components");
        assertFalse(source.contains("addTooltip("), "Do not cancel unrelated quality information");
    }

    @Test void expandedDescriptionsAreBlockedAtActualCreateAndLibTooltipsEntries() throws Exception {
        String[][] entries = {
                {"libs/create-1.20.1-6.0.8.jar", "com/simibubi/create/foundation/events/ClientEvents", "addToItemTooltip", "CreateItemDescriptionTooltipMixin"},
                {"libs/libtooltips-2.3-FORGE-1.20.1.jar", "com/wdiscute/libtooltips/Tooltips", "modifyItemTooltip", "LibItemDescriptionTooltipMixin"}
        };
        for (var entry : entries) {
            var type = upstream(entry[0], entry[1]);
            var method = type.methods.stream().filter(m -> m.name.equals(entry[2])).findFirst().orElseThrow();
            String source = Files.readString(Path.of("src/main/java/com/yinfires/icecore/mixin/" + entry[3] + ".java"));
            assertTrue(source.contains(method.name + method.desc));
            assertTrue(source.contains("@At(\"HEAD\")"));
            assertTrue(source.contains("if (ICECoreClientConfig.hideOriginalDetails()) ci.cancel();"));
        }
        var create = upstream(entries[0][0], "com/simibubi/create/foundation/item/ItemDescription");
        assertTrue(create.methods.stream().anyMatch(m -> m.name.equals("getCurrentLines")));
        assertTrue(create.fields.stream().anyMatch(f -> f.name.equals("linesOnShift")));
        assertTrue(create.fields.stream().anyMatch(f -> f.name.equals("linesOnCtrl")));
    }

    @Test void modNameSuppressionCoversFluidsAndLeavesOtherTooltipLinesUntouched() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/yinfires/icecore/mixin/JeiItemModNameTooltipMixin.java"));
        assertFalse(source.contains("instanceof ItemStack"));
        assertFalse(source.contains("getIngredient()"));
        assertFalse(source.contains("clear()"));
        assertEquals(2, source.split("cir.setReturnValue\\(lines\\)", -1).length - 1);
        assertTrue(source.contains("cir.setReturnValue(Optional.empty())"));
    }

    @Test void effectHooksMatchRealUpstreamMethodsAndPotionCall() throws Exception {
        String[][] cases = {
                {"libs/kaleidoscope_grilling-1.1.1-forge1.20.1.jar", "cn/breezeth/kaleidoscope_grilling/food/FoodTooltip", "GrillingFoodTooltipMixin", "appendEffect", "appendEffects"},
                {"libs/[森罗物语：厨房] kaleidoscopecookery-1.4.1-forge+mc1.20.1.jar", "com/github/ysbbbbbb/kaleidoscopecookery/item/FoodWithEffectsItem", "CookeryFoodTooltipMixin", "m_7373_"},
                {"libs/kaleidoscopecookery-1.5.1-forge+mc1.20.1.jar", "com/github/ysbbbbbb/kaleidoscopecookery/item/FoodWithEffectsItem", "CookeryFoodTooltipMixin", "m_7373_"}
        };
        for (String[] entry : cases) {
            ClassNode type = new ClassNode();
            try (JarFile jar = new JarFile(entry[0]); var input = jar.getInputStream(jar.getJarEntry(entry[1] + ".class"))) {
                new ClassReader(input).accept(type, 0);
            }
            String source = Files.readString(Path.of("src/main/java/com/yinfires/icecore/mixin/" + entry[2] + ".java"));
            for (int i = 3; i < entry.length; i++) {
                String name = entry[i];
                var method = type.methods.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow();
                assertTrue(source.contains(method.name + method.desc));
                if (name.equals("m_7373_")) {
                    boolean potionCall = false;
                    for (var instruction : method.instructions) {
                        if (instruction instanceof MethodInsnNode call && call.name.equals("m_257410_")) {
                            potionCall = true;
                            assertTrue(source.contains("L" + call.owner + ";" + call.name + call.desc));
                        }
                    }
                    assertTrue(potionCall);
                }
            }
        }
    }

    @Test void jeiModNameHooksMatchTheActualPublicApi() throws Exception {
        ClassNode type = new ClassNode();
        new ClassReader("mezz.jei.library.helpers.ModIdHelper").accept(type, 0);
        String source = Files.readString(Path.of("src/main/java/com/yinfires/icecore/mixin/JeiItemModNameTooltipMixin.java"));
        int hooks = 0;
        for (var method : type.methods) {
            if (method.name.equals("addModNameToIngredientTooltip") || method.name.equals("getModNameForTooltip")) {
                assertTrue(source.contains(method.name + method.desc));
                hooks++;
            }
        }
        assertEquals(3, hooks);
    }

    @Test void originalFilterRunsBeforeContextListeners() throws Exception {
        ClassNode hooks = new ClassNode();
        new ClassReader("net.minecraft.world.item.ItemStack").accept(hooks, 0);
        var method = hooks.methods.stream().filter(m -> m.name.equals("getTooltipLines"))
                .findFirst().orElseThrow();
        String source = Files.readString(Path.of("src/main/java/com/yinfires/icecore/mixin/ItemOriginalTooltipMixin.java"));
        assertTrue(source.contains("m_41651_" + method.desc));
        int count = 0;
        for (var instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals("net/minecraftforge/event/ForgeEventFactory")
                    && call.name.equals("onItemTooltip")) {
                count++;
                assertTrue(source.contains("L" + call.owner + ";" + call.name + call.desc));
            }
        }
        assertEquals(1, count);
        assertTrue(source.indexOf("ItemTooltipEvents.filterOriginal") < source.indexOf("return ForgeEventFactory.onItemTooltip"));
    }

    @Test void externalFoodMixinsAreOptionalAndOnlyClientSide() throws Exception {
        var plugin = new ICECoreMixinPlugin();
        for (String mixin : new String[]{"FarmersFoodTooltipMixin", "GrillingFoodTooltipMixin", "CookeryFoodTooltipMixin",
                "PonderItemTooltipMixin", "JeiItemModNameTooltipMixin", "QualityFoodTooltipMixin",
                "CreateItemDescriptionTooltipMixin", "LibItemDescriptionTooltipMixin"}) {
            assertFalse(plugin.shouldApplyMixin("missing.optional.Food", mixin));
            String config = Files.readString(Path.of("src/main/resources/icecore.mixins.json"));
            var json = com.google.gson.JsonParser.parseString(config).getAsJsonObject();
            assertTrue(json.getAsJsonArray("client").toString().contains(mixin));
            assertFalse(json.getAsJsonArray("mixins").toString().contains(mixin));
        }
    }
}
