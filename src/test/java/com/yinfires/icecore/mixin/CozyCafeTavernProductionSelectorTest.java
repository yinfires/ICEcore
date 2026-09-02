package com.yinfires.icecore.mixin;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CozyCafeTavernProductionSelectorTest {
    private static String source(String name) throws Exception {
        return Files.readString(Path.of("src/main/java/com/yinfires/icecore/mixin", name), StandardCharsets.UTF_8);
    }
    @Test void selectorsUseProductionDescriptors() throws Exception {
        assertTrue(source("CozyCafeSpawnRegionManagerMixin.java").contains("assignCustomersInArea(Lnet/minecraft/world/level/Level;"));
        assertTrue(source("CozyCafeSpawnRegionManagerMixin.java").contains("BlockState;m_60713_"));
        assertTrue(source("CozyCafeSpawnRegionManagerMixin.java").contains("ordinal = 1), remap = false"));
        assertTrue(source("CozyCafeSpawnRegionMenuMixin.java").contains("closeMenu(Z)V"));
        assertTrue(source("CozyCafeCustomerDoorMixin.java").contains("m_8119_()V"));
        assertTrue(source("TavernBoardPlacementMixin.java").contains("m_6402_(Lnet/minecraft/world/level/Level;"));
        assertTrue(source("TavernTextRendererMixin.java").contains("doTextRender(Lcom/github/ysbbbbbb/kaleidoscopetavern/blockentity/deco/TextBlockEntity;"));
        assertTrue(source("CozyCafeManagerNameMixin.java").contains("setName(Ljava/lang/String;)V"));
    }

    @Test void rendererUsesTavernSyncedTextInsteadOfTheStaleLinkSnapshot() throws Exception {
        String service = Files.readString(Path.of("src/main/java/com/yinfires/icecore/compat/cozycafe/board/CozyCafeBoardService.java"),
                StandardCharsets.UTF_8);
        assertTrue(service.contains("return isBound(board) ? board.getText() : \"\""));
        assertTrue(service.contains("setDisplay(board, manager.getCafeName(), manager.isOpen())"));
        assertTrue(service.contains("board.setText(name)"));
    }

    @Test void enterAndEscapeRenamePathSubmitsToTheServer() throws Exception {
        String source = source("CozyCafeManagerNameMixin.java");
        assertTrue(source.contains("blockEntity.getLevel().isClientSide"));
        assertTrue(source.contains("ServerBoundRenameCafePacket(blockEntity.getBlockPos(), name)"));
    }
}
