package com.yinfires.icecore.journal;

import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.yinfires.icecore.quest.*;
import com.yinfires.icecore.tutorial.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JournalGlobalServiceTest {
    private static final ResourceLocation ID = ResourceLocation.parse("icecore:test");

    private static void field(Class<?> type, Object target, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object parse(Class<?> type, String json) throws Exception {
        var method = type.getDeclaredMethod("parse", ResourceLocation.class, com.google.gson.JsonObject.class);
        method.setAccessible(true);
        return method.invoke(null, ID, JsonParser.parseString(json).getAsJsonObject());
    }

    @AfterEach void clear() throws Exception {
        QuestService.stop();
        TutorialService.stop();
        QuestService.installSyncHook(null);
        TutorialService.installSyncHook(null);
        field(QuestDefinitionManager.class, QuestDefinitionManager.INSTANCE, "definitions", Map.of());
        field(TutorialDefinitionManager.class, TutorialDefinitionManager.INSTANCE, "definitions", Map.of());
    }

    @Test void parsersDefaultToGlobalAndRespectPlayer() throws Exception {
        assertEquals(QuestScope.GLOBAL, ((QuestDefinition) parse(QuestDefinitionManager.class, "{\"title\":\"test\"}")).scope());
        assertEquals(QuestScope.PLAYER, ((QuestDefinition) parse(QuestDefinitionManager.class, "{\"title\":\"test\",\"scope\":\"player\"}")).scope());
        assertEquals(QuestScope.GLOBAL, ((TutorialDefinition) parse(TutorialDefinitionManager.class, "{\"title\":\"test\"}")).scope());
        assertEquals(QuestScope.PLAYER, ((TutorialDefinition) parse(TutorialDefinitionManager.class, "{\"title\":\"test\",\"scope\":\"player\"}")).scope());
    }

    @Test void globalChangesBroadcastLoginReadsCurrentStateAndCallbacksRunOnce() throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        // A plain JUnit JVM lacks Forge's transformed network event constructors.
        try (var hooks = mockStatic(net.minecraftforge.network.NetworkHooks.class)) {
            net.minecraft.server.Bootstrap.bootStrap();
        }
        MinecraftServer server = mock(MinecraftServer.class);
        PlayerList list = mock(PlayerList.class);
        Commands commands = mock(Commands.class);
        CommandSourceStack source = mock(CommandSourceStack.class);
        when(source.withPermission(2)).thenReturn(source);
        when(source.withSuppressedOutput()).thenReturn(source);
        when(source.hasPermission(2)).thenReturn(true);
        when(source.getServer()).thenReturn(server);
        when(server.getCommands()).thenReturn(commands);
        when(server.getPlayerList()).thenReturn(list);
        ServerPlayer a = mock(ServerPlayer.class), b = mock(ServerPlayer.class), late = mock(ServerPlayer.class);
        UUID aid = UUID.randomUUID(), bid = UUID.randomUUID(), lid = UUID.randomUUID();
        when(a.getUUID()).thenReturn(aid);
        when(b.getUUID()).thenReturn(bid);
        when(late.getUUID()).thenReturn(lid);
        when(a.createCommandSourceStack()).thenReturn(source);
        when(list.getPlayers()).thenReturn(List.of(a, b));
        QuestPlayerData players = new QuestPlayerData();
        QuestGlobalData global = new QuestGlobalData();
        field(QuestService.class, null, "server", server);
        field(QuestService.class, null, "playerData", players);
        field(QuestService.class, null, "globalData", global);
        field(TutorialService.class, null, "server", server);
        field(TutorialService.class, null, "data", new TutorialPlayerData());
        field(TutorialService.class, null, "global", new TutorialGlobalData());
        com.yinfires.icecore.quest.objective.ObjectiveRegistry.bootstrap();
        QuestDefinition def = (QuestDefinition) parse(QuestDefinitionManager.class,
                "{\"title\":\"test\",\"auto_complete\":false,\"on_accept\":[\"accept\"],\"on_complete\":[\"reward\"],\"objectives\":[{\"type\":\"icecore:command\",\"count\":3}]}");
        field(QuestDefinitionManager.class, QuestDefinitionManager.INSTANCE, "definitions", Map.of(ID, def));
        TutorialDefinition tutorial = (TutorialDefinition) parse(TutorialDefinitionManager.class, "{\"title\":\"test\"}");
        field(TutorialDefinitionManager.class, TutorialDefinitionManager.INSTANCE, "definitions", Map.of(ID, tutorial));
        Map<ServerPlayer, Integer> questSyncs = new HashMap<>(), tutorialSyncs = new HashMap<>();
        QuestService.installSyncHook(p -> questSyncs.merge(p, 1, Integer::sum));
        TutorialService.installSyncHook(p -> tutorialSyncs.merge(p, 1, Integer::sum));
        QuestService.reconcile();
        TutorialService.reconcile();
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        var root = Commands.literal("icecore");
        QuestCommands.attach(root);
        TutorialCommands.attach(root);
        dispatcher.register(root);
        assertEquals(1, dispatcher.execute("icecore quest acquire icecore:test @a", source));
        assertEquals(0, dispatcher.execute("icecore quest acquire icecore:test @a", source));
        assertEquals(ID, QuestService.marked(b));
        assertEquals(Map.of(a, 1, b, 1), questSyncs);
        QuestService.prepareLogin(late);
        assertEquals(ID, QuestService.marked(late));
        assertTrue(QuestService.isActive(late, ID));
        assertTrue(QuestService.setObjectiveProgress(b, ID, 0, 2));
        assertEquals(2, QuestService.progressOf(late, ID).count(0));
        // Snapshot reads and reconciliation never run side effects.
        QuestService.reconcile();
        assertEquals(1, dispatcher.execute("icecore quest complete icecore:test @a", source));
        assertEquals(0, dispatcher.execute("icecore quest complete icecore:test @a", source));
        assertNull(QuestService.marked(b));
        assertNull(QuestService.marked(late));
        verify(commands, times(1)).performPrefixedCommand(source, "accept");
        verify(commands, times(1)).performPrefixedCommand(source, "reward");
        assertEquals(Map.of(a, 3, b, 3), questSyncs);
        assertEquals(1, dispatcher.execute("icecore tutorial unlock icecore:test @a", source));
        assertEquals(0, dispatcher.execute("icecore tutorial unlock icecore:test @a", source));
        assertEquals(Map.of(a, 1, b, 1), tutorialSyncs);
        assertEquals(TutorialState.UNLOCKED_UNREAD, TutorialService.snapshot(late).get(ID));
        assertTrue(TutorialService.markRead(a, ID));
        assertEquals(TutorialState.UNLOCKED_READ, TutorialService.state(a, ID));
        assertEquals(TutorialState.UNLOCKED_UNREAD, TutorialService.state(b, ID));
        assertTrue(TutorialService.reset(b, ID));
        assertTrue(TutorialService.unlock(b, ID));
        assertEquals(TutorialState.UNLOCKED_UNREAD, TutorialService.state(a, ID));
        assertEquals(TutorialState.UNLOCKED_UNREAD, TutorialService.state(late, ID));
    }
}
