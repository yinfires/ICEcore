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
        var root=net.minecraft.commands.Commands.literal("icecore_render")
            .requires(source->net.minecraft.client.Minecraft.getInstance().player!=null && net.minecraft.client.Minecraft.getInstance().player.hasPermissions(2));
        for(var mode:com.yinfires.icecore.compat.render.RenderDiagnostics.Mode.values()) {
            root.then(net.minecraft.commands.Commands.literal(mode.name().toLowerCase(java.util.Locale.ROOT)).executes(context->{
                com.yinfires.icecore.compat.render.RenderDiagnostics.mode=mode;ShelfRenderer.clearModelCache();ShelfRenderStats.reset();return 1;
            }));
        }
        root.then(net.minecraft.commands.Commands.literal("stats").then(net.minecraft.commands.Commands.argument("enabled",com.mojang.brigadier.arguments.BoolArgumentType.bool()).executes(context->{
            ShelfRenderStats.ENABLED=com.mojang.brigadier.arguments.BoolArgumentType.getBool(context,"enabled");if(ShelfRenderStats.ENABLED) ShelfRenderer.clearModelCache();ShelfRenderStats.reset();return 1;
        })));
        root.then(net.minecraft.commands.Commands.literal("query").then(net.minecraft.commands.Commands.argument("enabled",com.mojang.brigadier.arguments.BoolArgumentType.bool()).executes(context->{
            com.yinfires.icecore.compat.render.RenderDiagnostics.queryCache=com.mojang.brigadier.arguments.BoolArgumentType.getBool(context,"enabled");ShelfRenderStats.reset();return 1;
        })));
        root.then(net.minecraft.commands.Commands.literal("frames")
            .then(net.minecraft.commands.Commands.literal("start").executes(context->{ShelfFrameRecording.start();return 1;}))
            .then(net.minecraft.commands.Commands.literal("stop").executes(context->{ShelfFrameRecording.stop();return 1;})));
        event.getDispatcher().register(root);
    }
    private ShelfRenderEvents() {}
}
