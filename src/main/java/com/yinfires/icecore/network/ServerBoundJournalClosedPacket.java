package com.yinfires.icecore.network;

import com.yinfires.icecore.ICECore;
import com.yinfires.icecore.journal.JournalType;
import com.yinfires.icecore.quest.QuestService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> server: the player closed a journal screen. Drives the {@code icecore:open_journal}
 * objective, whose match target is the journal name ("quest" or "tutorial"). Sent once when the
 * quest or tutorial screen is removed, so "open the tutorial then close it" completes such a quest.
 */
public record ServerBoundJournalClosedPacket(JournalType type) {

    public ServerBoundJournalClosedPacket(FriendlyByteBuf buffer) {
        this(buffer.readEnum(JournalType.class));
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(type);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                String target = type == JournalType.TUTORIAL ? "tutorial" : "quest";
                QuestService.notifyObjectiveEvent(player,
                        ResourceLocation.fromNamespaceAndPath(ICECore.MOD_ID, "open_journal"), target);
            }
        });
        context.setPacketHandled(true);
    }
}
