package com.yinfires.icecore.building;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class BuildingInteractionContractTest {
    @Test
    void dependencyFailurePrecedesContentAndDoesNotReleaseWrenchClaim() throws Exception {
        String server = source("building/BuildingServerActions");
        int dependency = server.indexOf("if (BuildingRules.hasDependents(level, keyPosition))");
        assertTrue(dependency > server.indexOf("if (!BuildingRules.canBreak(level, keyPosition, state))"));
        assertTrue(dependency < server.indexOf("int contentFingerprint ="));
        assertTrue(dependency < server.indexOf("if (!canFit("));
        String failure = server.substring(dependency, server.indexOf("ItemStack blockStack =", dependency));
        assertTrue(failure.contains("clearRemovalConfirmation(player)"));
        assertTrue(failure.contains("PlayerFeedback.show(player,"));
        assertTrue(failure.contains("icecore.build.block_depended_on"));
        assertTrue(failure.contains("return false;"));
        assertFalse(failure.contains("isShiftKeyDown"));
        String target = server.substring(server.indexOf("public static boolean isManagedTarget"),
                server.indexOf("private static boolean canOccupy"));
        assertFalse(target.contains("hasDependents"));
        String client = source("building/BuildingClientEvents");
        assertFalse(client.contains("hasDependents"));
        for (String lang : new String[]{"zh_cn", "en_us"}) {
            var json = com.google.gson.JsonParser.parseString(Files.readString(Path.of(
                    "src/main/resources/assets/icecore/lang/" + lang + ".json"))).getAsJsonObject();
            assertEquals(lang.equals("zh_cn") ? "该方块被其他方块依赖" : "Other blocks depend on this block",
                    json.get("icecore.build.block_depended_on").getAsString());
        }
    }

    private String source(String name) throws Exception {
        return Files.readString(Path.of("src/main/java/com/yinfires/icecore/" + name + ".java"));
    }

    @Test
    void placementIsClaimedBeforeMutationAndReassertedAfterwards() throws Exception {
        String events = source("building/BuildingEvents");
        int claim = events.indexOf("PLACEMENT_CLAIMS.add(event)");
        assertTrue(claim > events.indexOf("if (context != null)"));
        assertTrue(claim < events.indexOf("BuildingServerActions.executePlacement(player, context)"));
        assertTrue(events.contains("PLACEMENT_CLAIMS.remove(event)"));
        assertTrue(events.contains("setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS)"));
    }

    @Test
    void clientConsumesPlacementInsidePredictionWithoutSwallowingOuterPacket() throws Exception {
        String mixin = source("mixin/MultiPlayerGameModeMixin");
        assertTrue(mixin.contains("m_233746_(Lnet/minecraft/client/player/LocalPlayer;"));
        assertTrue(mixin.contains("BuildingClientEvents.claimPlacement(player, hand, hit)"));
        assertTrue(mixin.contains("callback.setReturnValue(InteractionResult.SUCCESS)"));
        assertFalse(mixin.contains("m_233732_("));
        assertTrue(mixin.contains(".isWrench(player.getItemInHand(hand)) ? InteractionResult.SUCCESS : InteractionResult.PASS"));
        String client = source("building/BuildingClientEvents");
        assertTrue(client.contains("placement == null || !placement.allowed()"));
        assertTrue(client.contains("!player.canReach(hit.getBlockPos(), 1.5D)"));
    }

    @Test
    void wrenchUsesRegionPermissionRatherThanManagedIdentity() throws Exception {
        String server = source("building/BuildingServerActions");
        String target = server.substring(server.indexOf("public static boolean isManagedTarget"),
                server.indexOf("private static boolean canOccupy"));
        assertTrue(target.contains("BuildingRules.canBreak(level, keyPosition, state)"));
        assertFalse(target.contains("BuildingRuntimeCache.isManaged"));
        String client = source("building/BuildingClientEvents");
        String clientTarget = client.substring(client.indexOf("private static WrenchPreview wrenchInteractionTarget"),
                client.indexOf("public static boolean shouldBlockWrenchBlockUse"));
        assertTrue(clientTarget.contains("BuildingClientRules.canBreak(minecraft.level, position, state)"));
        assertFalse(clientTarget.contains("BuildingClientRules.isManaged"));
        assertTrue(source("building/BuildingEvents").contains("BuildingServerActions.isWrench(stack)\n                && BuildingServerActions.isManagedTarget"));
    }
}
