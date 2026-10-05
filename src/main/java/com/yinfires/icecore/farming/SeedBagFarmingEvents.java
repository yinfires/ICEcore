package com.yinfires.icecore.farming;

import com.yinfires.icecore.item.SeedBagItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Right-click rules for seed-bag farming, mirrored on both sides so the client predicts the swing
 * while the server stays authoritative. RightClickBlock fires before vanilla's adventure-mode
 * checks, which is what lets adventure players plant, harvest and till here.
 */
@Mod.EventBusSubscriber(modid = "icecore", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SeedBagFarmingEvents {
    private SeedBagFarmingEvents() {}

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        Player player = event.getEntity();
        BlockPos pos = event.getPos();
        ItemStack stack = event.getItemStack();

        if (stack.getItem() instanceof SeedBagItem bag) {
            finish(event, SeedBagCrops.plant(level, player, event.getHand(), pos, bag.definition(), stack));
            return;
        }
        if (SeedBagCrops.isOriginalPlantingItem(stack)) {
            event.setUseItem(Event.Result.DENY);
        }

        BlockState state = level.getBlockState(pos);
        SeedBagCrops.Crop crop = SeedBagCrops.find(state.getBlock());
        if (crop != null) {
            if (SeedBagCrops.isHoe(stack)) {
                finish(event, SeedBagCrops.harvestWithHoe(level, player, pos, state));
            } else if (SeedBagCrops.isHarvestReady(crop, level, pos, state)) {
                // Upstream right-click harvesting only triggers on ripe plants; block it there so
                // immature interactions (e.g. tying cucumbers to ropes) keep working.
                event.setUseBlock(Event.Result.DENY);
            }
            return;
        }

        if (state.is(ModFarming.UNTILLED_DIRT.get()) && SeedBagCrops.isHoe(stack)
                && !player.getAbilities().mayBuild && level.getBlockState(pos.above()).isAir()) {
            finish(event, tillInAdventure(event));
        }
    }

    private static InteractionResult tillInAdventure(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        // ItemStack#useOn rejects adventure players before HoeItem runs; lift mayBuild only for this call.
        player.getAbilities().mayBuild = true;
        try {
            return event.getItemStack().useOn(new UseOnContext(player, event.getHand(), event.getHitVec()));
        } finally {
            player.getAbilities().mayBuild = false;
        }
    }

    private static void finish(PlayerInteractEvent.RightClickBlock event, InteractionResult result) {
        if (!result.consumesAction()) return;
        event.setCanceled(true);
        event.setCancellationResult(result);
    }
}
