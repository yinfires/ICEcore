package com.yinfires.icecore.time;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public record ClientBoundVotePacket(long revision, int total, List<PlayerEntry> confirmed) {
    public ClientBoundVotePacket(FriendlyByteBuf b) { this(b.readLong(), b.readVarInt(), read(b)); }
    private static List<PlayerEntry> read(FriendlyByteBuf b) { int n=b.readVarInt(); List<PlayerEntry> out=new ArrayList<>(n); for(int i=0;i<n;i++) out.add(new PlayerEntry(b.readUUID(),b.readUtf(64))); return List.copyOf(out); }
    public void encode(FriendlyByteBuf b) { b.writeLong(revision); b.writeVarInt(total); b.writeVarInt(confirmed.size()); for(var e:confirmed){b.writeUUID(e.uuid());b.writeUtf(e.name(),64);} }
    public void handle(Supplier<NetworkEvent.Context> s) { var c=s.get(); c.enqueueWork(()->TimeVoteClientState.accept(this)); c.setPacketHandled(true); }
    public record PlayerEntry(UUID uuid,String name) {}
}
