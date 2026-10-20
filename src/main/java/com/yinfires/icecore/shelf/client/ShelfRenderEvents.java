package com.yinfires.icecore.shelf.client;

import com.yinfires.icecore.ICECore;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=ICECore.MOD_ID,value=Dist.CLIENT)
public final class ShelfRenderEvents {
    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent event) {
        if(event.phase==TickEvent.Phase.START) ShelfRenderCache.frame();
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { ShelfRenderer.clearModelCache(); }
    @SubscribeEvent public static void unload(net.minecraftforge.event.level.ChunkEvent.Unload event) {
        if(event.getLevel().isClientSide()) ShelfSceneCache.unload(event.getLevel(),event.getChunk().getPos());
    }
    private ShelfRenderEvents() {}
}
