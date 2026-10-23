package com.yinfires.icecore.compat.cozycafe.customer;

import com.google.gson.Gson;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.nio.file.*;
import java.util.*;
import java.util.function.Supplier;

public final class CafeCustomerSnapshot {
    private static final Gson GSON=new Gson();
    public record Payload(CafeCustomerConfig config,Map<String,Integer> reputation,Map<String,byte[]> skins) {}
    private final long revision;
    private final Payload payload;
    public CafeCustomerSnapshot(long revision,CafeCustomerConfig config,Map<String,Integer> reputation,boolean full){
        Map<String,Integer> active=new LinkedHashMap<>();for(var type:config.types())active.put(type.id(),reputation.getOrDefault(type.id(),0));
        this.revision=revision;this.payload=new Payload(full?config:null,Map.copyOf(active),full?skinBytes:Map.of());
    }
    private static Map<String,byte[]> skinBytes=Map.of();
    public static void validateSkins(CafeCustomerConfig config,Path folder)throws java.io.IOException {
        Map<String,byte[]> loaded=new HashMap<>();int total=GSON.toJson(config).getBytes(java.nio.charset.StandardCharsets.UTF_8).length+16384;
        for(var t:config.types())for(var s:t.skins())if(!s.file().isEmpty()) {
            Path path=folder.resolve("cafe_skins").resolve(s.file());if(!Files.exists(path))continue;
            byte[] bytes=Files.readAllBytes(path);total+=bytes.length+t.id().length()+s.id().length()+16;
            if(total>900000)throw new IllegalArgumentException("Customer snapshot exceeds safe packet budget");
            if(bytes.length<24||bytes[0]!=(byte)137||bytes[1]!=80||bytes[2]!=78||bytes[3]!=71)throw new IllegalArgumentException("Invalid PNG");
            var header=java.nio.ByteBuffer.wrap(bytes);int w=header.getInt(16),h=header.getInt(20);
            if(w!=64||(h!=64&&h!=32))throw new IllegalArgumentException("Skin must be 64x64 or 64x32");
            loaded.put(t.id()+"/"+s.id(),bytes);
        }
        skinBytes=Map.copyOf(loaded);
    }
    public CafeCustomerSnapshot(FriendlyByteBuf b){
        revision=b.readLong();
        CafeCustomerConfig config=b.readBoolean()?GSON.fromJson(b.readUtf(262144),CafeCustomerConfig.class):null;
        int n=b.readVarInt();if(n<0||n>128)throw new IllegalArgumentException("Invalid reputation size");
        Map<String,Integer> reps=new LinkedHashMap<>();for(int i=0;i<n;i++)reps.put(b.readUtf(64),b.readVarInt());
        int k=b.readVarInt();if(k<0||k>4096)throw new IllegalArgumentException("Invalid skin size");
        Map<String,byte[]> skins=new HashMap<>();int total=0;
        for(int i=0;i<k;i++){String id=b.readUtf(130);byte[] bytes=b.readByteArray(65536);total+=bytes.length;if(total>1048576)throw new IllegalArgumentException("Skin payload too large");skins.put(id,bytes);}
        payload=new Payload(config,reps,skins);
    }
    public void encode(FriendlyByteBuf b){
        b.writeLong(revision);b.writeBoolean(payload.config!=null);if(payload.config!=null)b.writeUtf(GSON.toJson(payload.config),262144);
        // Only active definitions belong in the public snapshot; retired accounts stay server-side.
        b.writeVarInt(payload.reputation.size());payload.reputation.forEach((id,value)->{b.writeUtf(id,64);b.writeVarInt(value);});
        b.writeVarInt(payload.skins.size());payload.skins.forEach((id,bytes)->{b.writeUtf(id,130);b.writeByteArray(bytes);});
    }
    public void handle(Supplier<NetworkEvent.Context> supplier){
        var c=supplier.get();c.enqueueWork(()->{if(c.getNetworkManager().isConnected())CafeCustomerClientState.accept(payload,revision);});c.setPacketHandled(true);
    }
}
