package com.yinfires.icecore.compat.kaleidoscopecookery;

import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.StockpotBlock;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;

/** Cookery-only, server-authoritative lid state changes without an item inventory. */
public final class StockpotInteractions {
    private StockpotInteractions() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false,
                PlayerInteractEvent.RightClickBlock.class, StockpotInteractions::onRightClick);
    }

    static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        var player = event.getEntity();
        var level = event.getLevel();
        var pos = event.getPos();
        if (event.isCanceled() || event.getUseBlock() == Event.Result.DENY
                || event.getUseItem() == Event.Result.DENY || player.isSpectator()
                || !player.isShiftKeyDown() || !player.getMainHandItem().isEmpty()
                || !level.mayInteract(player, pos)) return;
        var state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof StockpotBlock)) return;
        // Also consume a secondary-hand retry, but only the main hand can mutate state.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        if (level.isClientSide || event.getHand() != InteractionHand.MAIN_HAND) return;
        boolean closed = !state.getValue(StockpotBlock.HAS_LID);
        if (level.setBlock(pos, state.setValue(StockpotBlock.HAS_LID, closed), 3)) {
            level.playSound(null, pos, closed ? SoundEvents.LANTERN_PLACE : SoundEvents.LANTERN_BREAK,
                    SoundSource.BLOCKS, 0.5F, 0.5F);
        }
    }
}
