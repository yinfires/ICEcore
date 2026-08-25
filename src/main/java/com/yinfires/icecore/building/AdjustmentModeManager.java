package com.yinfires.icecore.building;

import com.yinfires.icecore.compat.cozycafe.range.CozyCafeRangeAdjustmentManager;
import com.yinfires.icecore.feedback.PlayerFeedback;
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

/** Tracks the temporary, server-side region wand without changing the player's inventory permanently. */
public final class AdjustmentModeManager {
    private static final String MARKER = "icecore_adjustment_region";
    private static final Map<UUID, State> ACTIVE = new HashMap<>();

    private AdjustmentModeManager() {
    }

    public static boolean enter(ServerPlayer player, String regionName) {
        CozyCafeRangeAdjustmentManager.exit(player);
        InteractionHand hand = isPlainStick(player.getMainHandItem()) ? InteractionHand.MAIN_HAND
                : isPlainStick(player.getOffhandItem()) ? InteractionHand.OFF_HAND : null;
        if (hand == null) {
            return false;
        }
        ItemStack stack = player.getItemInHand(hand);
        exit(player);
        State state = new State(hand, stack.copy(), regionName);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(MARKER, regionName);
        stack.setHoverName(Component.literal(regionName));
        stack.enchant(Enchantments.UNBREAKING, 1);
        player.getInventory().setChanged();
        ACTIVE.put(player.getUUID(), state);
        return true;
    }

    public static boolean isActive(ServerPlayer player, InteractionHand hand) {
        State state = ACTIVE.get(player.getUUID());
        return state != null && state.hand == hand && isMarked(player.getItemInHand(hand), state.regionName);
    }

    public static boolean isActiveInAnyHand(ServerPlayer player) {
        State state = ACTIVE.get(player.getUUID());
        return state != null && isMarked(player.getItemInHand(state.hand), state.regionName);
    }

    private static State activeState(ServerPlayer player) {
        State state = ACTIVE.get(player.getUUID());
        return state != null && isMarked(player.getItemInHand(state.hand), state.regionName) ? state : null;
    }

    public static void setFirst(ServerPlayer player, BlockPos pos) {
        State active = activeState(player);
        String region = active == null ? null : active.regionName;
        if (region == null) return;
        boolean saved = BuildingDataManager.get().mutate(data -> {
            RegionDefinition definition = data.regions().get(region);
            if (definition != null) definition.setFirst(player.serverLevel().dimension(), pos);
        }, player);
        if (saved) PlayerFeedback.show(player, Component.translatable("icecore.build.adjust.first", pos.toShortString()));
    }

    public static void setSecond(ServerPlayer player, BlockPos pos) {
        State active = activeState(player);
        String region = active == null ? null : active.regionName;
        if (region == null) return;
        boolean saved = BuildingDataManager.get().mutate(data -> {
            RegionDefinition definition = data.regions().get(region);
            if (definition != null) definition.setSecond(player.serverLevel().dimension(), pos);
        }, player);
        if (saved) PlayerFeedback.show(player, Component.translatable("icecore.build.adjust.second", pos.toShortString()));
    }

    public static void tick(ServerPlayer player) {
        State state = ACTIVE.get(player.getUUID());
        if (state == null) return;
        if (isMarked(player.getItemInHand(state.hand), state.regionName)) return;
        exit(player);
    }

    public static void exit(ServerPlayer player) {
        State state = ACTIVE.remove(player.getUUID());
        if (state == null) return;
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (isMarked(stack, state.regionName)) {
                player.getInventory().items.set(i, state.original.copy());
                player.getInventory().setChanged();
                return;
            }
        }
        for (int i = 0; i < player.getInventory().offhand.size(); i++) {
            ItemStack stack = player.getInventory().offhand.get(i);
            if (isMarked(stack, state.regionName)) {
                player.getInventory().offhand.set(i, state.original.copy());
                player.getInventory().setChanged();
                return;
            }
        }
    }

    public static void clear() {
        ACTIVE.clear();
    }

    public static boolean isMarked(ItemStack stack, String regionName) {
        if (stack.isEmpty() || !stack.is(Items.STICK) || !stack.hasTag()) {
            return false;
        }
        String markedRegion = stack.getTag().getString(MARKER);
        return !markedRegion.isEmpty() && (regionName == null || regionName.equals(markedRegion));
    }

    private static boolean isPlainStick(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.STICK) && !stack.hasCustomHoverName()
                && !stack.isEnchanted() && !stack.hasTag();
    }

    private record State(InteractionHand hand, ItemStack original, String regionName) {
    }
}
