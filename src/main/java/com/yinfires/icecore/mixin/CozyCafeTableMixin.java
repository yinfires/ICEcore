package com.yinfires.icecore.mixin;

import com.yinfires.icecore.compat.cozycafe.*;
import com.yinfires.icecore.workstation.*;
import com.yinfires.icecore.feedback.PlayerFeedback;
import io.github.chakyl.cozycafe.CozyCafe;
import io.github.chakyl.cozycafe.blocks.CafeMenuBlock;
import io.github.chakyl.cozycafe.blockentities.CafeMenuBlockEntity;
import io.github.chakyl.cozycafe.data.*;
import io.github.chakyl.cozycafe.item.ServingPlateItem;
import io.github.chakyl.cozycafe.registry.CozyRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** One retained vessel per table; native order/payment/customer execution remains authoritative. */
@Pseudo
@Mixin(targets="io.github.chakyl.cozycafe.blockentities.CafeMenuBlockEntity",remap=false,priority=1100)
public abstract class CozyCafeTableMixin implements CafeTableAccess {
    @Shadow private ItemStack requestedItem;
    @Shadow private ItemStack eatingItem;
    @Shadow private ItemStack droppedItem;
    @Shadow private int orderTime;
    @Shadow private int waitTime;
    @Shadow @Final private static int ORDER_TIME;
    @Shadow private void setChangedForRender() { throw new AssertionError(); }
    @Unique private ItemStack icecore$pending = ItemStack.EMPTY;
    @Unique private ItemStack icecore$return = ItemStack.EMPTY;
    @Unique private ItemStack icecore$served = ItemStack.EMPTY;
    @Unique private boolean icecore$drink;
    @Unique private boolean icecore$plated;
    @Unique private int icecore$savedWait;
    @Unique private boolean icecore$paused;
    @Unique private boolean icecore$wasServing;
    @Unique private boolean icecore$incomingPlate;

