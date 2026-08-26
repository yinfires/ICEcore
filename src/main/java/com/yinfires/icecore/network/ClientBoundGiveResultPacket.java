package com.yinfires.icecore.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record ClientBoundGiveResultPacket(boolean success) {
    public ClientBoundGiveResultPacket(FriendlyByteBuf buffer) { this(buffer.readBoolean()); }
    public void encode(FriendlyByteBuf buffer) { buffer.writeBoolean(success); }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> { if (Minecraft.getInstance().player != null) Minecraft.getInstance().player.swing(InteractionHand.MAIN_HAND); });
        context.setPacketHandled(true);
    }
}
