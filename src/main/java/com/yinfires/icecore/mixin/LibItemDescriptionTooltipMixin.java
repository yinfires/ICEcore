package com.yinfires.icecore.mixin;

import com.yinfires.icecore.config.ICECoreClientConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** LibTooltips adds item descriptions and their key prompts, not recipe/menu data. */
@Mixin(targets = "com.wdiscute.libtooltips.Tooltips", remap = false)
public abstract class LibItemDescriptionTooltipMixin {
    @Inject(method = "modifyItemTooltip(Lnet/minecraftforge/event/entity/player/ItemTooltipEvent;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void icecore$hideDescription(CallbackInfo ci) {
        if (ICECoreClientConfig.hideOriginalDetails()) ci.cancel();
    }
}
