package com.yinfires.icecore.worldmap;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.*;
import java.io.ByteArrayOutputStream;
import io.netty.buffer.Unpooled;

public final class MapNetwork {
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation("icecore","world_map"),()->"1","1"::equals,"1"::equals);
    private MapNetwork(){}
    private static final Map<ServerPlayer,Transfer> TRANSFERS=new WeakHashMap<>();
    private static final class Transfer {
        final UUID id;final int count;final long deadline=System.nanoTime()+30000000000L;
        final ByteArrayOutputStream bytes=new ByteArrayOutputStream();int next;
        Transfer(UUID id,int count){this.id=id;this.count=count;}
    }
    public static void register(){
        CHANNEL.messageBuilder(Snapshot.class,0,NetworkDirection.PLAY_TO_CLIENT).encoder(Snapshot::encode).decoder(Snapshot::decode).consumerMainThread((p,c)->{
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->com.yinfires.icecore.worldmap.client.MapClient.accept(p));c.get().setPacketHandled(true);
        }).add();
        CHANNEL.messageBuilder(Fragment.class,2,NetworkDirection.PLAY_TO_SERVER).encoder(Fragment::encode).decoder(Fragment::decode).consumerMainThread((p,c)->{
            var player=c.get().getSender();if(player!=null&&player.hasPermissions(2))receive(player,p);c.get().setPacketHandled(true);
        }).add();
        CHANNEL.messageBuilder(Request.class,1,NetworkDirection.PLAY_TO_SERVER).encoder(Request::encode).decoder(Request::decode).consumerMainThread((p,c)->{
            var player=c.get().getSender();if(player!=null)MapService.request(player,p);c.get().setPacketHandled(true);
        }).add();
    }
    public static void send(ServerPlayer p,Snapshot packet){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),packet);}
    public static void request(Request packet){
        if(packet.payload().length()<6000){CHANNEL.sendToServer(packet);return;}
        FriendlyByteBuf buffer=new FriendlyByteBuf(Unpooled.buffer());
        try{packet.encode(buffer);if(buffer.readableBytes()>1000000)throw new IllegalArgumentException("map edit too large");UUID id=UUID.randomUUID();int count=(buffer.readableBytes()+23999)/24000;for(int i=0;i<count;i++){byte[] bytes=new byte[Math.min(24000,buffer.readableBytes())];buffer.readBytes(bytes);CHANNEL.sendToServer(new Fragment(id,i,count,bytes));}}finally{buffer.release();}
    }
    private static void receive(ServerPlayer player,Fragment f){
        if(f.count<1||f.count>42||f.index<0||f.index>=f.count)return;
        Transfer t=TRANSFERS.get(player);
        if(f.index==0){t=new Transfer(f.id,f.count);TRANSFERS.put(player,t);}
        if(t==null||!t.id.equals(f.id)||t.count!=f.count||t.next!=f.index||System.nanoTime()>t.deadline||t.bytes.size()+f.data.length>1000000){TRANSFERS.remove(player);return;}
        t.bytes.writeBytes(f.data);t.next++;
        if(t.next==t.count){TRANSFERS.remove(player);FriendlyByteBuf buffer=new FriendlyByteBuf(Unpooled.wrappedBuffer(t.bytes.toByteArray()));try{MapService.request(player,Request.decode(buffer));}finally{buffer.release();}}
    }
    static void expireTransfers(){long now=System.nanoTime();TRANSFERS.entrySet().removeIf(e->e.getKey().isRemoved()||now>e.getValue().deadline);}
    static void clearTransfers(){TRANSFERS.clear();}
    private record Fragment(UUID id,int index,int count,byte[] data){
        void encode(FriendlyByteBuf b){b.writeUUID(id);b.writeVarInt(index);b.writeVarInt(count);b.writeByteArray(data);}
        static Fragment decode(FriendlyByteBuf b){return new Fragment(b.readUUID(),b.readVarInt(),b.readVarInt(),b.readByteArray(24000));}
    }
    public record Request(String action,String id,String payload,long revision){
        void encode(FriendlyByteBuf b){b.writeUtf(action,16);b.writeUtf(id,48);b.writeUtf(payload,900000);b.writeLong(revision);}
        static Request decode(FriendlyByteBuf b){return new Request(b.readUtf(16),b.readUtf(48),b.readUtf(900000),b.readLong());}
    }
    public record Snapshot(long revision,String definition,Set<String> unlocked,boolean operator,boolean openEditor,List<String> sources){
        void encode(FriendlyByteBuf b){b.writeLong(revision);b.writeUtf(definition,900000);b.writeCollection(unlocked,(o,s)->o.writeUtf(s,48));b.writeBoolean(operator);b.writeBoolean(openEditor);b.writeCollection(sources,(o,s)->o.writeUtf(s,128));}
        static Snapshot decode(FriendlyByteBuf b){long rev=b.readLong();String json=b.readUtf(900000);int n=b.readVarInt();if(n<0||n>128)throw new IllegalArgumentException("unlock count");Set<String> ids=new HashSet<>();for(int i=0;i<n;i++)ids.add(b.readUtf(48));boolean op=b.readBoolean(),editor=b.readBoolean();n=b.readVarInt();if(n<0||n>4096)throw new IllegalArgumentException("binding count");List<String> sources=new ArrayList<>();for(int i=0;i<n;i++)sources.add(b.readUtf(128));return new Snapshot(rev,json,Set.copyOf(ids),op,editor,List.copyOf(sources));}
    }
}
