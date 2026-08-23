package com.yinfires.icecore.currency;

import com.yinfires.icecore.network.ICECoreNetwork;
import net.minecraft.server.level.ServerPlayer;

public final class CurrencyNetworking {
    private CurrencyNetworking() {
    }

    public static void sendSnapshot(ServerPlayer player, long balance, boolean hudEnabled) {
        ICECoreNetwork.sendToPlayer(new ClientBoundCurrencyPacket(balance, balance, 0L, hudEnabled, false), player);
    }

    public static void broadcastSnapshot() {
        for (ServerPlayer player : currentPlayers()) {
            sendSnapshot(player, CurrencyService.balance(), CurrencyService.hudEnabled());
        }
    }

    public static void broadcastChange(CurrencyChange change) {
        ClientBoundCurrencyPacket packet = new ClientBoundCurrencyPacket(change.previous(), change.current(),
                change.delta(), CurrencyService.hudEnabled(), true);
        for (ServerPlayer player : currentPlayers()) {
            ICECoreNetwork.sendToPlayer(packet, player);
        }
    }

    private static java.util.List<ServerPlayer> currentPlayers() {
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        return server == null ? java.util.List.of() : server.getPlayerList().getPlayers();
    }
}
