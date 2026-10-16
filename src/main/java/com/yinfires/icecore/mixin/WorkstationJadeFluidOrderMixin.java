package com.yinfires.icecore.mixin;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "snownee.jade.addon.universal.FluidStorageProvider", remap = false)
public abstract class WorkstationJadeFluidOrderMixin {
    @Inject(method = "getDefaultPriority()I", at = @At("HEAD"), cancellable = true)
    private void icecore$afterItems(CallbackInfoReturnable<Integer> cir) { cir.setReturnValue(1100); }
}
