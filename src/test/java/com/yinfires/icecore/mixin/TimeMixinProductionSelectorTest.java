package com.yinfires.icecore.mixin;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TimeMixinProductionSelectorTest {
    private static String source(String name) throws IOException {
        return Files.readString(Path.of("src/main/java/com/yinfires/icecore/mixin", name), StandardCharsets.UTF_8);
    }

    @Test
    void serverLevelSelectorsUseProductionNamesAndDescriptors() throws IOException {
        String source = source("ServerLevelTimeMixin.java");
        assertTrue(source.contains("m_8809_()V"));
        assertTrue(source.contains("Lnet/minecraft/server/level/ServerLevel;m_8615_(J)V"));
        assertTrue(source.contains("m_8615_(J)V"));
        assertFalse(source.contains("method=\"tickTime\"") || source.contains("method = \"tickTime\""));
        assertFalse(source.contains(";setDayTime(J)V"));
    }

    @Test
    void clientLevelSelectorsUseProductionNamesAndDescriptors() throws IOException {
        String source = source("ClientLevelTimeMixin.java");
        assertTrue(source.contains("m_104826_()V"));
        assertTrue(source.contains("Lnet/minecraft/client/multiplayer/ClientLevel;m_104746_(J)V"));
        assertFalse(source.contains("method=\"tickTime\"") || source.contains("method = \"tickTime\""));
        assertFalse(source.contains(";setDayTime(J)V"));
    }

    @Test
    void timeCommandSelectorsUseProductionNamesAndDescriptors() throws IOException {
        String source = source("TimeCommandMixin.java");
        assertTrue(source.contains("m_139077_(Lnet/minecraft/commands/CommandSourceStack;I)I"));
        assertTrue(source.contains("m_139082_(Lnet/minecraft/commands/CommandSourceStack;I)I"));
        assertFalse(source.contains("\"setTime\"") || source.contains("\"addTime\""));
    }

    @Test
    void adventureDropSelectorsUseProductionNames() throws IOException {
        String source = source("AdventureDropPacketMixin.java");
        assertTrue(source.contains("m_7502_(Lnet/minecraft/network/protocol/game/ServerboundPlayerActionPacket;)V"));
        assertTrue(source.contains("m_5914_(Lnet/minecraft/network/protocol/game/ServerboundContainerClickPacket;)V"));
        assertFalse(source.contains("handlePlayerAction") || source.contains("handleContainerClick"));
        assertTrue(source.contains("public net.minecraft.server.level.ServerPlayer f_9743_"));
        assertFalse(source.contains("public net.minecraft.server.level.ServerPlayer player"));
        assertTrue(source.contains("ClickType.PICKUP"));
        assertFalse(source.contains("slotNum() == -999\\n                && (packet.getClickType() == net.minecraft.world.inventory.ClickType.PICKUP"));
    }

    @Test
    void adventureCarriedSelectorUsesProductionName() throws IOException {
        String source = source("AdventureCarriedStackMixin.java");
        assertTrue(source.contains("m_142621_()"));
        assertTrue(source.contains("m_142503_("));
        assertTrue(source.contains("method=\"m_6877_(Lnet/minecraft/world/entity/player/Player;)V\""));
        assertTrue(source.contains("restoreAfterMenuClose(serverPlayer)"));
        assertFalse(source.contains("method=\"b(Lnet/minecraft/world/entity/player/Player;)V\""));
    }

    @Test
    void adventureLocalDropSelectorUsesProductionName() throws IOException {
        String source = source("AdventureLocalPlayerDropMixin.java");
        assertTrue(source.contains("m_108700_(Z)Z"));
        assertFalse(source.contains("m_36274_") || source.contains("drop(boolean)"));
    }

    @Test
    void adventureClientInputSelectorsUseProductionNames() throws IOException {
        String key = source("AdventureKeyDropMixin.java");
        assertTrue(key.contains("method = \"m_91279_()V\""));
        assertTrue(key.contains("LocalPlayer;m_108700_(Z)Z"));
        assertFalse(key.contains("LocalPlayer;drop(Z)Z"));
        assertTrue(source("AdventureMenuCloseMixin.java").contains("m_9230_()V"));
        String screen = source("AdventureContainerScreenMixin.java");
        assertTrue(screen.contains("m_7933_(III)Z"));
        assertTrue(screen.contains("m_6597_(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ClickType;)V"));
    }
}
