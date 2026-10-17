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
}
