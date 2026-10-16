package com.yinfires.icecore.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

final class CookeryStockpotLidMixinTest {
    @Test
    void supportedUpstreamVersionsInheritShieldBehavior() throws Exception {
        for (String jar : new String[]{
                "[森罗物语：厨房] kaleidoscopecookery-1.4.1-forge+mc1.20.1.jar",
                "kaleidoscopecookery-1.5.1-forge+mc1.20.1.jar"}) {
            try (ZipFile zip = new ZipFile(Path.of("libs", jar).toFile())) {
                ClassNode node = new ClassNode();
                new ClassReader(zip.getInputStream(zip.getEntry(
                        "com/github/ysbbbbbb/kaleidoscopecookery/item/StockpotLidItem.class"))).accept(node, 0);
                assertEquals("net/minecraft/world/item/ShieldItem", node.superName);
                assertTrue(node.methods.stream().noneMatch(m -> m.name.equals("m_7203_")
                        || m.name.equals("m_6164_") || m.name.equals("m_8105_")
                        || m.name.equals("canPerformAction")), jar);
            }
        }
    }

    @Test
    void overridesCannotStartUsingOrAdvertiseBlocking() throws Exception {
        ClassNode node = new ClassNode();
        new ClassReader(Files.readAllBytes(Path.of(
                "build/classes/java/main/com/yinfires/icecore/mixin/CookeryStockpotLidMixin.class"))).accept(node, 0);
        var use = node.methods.stream().filter(m -> m.name.equals("use")).findFirst().orElseThrow();
        assertEquals("(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResultHolder;", use.desc);
        boolean pass = false;
        for (var insn : use.instructions) {
            if (insn instanceof MethodInsnNode call) {
                assertNotEquals("startUsingItem", call.name);
                assertFalse(call.owner.equals("net/minecraft/world/item/ShieldItem"));
                pass |= call.owner.equals("net/minecraft/world/InteractionResultHolder") && call.name.equals("pass");
            }
        }
        assertTrue(pass);
        for (String name : new String[]{"getUseDuration", "canPerformAction"}) {
            var method = node.methods.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow();
            assertTrue(java.util.stream.StreamSupport.stream(method.instructions.spliterator(), false)
                    .anyMatch(i -> i.getOpcode() == Opcodes.ICONST_0));
        }
        String source = Files.readString(Path.of("src/main/java/com/yinfires/icecore/mixin/CookeryStockpotLidMixin.java"));
        assertTrue(source.contains("return UseAnim.NONE;"));
        assertFalse(new ICECoreMixinPlugin().shouldApplyMixin(
                "missing.cookery.StockpotLidItem", "com.yinfires.icecore.mixin.CookeryStockpotLidMixin"));
        String plugin = Files.readString(Path.of("src/main/java/com/yinfires/icecore/mixin/ICECoreMixinPlugin.java"));
        assertFalse(plugin.contains("Class.forName"));
        assertFalse(plugin.contains("loadClass("));
        String config = Files.readString(Path.of("src/main/resources/icecore.mixins.json"));
        assertTrue(config.substring(0, config.indexOf("\"client\"")).contains("\"CookeryStockpotLidMixin\""));
    }
}
