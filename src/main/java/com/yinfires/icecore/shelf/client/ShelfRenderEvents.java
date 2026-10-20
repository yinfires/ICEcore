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
        if(event.phase==TickEvent.Phase.START) { ShelfRenderCache.frame();ShelfSortCache.frame();ShelfFrameRecording.frame(); }
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { ShelfFrameRecording.stop();ShelfRenderer.clearModelCache(); }
    @SubscribeEvent public static void unload(net.minecraftforge.event.level.ChunkEvent.Unload event) {
        if(event.getLevel().isClientSide()) { com.mojang.blaze3d.systems.RenderSystem.recordRenderCall(ShelfSortCache::clear);ShelfSceneCache.unload(event.getLevel(),event.getChunk().getPos()); RackDisplayCache.unload(event.getLevel(),event.getChunk().getPos()); }
    }
    @SubscribeEvent public static void commands(net.minecraftforge.client.event.RegisterClientCommandsEvent event) {
        event.getDispatcher().register(ShelfDisplayCommands.tree(source->isOp(),ShelfRenderEvents::command));
    }
    private static boolean isOp() {
        var player=net.minecraft.client.Minecraft.getInstance().player;return player!=null && player.hasPermissions(2);
    }
    private static void feedback(String key,Object... values) {
        com.yinfires.icecore.feedback.PlayerFeedback.showLocal(net.minecraft.client.Minecraft.getInstance().player,
                net.minecraft.network.chat.Component.translatable("command.icecore.shelf."+key,values));
    }
    private static int command(String action,int value) {
        var config=com.yinfires.icecore.config.ICECoreClientConfig.SPEC;
        var enabled=com.yinfires.icecore.config.ICECoreClientConfig.SHELF_DISPLAY_ENABLED;
        var distance=com.yinfires.icecore.config.ICECoreClientConfig.SHELF_DISPLAY_DISTANCE;
        switch(action) {
            case "on","off","toggle","distance" -> {
                try {
                    if(action.equals("distance")) distance.set(value);
                    else enabled.set(action.equals("toggle")?!enabled.get():action.equals("on"));
                    config.save();
                } catch(RuntimeException failure) {
                    com.mojang.logging.LogUtils.getLogger().error("Cannot save shelf display preferences",failure);
                    feedback("save_failed");return 0;
                }
                status();
            }
            case "status" -> status();
            case "help" -> feedback(isOp()?"help_op":"help");
            case "native","optimized","hidden" -> {
                com.yinfires.icecore.compat.render.RenderDiagnostics.mode=com.yinfires.icecore.compat.render.RenderDiagnostics.Mode.valueOf(action.toUpperCase(java.util.Locale.ROOT));
                ShelfRenderer.clearModelCache();ShelfRenderStats.reset();feedback("mode",action);
            }
            case "stats" -> {
                ShelfRenderStats.ENABLED=value!=0;if(value!=0) ShelfRenderer.clearModelCache();ShelfRenderStats.reset();feedback("stats",value!=0);
            }
            case "query" -> { com.yinfires.icecore.compat.render.RenderDiagnostics.queryCache=value!=0;ShelfRenderStats.reset();feedback("query",value!=0); }
            case "frames_start" -> { ShelfFrameRecording.start();feedback("frames_start"); }
            case "frames_stop" -> { ShelfFrameRecording.stop();feedback("frames_stop"); }
            default -> throw new IllegalArgumentException(action);
        }
        return 1;
    }
    private static void status() {
        feedback("status",net.minecraft.network.chat.Component.translatable(com.yinfires.icecore.config.ICECoreClientConfig.SHELF_DISPLAY_ENABLED.get()?"options.on":"options.off"),
                com.yinfires.icecore.config.ICECoreClientConfig.SHELF_DISPLAY_DISTANCE.get(),com.yinfires.icecore.compat.render.RenderDiagnostics.mode.name().toLowerCase(java.util.Locale.ROOT));
    }
    private ShelfRenderEvents() {}
}
