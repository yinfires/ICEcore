package com.yinfires.icecore.workstation;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

/** Normal inventory insertion, with simulation before workstation contents are consumed. */
public final class WorkstationInventoryTransfer {
    private WorkstationInventoryTransfer() {}
    public static ItemStack insert(Player player, ItemStack stack, boolean simulate) {
        return ItemHandlerHelper.insertItemStacked(new PlayerMainInvWrapper(player.getInventory()), stack.copy(), simulate);
    }
    public static boolean giveOne(Player player, ItemStack stack) {
        ItemStack single = stack.copyWithCount(1);
        if (!insert(player, single, true).isEmpty()) return false;
        return insert(player, single, false).isEmpty();
    }

    /** Atomic main-hand container exchange, including capacity freed by consuming the last container. */
    public static int exchangeBatch(Player player, ItemStack replacement, int requested, boolean consume, boolean simulate) {
        int limit = consume ? Math.min(requested, player.getMainHandItem().getCount()) : requested;
        if (limit <= 0) return 0;
        int moved = maximumExchange(limit, count -> exchangeSnapshot(player, replacement, count, consume) != null);
        if (moved == 0) return 0;
        var snapshot = exchangeSnapshot(player, replacement, moved, consume);
        if (!simulate) for (int slot = 0; slot < 36; slot++)
            player.getInventory().setItem(slot, snapshot.getStackInSlot(slot));
        return moved;
    }

    /** Full consumption can free the selected slot, so test that boundary before binary search. */
    public static int maximumExchange(int limit, java.util.function.IntPredicate fits) {
        if (limit <= 0) return 0;
        if (fits.test(limit)) return limit;
        int low = 0, high = limit - 1;
        while (low < high) {
            int middle = low + (high - low + 1) / 2;
            if (fits.test(middle)) low = middle;
            else high = middle - 1;
        }
        return low;
    }

    private static net.minecraftforge.items.ItemStackHandler exchangeSnapshot(Player player, ItemStack replacement, int count, boolean consume) {
        var inventory = player.getInventory();
        var snapshot = new net.minecraftforge.items.ItemStackHandler(36);
        for (int slot = 0; slot < 36; slot++) snapshot.setStackInSlot(slot, inventory.getItem(slot).copy());
        if (consume && !player.getAbilities().instabuild) snapshot.getStackInSlot(inventory.selected).shrink(count);
        return ItemHandlerHelper.insertItemStacked(snapshot, replacement.copyWithCount(count), false).isEmpty() ? snapshot : null;
    }
}
