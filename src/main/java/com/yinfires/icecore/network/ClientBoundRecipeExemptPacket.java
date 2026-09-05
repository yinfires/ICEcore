package com.yinfires.icecore.network;

import com.yinfires.icecore.recipehide.client.RecipeHideClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Toggles the recipe-hide exempt mode on the receiving client (visual only; server ban unaffected). */
public final class ClientBoundRecipeExemptPacket {
    private final boolean exempt;

    public ClientBoundRecipeExemptPacket(boolean exempt) {
        this.exempt = exempt;
    }

    public ClientBoundRecipeExemptPacket(FriendlyByteBuf buffer) {
        this.exempt = buffer.readBoolean();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(exempt);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> RecipeHideClientState.setExemptMode(exempt));
        context.setPacketHandled(true);
    }
}
