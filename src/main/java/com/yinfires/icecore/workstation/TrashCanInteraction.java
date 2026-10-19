package com.yinfires.icecore.workstation;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.misc.TrashCanBlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Player-only bridge; the trash can continues to own storage, eviction and effects. */
public final class TrashCanInteraction {
    private TrashCanInteraction() {}

    public static void put(TrashCanBlockEntity trash, ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) return;
        ItemStack input = held.copy();
        ItemStack empty = WorkstationContainerInteraction.requiredEmptyContainer(input,
                WorkstationContainerInteraction.FluidContainerMode.STORED_AS_ITEM);
        if (!empty.isEmpty()) {
            int capacity = WorkstationInventoryTransfer.exchangeBatch(player, empty, input.getCount(), true, true);
            if (capacity == 0) return;
            input.setCount(capacity);
        }
        int offered = input.getCount();
        trash.putItem(input);
        int moved = offered - input.getCount();
        if (moved <= 0) return;
        if (!empty.isEmpty()) WorkstationInventoryTransfer.exchangeBatch(player, empty, moved, true, false);
        else if (!player.getAbilities().instabuild) held.shrink(moved);
    }

    public static int last(TrashCanBlockEntity trash) {
        var storage = trash.getStorage();
        for (int slot = storage.getSlots() - 1; slot >= 0; slot--)
            if (!storage.getStackInSlot(slot).isEmpty()
                    && !com.yinfires.icecore.spoilage.ShelfLife.expired(storage.getStackInSlot(slot))) return slot;
        return -1;
    }

    /** Receive first; the native withdraw method subsequently commits this remainder. */
    public static ItemStack receive(TrashCanBlockEntity trash, ServerPlayer player) {
        var storage = trash.getStorage();
        var state = ((WorkstationAccess) trash).icecore$slots();
        state.reconcile(storage.getSlots(), storage::getStackInSlot);
        int slot = last(trash);
        if (slot < 0) return null;
        ItemStack held = player.getMainHandItem();
        ItemStack required = state.container(slot);
        if (!WorkstationContainerInteraction.canTake(required, held)) {
            if (held.isEmpty() && !required.isEmpty()) WorkstationInteractionService.need(player, required);
            return null;
        }
        ItemStack stored = storage.getStackInSlot(slot);
        int moved;
        if (required.isEmpty()) {
            ItemStack simulated = WorkstationInventoryTransfer.insert(player, stored, true);
            moved = stored.getCount() - simulated.getCount();
            if (moved <= 0) return null;
            ItemStack remainder = WorkstationInventoryTransfer.insert(player, stored.copyWithCount(moved), false);
            moved -= remainder.getCount();
        } else {
            int requested = Math.min(stored.getCount(), held.getCount());
            moved = WorkstationInventoryTransfer.exchangeBatch(player, stored, requested, true, false);
        }
        return moved <= 0 ? null : stored.copyWithCount(stored.getCount() - moved);
    }
}
