package com.yinfires.icecore.network;

import com.yinfires.icecore.ICECore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ICECoreNetwork {
    private static final String PROTOCOL_VERSION = "2";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(ICECore.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static boolean registered;

    private ICECoreNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        CHANNEL.messageBuilder(ServerBoundAddMenuItemPacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ServerBoundAddMenuItemPacket::encode)
                .decoder(ServerBoundAddMenuItemPacket::new)
                .consumerMainThread(ServerBoundAddMenuItemPacket::handle)
                .add();
        CHANNEL.messageBuilder(ClientBoundAddMenuItemPacket.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientBoundAddMenuItemPacket::encode)
                .decoder(ClientBoundAddMenuItemPacket::new)
                .consumerMainThread(ClientBoundAddMenuItemPacket::handle)
                .add();
    }

    public static void sendToServer(ServerBoundAddMenuItemPacket packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendToPlayer(ClientBoundAddMenuItemPacket packet, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
