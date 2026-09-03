package com.yinfires.icecore.network;

import com.yinfires.icecore.tutorial.TutorialService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: mark a tutorial read (sent when the player opens its detail in the UI). */
public record ServerBoundMarkTutorialReadPacket(ResourceLocation tutorialId) {

    public ServerBoundMarkTutorialReadPacket(FriendlyByteBuf buffer) {
        this(buffer.readResourceLocation());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(tutorialId);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                TutorialService.markRead(player, tutorialId);
            }
        });
        context.setPacketHandled(true);
    }
}
