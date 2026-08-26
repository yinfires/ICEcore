package com.yinfires.icecore.adventure;

import com.yinfires.icecore.ICECore;
import com.yinfires.icecore.feedback.PlayerFeedback;
import com.yinfires.icecore.network.ICECoreNetwork;
import com.yinfires.icecore.network.ClientBoundGiveResultPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AdventureItemService {
    private static final String ID = "icecore_adventure_items";
    private static MinecraftServer server;
    private static PendingData data;
    private AdventureItemService() {}

    public static void start(MinecraftServer value) {
        server = value;
        data = value.overworld().getDataStorage().computeIfAbsent(PendingData::load, PendingData::new, ID);
    }
    public static void stop() { server = null; data = null; }
    public static boolean isAdventure(net.minecraft.world.entity.player.Player p) {
        return p != null && p.level().isClientSide ? false /* client callers use Minecraft.gameMode */
                : p instanceof ServerPlayer sp && sp.gameMode.getGameModeForPlayer() == GameType.ADVENTURE;
    }
    public static boolean isAdventure(ServerPlayer p) { return p.gameMode.getGameModeForPlayer() == GameType.ADVENTURE; }

    public static void captureCarried(ServerPlayer player, ItemStack stack) {
        if (!isAdventure(player) || stack.isEmpty() || data == null) return;
        data.items.put(player.getUUID(), stack.copy()); data.setDirty();
    }
    public static ItemStack takePending(ServerPlayer player) {
        if (data == null) return ItemStack.EMPTY;
        ItemStack stack = data.items.remove(player.getUUID());
        if (stack != null) data.setDirty();
        return stack == null ? ItemStack.EMPTY : stack;
    }
    public static ItemStack peekPending(ServerPlayer player) {
        if (data == null) return ItemStack.EMPTY;
        ItemStack stack = data.items.get(player.getUUID());
        return stack == null ? ItemStack.EMPTY : stack.copy();
    }
    public static void restore(ServerPlayer player) {
        ItemStack pending = peekPending(player);
        if (pending.isEmpty()) return;
        if (!player.containerMenu.getCarried().isEmpty()) return;
        takePending(player);
        player.containerMenu.setCarried(pending);
        player.containerMenu.broadcastChanges();
    }
    public static void restoreIntoOpenedMenu(ServerPlayer player) {
        if (!isAdventure(player)) return;
        if (player.containerMenu != player.inventoryMenu
                && player.containerMenu.getCarried().isEmpty()
                && !player.inventoryMenu.getCarried().isEmpty()) {
            ItemStack carried = player.inventoryMenu.getCarried().copy();
            player.inventoryMenu.setCarried(ItemStack.EMPTY);
            player.inventoryMenu.broadcastChanges();
            player.containerMenu.setCarried(carried);
            player.containerMenu.broadcastChanges();
            return;
        }
        restore(player);
    }
    public static void restoreAfterMenuClose(ServerPlayer player) {
        restoreAfterMenuClose(player, 3);
    }

    private static void restoreAfterMenuClose(ServerPlayer player, int attempts) {
        player.server.execute(() -> {
            if (attempts > 1) {
                restoreAfterMenuClose(player, attempts - 1);
            } else {
                restore(player);
            }
        });
    }
    public static void returnToInventory(ServerPlayer player) {
        if (data == null) return;
        ItemStack pending = peekPending(player);
        if (pending.isEmpty()) return;
        ItemStack remainder = pending.copy();
        player.getInventory().add(remainder);
        if (remainder.isEmpty()) takePending(player);
        else captureCarried(player, remainder);
    }

    public static void give(ServerPlayer source, UUID targetId, boolean all) {
        if (!isAdventure(source) || source.getMainHandItem().isEmpty()) return;
        ServerPlayer target = source.serverLevel().getServer().getPlayerList().getPlayer(targetId);
        if (target == null || target == source || target.level() != source.level() || !target.isAlive()) return;
        if (source.distanceToSqr(target) > 36.0D || !source.hasLineOfSight(target)) return;
        ItemStack held = source.getMainHandItem();
        int want = all ? held.getCount() : 1;
        ItemStack probe = held.copyWithCount(want);
        int before = probe.getCount();
        if (!target.getInventory().add(probe)) {
            int accepted = before - probe.getCount();
            if (accepted == 0) {
                PlayerFeedback.show(source, Component.empty().append(Component.translatable("icecore.adventure_give.full")).withStyle(net.minecraft.ChatFormatting.RED));
                ICECoreNetwork.sendToPlayer(new ClientBoundGiveResultPacket(false), source);
                return;
            }
            source.getMainHandItem().shrink(accepted);
        } else {
            source.getMainHandItem().shrink(before);
        }
        ICECoreNetwork.sendToPlayer(new ClientBoundGiveResultPacket(true), source);
        target.inventoryMenu.broadcastChanges(); source.containerMenu.broadcastChanges();
    }

    public static final class PendingData extends SavedData {
        private final Map<UUID, ItemStack> items = new HashMap<>();
        static PendingData load(CompoundTag tag) {
            PendingData d = new PendingData(); ListTag list = tag.getList("Items", 10);
            for (int i=0;i<list.size();i++) { CompoundTag e=list.getCompound(i); d.items.put(e.getUUID("UUID"), ItemStack.of(e.getCompound("Stack"))); }
            return d;
        }
        @Override public CompoundTag save(CompoundTag tag) {
            ListTag list = new ListTag();
            items.forEach((id, stack) -> { CompoundTag e=new CompoundTag(); e.putUUID("UUID", id); e.put("Stack", stack.save(new CompoundTag())); list.add(e); });
            tag.put("Items", list); return tag;
        }
    }
}
