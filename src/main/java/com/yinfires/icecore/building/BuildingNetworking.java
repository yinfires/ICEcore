package com.yinfires.icecore.building;

import com.yinfires.icecore.network.ICECoreNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Network bridge kept separate from the data manager to avoid a client class dependency. */
public final class BuildingNetworking {
    private BuildingNetworking() {
    }

    public static void broadcastRules(MinecraftServer server, BuildingData data, long revision) {
        // Rule snapshots are added by the client protocol implementation. Keeping this hook server-safe
        // lets data reloads take effect immediately even before a player reconnects.
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ICECoreNetwork.sendToPlayer(new ClientBoundBuildingRulesPacket(data, revision), player);
        }
    }

    public static void sendRules(ServerPlayer player, BuildingData data, long revision) {
        ICECoreNetwork.sendToPlayer(new ClientBoundBuildingRulesPacket(data, revision), player);
    }
}
