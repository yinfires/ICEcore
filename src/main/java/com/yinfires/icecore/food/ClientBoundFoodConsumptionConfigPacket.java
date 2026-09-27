package com.yinfires.icecore.food;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientBoundFoodConsumptionConfigPacket(boolean disabled, boolean modCompatibility) {
    public ClientBoundFoodConsumptionConfigPacket(FriendlyByteBuf buffer) {
        this(buffer.readBoolean(), buffer.readBoolean());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(disabled);
        buffer.writeBoolean(modCompatibility);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> FoodConsumptionClientState.accept(disabled, modCompatibility));
        context.setPacketHandled(true);
    }
}
