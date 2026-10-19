package com.yinfires.icecore.shelf;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class ShelfMenu extends AbstractContainerMenu {
    private final Container container;
    public ShelfMenu(int id, Inventory inventory) { this(id,inventory,new SimpleContainer(72)); }
    public ShelfMenu(int id, Inventory inventory, Container container) {
        super(ModShelf.MENU.get(),id);
        checkContainerSize(container,72); this.container=container; container.startOpen(inventory.player);
        for(int row=0;row<8;row++) for(int col=0;col<9;col++) addSlot(new Slot(container,row*9+col,8+col*18,18+row*18));
        for(int row=0;row<3;row++) for(int col=0;col<9;col++) addSlot(new Slot(inventory,row*9+col+9,8+col*18,175+row*18));
        for(int col=0;col<9;col++) addSlot(new Slot(inventory,col,8+col*18,233));
    }
    @Override public boolean stillValid(Player player) { return container.stillValid(player); }
    @Override public void removed(Player player) { super.removed(player); container.stopOpen(player); }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        Slot slot=slots.get(index);
        if(!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(), copy=stack.copy();
        if(index<72 ? !moveItemStackTo(stack,72,108,true) : !moveItemStackTo(stack,0,72,false)) return ItemStack.EMPTY;
        if(stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player,stack); return copy;
    }
}
