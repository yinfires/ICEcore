package com.yinfires.icecore.mixin;

import com.yinfires.icecore.spoilage.ShelfLife;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Inventory.class)
public abstract class ShelfLifeInventoryMixin {
    @Shadow(remap = false) @Final public Player f_35978_;
    @Unique private final java.util.function.BooleanSupplier icecore$creative = () -> f_35978_.isCreative();
    @Unique private void icecore$own(ItemStack stack) { ShelfLife.own(stack, icecore$creative); }
    @Inject(method = "m_36040_(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"))
    private void icecore$add(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) { icecore$own(stack); }
    @Inject(method = "m_6836_(ILnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"))
    private void icecore$set(int slot, ItemStack stack, CallbackInfo ci) { icecore$own(stack); }
    @Inject(method = "m_8020_(I)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
    private void icecore$get(int slot, CallbackInfoReturnable<ItemStack> cir) { icecore$own(cir.getReturnValue()); }
}
