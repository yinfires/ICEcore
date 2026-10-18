package com.yinfires.icecore.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import java.util.List;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DescriptionRenderingContractTest {
    @Test void keycapsHaveNativeHeightAndBodyWrapDoesNotOverlapNextRow() {
        Font font = mock(Font.class);
        when(font.width(anyString())).thenAnswer(call -> ((String)call.getArgument(0)).length() * 6);
        when(font.width(any(net.minecraft.util.FormattedCharSequence.class))).thenReturn(30);
        when(font.split(any(net.minecraft.network.chat.FormattedText.class), anyInt()))
            .thenReturn(List.of(net.minecraft.util.FormattedCharSequence.EMPTY, net.minecraft.util.FormattedCharSequence.EMPTY));
        var action = new DescriptionAction(List.of("custom"), Component.literal("body"));
        var tooltip = new DescriptionActionTooltip(new DescriptionActionTooltip.Data(action, 320, true));
        assertTrue(tooltip.getWidth(font) > 0);
        assertTrue(tooltip.getHeight() >= 16);
        var longCombo = new DescriptionActionTooltip(new DescriptionActionTooltip.Data(
            new DescriptionAction(List.of("custom", "custom", "custom"), Component.literal("body")), 70, true));
        longCombo.getWidth(font);
        assertTrue(longCombo.getHeight() > tooltip.getHeight());
        assertTrue(longCombo.getWidth(font) <= 70);
    }
    @Test void realModernUiInvokesBothNativeContentPasses() throws Exception {
        try (var jar = new ZipFile("libs/ModernUI-Forge-1.20.1-3.12.0.1-universal.jar")) {
            var node = read(jar, "icyllis/modernui/mc/TooltipRenderer.class");
            var draw = node.methods.stream().filter(m -> m.name.equals("drawTooltip")).findFirst().orElseThrow();
            assertTrue(calls(draw, "net/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent", "m_142440_") > 0);
            assertTrue(calls(draw, "net/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent", "m_183452_") > 0);
        }
    }
    @Test void nativeKeyTranslationAndKeycapInterfacesExistAndDoNotReadBindings() throws Exception {
        try (var jar = new ZipFile("libs/smartkeyprompts-forge-1.20.1-1.1.3-hotfix-all.jar")) {
            var keys = read(jar, "com/mafuyu404/smartkeyprompts/util/KeyUtils.class");
            var translate = keys.methods.stream().filter(m -> m.name.equals("translateKey")).findFirst().orElseThrow();
            assertEquals("(Ljava/lang/String;)Ljava/lang/String;", translate.desc);
            assertEquals(0, calls(translate, "com/mafuyu404/smartkeyprompts/util/KeyUtils", "getKeyByDesc"));
            var renderer = read(jar, "com/mafuyu404/smartkeyprompts/init/KeyRenderer.class");
            assertTrue(renderer.methods.stream().anyMatch(m -> m.name.equals("drawKeyBoardKey") &&
                m.desc.equals("(Lnet/minecraft/client/gui/GuiGraphics;IILjava/lang/String;Z)V")));
        }
    }
    private static ClassNode read(ZipFile jar, String path) throws Exception {
        var node = new ClassNode();
        try (var stream = jar.getInputStream(jar.getEntry(path))) { new ClassReader(stream).accept(node, 0); }
        return node;
    }
    private static long calls(MethodNode method, String owner, String name) {
        return java.util.stream.StreamSupport.stream(method.instructions.spliterator(), false)
            .filter(i -> i instanceof MethodInsnNode call && call.owner.equals(owner) && call.name.equals(name)).count();
    }
}