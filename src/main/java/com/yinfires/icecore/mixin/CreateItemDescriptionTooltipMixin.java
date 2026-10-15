package com.yinfires.icecore.mixin;

import com.yinfires.icecore.config.ICECoreClientConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Create's Shift/Ctrl item descriptions are added after the original tooltip. */
@Mixin(targets = "com.simibubi.create.foundation.events.ClientEvents", remap = false)
public abstract class CreateItemDescriptionTooltipMixin {
    @Inject(method = "addToItemTooltip(Lnet/minecraftforge/event/entity/player/ItemTooltipEvent;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void icecore$hideDescription(CallbackInfo ci) {
        if (ICECoreClientConfig.hideOriginalDetails()) ci.cancel();
    }
}