    public ItemStack icecore$tableRemainder() { return icecore$pending; }
    public boolean icecore$tableDrink() { return icecore$drink; }
    public boolean icecore$tablePlated() { return icecore$plated; }
    public boolean icecore$hideMenu() { return com.yinfires.icecore.compat.cozycafe.CafeDisplayRules.hideMenu(icecore$drink,!icecore$pending.isEmpty(),!eatingItem.isEmpty(),icecore$plated); }
    public boolean icecore$hasContainerContent() {
        return !icecore$pending.isEmpty() || !eatingItem.isEmpty() || !droppedItem.isEmpty()
                || ((BlockEntity)(Object)this).getBlockState().getValue(CafeMenuBlock.DIRTY);
    }
    public int icecore$contentFingerprint() {
        return java.util.Objects.hash(icecore$pending.getItem(), icecore$pending.getTag(), icecore$pending.getCount(),
                eatingItem.getItem(), eatingItem.getTag(), droppedItem.getItem(), icecore$plated);
    }
    @Unique private boolean icecore$isDrink() {
        var item = CafeMenuItemRegistry.INSTANCE.getForItem(requestedItem.getItem());
        return item != null && item.category() == CafeMenuItem.MenuItemCategory.DRINK;
    }
    @Inject(method="handleServe(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V",at=@At("HEAD"),cancellable=true)
    private void icecore$beforeServe(BlockPos pos, Player player, ItemStack hand, CallbackInfo ci) {
        icecore$wasServing=false;
        if (!icecore$pending.isEmpty()) { ci.cancel(); return; }
        icecore$incomingPlate=hand.is(CozyRegistry.ItemRegistry.SERVING_PLATE.get());
        icecore$served = icecore$incomingPlate ? ServingPlateItem.getStoredFood(hand).copy() : hand.copyWithCount(1);
        if (requestedItem.isEmpty()) { ci.cancel(); return; }
        boolean drink=icecore$isDrink();
        if (CozyCafe.CONFIG.platingRequired.get() && CafeServingRules.requiresPlate(requestedItem,drink)
                && !hand.is(CozyRegistry.ItemRegistry.SERVING_PLATE.get())) {
            if (player instanceof net.minecraft.server.level.ServerPlayer server)
                PlayerFeedback.show(server, Component.translatable("block.cozycafe.cafe_menu.not_plated"));
            ci.cancel(); return;
        }
        icecore$wasServing=true;
        // Commit these only after native success, below.
    }
    @Redirect(method="handleServe(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V",at=@At(value="INVOKE",target="Lnet/minecraftforge/common/ForgeConfigSpec$BooleanValue;get()Ljava/lang/Object;"))
    private Object icecore$platePermission(net.minecraftforge.common.ForgeConfigSpec.BooleanValue config) { return false; }
    @Redirect(method="handleServe(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V",at=@At(value="INVOKE",target="Lio/github/chakyl/cozycafe/data/CafeMenuItem;category()Lio/github/chakyl/cozycafe/data/CafeMenuItem$MenuItemCategory;"))
    private CafeMenuItem.MenuItemCategory icecore$allowDessertPlate(CafeMenuItem item) {
        return item.category()==CafeMenuItem.MenuItemCategory.DRINK ? item.category() : CafeMenuItem.MenuItemCategory.MAIN;
    }
    @Inject(method="handleServe(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V",at=@At("RETURN"))
    private void icecore$afterServe(BlockPos pos, Player player, ItemStack hand, CallbackInfo ci) {
        if (!icecore$wasServing || !requestedItem.isEmpty()) return;
        var item=CafeMenuItemRegistry.INSTANCE.getForItem(icecore$served.getItem());
        icecore$drink=item != null && item.category()==CafeMenuItem.MenuItemCategory.DRINK;
        icecore$plated=icecore$incomingPlate;
        icecore$return=icecore$plated ? new ItemStack(CozyRegistry.ItemRegistry.DIRTY_SERVING_PLATE.get())
                : WorkstationContainerCompat.hasServingVessel(icecore$served)
                    ? WorkstationContainerCompat.resolveEmptyContainer(icecore$served).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
        droppedItem=ItemStack.EMPTY;
        eatingItem=icecore$served.copyWithCount(1);
        icecore$setDish(icecore$plated, false);
        setChangedForRender();
    }
    @Unique private void icecore$setDish(boolean dish, boolean dirty) {
        BlockEntity self=(BlockEntity)(Object)this;
        BlockState state=self.getBlockState().setValue(CafeMenuBlock.DISH,dish).setValue(CafeMenuBlock.DIRTY,dirty);
        if (state!=self.getBlockState()) self.getLevel().setBlock(self.getBlockPos(),state,3);
    }
    @Unique private void icecore$finish() {
        if (eatingItem.isEmpty()) return;
        if (icecore$pending.isEmpty()) icecore$pending=icecore$return.isEmpty() ? droppedItem.copy() : icecore$return.copy();
        icecore$return=ItemStack.EMPTY;
        droppedItem=ItemStack.EMPTY;
        eatingItem=ItemStack.EMPTY;
        icecore$setDish(false,false);
        setChangedForRender();
    }
    @Inject(method="tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",at=@At("HEAD"))
    private void icecore$beforeTick(Level level, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (level.isClientSide) return;
        if (icecore$pending.isEmpty() && eatingItem.isEmpty() && state.getValue(CafeMenuBlock.DIRTY)) {
            icecore$pending=new ItemStack(CozyRegistry.ItemRegistry.DIRTY_SERVING_PLATE.get());
            icecore$setDish(false,false); setChangedForRender();
        }
        if (orderTime==ORDER_TIME) icecore$finish();
        icecore$paused=!icecore$pending.isEmpty() && !requestedItem.isEmpty();
        if (icecore$paused) { icecore$savedWait=waitTime; waitTime=-2; }
    }
    @Inject(method="tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",at=@At("TAIL"))
    private void icecore$afterTick(Level level, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (level.isClientSide) return;
        if (icecore$paused) { waitTime=icecore$savedWait; icecore$paused=false; }
        if (!icecore$pending.isEmpty() || eatingItem.isEmpty()) icecore$setDish(false,false);
    }
    @Redirect(method="tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",at=@At(value="INVOKE",target="Lnet/minecraftforge/common/ForgeConfigSpec$BooleanValue;get()Ljava/lang/Object;"))
    private Object icecore$noAutomaticDirtyPlate(net.minecraftforge.common.ForgeConfigSpec.BooleanValue config) { return false; }
    @Inject(method="closeMenu(Z)V",at=@At("HEAD"))
    private void icecore$keepReturn(boolean gentle, CallbackInfo ci) { icecore$finish(); }
    @Inject(method="canReceiveNewCustomer()Z",at=@At("RETURN"),cancellable=true)
    private void icecore$cleanBeforeCustomer(CallbackInfoReturnable<Boolean> cir) {
        if (!icecore$pending.isEmpty()) cir.setReturnValue(false);
    }
    @Inject(method="handleClearDirtyIfPossible(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V",at=@At("HEAD"),cancellable=true)
    private void icecore$collect(BlockPos pos, Player player, ItemStack hand, CallbackInfo ci) {
        if (!hand.isEmpty()) { ci.cancel(); return; }
        if (icecore$pending.isEmpty() && ((BlockEntity)(Object)this).getBlockState().getValue(CafeMenuBlock.DIRTY))
            icecore$pending=new ItemStack(CozyRegistry.ItemRegistry.DIRTY_SERVING_PLATE.get());
        if (icecore$pending.isEmpty()) return;
        ci.cancel();
        if (!WorkstationInventoryTransfer.giveOne(player,icecore$pending)) return;
        icecore$pending.shrink(1);
        icecore$setDish(false,false);
        setChangedForRender();
    }
    @Inject(method="m_183515_(Lnet/minecraft/nbt/CompoundTag;)V",at=@At("TAIL"))
    private void icecore$save(CompoundTag tag, CallbackInfo ci) {
        tag.put("icecoreTableReturn",icecore$return.save(new CompoundTag()));
        tag.put("icecoreTablePending",icecore$pending.save(new CompoundTag()));
        tag.putBoolean("icecoreTableDrink",icecore$drink); tag.putBoolean("icecoreTablePlated",icecore$plated);
    }
    @Inject(method="m_142466_(Lnet/minecraft/nbt/CompoundTag;)V",at=@At("TAIL"))
    private void icecore$load(CompoundTag tag, CallbackInfo ci) {
        icecore$return=ItemStack.of(tag.getCompound("icecoreTableReturn"));
        icecore$pending=ItemStack.of(tag.getCompound("icecoreTablePending"));
        icecore$drink=tag.contains("icecoreTableDrink") ? tag.getBoolean("icecoreTableDrink") : ((CafeMenuBlockEntity)(Object)this).getCurrentCourse()==1;
        icecore$plated=tag.getBoolean("icecoreTablePlated");
        if (!tag.contains("icecoreTablePending") && eatingItem.isEmpty() && !droppedItem.isEmpty()) {
            icecore$pending=droppedItem.copy(); droppedItem=ItemStack.EMPTY;
        }
        BlockEntity self=(BlockEntity)(Object)this;
        if (self.getLevel()!=null && self.getLevel().isClientSide) {
            self.requestModelDataUpdate();
            self.getLevel().sendBlockUpdated(self.getBlockPos(),self.getBlockState(),self.getBlockState(),8);
        }
    }
}
