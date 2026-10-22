package com.yinfires.icecore.compat.cozycafe;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="icecore")
public final class CafeTableEvents {
    private CafeTableEvents() {}
    @SubscribeEvent public static void preventOccupiedBreak(net.minecraftforge.event.level.BlockEvent.BreakEvent event) {
        if (event.getLevel().getBlockEntity(event.getPos()) instanceof CafeTableAccess table && table.icecore$hasContainerContent())
            event.setCanceled(true);
    }
}
