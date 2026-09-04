package com.yinfires.icecore.network;

import com.yinfires.icecore.ICECore;
import com.yinfires.icecore.building.ClientBoundBuildingRulesPacket;
import com.yinfires.icecore.compat.cozycafe.range.ClientBoundCozyCafeRangesPacket;
import com.yinfires.icecore.currency.ClientBoundCurrencyPacket;
import com.yinfires.icecore.time.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ICECoreNetwork {
    private static final String PROTOCOL_VERSION = "8";
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
        CHANNEL.messageBuilder(ClientBoundBuildingRulesPacket.class, 2, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientBoundBuildingRulesPacket::encode)
                .decoder(ClientBoundBuildingRulesPacket::new)
                .consumerMainThread(ClientBoundBuildingRulesPacket::handle)
                .add();
        CHANNEL.messageBuilder(ClientBoundCozyCafeRangesPacket.class, 3, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientBoundCozyCafeRangesPacket::encode)
                .decoder(ClientBoundCozyCafeRangesPacket::new)
                .consumerMainThread(ClientBoundCozyCafeRangesPacket::handle)
                .add();
        CHANNEL.messageBuilder(ClientBoundCurrencyPacket.class, 4, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientBoundCurrencyPacket::encode)
                .decoder(ClientBoundCurrencyPacket::new)
                .consumerMainThread(ClientBoundCurrencyPacket::handle)
                .add();
        CHANNEL.messageBuilder(ClientBoundTimeSnapshotPacket.class, 5, NetworkDirection.PLAY_TO_CLIENT).encoder(ClientBoundTimeSnapshotPacket::encode).decoder(ClientBoundTimeSnapshotPacket::new).consumerMainThread(ClientBoundTimeSnapshotPacket::handle).add();
        CHANNEL.messageBuilder(ClientBoundVotePacket.class, 6, NetworkDirection.PLAY_TO_CLIENT).encoder(ClientBoundVotePacket::encode).decoder(ClientBoundVotePacket::new).consumerMainThread(ClientBoundVotePacket::handle).add();
        CHANNEL.messageBuilder(ClientBoundCutscenePacket.class, 7, NetworkDirection.PLAY_TO_CLIENT).encoder(ClientBoundCutscenePacket::encode).decoder(ClientBoundCutscenePacket::new).consumerMainThread(ClientBoundCutscenePacket::handle).add();
        CHANNEL.messageBuilder(ServerBoundCutsceneAckPacket.class, 8, NetworkDirection.PLAY_TO_SERVER).encoder(ServerBoundCutsceneAckPacket::encode).decoder(ServerBoundCutsceneAckPacket::new).consumerMainThread(ServerBoundCutsceneAckPacket::handle).add();
        CHANNEL.messageBuilder(ClientBoundDaySummaryPacket.class, 9, NetworkDirection.PLAY_TO_CLIENT).encoder(ClientBoundDaySummaryPacket::encode).decoder(ClientBoundDaySummaryPacket::new).consumerMainThread(ClientBoundDaySummaryPacket::handle).add();
        CHANNEL.messageBuilder(ServerBoundGiveItemPacket.class, 10, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ServerBoundGiveItemPacket::encode).decoder(ServerBoundGiveItemPacket::new)
                .consumerMainThread(ServerBoundGiveItemPacket::handle).add();
        CHANNEL.messageBuilder(ClientBoundGiveResultPacket.class, 11, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientBoundGiveResultPacket::encode).decoder(ClientBoundGiveResultPacket::new)
                .consumerMainThread(ClientBoundGiveResultPacket::handle).add();
        CHANNEL.messageBuilder(ClientBoundQuestSyncPacket.class, 12, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientBoundQuestSyncPacket::encode).decoder(ClientBoundQuestSyncPacket::new)
                .consumerMainThread(ClientBoundQuestSyncPacket::handle).add();
        CHANNEL.messageBuilder(ClientBoundTutorialSyncPacket.class, 13, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientBoundTutorialSyncPacket::encode).decoder(ClientBoundTutorialSyncPacket::new)
                .consumerMainThread(ClientBoundTutorialSyncPacket::handle).add();
        CHANNEL.messageBuilder(ServerBoundSetMarkPacket.class, 14, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ServerBoundSetMarkPacket::encode).decoder(ServerBoundSetMarkPacket::new)
                .consumerMainThread(ServerBoundSetMarkPacket::handle).add();
        CHANNEL.messageBuilder(ServerBoundMarkTutorialReadPacket.class, 15, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ServerBoundMarkTutorialReadPacket::encode).decoder(ServerBoundMarkTutorialReadPacket::new)
                .consumerMainThread(ServerBoundMarkTutorialReadPacket::handle).add();
        CHANNEL.messageBuilder(ServerBoundJournalClosedPacket.class, 16, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ServerBoundJournalClosedPacket::encode).decoder(ServerBoundJournalClosedPacket::new)
                .consumerMainThread(ServerBoundJournalClosedPacket::handle).add();
        CHANNEL.messageBuilder(com.yinfires.icecore.island.ClientBoundIslandEntrancePacket.class, 17, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(com.yinfires.icecore.island.ClientBoundIslandEntrancePacket::encode)
                .decoder(com.yinfires.icecore.island.ClientBoundIslandEntrancePacket::new)
                .consumerMainThread(com.yinfires.icecore.island.ClientBoundIslandEntrancePacket::handle).add();
        CHANNEL.messageBuilder(com.yinfires.icecore.island.ClientBoundIslandEntranceDonePacket.class, 18, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(com.yinfires.icecore.island.ClientBoundIslandEntranceDonePacket::encode)
                .decoder(com.yinfires.icecore.island.ClientBoundIslandEntranceDonePacket::new)
                .consumerMainThread(com.yinfires.icecore.island.ClientBoundIslandEntranceDonePacket::handle).add();
    }

    public static void sendToServer(ServerBoundAddMenuItemPacket packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendToPlayer(ClientBoundAddMenuItemPacket packet, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToPlayer(ClientBoundBuildingRulesPacket packet, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToPlayer(ClientBoundCozyCafeRangesPacket packet, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToPlayer(ClientBoundCurrencyPacket packet, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
    public static void sendToPlayer(ClientBoundTimeSnapshotPacket packet,ServerPlayer player){CHANNEL.send(PacketDistributor.PLAYER.with(()->player),packet);}
    public static void sendToPlayer(ClientBoundVotePacket packet,ServerPlayer player){CHANNEL.send(PacketDistributor.PLAYER.with(()->player),packet);}
    public static void sendToPlayer(ClientBoundCutscenePacket packet,ServerPlayer player){CHANNEL.send(PacketDistributor.PLAYER.with(()->player),packet);}
    public static void sendToPlayer(ClientBoundDaySummaryPacket packet,ServerPlayer player){CHANNEL.send(PacketDistributor.PLAYER.with(()->player),packet);}
    public static void sendToServer(ServerBoundCutsceneAckPacket packet){CHANNEL.sendToServer(packet);}
    public static void sendToServer(ServerBoundGiveItemPacket packet){CHANNEL.sendToServer(packet);}
    public static void sendToPlayer(ClientBoundGiveResultPacket packet, ServerPlayer player){CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);}
    public static void sendToPlayer(ClientBoundQuestSyncPacket packet, ServerPlayer player){CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);}
    public static void sendToPlayer(ClientBoundTutorialSyncPacket packet, ServerPlayer player){CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);}
    public static void sendToServer(ServerBoundSetMarkPacket packet){CHANNEL.sendToServer(packet);}
    public static void sendToServer(ServerBoundMarkTutorialReadPacket packet){CHANNEL.sendToServer(packet);}
    public static void sendToServer(ServerBoundJournalClosedPacket packet){CHANNEL.sendToServer(packet);}

    /** Broadcasts an island entrance cutscene to every player in the given dimension. */
    public static void sendToDimension(com.yinfires.icecore.island.ClientBoundIslandEntrancePacket packet,
                                       net.minecraft.server.level.ServerLevel level) {
        CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension), packet);
    }

    /** Broadcasts island-placement-done (reveal) to every player in the given dimension. */
    public static void sendToDimension(com.yinfires.icecore.island.ClientBoundIslandEntranceDonePacket packet,
                                       net.minecraft.server.level.ServerLevel level) {
        CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension), packet);
    }
}
