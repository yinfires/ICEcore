package com.yinfires.icecore.workstation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import java.util.Arrays;
import java.util.function.IntFunction;

/** Bounded metadata for externally owned slots; reconciled only on interaction/load. */
public final class WorkstationSlotState {
    private long[] order = new long[0];
    private ItemStack[] containers = new ItemStack[0];
    private ItemStack[] observed = new ItemStack[0];
    private long next = 1;
    private CompoundTag pending;

    public void reconcile(int size, IntFunction<ItemStack> items) {
        if (order.length != size) {
            order = new long[size]; containers = new ItemStack[size]; observed = new ItemStack[size];
            Arrays.fill(containers, ItemStack.EMPTY); Arrays.fill(observed, ItemStack.EMPTY);
        }
        if (pending != null) {
            ListTag entries = pending.getList("Containers", 10);
            ListTag previous = pending.getList("Observed", 10);
            for (int i = 0; i < size; i++) {
                containers[i] = i < entries.size() ? ItemStack.of(entries.getCompound(i)) : required(items.apply(i));
                observed[i] = i < previous.size() ? ItemStack.of(previous.getCompound(i)) : items.apply(i).copy();
            }
            next = WorkstationItemOrder.restore(order, pending.getLongArray("Order"), pending.getLong("Next"), i -> !items.apply(i).isEmpty());
            pending = null;
        }
        for (int i = 0; i < size; i++) {
            ItemStack item = items.apply(i);
            if (ItemStack.isSameItemSameTags(item, observed[i]) && item.getCount() == observed[i].getCount()) continue;
            if (!item.isEmpty() && ItemStack.isSameItemSameTags(item, observed[i])) { observed[i] = item.copy(); continue; }
            if (item.isEmpty()) { order[i] = 0; containers[i] = ItemStack.EMPTY; }
            else { order[i] = allocate(); containers[i] = required(item); }
            observed[i] = item.copy();
        }
    }
    private static ItemStack required(ItemStack stack) {
        return WorkstationContainerInteraction.requiredEmptyContainer(stack, WorkstationContainerInteraction.FluidContainerMode.STORED_AS_ITEM);
    }
    public int last(IntFunction<ItemStack> items) { return WorkstationItemOrder.lastSlot(order, i -> !items.apply(i).isEmpty()); }
    public ItemStack container(int slot) { return containers[slot]; }
    public void inserted(int slot, ItemStack item, ItemStack empty) {
        order[slot] = allocate(); containers[slot] = empty.copy(); observed[slot] = item.copy();
    }
    private long allocate() {
        if (next == Long.MAX_VALUE) next = WorkstationItemOrder.restore(order, new long[0], 1, i -> !observed[i].isEmpty());
        return next++;
    }
    public void load(CompoundTag tag) { pending = tag.contains("ICEcoreSlots") ? tag.getCompound("ICEcoreSlots").copy() : null; order = new long[0]; }
    public void save(CompoundTag tag) {
        if (pending != null) { tag.put("ICEcoreSlots", pending.copy()); return; }
        CompoundTag data = new CompoundTag(); data.putLongArray("Order", order); data.putLong("Next", next);
        ListTag entries = new ListTag(); for (ItemStack item : containers) entries.add(item.save(new CompoundTag()));
        ListTag previous = new ListTag(); for (ItemStack item : observed) previous.add(item.save(new CompoundTag()));
        data.put("Observed", previous);
        data.put("Containers", entries); tag.put("ICEcoreSlots", data);
    }
}
