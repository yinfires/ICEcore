package com.yinfires.icecore.compat.cozycafe.customer.client;
import com.yinfires.icecore.ICECore;
import com.yinfires.icecore.compat.cozycafe.customer.CafeCustomerClientState;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

public final class CafeCustomerKeys {
    public static final KeyMapping OPEN=new KeyMapping("key.icecore.customers",KeyConflictContext.IN_GAME,InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_H,"key.categories.icecore");
    private CafeCustomerKeys(){}
    @Mod.EventBusSubscriber(modid=ICECore.MOD_ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration{
        @SubscribeEvent public static void register(RegisterKeyMappingsEvent e){if(net.minecraftforge.fml.ModList.get().isLoaded("cozycafe"))e.register(OPEN);}
        @SubscribeEvent public static void resources(RegisterClientReloadListenersEvent e){e.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)manager->CafeCustomerSkins.invalidate());}
    }
    @Mod.EventBusSubscriber(modid=ICECore.MOD_ID,value=Dist.CLIENT)
    public static final class Events{
        @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
        public static void nameplate(RenderNameTagEvent event){
            if(net.minecraftforge.fml.ModList.get().isLoaded("cozycafe")
                    &&event.getEntity() instanceof io.github.chakyl.cozycafe.entities.CustomerEntity)
                event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
        }
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
            if(e.phase!=TickEvent.Phase.END||!net.minecraftforge.fml.ModList.get().isLoaded("cozycafe"))return;
            var m=Minecraft.getInstance();if(m.player!=null)CafeCustomerSkins.prepare();while(OPEN.consumeClick())if(m.player!=null&&m.screen==null&&!com.yinfires.icecore.client.cutscene.CutsceneCamera.engaged())m.setScreen(new CafeCustomerScreen());
        }
        @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){CafeCustomerClientState.clear();CafeCustomerSkins.clear();}
    }
}
