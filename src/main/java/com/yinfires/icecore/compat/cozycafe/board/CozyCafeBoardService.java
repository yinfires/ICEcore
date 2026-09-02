package com.yinfires.icecore.compat.cozycafe.board;

import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.deco.TextBlockEntity;
import io.github.chakyl.cozycafe.blockentities.CafeManagerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Persistent board links with an event-driven in-memory reverse index. */
public final class CozyCafeBoardService {
    public static final String ITEM_LINK = "IcecoreCozyCafeBoard";
    public static final String BLOCK_LINK = "icecoreCozyCafeBoard";
    private static final Map<String, Set<BoardLocation>> BOARDS = new HashMap<>();

    private CozyCafeBoardService() {}

    public static void bindItem(ItemStack stack, CafeManagerBlockEntity manager) {
        CompoundTag link = new CompoundTag();
        link.putString("dimension", manager.getLevel().dimension().location().toString());
        link.putLong("computer", manager.getBlockPos().asLong());
        link.putString("name", manager.getCafeName());
        link.putBoolean("open", manager.isOpen());
        stack.getOrCreateTag().put(ITEM_LINK, link);
    }

    public static boolean place(Level level, BlockPos position, ItemStack stack, TextBlockEntity board) {
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(ITEM_LINK, CompoundTag.TAG_COMPOUND)) return false;
        CompoundTag itemLink = root.getCompound(ITEM_LINK);
        CompoundTag blockLink = itemLink.copy();
        board.getPersistentData().put(BLOCK_LINK, blockLink);
        String name = itemLink.getString("name");
        boolean open = itemLink.getBoolean("open");
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            CafeManagerBlockEntity manager = manager(serverLevel, itemLink);
            if (manager != null) { name = manager.getCafeName(); open = manager.isOpen(); }
            register(serverLevel.dimension(), position, itemLink);
        }
        setDisplay(board, name, open);
        return true;
    }

    public static void registerLoaded(TextBlockEntity board) {
        if (!(board.getLevel() instanceof ServerLevel level)) return;
        CompoundTag link = blockLink(board);
        if (link != null) register(level.dimension(), board.getBlockPos(), link);
    }

    public static void unbind(TextBlockEntity board) {
        CompoundTag link = blockLink(board);
        if (link == null) return;
        if (board.getLevel() != null) unregister(board.getLevel().dimension(), board.getBlockPos(), link);
        board.getPersistentData().remove(BLOCK_LINK);
        board.setChanged();
    }

    public static void sync(CafeManagerBlockEntity manager) {
        if (!(manager.getLevel() instanceof ServerLevel level)) return;
        String key = managerKey(level.dimension(), manager.getBlockPos());
        Set<BoardLocation> locations = BOARDS.get(key);
        if (locations == null) return;
        locations.removeIf(location -> {
            ServerLevel boardLevel = level.getServer().getLevel(location.dimension());
            if (boardLevel == null || !boardLevel.hasChunkAt(location.position())) return false;
            BlockEntity entity = boardLevel.getBlockEntity(location.position());
            if (!(entity instanceof TextBlockEntity board) || blockLink(board) == null) return true;
            CompoundTag link = blockLink(board);
            link.putString("name", manager.getCafeName()); link.putBoolean("open", manager.isOpen());
            setDisplay(board, manager.getCafeName(), manager.isOpen());
            return false;
        });
        if (locations.isEmpty()) BOARDS.remove(key);
    }

    public static CompoundTag blockLink(TextBlockEntity board) {
        CompoundTag data = board.getPersistentData();
        return data.contains(BLOCK_LINK, CompoundTag.TAG_COMPOUND) ? data.getCompound(BLOCK_LINK) : null;
    }

    public static boolean isBound(TextBlockEntity board) { return blockLink(board) != null; }
    /** Tavern's native text field is part of its update packet; ForgeData is only persistent link metadata. */
    public static String name(TextBlockEntity board) { return isBound(board) ? board.getText() : ""; }
    public static boolean isOpen(TextBlockEntity board) { CompoundTag tag = blockLink(board); return tag != null && tag.getBoolean("open"); }

    public static void clear() { BOARDS.clear(); }

    private static void setDisplay(TextBlockEntity board, String name, boolean open) {
        board.setText(name);
        board.refresh();
    }

    private static CafeManagerBlockEntity manager(ServerLevel level, CompoundTag link) {
        ResourceLocation id = ResourceLocation.tryParse(link.getString("dimension"));
        if (id == null || !level.dimension().location().equals(id)) return null;
        BlockPos computer = BlockPos.of(link.getLong("computer"));
        if (!level.hasChunkAt(computer)) return null;
        BlockEntity entity = level.getBlockEntity(computer);
        return entity instanceof CafeManagerBlockEntity manager ? manager : null;
    }

    private static void register(ResourceKey<Level> dimension, BlockPos position, CompoundTag link) {
        BOARDS.computeIfAbsent(managerKey(link), ignored -> new HashSet<>()).add(new BoardLocation(dimension, position.immutable()));
    }

    private static void unregister(ResourceKey<Level> dimension, BlockPos position, CompoundTag link) {
        Set<BoardLocation> locations = BOARDS.get(managerKey(link));
        if (locations != null) locations.remove(new BoardLocation(dimension, position));
    }

    private static String managerKey(CompoundTag link) { return link.getString("dimension") + "|" + link.getLong("computer"); }
    private static String managerKey(ResourceKey<Level> dimension, BlockPos computer) { return dimension.location() + "|" + computer.asLong(); }
    private record BoardLocation(ResourceKey<Level> dimension, BlockPos position) {}
}
