package com.yinfires.icecore.network;

import com.yinfires.icecore.tutorial.TutorialState;
import com.yinfires.icecore.tutorial.client.TutorialClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Server -> client: the player's unlocked tutorials and their read state. */
public record ClientBoundTutorialSyncPacket(Map<ResourceLocation, TutorialState> states) {

    public ClientBoundTutorialSyncPacket(FriendlyByteBuf buffer) {
        this(readStates(buffer));
    }

    private static Map<ResourceLocation, TutorialState> readStates(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        Map<ResourceLocation, TutorialState> map = new HashMap<>(count);
        for (int i = 0; i < count; i++) {
            ResourceLocation id = buffer.readResourceLocation();
            map.put(id, buffer.readEnum(TutorialState.class));
        }
        return map;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(states.size());
        states.forEach((id, state) -> {
            buffer.writeResourceLocation(id);
            buffer.writeEnum(state);
        });
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> TutorialClientState.accept(states));
        context.setPacketHandled(true);
    }
}
