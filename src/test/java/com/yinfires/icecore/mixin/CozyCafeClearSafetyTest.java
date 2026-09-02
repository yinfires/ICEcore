package com.yinfires.icecore.mixin;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class CozyCafeClearSafetyTest {
    private static String source(String name) throws IOException {
        return Files.readString(Path.of("src/main/java/com/yinfires/icecore/mixin", name), StandardCharsets.UTF_8);
    }

    @Test
    void clearIsRejectedWhileTheCafeIsOpen() throws IOException {
        String source = source("CozyCafeClearCafeMixin.java");
        assertTrue(source.contains("method = \"clearCafeData()V\""));
        assertTrue(source.contains("at = @At(\"HEAD\")"));
        assertTrue(source.contains("cancellable = true"));
        assertTrue(source.contains("if (isOpen())"));
        assertTrue(source.contains("callback.cancel()"));
    }

    @Test
    void orphanedSavedTablesAreRecoveredWithoutCallingTheMissingSign() throws IOException {
        String source = source("CozyCafeOrphanedMenuMixin.java");
        assertTrue(source.contains("method = \"closeMenu(Z)V\""));
        assertTrue(source.contains("access.icecore$getLinkedSign() != null"));
        assertTrue(source.contains("hasCustomer = false"));
        assertTrue(source.contains("callback.cancel()"));
    }

    @Test
    void openCafeClearClickIsConsumedBeforeSendingOrClosing() throws IOException {
        String source = source("CozyCafeClearButtonMixin.java");
        assertTrue(source.contains("method = \"m_7856_()V\""));
        assertTrue(source.contains("method = \"m_88315_(Lnet/minecraft/client/gui/GuiGraphics;IIF)V\""));
        assertTrue(source.contains("access.icecore$isCafeOpen()"));
        assertTrue(source.contains("boolean open = access.icecore$isCafeOpen()"));
        assertTrue(source.contains("icecore$clearButton.active = !open"));
    }

    @Test
    void openCafeHidesClearButtonAndRemovesItsTooltip() throws IOException {
        String source = source("CozyCafeClearButtonMixin.java");
        assertTrue(source.contains("icecore$clearButton.visible = !open"));
        assertTrue(source.contains("icecore$clearButton.active = !open"));
        assertTrue(source.contains("setTooltip(open ? null"));
    }
}
