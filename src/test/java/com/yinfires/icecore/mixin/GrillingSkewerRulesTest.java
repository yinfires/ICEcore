package com.yinfires.icecore.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.*;

final class GrillingSkewerRulesTest {
    private static final String ROOT = "src/main/java/com/yinfires/icecore/mixin/";

    @Test
    void jadeOnlySkipsTitleIconsAndPreservesStatusProvider() throws Exception {
        String source = Files.readString(Path.of(ROOT + "GrillingJadeGrillMixin.java"));
        var tooltip = upstream("compat/jade/GrillProvider").methods.stream()
                .filter(m -> m.name.equals("appendTooltip")).findFirst().orElseThrow();
        assertTrue(source.contains("method = \"" + tooltip.name + tooltip.desc + "\""));
        int icons = 0;
        boolean status = false;
        for (var instruction : tooltip.instructions) {
            if (instruction instanceof MethodInsnNode call) {
                if (call.name.equals("appendItems")) {
                    icons++;
                    assertTrue(source.contains("L" + call.owner + ";" + call.name + call.desc));
                }
                status |= call.name.equals("statusKey");
            }
        }
        assertEquals(1, icons);
        assertTrue(status);
        assertFalse(source.contains("@Inject"));
        assertFalse(source.contains("tooltip.clear"));
        assertFalse(new ICECoreMixinPlugin().shouldApplyMixin("missing.grilling.GrillProvider",
                "com.yinfires.icecore.mixin.GrillingJadeGrillMixin"));
        var config = new com.google.gson.Gson().fromJson(Files.readString(Path.of("src/main/resources/icecore.mixins.json")),
                com.google.gson.JsonObject.class);
        assertTrue(config.getAsJsonArray("client").toString().contains("GrillingJadeGrillMixin"));
        assertFalse(config.getAsJsonArray("mixins").toString().contains("GrillingJadeGrillMixin"));
    }

    @Test
    void redirectsOnlyLoosePlacementAndLeavesBothHandDisassemblyAndPackedPlates() throws Exception {
        String source = Files.readString(Path.of(ROOT + "GrillingSkewerPlacementMixin.java"));
        assertTrue(source.contains("onRightClickBlock(Lnet/minecraftforge/event/entity/player/PlayerInteractEvent$RightClickBlock;)V"));
        assertTrue(source.contains("return InteractionResult.PASS;"));
        assertFalse(source.contains("setCanceled"));
        assertFalse(source.contains("placePacked("));
        ClassNode upstream = upstream("skewer/SkewerPlatePlacement");
        var redirect = upstream.methods.stream().filter(m -> m.name.equals("onRightClickBlock")).findFirst().orElseThrow();
        int placements = 0;
        boolean offhandDisassembly = false;
        boolean mainhandDisassembly = false;
        for (var instruction : redirect.instructions) {
            if (instruction instanceof MethodInsnNode call) {
                if (call.name.equals("place")) {
                    placements++;
                    assertTrue(source.contains("L" + call.owner + ";" + call.name + call.desc));
                }
                offhandDisassembly |= call.name.equals("disassembleOffhand");
                mainhandDisassembly |= call.name.equals("disassemble");
            }
        }
        assertEquals(1, placements);
        assertTrue(offhandDisassembly);
        assertTrue(mainhandDisassembly);
        assertTrue(upstream.methods.stream().anyMatch(m -> m.name.equals("placePacked")));
    }

    @Test
    void heatSelectorsMatchUpstreamAndOnlyRejectRecognizedSkewers() throws Exception {
        String source = Files.readString(Path.of(ROOT + "GrillingSkewerHeatMixin.java"));
        ClassNode state = upstream("food/FoodState");
        for (String name : new String[]{"setHot", "isHot", "remainingHeat"}) {
            var method = state.methods.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow();
            assertTrue(source.contains("method = \"" + name + method.desc + "\""));
        }
        assertEquals(3, source.split("if \\(SkewerPlateItem.isSkewer\\(stack\\)\\)", -1).length - 1);
        assertTrue(source.contains("ci.cancel()"));
        assertTrue(source.contains("cir.setReturnValue(false)"));
        assertTrue(source.contains("cir.setReturnValue(0L)"));
        assertFalse(source.contains("getOrCreateTag"));
        assertFalse(source.contains("getInventory"));
        // The direct legacy HotUntil tooltip read must remain behind the isHot gate.
        var tooltip = upstream("food/HotFoodHandler").methods.stream()
                .filter(m -> m.name.equals("onTooltip")).findFirst().orElseThrow();
        boolean gated = false;
        for (var instruction : tooltip.instructions) {
            if (instruction instanceof MethodInsnNode call && call.owner.endsWith("/FoodState") && call.name.equals("isHot")) gated = true;
            if (instruction instanceof org.objectweb.asm.tree.LdcInsnNode literal && "HotUntil".equals(literal.cst)) assertTrue(gated);
        }
        assertTrue(gated);
    }

    @Test
    void optionalTargetsAreGatedWithoutLoadingUpstreamClasses() throws Exception {
        String config = Files.readString(Path.of("src/main/resources/icecore.mixins.json"));
        for (String mixin : new String[]{"GrillingSkewerPlacementMixin", "GrillingSkewerHeatMixin"}) {
            assertTrue(config.contains("\"" + mixin + "\""));
            assertFalse(new ICECoreMixinPlugin().shouldApplyMixin("missing.grilling.Target", "com.yinfires.icecore.mixin." + mixin));
        }
        String plugin = Files.readString(Path.of(ROOT + "ICECoreMixinPlugin.java"));
        assertFalse(plugin.contains("Class.forName"));
        assertFalse(plugin.contains("loadClass("));
    }

    private static ClassNode upstream(String name) throws Exception {
        try (JarFile jar = new JarFile("libs/kaleidoscope_grilling-1.1.1-forge1.20.1.jar")) {
            var entry = jar.getJarEntry("cn/breezeth/kaleidoscope_grilling/" + name + ".class");
            assertNotNull(entry);
            try (var input = jar.getInputStream(entry)) {
                ClassNode node = new ClassNode();
                new ClassReader(input).accept(node, 0);
                return node;
            }
        }
    }
}
