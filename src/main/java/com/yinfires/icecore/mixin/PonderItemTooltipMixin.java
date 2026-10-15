package com.yinfires.icecore.mixin;

import com.yinfires.icecore.config.ICECoreClientConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.createmod.ponder.foundation.PonderTooltipHandler", remap = false)
public abstract class PonderItemTooltipMixin {
    @Inject(method = "addToTooltip(Ljava/util/List;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void icecore$hidePonder(CallbackInfo ci) {
        if (ICECoreClientConfig.hideOriginalDetails()) ci.cancel();
    }
}
