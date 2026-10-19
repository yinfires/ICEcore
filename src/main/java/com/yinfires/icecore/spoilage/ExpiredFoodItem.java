package com.yinfires.icecore.spoilage;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.nbt.CompoundTag;

/** No food properties, tags, remainder, fluid capabilities or original gameplay delegation. */
public final class ExpiredFoodItem extends Item {
    public ExpiredFoodItem() { super(new Properties()); }
    public static ItemStack original(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains("Original", 10)
                ? ItemStack.of(stack.getTag().getCompound("Original")) : ItemStack.EMPTY;
    }
    public static ItemStack container(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains("EmptyContainer", 10)
                ? ItemStack.of(stack.getTag().getCompound("EmptyContainer")) : ItemStack.EMPTY;
    }
    @Override public Component getName(ItemStack stack) {
        ItemStack original = original(stack);
        return Component.translatable("item.icecore.expired_food.name", original.getHoverName());
    }
    @Override public int getMaxStackSize(ItemStack stack) {
        return stack.hasTag() ? Math.max(1, Math.min(64, stack.getTag().getInt("StackLimit"))) : 64;
    }
}
