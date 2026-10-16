package com.yinfires.icecore.workstation;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.capability.IFluidHandler;

/** Input and output support are independent: cuisine boards implement input only. */
public interface WorkstationAdapter {
    boolean allowsInput();
    boolean allowsOutput();
    int size();
    ItemStack item(int slot);
    void item(int slot, ItemStack stack);
    WorkstationSlotState state();
    default IFluidHandler fluid() { return null; }
    default ItemStack output() { return ItemStack.EMPTY; }
    default Ingredient carrier() { return Ingredient.EMPTY; }
    default void takeOutput() { throw new IllegalStateException("Input-only workstation"); }
    default boolean accepts(ItemStack stack) { return true; }
    void changed();
}
