package com.yinfires.icecore.network;

import com.yinfires.icecore.recipehide.client.RecipeHideClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Full snapshot of the recipe-hide state, pushed to a client on login and on every change.
 * {@code hideAll} is the baseline; {@code recipeIds} are recipe exemptions when hideAll, else the
 * hidden recipe set; {@code shownItemIds}/{@code hiddenItemIds} are the per-item overrides used to
 * drive the JEI item list.
 */
public final class ClientBoundRecipeHidePacket {
    private final boolean hideAll;
    private final List<ResourceLocation> recipeIds;
    private final List<ResourceLocation> shownItemIds;
    private final List<ResourceLocation> hiddenItemIds;

    public ClientBoundRecipeHidePacket(boolean hideAll, List<ResourceLocation> recipeIds,
                                       List<ResourceLocation> shownItemIds, List<ResourceLocation> hiddenItemIds) {
        this.hideAll = hideAll;
        this.recipeIds = recipeIds;
        this.shownItemIds = shownItemIds;
        this.hiddenItemIds = hiddenItemIds;
    }

    public ClientBoundRecipeHidePacket(FriendlyByteBuf buffer) {
        this.hideAll = buffer.readBoolean();
        this.recipeIds = readIds(buffer);
        this.shownItemIds = readIds(buffer);
        this.hiddenItemIds = readIds(buffer);
    }

    private static List<ResourceLocation> readIds(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<ResourceLocation> ids = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ids.add(buffer.readResourceLocation());
        }
        return ids;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(hideAll);
        writeIds(buffer, recipeIds);
        writeIds(buffer, shownItemIds);
        writeIds(buffer, hiddenItemIds);
    }

    private static void writeIds(FriendlyByteBuf buffer, List<ResourceLocation> ids) {
        buffer.writeVarInt(ids.size());
        for (ResourceLocation id : ids) {
            buffer.writeResourceLocation(id);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> RecipeHideClientState.accept(hideAll, recipeIds, shownItemIds, hiddenItemIds));
        context.setPacketHandled(true);
    }
}
