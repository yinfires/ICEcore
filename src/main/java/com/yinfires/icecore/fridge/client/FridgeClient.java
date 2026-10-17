package com.yinfires.icecore.fridge.client;

import com.yinfires.icecore.ICECore;
import com.yinfires.icecore.fridge.ModFridge;
import net.blay09.mods.cookingforblockheads.block.BlockKitchen;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = ICECore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FridgeClient {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemBlockRenderTypes.setRenderLayer(ModFridge.FRIDGE.get(), RenderType.cutoutMipped()));
    }
    @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Block event) {
        // Identical to ModRenderers' native fridge handler; no per-face state allocation.
        event.register((state, level, pos, tint) -> state.getValue(BlockKitchen.COLOR).getTextColor(), ModFridge.FRIDGE.get());
    }
    private FridgeClient() {}
}
