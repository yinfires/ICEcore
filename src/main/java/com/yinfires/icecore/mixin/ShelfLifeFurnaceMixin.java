package com.yinfires.icecore.mixin;

import com.yinfires.icecore.spoilage.ShelfLife;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class ShelfLifeFurnaceMixin {
    @Inject(method = "m_6836_(ILnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"))
    private void icecore$set(int slot, ItemStack stack, CallbackInfo ci) { ShelfLife.release(stack); }
    @Inject(method = "m_8020_(I)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
    private void icecore$get(int slot, CallbackInfoReturnable<ItemStack> cir) { ShelfLife.release(cir.getReturnValue()); }
}
