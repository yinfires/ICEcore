package com.yinfires.icecore.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;

class SmartKeyPromptsComboContractTest {
    @Test void bothSingleAndComboCapsAlwaysPassUnpressedToNativeRenderer() throws Exception {
        try (var stream = getClass().getClassLoader().getResourceAsStream(
                "com/yinfires/icecore/mixin/SmartKeyPromptsComboMixin.class")) {
            assertNotNull(stream);
            var node = new ClassNode();
            new ClassReader(stream).accept(node, 0);
            var draw = method(node, "icecore$draw", "(Lnet/minecraft/client/gui/GuiGraphics;IILjava/lang/String;Z)V");
            int checked = 0;
            for (var instruction : draw.instructions) {
                if (instruction instanceof MethodInsnNode call && call.name.equals("drawKeyBoardKey")) {
                    assertEquals(org.objectweb.asm.Opcodes.ICONST_0, call.getPrevious().getOpcode());
                    checked++;
                }
            }
            assertEquals(2, checked);
        }
    }
    @Test void realHudHasAllRedirectSitesAndNativeDrawingDescriptors() throws Exception {
        try (var jar = new ZipFile("libs/smartkeyprompts-forge-1.20.1-1.1.3-hotfix-all.jar")) {
            ClassNode hud = read(jar, "init/HUD");
            var render = method(hud, "renderKeyPrompts",
                    "(Lnet/minecraft/client/gui/GuiGraphics;Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/util/List;IIIFZZ)V");
            assertEquals(1, calls(render, "java/util/List", "get"));
            assertEquals(5, calls(render, "net/minecraft/client/gui/Font", "m_92895_"));
            assertEquals(3, calls(render, "com/mafuyu404/smartkeyprompts/init/KeyRenderer", "drawKeyBoardKey"));
            assertEquals(3, calls(render, "com/mafuyu404/smartkeyprompts/init/KeyRenderer", "drawText"));
            method(hud, "updateCachedPrompts", "()V");
            method(hud, "clearCache", "()V");
            method(hud, "getCachedKeyTranslation", "(Ljava/lang/String;)Ljava/lang/String;");
            var renderer = read(jar, "init/KeyRenderer");
            method(renderer, "drawKeyBoardKey", "(Lnet/minecraft/client/gui/GuiGraphics;IILjava/lang/String;Z)V");
            method(renderer, "drawText", "(Lnet/minecraft/client/gui/GuiGraphics;IILjava/lang/String;)V");
            assertTrue(hud.fields.stream().anyMatch(f -> f.name.equals("font") && f.desc.equals("Lnet/minecraft/client/gui/Font;")));
            assertTrue(hud.fields.stream().anyMatch(f -> f.name.equals("cachedDefaultPrompts")));
        }
        assertTrue(Files.readString(Path.of("build/createMcpToSrg/output.tsrg")).contains("width (Ljava/lang/String;)I m_92895_"));
    }
    @Test void compatibilityIsOptionalClientOnlyAndKeepsNativeDrawing() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/yinfires/icecore/mixin/SmartKeyPromptsComboMixin.java"));
        assertTrue(source.contains("KeyRenderer.drawKeyBoardKey"));
        assertTrue(source.contains("KeyRenderer.drawText"));
        assertFalse(source.contains("drawString"));
        assertTrue(source.contains("icecore$layouts.size() >= 128"));
        assertTrue(source.contains("icecore$font != font"));
        String config = Files.readString(Path.of("src/main/resources/icecore.mixins.json"));
        assertTrue(config.indexOf("SmartKeyPromptsComboMixin") > config.indexOf("\"client\""));
        assertTrue(Files.readString(Path.of("src/main/java/com/yinfires/icecore/mixin/ICECoreMixinPlugin.java"))
                .contains("if (mixinClassName.contains(\"SmartKeyPromptsCombo\")) return present(targetClassName)"));
    }
    private static ClassNode read(ZipFile jar, String suffix) throws Exception {
        var node = new ClassNode();
        new ClassReader(jar.getInputStream(jar.getEntry("com/mafuyu404/smartkeyprompts/" + suffix + ".class"))).accept(node, 0);
        return node;
    }
    private static MethodNode method(ClassNode node, String name, String desc) {
        return node.methods.stream().filter(m -> m.name.equals(name) && m.desc.equals(desc)).findFirst().orElseThrow();
    }
    private static long calls(MethodNode method, String owner, String name) {
        return java.util.stream.StreamSupport.stream(method.instructions.spliterator(), false)
                .filter(i -> i instanceof MethodInsnNode call && call.owner.equals(owner) && call.name.equals(name)).count();
    }
}
