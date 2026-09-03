package com.yinfires.icecore.network;

import com.yinfires.icecore.quest.QuestSnapshot;
import com.yinfires.icecore.quest.client.QuestClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Server -> client: the player's ACTIVE quests and single marked quest id. */
public record ClientBoundQuestSyncPacket(List<QuestSnapshot> snapshots, ResourceLocation marked) {

    public ClientBoundQuestSyncPacket(FriendlyByteBuf buffer) {
        this(readSnapshots(buffer), buffer.readBoolean() ? buffer.readResourceLocation() : null);
    }

    private static List<QuestSnapshot> readSnapshots(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        List<QuestSnapshot> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(QuestSnapshot.decode(buffer));
        }
        return list;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(snapshots.size());
        for (QuestSnapshot snapshot : snapshots) {
            snapshot.encode(buffer);
        }
        boolean hasMark = marked != null;
        buffer.writeBoolean(hasMark);
        if (hasMark) {
            buffer.writeResourceLocation(marked);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> QuestClientState.accept(snapshots, marked));
        context.setPacketHandled(true);
    }
}
