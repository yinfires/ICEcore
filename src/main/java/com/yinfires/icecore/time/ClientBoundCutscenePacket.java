package com.yinfires.icecore.time;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record ClientBoundCutscenePacket(long sequence, boolean abort, TimeCamera camera, long fromTime, long targetTime,
                                         long income, long serverGameTime, TimeTimings timings) {
    public ClientBoundCutscenePacket(FriendlyByteBuf b) { this(b.readLong(),b.readBoolean(),new TimeCamera(b.readDouble(),b.readDouble(),b.readDouble(),b.readFloat(),b.readFloat()),b.readLong(),b.readLong(),b.readLong(),b.readLong(),readTimings(b)); }
    private static TimeTimings readTimings(FriendlyByteBuf b){TimeTimings t=new TimeTimings();String[] n={"fadeToCamera","revealCamera","fastForward","typewriterPerCharacter","summaryHold","summaryFade","fadeToPlayer","revealPlayer"};for(String x:n)t.set(x,b.readVarInt());return t;}
    public void encode(FriendlyByteBuf b){b.writeLong(sequence);b.writeBoolean(abort);b.writeDouble(camera.x());b.writeDouble(camera.y());b.writeDouble(camera.z());b.writeFloat(camera.yaw());b.writeFloat(camera.pitch());b.writeLong(fromTime);b.writeLong(targetTime);b.writeLong(income);b.writeLong(serverGameTime);for(String n:new String[]{"fadeToCamera","revealCamera","fastForward","typewriterPerCharacter","summaryHold","summaryFade","fadeToPlayer","revealPlayer"})b.writeVarInt(timings.get(n));}
    public void handle(Supplier<NetworkEvent.Context> s){var c=s.get();c.enqueueWork(()->TimeCutsceneClient.accept(this));c.setPacketHandled(true);}
}
