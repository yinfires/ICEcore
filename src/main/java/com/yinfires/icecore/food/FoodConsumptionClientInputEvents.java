package com.yinfires.icecore.food;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FoodConsumptionClientInputEvents {
    private FoodConsumptionClientInputEvents() {
    }
}
