package com.yinfires.icecore.building;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ClientBoundBuildingRulesPacket {
    private final String json;
    private final long revision;

    public ClientBoundBuildingRulesPacket(BuildingData data, long revision) {
        this.json = BuildingData.gson().toJson(data.toJson());
        this.revision = revision;
    }

    public ClientBoundBuildingRulesPacket(FriendlyByteBuf buffer) {
        this.json = buffer.readUtf(1_000_000);
        this.revision = buffer.readLong();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(json, 1_000_000);
        buffer.writeLong(revision);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Client preview state is intentionally updated in a client-only bridge.
            BuildingClientState.accept(json, revision);
        });
        context.setPacketHandled(true);
    }
}
