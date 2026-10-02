package com.yinfires.icecore.item;

import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Prevents only direct vanilla planting; other material interactions remain untouched. */
@Mod.EventBusSubscriber(modid = "icecore", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SeedBagEvents {
    private SeedBagEvents() {}

    @SubscribeEvent
    public static void blockVanillaPlanting(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof FarmBlock)) return;
        var stack = event.getItemStack();
        if (stack.is(Items.WHEAT_SEEDS) || stack.is(Items.CARROT)) {
            event.setUseBlock(Event.Result.DENY);
            event.setUseItem(Event.Result.DENY);
        }
    }
}
