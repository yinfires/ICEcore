package com.yinfires.icecore.network;

import com.yinfires.icecore.quest.QuestService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> server: set or clear the player's single marked quest. A null id (hasId=false) clears
 * the mark; the server also treats re-marking the already-marked quest as a clear.
 */
public record ServerBoundSetMarkPacket(ResourceLocation questId) {

    public ServerBoundSetMarkPacket(FriendlyByteBuf buffer) {
        this(buffer.readBoolean() ? buffer.readResourceLocation() : null);
    }

    public void encode(FriendlyByteBuf buffer) {
        boolean hasId = questId != null;
        buffer.writeBoolean(hasId);
        if (hasId) {
            buffer.writeResourceLocation(questId);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                QuestService.setMarked(player, questId);
            }
        });
        context.setPacketHandled(true);
    }
}
