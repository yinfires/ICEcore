package com.yinfires.icecore.shelf;

import com.yinfires.icecore.mixing.ContainerContentAccess;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.*;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.Nullable;

public final class ShelfBlockEntity extends BlockEntity implements Container, MenuProvider, ContainerContentAccess {
    public static final int CAPACITY = 72;
    private final NonNullList<ItemStack> items = NonNullList.withSize(CAPACITY,ItemStack.EMPTY);
    private LazyOptional<IItemHandler> handler = LazyOptional.of(() -> new InvWrapper(this));
    private boolean pendingUpdate;
    private long renderRevision;
    private AABB renderBounds;
    public long renderRevision() { return renderRevision; }
    public ShelfBlockEntity(BlockPos pos, BlockState state) { super(ModShelf.ENTITY.get(),pos,state); }
    @Override public int getContainerSize() { return CAPACITY; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) {
        ItemStack stack=items.get(slot);
        if(level!=null && !level.isClientSide) com.yinfires.icecore.spoilage.ShelfLife.release(stack);
        return stack;
    }
    @Override public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items,slot,amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { ItemStack result=ContainerHelper.takeItem(items,slot); if(!result.isEmpty()) setChanged(); return result; }
    @Override public void setItem(int slot, ItemStack stack) {
        if(level!=null && !level.isClientSide) com.yinfires.icecore.spoilage.ShelfLife.release(stack);
        items.set(slot,stack); stack.setCount(Math.min(stack.getCount(),getMaxStackSize())); setChanged();
    }
    @Override public boolean stillValid(Player player) { return !isRemoved() && level != null && level.getBlockEntity(worldPosition) == this && player.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+1,worldPosition.getZ()+.5) <= 64; }
    @Override public void clearContent() { items.clear(); setChanged(); }
    @Override public void setChanged() {
        renderRevision++;
        super.setChanged();
        if (level != null && !level.isClientSide && !isRemoved() && !level.restoringBlockSnapshots && !pendingUpdate) {
            pendingUpdate=true;
            level.scheduleTick(worldPosition,getBlockState().getBlock(),1);
        }
    }
    public void flushUpdate() {
        if(pendingUpdate && level!=null && !level.isClientSide) {
            pendingUpdate=false;
            level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);
        }
    }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); ContainerHelper.saveAllItems(tag,items); }
    @Override public void load(CompoundTag tag) { super.load(tag); items.clear(); ContainerHelper.loadAllItems(tag,items); renderRevision++; }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) { if (packet.getTag()!=null) load(packet.getTag()); }
    @Override public void handleUpdateTag(CompoundTag tag) { load(tag); }
    @Override public Component getDisplayName() { return Component.translatable("block.icecore.shelf"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new ShelfMenu(id,inventory,this); }
    @Override public boolean icecore$hasContainerContent() { return !isEmpty(); }
    @Override public int icecore$contentFingerprint() {
        int hash=1;
        for (ItemStack stack:items) hash=31*hash+(stack.isEmpty()?0:31*System.identityHashCode(stack.getItem())+31*stack.getCount()+java.util.Objects.hashCode(stack.getTag()));
        return hash;
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        return capability == ForgeCapabilities.ITEM_HANDLER ? handler.cast() : super.getCapability(capability,side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); handler.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); handler=LazyOptional.of(() -> new InvWrapper(this)); }
    @Override public AABB getRenderBoundingBox() { if(renderBounds==null) renderBounds=new AABB(worldPosition).expandTowards(0,1.5,0).inflate(.25,0,.25); return renderBounds; }
}
