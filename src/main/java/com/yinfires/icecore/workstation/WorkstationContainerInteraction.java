package com.yinfires.icecore.workstation;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Shared player inventory exchange rules for workstation container interactions. */
public final class WorkstationContainerInteraction {
    public enum FluidContainerMode { REAL_FLUID_STORAGE, STORED_AS_ITEM }

    private WorkstationContainerInteraction() {}

    public static ItemStack requiredEmptyContainer(ItemStack filled, FluidContainerMode mode) {
        return WorkstationContainerCompat.describe(filled)
                .filter(entry -> mode == FluidContainerMode.STORED_AS_ITEM
                        || entry.behavior() == WorkstationContainerCompat.Behavior.INGREDIENT)
                .map(WorkstationContainerCompat.Entry::empty)
                .orElse(ItemStack.EMPTY).copyWithCount(1);
    }

    public static void exchange(Player player, InteractionHand hand, ItemStack held, ItemStack replacement) {
        if (replacement.isEmpty()) return;
        if (player.getAbilities().instabuild) {
            player.getInventory().placeItemBackInInventory(replacement.copyWithCount(1));
            return;
        }
        if (held.getCount() == 1) player.setItemInHand(hand, replacement.copyWithCount(1));
        else {
            held.shrink(1);
            player.getInventory().placeItemBackInInventory(replacement.copyWithCount(1));
        }
    }
}
