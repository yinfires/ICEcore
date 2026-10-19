package com.yinfires.icecore.mixin;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Disable the old scheduler and pickup averaging, retaining the installed original jar. */
@Pseudo
@Mixin(targets = "com.mrbysco.spoiled.handler.SpoilHandler", remap = false)
public abstract class SpoiledDisableMixin {
    @Inject(method = {"onWorldTick", "onPlayerTick", "onItemPickup"}, at = @At("HEAD"), cancellable = true)
    private void icecore$disable(CallbackInfo ci) { ci.cancel(); }
}
