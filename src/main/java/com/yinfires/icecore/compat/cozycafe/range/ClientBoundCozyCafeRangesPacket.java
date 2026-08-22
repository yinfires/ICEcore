package com.yinfires.icecore.compat.cozycafe.range;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ClientBoundCozyCafeRangesPacket {
    private final String json;
    private final long revision;

    public ClientBoundCozyCafeRangesPacket(CozyCafeRangeData data, long revision) {
        json = CozyCafeRangeData.gson().toJson(data.toJson());
        this.revision = revision;
    }

    public ClientBoundCozyCafeRangesPacket(FriendlyByteBuf buffer) {
        json = buffer.readUtf(1_000_000);
        revision = buffer.readLong();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(json, 1_000_000);
        buffer.writeLong(revision);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> CozyCafeRangeClientState.accept(json, revision));
        context.setPacketHandled(true);
    }
}
