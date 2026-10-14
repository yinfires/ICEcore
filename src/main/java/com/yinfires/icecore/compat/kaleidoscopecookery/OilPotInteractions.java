package com.yinfires.icecore.compat.kaleidoscopecookery;

import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.OilPotBlock;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModList;

/** Loaded only when Cookery is installed. No polling or cached world state. */
public final class OilPotInteractions {
    private static final boolean GRILLING = ModList.get().isLoaded("kaleidoscope_grilling");

    private OilPotInteractions() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false,
                PlayerInteractEvent.RightClickBlock.class, OilPotInteractions::onRightClick);
    }

    private static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        var player = event.getEntity();
        var level = event.getLevel();
        var pos = event.getPos();
        if (event.isCanceled() || event.getUseBlock() == Event.Result.DENY
                || event.getUseItem() == Event.Result.DENY
                || event.getHand() != InteractionHand.MAIN_HAND || player.isSpectator()
                || !level.mayInteract(player, pos)) return;
        var state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof OilPotBlock pot)) return;
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() && player.isShiftKeyDown()) {
            consumeClick(event);
            if (level.isClientSide) return;
            ItemStack pickedUp = pot.getCloneItemStack(state, event.getHitVec(), level, pos, player);
            if (pickedUp.isEmpty()) return;
            var replacement = state.getValue(OilPotBlock.WATERLOGGED)
                    ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
            // No destroyBlock: its loot would duplicate the item we hand to the player.
            if (level.setBlock(pos, replacement, 3)) {
                player.setItemInHand(InteractionHand.MAIN_HAND, pickedUp);
            }
        } else if (GRILLING && held.is(Items.BUCKET)) {
            GrillingOilPotBuckets.tryFill(event);
        }
    }

    static void consumeClick(PlayerInteractEvent.RightClickBlock event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
    }
}
