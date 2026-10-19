package com.yinfires.icecore.mixin;

import com.yinfires.icecore.spoilage.*;
import net.minecraft.world.item.*;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.function.BooleanSupplier;

@Mixin(ItemStack.class)
public abstract class ShelfLifeItemStackMixin implements ShelfLifeStackAccess {
    @Shadow(remap = false) @Final @Mutable private Item f_41589_;
    @Shadow(remap = false) @Final @Mutable private net.minecraft.core.Holder.Reference<Item> delegate;
    @Shadow(remap = false) private CompoundTag capNBT;
    @Unique private BooleanSupplier icecore$owner;
    @Unique private boolean icecore$ready;
    @Unique private boolean icecore$detached;
    @Override public void icecore$detach() { icecore$detached = true; icecore$owner = null; }
    @Override public boolean icecore$detached() { return icecore$detached; }
    @Unique private long icecore$checkedDay = Long.MIN_VALUE;
    @Unique private boolean icecore$checkedPaused;
    @Unique private int icecore$checkedRevision = -1;
    @Unique private CompoundTag icecore$checkedTag;
    @Unique private net.minecraft.nbt.Tag icecore$checkedFood;
    @Unique private net.minecraft.nbt.Tag icecore$checkedContents;
    @Override public boolean icecore$needsSettlement(long day, boolean paused, int revision) {
        ItemStack stack = (ItemStack)(Object)this;
        CompoundTag tag = stack.getTag();
        var food = tag == null ? null : tag.get("platedFood");
        var contents = tag == null ? null : tag.get("BlockEntityTag");
        if (day == icecore$checkedDay && paused == icecore$checkedPaused && revision == icecore$checkedRevision
                && tag == icecore$checkedTag && food == icecore$checkedFood && contents == icecore$checkedContents) return false;
        icecore$checkedDay = day; icecore$checkedPaused = paused; icecore$checkedRevision = revision;
        icecore$checkedTag = tag; icecore$checkedFood = food;
        icecore$checkedContents = contents;
        return true;
    }
    @Inject(method = "<init>", at = @At("RETURN"))
    private void icecore$constructed(CallbackInfo ci) { icecore$ready = true; }
    @Override public BooleanSupplier icecore$owner() { return icecore$owner; }
    @Override public void icecore$owner(BooleanSupplier owner) { icecore$owner = owner; icecore$detached = false; }
    @Override public void icecore$replace(ItemStack replacement) {
        ItemStack self = (ItemStack)(Object)this;
        self.invalidateCaps();
        f_41589_ = replacement.getItem();
        delegate = net.minecraftforge.registries.ForgeRegistries.ITEMS.getDelegateOrThrow(f_41589_);
        capNBT = null;
        self.setTag(replacement.getTag().copy());
        // Keep capabilities invalid: expired items cannot expose their former fluid/item handler.
    }
    @Inject(method = "m_41720_()Lnet/minecraft/world/item/Item;", at = @At("HEAD"))
    private void icecore$settle(CallbackInfoReturnable<Item> cir) { if (icecore$ready) ShelfLife.observe((ItemStack)(Object)this); }
    @Inject(method = "m_41777_()Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
    private void icecore$copy(CallbackInfoReturnable<ItemStack> cir) {
        if (!cir.getReturnValue().isEmpty()) ((ShelfLifeStackAccess)(Object)cir.getReturnValue()).icecore$detach();
    }
    @Inject(method = "m_41666_(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;IZ)V", at = @At("HEAD"))
    private void icecore$inventory(net.minecraft.world.level.Level level, net.minecraft.world.entity.Entity entity,
                                   int slot, boolean selected, CallbackInfo ci) {
        if (entity instanceof net.minecraft.world.entity.player.Player player) ShelfLife.player((ItemStack)(Object)this, player);
    }
    @Inject(method = "m_41739_(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/nbt/CompoundTag;", at = @At("RETURN"))
    private void icecore$save(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        if (f_41589_ instanceof ExpiredFoodItem) cir.getReturnValue().remove("ForgeCaps");
    }
    @Inject(method = "m_150942_(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private static void icecore$same(ItemStack a, ItemStack b, CallbackInfoReturnable<Boolean> cir) {
        ShelfLife.observe(a); ShelfLife.observe(b);
        Boolean same = ShelfLife.same(a, b);
        if (same != null) cir.setReturnValue(same);
    }
    @Inject(method = "m_41728_(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private static void icecore$networkEquals(ItemStack a, ItemStack b, CallbackInfoReturnable<Boolean> cir) {
        // Menu snapshots require exact clock/pause equality, unlike gameplay stack merging.
        if (a.hasTag() && a.getTag().contains(ShelfLife.KEY, 10) || b.hasTag() && b.getTag().contains(ShelfLife.KEY, 10))
            cir.setReturnValue(a.getCount() == b.getCount() && a.getItem() == b.getItem()
                    && java.util.Objects.equals(a.getTag(), b.getTag()) && a.areCapsCompatible(b));
    }
}
