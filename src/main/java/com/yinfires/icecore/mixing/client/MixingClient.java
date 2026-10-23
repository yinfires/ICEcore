package com.yinfires.icecore.mixing.client;

import com.yinfires.icecore.ICECore;
import com.yinfires.icecore.mixing.MixingBowlRenderer;
import com.yinfires.icecore.mixing.ModMixing;
import com.yinfires.icecore.oven.ModOven;
import com.yinfires.icecore.oven.OvenRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ICECore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MixingClient {
    private MixingClient() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModMixing.MIXING_BOWL_ENTITY.get(), MixingBowlRenderer::new);
        event.registerBlockEntityRenderer(ModOven.OVEN_ENTITY.get(), OvenRenderer::new);
    }
}
