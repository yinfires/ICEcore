package com.yinfires.icecore.mixin;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.mrbysco.spoiled.util.SpoilHelper", remap = false)
public abstract class SpoiledHelperDisableMixin {
    @Inject(method = {"updateSpoilingStack", "spoilSingleItemAndReplace", "spoilItemForPlayer", "spoilItemForEntity"}, at = @At("HEAD"), cancellable = true)
    private static void icecore$disable(CallbackInfo ci) { ci.cancel(); }
}
