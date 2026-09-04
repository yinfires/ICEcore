package com.yinfires.icecore.island;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Signals that the server has finished placing an island's blocks, so the client
 * can end the entrance cutscene: the swirl distortion has been intensifying the
 * whole time the (batched) placement ran, hidden behind the effect; on this signal
 * the client flashes white and reveals the finished island all at once. Carries only
 * the dimension so the client can ignore signals for a level it is not in.
 */
public record ClientBoundIslandEntranceDonePacket(ResourceLocation dimension) {
    public ClientBoundIslandEntranceDonePacket(FriendlyByteBuf buffer) {
        this(buffer.readResourceLocation());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(dimension);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> IslandEntranceClient.finish(this)));
        context.setPacketHandled(true);
    }
}
