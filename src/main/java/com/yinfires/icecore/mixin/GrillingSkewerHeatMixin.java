package com.yinfires.icecore.mixin;

import cn.breezeth.kaleidoscope_grilling.skewer.SkewerPlateItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Rejects new heat and makes legacy heat inert without inventory scans or NBT rewrites. */
@Mixin(targets = "cn.breezeth.kaleidoscope_grilling.food.FoodState", remap = false)
public abstract class GrillingSkewerHeatMixin {
    @Inject(method = "setHot(Lnet/minecraft/world/item/ItemStack;J)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void icecore$skipSkewerHeat(ItemStack stack, long until, CallbackInfo ci) {
        if (SkewerPlateItem.isSkewer(stack)) ci.cancel();
    }

    @Inject(method = "isHot(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;)Z",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void icecore$skewersAreCold(ItemStack stack, Level level, CallbackInfoReturnable<Boolean> cir) {
        if (SkewerPlateItem.isSkewer(stack)) cir.setReturnValue(false);
    }

    @Inject(method = "remainingHeat(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;)J",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void icecore$noSkewerCountdown(ItemStack stack, Level level, CallbackInfoReturnable<Long> cir) {
        if (SkewerPlateItem.isSkewer(stack)) cir.setReturnValue(0L);
    }
}
