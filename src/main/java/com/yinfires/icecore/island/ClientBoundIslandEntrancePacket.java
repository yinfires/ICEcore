package com.yinfires.icecore.island;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Tells clients in a dimension to play the island entrance cutscene: take over the
 * view with the recorded entrance camera, run the screen-warp post effect and a
 * white flash, and let the (server-placed) island pop in behind the flash at the
 * buildup peak. Carries no block data — the server places the real blocks; the
 * client only drives the camera and the effect.
 *
 * @param eyeX      recorded camera eye position (world)
 * @param yaw       recorded look angles (degrees)
 * @param buildup    ticks from receipt to when server placement starts; the swirl
 *                   distortion ramps up over this window and keeps intensifying while
 *                   placement runs, until the done packet triggers the flash + reveal
 * @param placeTicks server estimate of placement duration (deterministic); the client
 *                   paces the swirl so its peak lines up with the actual reveal
 */
public record ClientBoundIslandEntrancePacket(ResourceLocation dimension,
                                              double eyeX, double eyeY, double eyeZ,
                                              float yaw, float pitch,
                                              int buildup, int placeTicks) {
    public ClientBoundIslandEntrancePacket(FriendlyByteBuf buffer) {
        this(buffer.readResourceLocation(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readFloat(), buffer.readFloat(),
                buffer.readVarInt(), buffer.readVarInt());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(dimension);
        buffer.writeDouble(eyeX);
        buffer.writeDouble(eyeY);
        buffer.writeDouble(eyeZ);
        buffer.writeFloat(yaw);
        buffer.writeFloat(pitch);
        buffer.writeVarInt(buildup);
        buffer.writeVarInt(placeTicks);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> IslandEntranceClient.begin(this)));
        context.setPacketHandled(true);
    }
}
