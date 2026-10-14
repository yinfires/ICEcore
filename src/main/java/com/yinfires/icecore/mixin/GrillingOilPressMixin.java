package com.yinfires.icecore.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Removes the byproduct before either Create insertion or world ejection. */
@Mixin(targets = "cn.breezeth.kaleidoscope_grilling.oil.OilPressBlockEntity", remap = false)
public abstract class GrillingOilPressMixin {
    @Redirect(method = "tryFinish()Z", at = @At(value = "NEW",
            target = "(Lnet/minecraft/world/level/ItemLike;I)Lnet/minecraft/world/item/ItemStack;",
            remap = false), remap = false)
    private ItemStack icecore$omitOilResidue(ItemLike item, int count) {
        return ItemStack.EMPTY;
    }
}
