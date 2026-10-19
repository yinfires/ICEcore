package com.yinfires.icecore.spoilage;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Main-world day for tooltip prediction; item NBT remains authoritative. */
public record ClientBoundShelfLifePacket(long day) {
    public static long currentDay;
    public ClientBoundShelfLifePacket(FriendlyByteBuf buffer) { this(buffer.readLong()); }
    public void encode(FriendlyByteBuf buffer) { buffer.writeLong(day); }
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> currentDay = day);
        context.get().setPacketHandled(true);
    }
}
