package com.yinfires.icecore.mixin;

import com.yinfires.icecore.config.ICECoreClientConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "cn.breezeth.kaleidoscope_grilling.food.FoodTooltip", remap = false)
public abstract class GrillingFoodTooltipMixin {
    @Inject(method = {
            "appendEffect(Ljava/util/List;Lnet/minecraft/resources/ResourceLocation;I)V",
            "appendEffects(Ljava/util/List;Lnet/minecraft/resources/ResourceLocation;ILnet/minecraft/resources/ResourceLocation;I)V"
    }, at = @At("HEAD"), cancellable = true, remap = false)
    private static void icecore$hideEffects(CallbackInfo ci) {
        if (ICECoreClientConfig.hideFoodEffects()) ci.cancel();
    }
}
