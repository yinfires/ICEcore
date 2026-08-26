package com.yinfires.icecore.time;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record ServerBoundCutsceneAckPacket(long sequence) {
    public ServerBoundCutsceneAckPacket(FriendlyByteBuf b){this(b.readLong());}
    public void encode(FriendlyByteBuf b){b.writeLong(sequence);}
    public void handle(Supplier<NetworkEvent.Context> s){var c=s.get();var p=c.getSender();c.enqueueWork(()->{if(p!=null)TimeVoteManager.ack(p,sequence);});c.setPacketHandled(true);}
}
