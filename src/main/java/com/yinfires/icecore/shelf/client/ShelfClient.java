package com.yinfires.icecore.shelf.client;

import com.yinfires.icecore.ICECore;
import com.yinfires.icecore.shelf.ModShelf;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid=ICECore.MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ShelfClient {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) { event.enqueueWork(() -> MenuScreens.register(ModShelf.MENU.get(),ShelfScreen::new)); }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) { event.registerBlockEntityRenderer(ModShelf.ENTITY.get(),ShelfRenderer::new); }
    @SubscribeEvent public static void baked(ModelEvent.BakingCompleted event) { ShelfRenderer.clearModelCache(); }
    private ShelfClient() {}
}
