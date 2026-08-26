package com.yinfires.icecore.time;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record ClientBoundTimeSnapshotPacket(String json, long dayTime, boolean daylightCycle) {
    public ClientBoundTimeSnapshotPacket(FriendlyByteBuf b) { this(b.readUtf(32767), b.readLong(), b.readBoolean()); }
    public void encode(FriendlyByteBuf b) { b.writeUtf(json,32767); b.writeLong(dayTime); b.writeBoolean(daylightCycle); }
    public void handle(Supplier<NetworkEvent.Context> s) { var c=s.get(); c.enqueueWork(()->TimeClientState.accept(json,dayTime,daylightCycle)); c.setPacketHandled(true); }
}
