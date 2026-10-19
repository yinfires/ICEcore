package com.yinfires.icecore.spoilage;

import com.mojang.logging.LogUtils;
import com.yinfires.icecore.item.ModItems;
import com.yinfires.icecore.time.TimeCalendar;
import com.yinfires.icecore.workstation.WorkstationContainerCompat;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.function.BooleanSupplier;

/** Server-only lazy settlement: every gameplay item identity read crosses this boundary. */
public final class ShelfLife {
    public static final String KEY = "icecore:ShelfLife";
    private static MinecraftServer server;
    private static ShelfLifeRules rules = ShelfLifeRules.EMPTY;
    private static java.util.Map<Item, Integer> byItem = java.util.Map.of();
    private static boolean resolving;
    private static int revision;
    private static final BooleanSupplier NORMAL = () -> false;
    private static final BooleanSupplier FROZEN = () -> true;
    private static final java.util.Map<Player, BooleanSupplier> PLAYERS = new java.util.WeakHashMap<>();
    private static int nestedBudget;
    private ShelfLife() {}
    public static void start(MinecraftServer value) { server = value; reload(); }
    public static void stop() { server = null; rules = ShelfLifeRules.EMPTY; byItem = java.util.Map.of(); PLAYERS.clear(); }
    public static long day() { return server == null ? 0 : TimeCalendar.day(server.overworld().getDayTime()); }
    public static boolean active() { return server != null && server.isSameThread() && server.overworld() != null; }
    public static void reload() {
        var path = FMLPaths.CONFIGDIR.get().resolve("icecore/shelf_life.json");
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            ShelfLifeRules next = ShelfLifeRules.parse(JsonParser.parseReader(reader));
            for (String id : next.items().keySet()) {
                if (!ForgeRegistries.ITEMS.containsKey(net.minecraft.resources.ResourceLocation.parse(id)))
                    throw new IllegalArgumentException("Unregistered shelf-life item: " + id);
            }
            var resolved = new java.util.IdentityHashMap<Item, Integer>();
            next.items().forEach((id, days) -> resolved.put(ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse(id)), days));
            rules = next; byItem = resolved; revision++;
        } catch (java.nio.file.NoSuchFileException absent) {
            LogUtils.getLogger().warn("No shelf-life configuration at {}; no implicit foods enabled", path);
        } catch (Exception error) {
            LogUtils.getLogger().error("Cannot load {}; keeping previous shelf-life rules: {}", path, error.getMessage());
        }
    }
    public static void own(ItemStack stack, BooleanSupplier owner) {
        if (stack.isEmpty() || !active()) return;
        ((ShelfLifeStackAccess)(Object)stack).icecore$owner(owner);
        settle(stack);
    }
    public static void player(ItemStack stack, Player player) {
        if (active()) own(stack, PLAYERS.computeIfAbsent(player, p -> {
            java.lang.ref.WeakReference<Player> weak = new java.lang.ref.WeakReference<>(p);
            return () -> weak.get() != null && weak.get().isCreative();
        }));
    }
    public static void release(ItemStack stack) { own(stack, NORMAL); }
    public static void freeze(ItemStack stack) { own(stack, FROZEN); }
    public static void settle(ItemStack stack) {
        if (!active() || resolving || stack.isEmpty()) return;
        var access = (ShelfLifeStackAccess)(Object)stack;
        boolean paused = access.icecore$owner() != null && access.icecore$owner().getAsBoolean();
        if (!access.icecore$needsSettlement(day(), paused, revision)) return;
        resolving = true;
        nestedBudget = 256;
        try { settleInner(stack); } finally { resolving = false; }
    }
    public static void observe(ItemStack stack) {
        // Recipe/JEI model stacks are templates, not acquired goods.
        if (((ShelfLifeStackAccess)(Object)stack).icecore$detached()) return;
        if (((ShelfLifeStackAccess)(Object)stack).icecore$owner() != null
                || stack.hasTag() && (stack.getTag().contains(KEY, 10) || stack.getTag().contains("platedFood", 10))) settle(stack);
    }
    private static void settleInner(ItemStack stack) {
        if (--nestedBudget < 0) return;
        if (stack.getItem() instanceof ExpiredFoodItem) return;
        var access = (ShelfLifeStackAccess)(Object)stack;
        boolean paused = access.icecore$owner() != null && access.icecore$owner().getAsBoolean();
        long now = day();
        // Vanilla item containers serialize their inventory here. Bound total work per outer stack.
        if (stack.hasTag() && stack.getTag().contains("BlockEntityTag", 10)) {
            CompoundTag block = stack.getTag().getCompound("BlockEntityTag");
            if (block.contains("Items", 9)) {
                ListTag contents = block.getList("Items", 10);
                for (int i = 0; i < contents.size() && nestedBudget > 0; i++) {
                    CompoundTag saved = contents.getCompound(i);
                    ItemStack inner = ItemStack.of(saved);
                    ((ShelfLifeStackAccess)(Object)inner).icecore$owner(access.icecore$owner());
                    settleInner(inner);
                    CompoundTag replacement = inner.save(new CompoundTag());
                    replacement.putByte("Slot", saved.getByte("Slot"));
                    if (!replacement.equals(saved)) contents.set(i, replacement);
                }
            }
        }
        // CozyCafe's native nested stack; never replace the plate's identity.
        if (stack.hasTag() && stack.getTag().contains("platedFood", 10)) {
            ItemStack food = ItemStack.of(stack.getTag().getCompound("platedFood"));
            ((ShelfLifeStackAccess)(Object)food).icecore$owner(access.icecore$owner());
            settleInner(food);
            CompoundTag saved = food.save(new CompoundTag());
            if (!saved.equals(stack.getTag().getCompound("platedFood"))) stack.getTag().put("platedFood", saved);
            return;
        }
        Integer configured = byItem.get(stack.getItem());
        if (configured == null) {
            if (stack.hasTag()) { stack.getTag().remove(KEY); clearLegacy(stack.getTag()); }
            return;
        }
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(KEY, 10)) {
            CompoundTag state = new CompoundTag();
            state.putInt("Days", configured); state.putLong("Day", now);
            if (paused) state.putBoolean("Paused", true);
            stack.getOrCreateTag().put(KEY, state);
            clearLegacy(stack.getTag());
            return;
        }
        CompoundTag state = root.getCompound(KEY);
        boolean wasPaused = state.getBoolean("Paused");
        ShelfLifeState before = new ShelfLifeState(state.getInt("Days"), state.getLong("Day"), wasPaused);
        ShelfLifeState after = before.advance(now, paused);
        int days = after.days();
        if (days <= 0) { expire(stack); return; }
        if (wasPaused != paused || (!paused && now > state.getLong("Day"))) {
            state.putInt("Days", days); state.putLong("Day", after.day());
            if (paused) state.putBoolean("Paused", true); else state.remove("Paused");
        }
    }
    private static void clearLegacy(CompoundTag root) {
        root.remove("spoiled:SpoilTimer"); root.remove("spoiled:SpoilMaxTime");
    }
    private static void expire(ItemStack stack) {
        ItemStack original = stack.copyWithCount(1);
        original.getTag().remove(KEY); clearLegacy(original.getTag());
        ItemStack empty = WorkstationContainerCompat.resolveEmptyContainer(original).orElse(ItemStack.EMPTY);
        ItemStack expired = new ItemStack(ModItems.EXPIRED_FOOD.get(), stack.getCount());
        CompoundTag tag = expired.getOrCreateTag();
        tag.put("Original", original.save(new CompoundTag()));
        tag.putInt("StackLimit", original.getMaxStackSize());
        if (!empty.isEmpty()) tag.put("EmptyContainer", empty.save(new CompoundTag()));
        ((ShelfLifeStackAccess)(Object)stack).icecore$replace(expired);
    }
    public static int remaining(ItemStack stack) {
        if (!stack.hasTag()) return -1;
        CompoundTag root = stack.getTag();
        if (root.contains("platedFood", 10)) return remaining(ItemStack.of(root.getCompound("platedFood")));
        return root.contains(KEY, 10) ? root.getCompound(KEY).getInt("Days") : -1;
    }
    public static int displayRemaining(ItemStack stack, long now) {
        if (!stack.hasTag()) return -1;
        CompoundTag root = stack.getTag();
        if (root.contains("platedFood", 10)) return displayRemaining(ItemStack.of(root.getCompound("platedFood")), now);
        if (!root.contains(KEY, 10)) return -1;
        CompoundTag state = root.getCompound(KEY);
        return state.getBoolean("Paused") ? state.getInt("Days")
                : ShelfLifeMath.remaining(state.getInt("Days"), state.getLong("Day"), now);
    }
    public static boolean expired(ItemStack stack) { return stack.getItem() instanceof ExpiredFoodItem; }
    /** Canonical comparison ignores bookkeeping only, retaining all other NBT and capabilities. */
    public static Boolean same(ItemStack a, ItemStack b) {
        if (!a.hasTag() || !b.hasTag() || !a.getTag().contains(KEY, 10) || !b.getTag().contains(KEY, 10)) return null;
        if (a.getItem() != b.getItem() || remaining(a) != remaining(b) || !a.areCapsCompatible(b)) return false;
        return withoutClock(a).equals(withoutClock(b));
    }
    static CompoundTag withoutClock(ItemStack stack) {
        CompoundTag tag = stack.hasTag() ? stack.getTag().copy() : new CompoundTag();
        tag.remove(KEY); return tag;
    }
    public static boolean manual(ItemStack target, ItemStack source, net.minecraft.world.inventory.Slot slot,
                                 Player player, boolean secondary) {
        if (target.isEmpty() || source.isEmpty() || expired(target) || expired(source)
                || remaining(target) < 1 || remaining(source) < 1 || target.getItem() != source.getItem()
                || !withoutClock(target).equals(withoutClock(source)) || !target.areCapsCompatible(source)
                || !slot.mayPlace(source) || !slot.mayPickup(player) || remaining(target) == remaining(source)) return false;
        int moved = ShelfLifeMath.moved(source.getCount(), target.getCount(),
                Math.min(target.getMaxStackSize(), slot.getMaxStackSize(source)), secondary);
        if (moved == 0) return false;
        CompoundTag state = target.getTag().getCompound(KEY);
        state.putInt("Days", Math.min(remaining(target), remaining(source)));
        state.putLong("Day", Math.max(state.getLong("Day"), source.getTag().getCompound(KEY).getLong("Day")));
        target.grow(moved); source.shrink(moved); slot.setChanged();
        return true;
    }
}
