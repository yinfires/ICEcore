package com.yinfires.icecore.compat.cozycafe.range;

import com.yinfires.icecore.network.ICECoreNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class CozyCafeRangeNetworking {
    private CozyCafeRangeNetworking() {
    }

    public static void broadcast(MinecraftServer server, CozyCafeRangeData data, long revision) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ICECoreNetwork.sendToPlayer(new ClientBoundCozyCafeRangesPacket(data, revision), player);
        }
    }

    public static void send(ServerPlayer player, CozyCafeRangeData data, long revision) {
        ICECoreNetwork.sendToPlayer(new ClientBoundCozyCafeRangesPacket(data, revision), player);
    }
}
