package com.yinfires.icecore.worldmap.client;

import com.yinfires.icecore.ICECore;
import com.yinfires.icecore.worldmap.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import java.util.concurrent.*;

public final class MapClient {
    public static final KeyMapping OPEN=new KeyMapping("key.icecore.map",KeyConflictContext.IN_GAME,InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_M,"key.categories.icecore");
    static final ExecutorService WORK=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"icecore-map-images");t.setDaemon(true);return t;});
    static MapDefinition definition=new MapDefinition();
    static Set<String> unlocked=Set.of();
    static List<String> sources=List.of();
    static long revision=-1;
    static boolean operator;
    private static net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> lastDimension;
    private MapClient(){}
    public static void accept(MapNetwork.Snapshot p){
        if(p.revision()<revision)return;
        String previous=MapDefinition.JSON.toJson(definition);
        boolean changed=p.revision()!=revision || !p.definition().equals(previous);
        definition=MapDefinition.parse(p.definition());unlocked=p.unlocked();sources=p.sources();operator=p.operator();revision=p.revision();
        var mc=Minecraft.getInstance();
        if(!operator&&(mc.screen instanceof IslandMapScreen screen&&screen.isEditor()||mc.screen instanceof MapUnlockPreviewScreen||mc.screen instanceof MapChoiceScreen)){disposeSession();mc.setScreen(null);return;}
        if(p.openEditor()&&operator){if(available())try{mc.setScreen(new IslandMapScreen(true));}catch(IllegalStateException ex){mc.setScreen(null);feedback("xaero_not_ready");}else feedback("missing_xaero");}
        else if(changed&&mc.screen instanceof IslandMapScreen screen)screen.serverChanged(previous);
        else if(changed&&mc.screen instanceof MapUnlockPreviewScreen screen)screen.serverChanged(previous);
        else if(changed&&mc.screen instanceof MapChoiceScreen screen)screen.serverChanged(previous);
    }
    static void request(String action,String id,String payload){MapNetwork.request(new MapNetwork.Request(action,id,payload,revision));}
    static void feedback(String key){com.yinfires.icecore.feedback.PlayerFeedback.showLocal(Minecraft.getInstance().player,Component.translatable("icecore.map."+key));}
    private static boolean available(){return ModList.get().isLoaded("xaeroworldmap");}
    private static void disposeSession(){var screen=Minecraft.getInstance().screen;if(screen instanceof MapChoiceScreen choice)choice.disposeSession();else if(screen instanceof MapUnlockPreviewScreen preview)preview.disposeSession();else if(screen instanceof IslandMapScreen map)map.removed();}
    @Mod.EventBusSubscriber(modid=ICECore.MOD_ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent e){e.register(OPEN);}
        @SubscribeEvent public static void resources(RegisterClientReloadListenersEvent e){e.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)m->Minecraft.getInstance().execute(()->{var screen=Minecraft.getInstance().screen;if(screen instanceof IslandMapScreen s)s.refresh();else if(screen instanceof MapUnlockPreviewScreen p)p.resourceChanged();else if(screen instanceof MapChoiceScreen c)c.resourceChanged();}));}
    }
    @Mod.EventBusSubscriber(modid=ICECore.MOD_ID,value=Dist.CLIENT)
    public static final class Events {
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
            if(e.phase!=TickEvent.Phase.END)return;
            var mc=Minecraft.getInstance();var current=mc.level==null?null:mc.level.dimension();
            if(!Objects.equals(current,lastDimension)){lastDimension=current;if(available())XaeroSnapshot.cancel();if(mc.screen instanceof IslandMapScreen screen)screen.dimensionChanged();else if(mc.screen instanceof MapChoiceScreen||mc.screen instanceof MapUnlockPreviewScreen){disposeSession();mc.setScreen(null);}}
            while(OPEN.consumeClick())if(mc.player!=null&&mc.screen==null&&!com.yinfires.icecore.client.cutscene.CutsceneCamera.engaged()){
                if(!available()){feedback("missing_xaero");continue;}request("open","","");mc.setScreen(new IslandMapScreen(false));
            }
        }
        @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){
            disposeSession();
            if(available())XaeroSnapshot.cancel();lastDimension=null;revision=-1;definition=new MapDefinition();unlocked=Set.of();sources=List.of();operator=false;
        }
        @SubscribeEvent public static void render(TickEvent.RenderTickEvent e){if(e.phase==TickEvent.Phase.END&&available())XaeroSnapshot.renderStep();}
    }
}
