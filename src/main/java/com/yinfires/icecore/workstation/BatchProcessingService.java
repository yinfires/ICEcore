package com.yinfires.icecore.workstation;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;

/** Shared unordered, bounded batch matching used by workstations. */
public final class BatchProcessingService {
    private BatchProcessingService() {}

    public record Match(int batches, List<Integer> slots) {}

    public static Match match(Container container, NonNullList<Ingredient> ingredients, int limit) {
        List<Integer> available = new ArrayList<>();
        for (int i = 0; i < container.getContainerSize(); i++) if (!container.getItem(i).isEmpty()) available.add(i);
        int batches = 0; List<Integer> used = new ArrayList<>();
        while (batches < limit) {
            boolean[] taken = new boolean[available.size()];
            List<Integer> found = new ArrayList<>();
            if (!find(0, ingredients, available, taken, container, found)) break;
            used.addAll(found); batches++;
            for (int slot : found) available.remove((Integer) slot);
        }
        return new Match(batches, used);
    }

    private static boolean find(int index, List<Ingredient> ingredients, List<Integer> available,
                                boolean[] taken, Container container, List<Integer> found) {
        if (index == ingredients.size()) return true;
        for (int i = 0; i < available.size(); i++) if (!taken[i]
                && ingredients.get(index).test(container.getItem(available.get(i)))) {
            taken[i] = true; found.add(available.get(i));
            if (find(index + 1, ingredients, available, taken, container, found)) return true;
            found.remove(found.size() - 1); taken[i] = false;
        }
        return false;
    }

    public static boolean canInsert(NonNullList<ItemStack> stacks, ItemStack result, int capacity) {
        int empty = 0;
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) empty++;
            else if (ItemStack.isSameItemSameTags(stack, result)) {
                int room = Math.min(stack.getMaxStackSize(), capacity) - stack.getCount();
                if (room >= result.getCount()) return true;
            }
        }
        return empty > 0;
    }
}
