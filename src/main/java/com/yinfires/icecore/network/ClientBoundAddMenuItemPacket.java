package com.yinfires.icecore.network;

import com.yinfires.icecore.compat.cozycafe.CozyCafeCompat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ClientBoundAddMenuItemPacket {
    private final ItemStack itemStack;

    public ClientBoundAddMenuItemPacket(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public ClientBoundAddMenuItemPacket(FriendlyByteBuf buffer) {
        this(buffer.readItem());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeItem(itemStack);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> CozyCafeCompat.addToClientMenu(itemStack));
        context.setPacketHandled(true);
    }
}
