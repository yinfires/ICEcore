package com.yinfires.icecore.network;
import com.yinfires.icecore.adventure.AdventureItemService;
import net.minecraft.network.FriendlyByteBuf;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraftforge.network.NetworkEvent;
public record ServerBoundGiveItemPacket(UUID target, boolean all) {
 public ServerBoundGiveItemPacket(FriendlyByteBuf b){this(b.readUUID(),b.readBoolean());}
 public void encode(FriendlyByteBuf b){b.writeUUID(target);b.writeBoolean(all);}
 public void handle(Supplier<NetworkEvent.Context> s){var c=s.get(); c.enqueueWork(()->{var p=c.getSender();if(p!=null)AdventureItemService.give(p,target,all);});c.setPacketHandled(true);}
}
