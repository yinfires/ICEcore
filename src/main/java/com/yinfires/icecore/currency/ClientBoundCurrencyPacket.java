package com.yinfires.icecore.currency;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientBoundCurrencyPacket(long previous, long current, long delta,
                                        boolean hudEnabled, boolean animate) {
    public ClientBoundCurrencyPacket(FriendlyByteBuf buffer) {
        this(buffer.readLong(), buffer.readLong(), buffer.readLong(), buffer.readBoolean(), buffer.readBoolean());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeLong(previous);
        buffer.writeLong(current);
        buffer.writeLong(delta);
        buffer.writeBoolean(hudEnabled);
        buffer.writeBoolean(animate);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> CurrencyClientState.accept(previous, current, delta, hudEnabled, animate));
        context.setPacketHandled(true);
    }
}
