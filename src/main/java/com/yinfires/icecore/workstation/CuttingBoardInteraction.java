package com.yinfires.icecore.workstation;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** On-demand transfers; retained output counts are reduced only by actual inventory acceptance. */
public final class CuttingBoardInteraction {
    private CuttingBoardInteraction() {}
    public static void interact(BlockEntity entity, ServerPlayer player, InteractionHand hand) {
        CuttingBoardAccess board = (CuttingBoardAccess)entity;
        var products = board.icecore$products();
        var inventory = board.icecore$inventory();
        boolean finished = !products.isEmpty();
        int size = finished ? products.size() : 1;
        java.util.function.IntFunction<ItemStack> items = finished ? products::get : inventory::getStackInSlot;
        var state = board.icecore$containers();
        state.reconcile(size, items);
        ItemStack held = player.getItemInHand(hand);
        int last = state.last(items);
        if (held.isEmpty()) {
            boolean changed = false;
            while (last >= 0) {
                ItemStack required = state.container(last);
                if (!required.isEmpty()) { WorkstationInteractionService.need(player, required); break; }
                ItemStack stored = items.apply(last);
                ItemStack remaining = WorkstationInventoryTransfer.insert(player, stored, false);
                int moved = stored.getCount() - remaining.getCount();
                if (moved == 0) break;
                stored.shrink(moved); changed = true;
                if (!stored.isEmpty()) break;
                state.reconcile(size, items); last = state.last(items);
            }
            if (changed) { finish(entity, board); sound(entity, player, false); }
            return;
        }
        if (last >= 0 && !state.container(last).isEmpty()
                && WorkstationContainerCompat.emptyMatches(state.container(last), held)) {
            // exchange can otherwise drop a replacement when the held stack has multiple containers.
            ItemStack output = items.apply(last).copyWithCount(1);
            if ((held.getCount() > 1 || player.getAbilities().instabuild) && !canFit(player, output)) return;
            WorkstationContainerInteraction.exchange(player, hand, held, output);
            items.apply(last).shrink(1); finish(entity, board); sound(entity, player, false); return;
        }
        if (finished) return;
        if (!inventory.getStackInSlot(0).isEmpty()) {
            ExternalWorkstation.invoke(entity, "processStoredItemUsingTool",
                    new Class<?>[]{ItemStack.class, net.minecraft.world.entity.player.Player.class}, held, player);
            return;
        }
        if (board.icecore$carved()) return;
        ItemStack input = held.copyWithCount(1);
        ItemStack empty = WorkstationContainerInteraction.requiredEmptyContainer(input,
                WorkstationContainerInteraction.FluidContainerMode.STORED_AS_ITEM);
        if (!empty.isEmpty() && (held.getCount() > 1 || player.getAbilities().instabuild) && !canFit(player, empty)) return;
        inventory.setStackInSlot(0, input);
        state.reconcile(1, inventory::getStackInSlot); state.inserted(0, input, empty);
        if (!empty.isEmpty()) WorkstationContainerInteraction.exchange(player, hand, held, empty);
        else if (!player.getAbilities().instabuild) held.shrink(1);
        ExternalWorkstation.changed(entity);
        sound(entity, player, true);
    }
    private static boolean canFit(ServerPlayer player, ItemStack stack) {
        return net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(
                new net.minecraftforge.items.wrapper.PlayerMainInvWrapper(player.getInventory()), stack.copy(), true).isEmpty();
    }
    private static void sound(BlockEntity entity, ServerPlayer player, boolean input) {
        player.level().playSound(null, entity.getBlockPos(), input ? net.minecraft.sounds.SoundEvents.ITEM_FRAME_ADD_ITEM
                : net.minecraft.sounds.SoundEvents.ITEM_FRAME_REMOVE_ITEM, net.minecraft.sounds.SoundSource.BLOCKS, .8F, 1.1F);
    }
    private static void finish(BlockEntity entity, CuttingBoardAccess board) {
        boolean finished = !board.icecore$products().isEmpty();
        var items = board.icecore$products();
        board.icecore$containers().reconcile(finished ? items.size() : 1,
                finished ? items::get : board.icecore$inventory()::getStackInSlot);
        if (finished && items.stream().allMatch(ItemStack::isEmpty)) {
            items.clear(); board.icecore$containers().reconcile(1, board.icecore$inventory()::getStackInSlot);
        }
        if (board.icecore$inventory().getStackInSlot(0).isEmpty()) board.icecore$uncarve();
        ExternalWorkstation.changed(entity);
    }
}
