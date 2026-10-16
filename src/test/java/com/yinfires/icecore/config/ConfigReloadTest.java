package com.yinfires.icecore.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.yinfires.icecore.food.ClientBoundFoodConsumptionConfigPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigReloadTest {
    @AfterEach
    void reset() throws Exception {
        ICECoreConfig.SPEC.setConfig(null);
        ICECoreClientConfig.SPEC.setConfig(null);
        var field = ICECoreConfigEvents.class.getDeclaredField("lastBroadcast");
        field.setAccessible(true);
        field.set(null, null);
    }

    @Test
    void editorBackingValueChangesReachPlayersOnServerThreadAndOnlyOnce() throws Exception {
        var data = CommentedConfig.inMemory();
        ICECoreConfig.SPEC.correct(data);
        ICECoreConfig.SPEC.setConfig(data);
        assertFalse(ICECoreConfig.disableFoodConsumption());
        assertTrue(ICECoreConfig.enableModCompatibility());
        // Editors can update the backing config without calling ConfigValue.set().
        data.set("food.disableConsumption", true);
        data.set("food.enableModCompatibility", false);
        assertFalse(ICECoreConfig.disableFoodConsumption());

        var server = mock(MinecraftServer.class);
        var players = mock(PlayerList.class);
        when(server.getPlayerList()).thenReturn(players);
        when(players.getPlayers()).thenReturn(List.of());
        var snapshot = ICECoreConfigEvents.class.getDeclaredField("lastBroadcast");
        snapshot.setAccessible(true);
        try (var lifecycle = mockStatic(ServerLifecycleHooks.class)) {
            lifecycle.when(ServerLifecycleHooks::getCurrentServer).thenReturn(server);
            ICECoreConfigEvents.synchronize(server);
            var task = ArgumentCaptor.forClass(Runnable.class);
            verify(server).execute(task.capture());
            verify(server, never()).getPlayerList();
            task.getValue().run();
            assertEquals(new ClientBoundFoodConsumptionConfigPacket(true, false), snapshot.get(null));
            task.getValue().run();
            verify(server, times(1)).getPlayerList();

            data.set("food.disableConsumption", false);
            data.set("food.enableModCompatibility", true);
            task.getValue().run();
            assertEquals(new ClientBoundFoodConsumptionConfigPacket(false, true), snapshot.get(null));
            verify(server, times(2)).getPlayerList();
        }
    }

    @Test
    void queuedReloadCannotSendAfterServerStops() {
        var server = mock(MinecraftServer.class);
        try (var lifecycle = mockStatic(ServerLifecycleHooks.class)) {
            lifecycle.when(ServerLifecycleHooks::getCurrentServer).thenReturn(null);
            ICECoreConfigEvents.synchronize(server);
            var task = ArgumentCaptor.forClass(Runnable.class);
            verify(server).execute(task.capture());
            task.getValue().run();
            verify(server, never()).getPlayerList();
        }
    }

    @Test
    void tooltipCacheRefreshReadsEditedValuesWithoutRestart() {
        var data = CommentedConfig.inMemory();
        ICECoreClientConfig.SPEC.correct(data);
        ICECoreClientConfig.SPEC.setConfig(data);
        assertTrue(ICECoreClientConfig.hideOriginalDetails());
        assertTrue(ICECoreClientConfig.hideFoodEffects());
        data.set("tooltip.hideOriginalDetails", false);
        data.set("tooltip.hideFoodEffects", false);
        assertTrue(ICECoreClientConfig.hideOriginalDetails());
        ICECoreClientConfig.SPEC.afterReload();
        assertFalse(ICECoreClientConfig.hideOriginalDetails());
        assertFalse(ICECoreClientConfig.hideFoodEffects());
    }

    @Test
    void screenSaveFallbackRunsAfterRemovedAndOnlyUsesIntegratedServer() throws Exception {
        var source = Files.readString(Path.of(
                "src/main/java/com/yinfires/icecore/config/ICECoreClientConfigEvents.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("ScreenEvent.Closing"));
        assertTrue(source.contains(".tell(ICECoreClientConfigEvents::refreshAfterSave)"));
        assertTrue(source.contains("getSingleplayerServer()"));
        assertFalse(source.contains("ServerLifecycleHooks"));
        assertFalse(source.contains("TickEvent"));
        var events = Files.readString(Path.of(
                "src/main/java/com/yinfires/icecore/config/ICECoreConfigEvents.java"), StandardCharsets.UTF_8);
        assertTrue(events.contains("ModConfigEvent.Reloading"));
        assertTrue(events.contains("getSpec() != ICECoreConfig.SPEC"));
        assertTrue(events.contains("synchronize(ServerLifecycleHooks.getCurrentServer())"));
        assertTrue(events.contains("ICECoreNetwork.sendToPlayer(packet, player)"));
    }
}
