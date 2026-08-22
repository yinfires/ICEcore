package com.yinfires.icecore.compat.cozycafe.range;

import com.yinfires.icecore.building.AdjustmentModeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Tracks temporary, server-side range wands for individual CozyCafe computers. */
public final class CozyCafeRangeAdjustmentManager {
    public static final String MARKER = "icecore_cozycafe_range";
    private static final Map<UUID, State> ACTIVE = new HashMap<>();

    private CozyCafeRangeAdjustmentManager() {
    }

    public static boolean enter(ServerPlayer player, BlockPos computer) {
        AdjustmentModeManager.exit(player);
        exit(player);
        InteractionHand hand = plainStickHand(player);
        if (hand == null) {
            return false;
        }
        if (!CozyCafeRangeDataManager.get().ensureComputer(player, computer)) {
            return false;
        }

        String key = CozyCafeRangeDefinition.key(player.serverLevel().dimension(), computer);
        ItemStack stack = player.getItemInHand(hand);
        State state = new State(hand, stack.copy(), key);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(MARKER, key);
        stack.setHoverName(Component.translatable("icecore.cozycafe.range.wand_name",
                computer.toShortString()));
        stack.enchant(Enchantments.UNBREAKING, 1);
        player.getInventory().setChanged();
        ACTIVE.put(player.getUUID(), state);
        return true;
    }

    public static boolean hasPlainStick(ServerPlayer player) {
        return plainStickHand(player) != null;
    }

    public static boolean canEnter(ServerPlayer player) {
        return hasPlainStick(player) || isActiveInAnyHand(player)
                || AdjustmentModeManager.isActiveInAnyHand(player);
    }

    public static boolean isActive(ServerPlayer player, InteractionHand hand) {
        State state = ACTIVE.get(player.getUUID());
        return state != null && state.hand == hand && isMarked(player.getItemInHand(hand), state.key);
    }

    public static boolean isActiveInAnyHand(ServerPlayer player) {
        State state = ACTIVE.get(player.getUUID());
        return state != null && isMarked(player.getItemInHand(state.hand), state.key);
    }

    public static void setFirst(ServerPlayer player, BlockPos position) {
        State state = activeState(player);
        if (state == null) {
            return;
        }
        if (CozyCafeRangeDataManager.get().setFirst(player, state.key, position)) {
            player.sendSystemMessage(Component.translatable(
                    "icecore.cozycafe.range.first", position.toShortString()));
        }
    }

    public static void setSecond(ServerPlayer player, BlockPos position) {
        State state = activeState(player);
        if (state == null) {
            return;
        }
        if (CozyCafeRangeDataManager.get().setSecond(player, state.key, position)) {
            player.sendSystemMessage(Component.translatable(
                    "icecore.cozycafe.range.second", position.toShortString()));
        }
    }

    public static void tick(ServerPlayer player) {
        State state = ACTIVE.get(player.getUUID());
        if (state != null && !isMarked(player.getItemInHand(state.hand), state.key)) {
            exit(player);
        }
    }

    public static void exit(ServerPlayer player) {
        State state = ACTIVE.remove(player.getUUID());
        if (state == null) {
            return;
        }
        for (int index = 0; index < player.getInventory().items.size(); index++) {
            ItemStack stack = player.getInventory().items.get(index);
            if (isMarked(stack, state.key)) {
                player.getInventory().items.set(index, state.original.copy());
                player.getInventory().setChanged();
                return;
            }
        }
        for (int index = 0; index < player.getInventory().offhand.size(); index++) {
            ItemStack stack = player.getInventory().offhand.get(index);
            if (isMarked(stack, state.key)) {
                player.getInventory().offhand.set(index, state.original.copy());
                player.getInventory().setChanged();
                return;
            }
        }
    }

    public static void clear() {
        ACTIVE.clear();
    }

    public static boolean isMarked(ItemStack stack, String key) {
        if (stack.isEmpty() || !stack.is(Items.STICK) || !stack.hasTag()) {
            return false;
        }
        String markedKey = stack.getTag().getString(MARKER);
        return !markedKey.isEmpty() && (key == null || key.equals(markedKey));
    }

    private static State activeState(ServerPlayer player) {
        State state = ACTIVE.get(player.getUUID());
        return state != null && isMarked(player.getItemInHand(state.hand), state.key) ? state : null;
    }

    private static InteractionHand plainStickHand(ServerPlayer player) {
        if (isPlainStick(player.getMainHandItem())) {
            return InteractionHand.MAIN_HAND;
        }
        return isPlainStick(player.getOffhandItem()) ? InteractionHand.OFF_HAND : null;
    }

    private static boolean isPlainStick(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.STICK) && !stack.hasCustomHoverName()
                && !stack.isEnchanted() && !stack.hasTag();
    }

    private record State(InteractionHand hand, ItemStack original, String key) {
    }
}
