package com.yinfires.icecore.compat.cozycafe.customer.client;

import com.yinfires.icecore.compat.cozycafe.customer.CafeCustomerClientState;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import java.util.*;

/** Decode on snapshot/resource changes; the renderer only performs bounded map lookups. */
public final class CafeCustomerSkins {
    private static final ResourceLocation STEVE=new ResourceLocation("minecraft","textures/entity/player/wide/steve.png");
    private static final Map<String,ResourceLocation> LOCAL=new HashMap<>();
    private static final Map<String,ResourceLocation> RESOURCES=new HashMap<>();
    private record Pending(String type,com.yinfires.icecore.compat.cozycafe.customer.CafeCustomerConfig.Skin skin){}
    private static final Deque<Pending> PENDING=new ArrayDeque<>();
    private static Object loaded;
    private CafeCustomerSkins(){}
    public static void prepare(){
        var payload=CafeCustomerClientState.payload();
        if(loaded!=payload.config()){
            clear();loaded=payload.config();
            for(var type:payload.config().types())for(var skin:type.skins())PENDING.addLast(new Pending(type.id(),skin));
        }
        var minecraft=Minecraft.getInstance();
        for(int budget=0;budget<2&&!PENDING.isEmpty();budget++){
            var pending=PENDING.removeFirst();var skin=pending.skin();String id=pending.type()+"/"+skin.id();
            if(!skin.resource().isEmpty()){
                ResourceLocation resource=ResourceLocation.tryParse(skin.resource());
                if(resource!=null&&minecraft.getResourceManager().getResource(resource).isPresent())RESOURCES.put(skin.resource(),resource);
                continue;
            }
            byte[] bytes=payload.skins().get(id);if(bytes==null)continue;
            try{
                NativeImage image=NativeImage.read(new java.io.ByteArrayInputStream(bytes));
                if(image.getWidth()!=64||image.getHeight()!=64){image.close();continue;}
                LOCAL.put(id,minecraft.getTextureManager().register("icecore_cafe_skin",new DynamicTexture(image)));
            }catch(java.io.IOException|RuntimeException invalid){/* Missing or invalid skin uses Steve. */}
        }
    }
    public static ResourceLocation texture(CompoundTag visit){
        return visit.getString("Resource").isEmpty()?LOCAL.getOrDefault(visit.getString("Skin"),STEVE):RESOURCES.getOrDefault(visit.getString("Resource"),STEVE);
    }
    public static boolean slim(CompoundTag visit){return visit.getBoolean("Slim")&&!texture(visit).equals(STEVE);}
    public static void invalidate(){loaded=null;}
    public static void clear(){var manager=Minecraft.getInstance().getTextureManager();LOCAL.values().forEach(manager::release);LOCAL.clear();RESOURCES.clear();PENDING.clear();loaded=null;}
}
