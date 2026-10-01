package com.yinfires.icecore.food;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Client-thread marker scoped to one vanilla right-click dispatch. */
public final class BlockedFoodUse {
    private static boolean active;
    private static Player player;
    private static ItemStack stack;

    private BlockedFoodUse() {}

    public static void begin() {
        clear();
        active = true;
    }

    public static void record(Player source, ItemStack item) {
        if (active) {
            player = source;
            stack = item;
        }
    }

    public static boolean matches(Player source, InteractionHand hand) {
        return active && player == source && source != null
                && stack == source.getItemInHand(hand);
    }

    public static void clear() {
        active = false;
        player = null;
        stack = null;
    }
}
