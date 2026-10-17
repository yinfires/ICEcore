package com.yinfires.icecore.interaction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class InteractionIntegrationTest {
    @TempDir Path directory;
    @Test void clientRejectsOlderSnapshotsAndClearsBetweenConnections() {
        InteractionClientState.Events.logout(null);
        var location = new InteractionExceptions.Location("minecraft:overworld", 0, 64, 0);
        var newer = new InteractionSnapshot(java.util.Set.of(), java.util.Set.of(), InteractionExceptions.empty().edit(location, "minecraft:chest"));
        InteractionClientState.accept(newer, 5);
        InteractionClientState.accept(InteractionSnapshot.empty(), 4);
        assertEquals(newer, InteractionClientState.snapshot());
        InteractionClientState.Events.logout(null);
        assertEquals(InteractionSnapshot.empty(), InteractionClientState.snapshot());
        InteractionClientState.accept(newer, 1);
        assertEquals(newer, InteractionClientState.snapshot());
        InteractionClientState.Events.logout(null);
    }
    @Test void actualMinecraftTargetsRetainPredictionPacketAndForgeEventEntrypoints() throws Exception {
        var mode = bytecode("net/minecraft/client/multiplayer/MultiPlayerGameMode");
        var inner = mode.methods.stream().filter(method -> method.name.equals("performUseItemOn") || method.name.equals("m_233746_")).findFirst().orElseThrow();
        assertEquals("(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;", inner.desc);
        assertTrue(calls(inner, "net/minecraftforge/common/ForgeHooks", "onRightClickBlock"));
        assertTrue(mode.methods.stream().anyMatch(method -> calls(method, "net/minecraft/client/multiplayer/MultiPlayerGameMode", "startPrediction")
                || calls(method, "net/minecraft/client/multiplayer/MultiPlayerGameMode", "m_233729_")));
        var state = bytecode("net/minecraft/world/level/block/state/BlockBehaviour$BlockStateBase");
        assertTrue(state.methods.stream().anyMatch(method -> (method.name.equals("use") || method.name.equals("m_60664_"))
                && method.desc.equals("(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;")));
    }
    private org.objectweb.asm.tree.ClassNode bytecode(String name) throws Exception {
        try (var stream = getClass().getClassLoader().getResourceAsStream(name + ".class")) {
            assertNotNull(stream, name); var node = new org.objectweb.asm.tree.ClassNode();
            new org.objectweb.asm.ClassReader(stream).accept(node, 0); return node;
        }
    }
    private boolean calls(org.objectweb.asm.tree.MethodNode method, String owner, String name) {
        for (var instruction : method.instructions) if (instruction instanceof org.objectweb.asm.tree.MethodInsnNode call
                && call.owner.equals(owner) && call.name.equals(name)) return true;
        return false;
    }
    @Test void atomicWriterRoundTripsUtf8AndLeavesNoTemporaryFiles() throws Exception {
        Path file = directory.resolve("rules.json");
        InteractionManager.write(file, InteractionConfig.empty().json());
        var json = com.google.gson.JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
        assertEquals(InteractionConfig.empty(), InteractionConfig.parse(json));
        try (var files = Files.list(directory)) { assertEquals(1, files.count()); }
        InteractionManager.write(file, new InteractionConfig(java.util.List.of(new InteractionConfig.Rule("minecraft:chest", null, null)), java.util.List.of()).json());
        assertEquals(1, InteractionConfig.parse(com.google.gson.JsonParser.parseString(Files.readString(file))).blacklist().size());
    }
    @Test void failedReplacementPreservesDestinationAndCleansTemporaryFile() throws Exception {
        Path destination = directory.resolve("rules.json"); Files.createDirectory(destination);
        Path marker = destination.resolve("keep"); Files.writeString(marker, "previous");
        assertThrows(java.io.IOException.class, () -> InteractionManager.write(destination, InteractionConfig.empty().json()));
        assertEquals("previous", Files.readString(marker));
        try (var files = Files.list(directory)) { assertEquals(1, files.count()); }
    }
    private String source(String path) throws Exception {
        return Files.readString(Path.of("src/main/java/com/yinfires/icecore/" + path + ".java"), StandardCharsets.UTF_8);
    }
    @Test void serverPlacementClaimsPrecedeRestrictionAndCommitBeforePublishing() throws Exception {
        String events = source("building/BuildingEvents");
        assertTrue(events.indexOf("BuildingServerActions.executePlacement(player, context)") < events.indexOf("InteractionEvents.blocked"));
        assertTrue(events.contains("PLACEMENT_CLAIMS.remove(event)"));
        String manager = source("interaction/InteractionManager");
        assertTrue(manager.indexOf("write(rulesFile(), next.json()); publish") > 0);
        assertTrue(manager.contains("write(exceptionsFile(), next.json())"));
        String reload = manager.substring(manager.indexOf("public void reload()"), manager.indexOf("private static JsonElement read"));
        assertTrue(reload.indexOf("InteractionExceptions.parse") < reload.indexOf("publish(next, nextSnapshot)"));
    }
    @Test void bothHandsAndClientPredictionUseFailWithoutReplacingOuterPacketEntry() throws Exception {
        String mixin = source("mixin/MultiPlayerGameModeMixin");
        assertTrue(mixin.indexOf("BuildingClientEvents.claimPlacement") < mixin.indexOf("shouldBlockRestrictedUse"));
        assertTrue(mixin.contains("InteractionResult.FAIL")); assertTrue(mixin.contains("m_233746_("));
        assertFalse(mixin.contains("m_233732_("));
        String client = source("building/BuildingClientEvents");
        assertTrue(client.contains("INTERACTION_PLACEMENT_CLAIMS.remove(event)"));
        assertTrue(client.contains("!isAdjustmentStick(player.getOffhandItem())"));
        assertTrue(source("interaction/InteractionClientState").contains("revision = -1"));
        assertTrue(source("interaction/InteractionEvents").contains("TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD"));
        assertTrue(source("interaction/InteractionCommands").contains("source.hasPermission(2)"));
        assertFalse(source("interaction/InteractionCommands").contains("sendSystemMessage"));
    }
}
