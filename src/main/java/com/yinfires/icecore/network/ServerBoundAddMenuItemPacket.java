package com.yinfires.icecore.network;

import com.yinfires.icecore.compat.cozycafe.CozyCafeCompat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ServerBoundAddMenuItemPacket {
    private final ItemStack itemStack;

    public ServerBoundAddMenuItemPacket(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public ServerBoundAddMenuItemPacket(FriendlyByteBuf buffer) {
        this(buffer.readItem());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeItem(itemStack);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            ItemStack stack = itemStack.copy();
            stack.setCount(1);
            if (CozyCafeCompat.addToMenu(player, stack)) {
                ICECoreNetwork.sendToPlayer(new ClientBoundAddMenuItemPacket(stack), player);
            }
        });
        context.setPacketHandled(true);
    }
}
