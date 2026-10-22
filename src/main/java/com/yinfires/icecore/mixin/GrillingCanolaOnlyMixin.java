package com.yinfires.icecore.mixin;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "cn.breezeth.kaleidoscope_grilling.GrillAutomationApi", remap = false)
public abstract class GrillingCanolaOnlyMixin {
    @Redirect(method = "brushOil(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;ZZ)Lcn/breezeth/kaleidoscope_grilling/GrillAutomationApi$Result;",
        at = @At(value = "INVOKE", target = "Lcn/breezeth/kaleidoscope_grilling/oil/OilPotCompat;getCount(Lnet/minecraft/world/item/ItemStack;)I"))
    private static int icecore$canola(ItemStack stack) {
        return stack.hasTag() && "canola".equals(stack.getTag().getString("grilling_oil_type")) ? stack.getTag().getInt("oil_count") : 0;
    }
}
