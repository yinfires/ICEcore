package com.yinfires.icecore.mixin;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.mrbysco.spoiled.util.SpoilMerger", remap = false)
public abstract class SpoiledMergeDisableMixin {
    @Inject(method = {"normalise", "reconcile"}, at = @At("HEAD"), cancellable = true)
    private static void icecore$disable(CallbackInfoReturnable<Boolean> cir) { cir.setReturnValue(false); }
}
