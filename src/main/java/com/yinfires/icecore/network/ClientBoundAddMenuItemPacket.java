package com.yinfires.icecore.network;

import com.yinfires.icecore.compat.cozycafe.CozyCafeCompat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ClientBoundAddMenuItemPacket {
    private final ItemStack itemStack;
    private final boolean added;

    public ClientBoundAddMenuItemPacket(ItemStack itemStack, boolean added) {
        this.itemStack = itemStack;
        this.added = added;
    }

    public ClientBoundAddMenuItemPacket(FriendlyByteBuf buffer) {
        this(buffer.readItem(), buffer.readBoolean());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeItem(itemStack);
        buffer.writeBoolean(added);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> CozyCafeCompat.handleClientMenuAddition(itemStack, added));
        context.setPacketHandled(true);
    }
}
