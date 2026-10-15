package com.yinfires.icecore.mixin;

import com.yinfires.icecore.config.ICECoreClientConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Stop Quality Food's graphical effect components before they are allocated. */
@Mixin(targets = "de.cadentem.quality_food.events.ForgeEvents", remap = false)
public abstract class QualityFoodTooltipMixin {
    @Inject(method = "addEffectTooltip(Lnet/minecraftforge/client/event/RenderTooltipEvent$GatherComponents;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void icecore$hideEffects(CallbackInfo ci) {
        if (ICECoreClientConfig.hideFoodEffects()) ci.cancel();
    }
}
