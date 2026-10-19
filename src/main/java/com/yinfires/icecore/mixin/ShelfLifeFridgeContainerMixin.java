package com.yinfires.icecore.mixin;

import com.yinfires.icecore.spoilage.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;
import net.blay09.mods.balm.api.container.DefaultContainer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value = DefaultContainer.class, remap = false)
public abstract class ShelfLifeFridgeContainerMixin implements PreservedContainer {
    @Shadow public abstract NonNullList<ItemStack> getItems();
    @Unique private boolean icecore$preserved;
    @Override public boolean icecore$preserved() { return icecore$preserved; }
    @Override public void icecore$preserve(boolean preserve) {
        if (icecore$preserved == preserve) return;
        icecore$preserved = preserve;
        if (preserve) {
            for (ItemStack stack : getItems()) ShelfLife.freeze(stack);
            ((DefaultContainer)(Object)this).setChanged();
        }
    }
    @Inject(method = "deserialize(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"))
    private void icecore$loaded(net.minecraft.nbt.CompoundTag tag, CallbackInfo ci) {
        if (icecore$preserved) for (ItemStack stack : getItems()) ShelfLife.freeze(stack);
    }
    // Override the inherited interface methods so menus and Balm's automation share the same boundary.
    public ItemStack m_8020_(int slot) {
        ItemStack stack = getItems().get(slot);
        if (icecore$preserved) ShelfLife.freeze(stack);
        else ShelfLife.release(stack);
        return stack;
    }
    public void m_6836_(int slot, ItemStack stack) {
        if (icecore$preserved) ShelfLife.freeze(stack); else ShelfLife.release(stack);
        getItems().set(slot, stack);
        if (stack.getCount() > ((DefaultContainer)(Object)this).getMaxStackSize())
            stack.setCount(((DefaultContainer)(Object)this).getMaxStackSize());
        ((DefaultContainer)(Object)this).setChanged();
        ((DefaultContainer)(Object)this).slotChanged(slot);
    }
    public ItemStack m_7407_(int slot, int count) {
        ItemStack stack = net.minecraft.world.ContainerHelper.removeItem(getItems(), slot, count);
        ShelfLife.release(stack);
        ((DefaultContainer)(Object)this).setChanged();
        ((DefaultContainer)(Object)this).slotChanged(slot);
        return stack;
    }
    public ItemStack m_8016_(int slot) {
        ItemStack stack = net.minecraft.world.ContainerHelper.takeItem(getItems(), slot);
        ShelfLife.release(stack);
        ((DefaultContainer)(Object)this).slotChanged(slot);
        return stack;
    }
}
