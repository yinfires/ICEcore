package com.yinfires.icecore.time;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientBoundDaySummaryPacket(long sequence, long income, TimeTimings timings) {
    public ClientBoundDaySummaryPacket(FriendlyByteBuf buffer) {
        this(buffer.readLong(), buffer.readLong(), readTimings(buffer));
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeLong(sequence);
        buffer.writeLong(income);
        buffer.writeVarInt(timings.typewriterTicksPerCharacter());
        buffer.writeVarInt(timings.summaryHoldTicks());
        buffer.writeVarInt(timings.summaryFadeTicks());
    }

    private static TimeTimings readTimings(FriendlyByteBuf buffer) {
        TimeTimings timings = new TimeTimings();
        timings.set("typewriterPerCharacter", buffer.readVarInt());
        timings.set("summaryHold", buffer.readVarInt());
        timings.set("summaryFade", buffer.readVarInt());
        return timings;
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> NaturalDaySummaryClient.accept(this));
        context.setPacketHandled(true);
    }
}
