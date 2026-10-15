package com.yinfires.icecore.mixin;

import com.yinfires.icecore.config.ICECoreClientConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "vectorwing.farmersdelight.common.utility.TextUtils", remap = false)
public abstract class FarmersFoodTooltipMixin {
    @Inject(method = "addFoodEffectTooltip(Lnet/minecraft/world/item/ItemStack;Ljava/util/List;F)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void icecore$hideEffects(CallbackInfo ci) {
        if (ICECoreClientConfig.hideFoodEffects()) ci.cancel();
    }
}
