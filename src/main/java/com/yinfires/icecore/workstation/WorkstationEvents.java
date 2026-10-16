package com.yinfires.icecore.workstation;

import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.*;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.*;
import com.github.ysbbbbbb.kaleidoscopecookery.init.tag.TagMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.*;

public final class WorkstationEvents {
    private WorkstationEvents() {}
    public static void register() { MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, PlayerInteractEvent.RightClickBlock.class, WorkstationEvents::click); }
    private static void click(PlayerInteractEvent.RightClickBlock event) {
        var player = event.getEntity(); var level = event.getLevel(); var pos = event.getPos();
        if (event.isCanceled() || event.getUseBlock() == Event.Result.DENY || event.getUseItem() == Event.Result.DENY
                || player.isSpectator() || !level.mayInteract(player, pos)) return;
        var entity = level.getBlockEntity(pos);
        if (!(entity instanceof WorkstationAccess)) return;
        var block = level.getBlockState(pos);
        String type = entity.getClass().getSimpleName();
        boolean ferment = type.equals("FermentationTankBlockEntity");
        boolean freezer = type.equals("FreezerBlockEntity");
        var held = player.getMainHandItem();
        if (held.is(com.yinfires.icecore.item.ModItems.WRENCH.get()) || player.getOffhandItem().is(com.yinfires.icecore.item.ModItems.WRENCH.get())) return;
        if (ferment && player.isShiftKeyDown() && held.isEmpty()) {
            consume(event);
            if (!level.isClientSide && event.getHand() == InteractionHand.MAIN_HAND) {
                boolean open = !block.getValue(BlockStateProperties.OPEN);
                if (open) ExternalWorkstation.set(entity, "recipeProgress", 0);
                level.setBlock(pos, block.setValue(BlockStateProperties.OPEN, open), 3);
                ExternalWorkstation.call(entity, "notifyTile");
            }
            return;
        }
        if (ferment && (player.isShiftKeyDown() || !block.getValue(BlockStateProperties.OPEN))) { consume(event); return; }
        if (freezer && (player.isShiftKeyDown() || !block.getValue(BlockStateProperties.OPEN))) return;
        boolean cookery = net.minecraftforge.fml.ModList.get().isLoaded("kaleidoscope_cookery");
        if (cookery && entity instanceof StockpotBlockEntity pot) {
            if (player.isShiftKeyDown()) return;
            if (pot.hasLid()) { consume(event); return; }
            // Native soup-base operations are distinct from item slots.
            if (pot.getStatus() == 0) return;
            if (pot.getStatus() == 1 && pot.isEmpty() && !held.isEmpty()
                    && (held.getItem() instanceof net.minecraft.world.item.BucketItem || held.is(net.minecraft.world.item.Items.BUCKET))) return;
        }
        if (cookery && entity instanceof PotBlockEntity pot) {
            if (player.isShiftKeyDown() && !held.isEmpty()) return;
            if (held.is(TagMod.KITCHEN_SHOVEL) || !held.isEmpty() && !block.getValue(PotBlock.HAS_OIL)) return;
            if (pot.getStatus() == 0 && !held.isEmpty() && !pot.hasHeatSource(level)) return;
        }
        if (freezer && (Boolean)ExternalWorkstation.call(entity, "isWorking")) { consume(event); return; }
        consume(event);
        if (level.isClientSide || event.getHand() != InteractionHand.MAIN_HAND) return;
        WorkstationAdapter adapter = ExternalWorkstationAdapters.create(entity);
        if (adapter != null) WorkstationInteractionService.interact(adapter, (ServerPlayer)player, InteractionHand.MAIN_HAND);
    }
    private static void consume(PlayerInteractEvent.RightClickBlock event) {
        event.setCanceled(true); event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
    }
}
