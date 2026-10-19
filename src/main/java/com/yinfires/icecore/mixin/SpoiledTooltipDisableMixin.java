package com.yinfires.icecore.mixin;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.mrbysco.spoiled.handler.TooltipHandler", remap = false)
public abstract class SpoiledTooltipDisableMixin {
    @Inject(method = "onItemTooltip", at = @At("HEAD"), cancellable = true)
    private void icecore$disable(CallbackInfo ci) { ci.cancel(); }
}
