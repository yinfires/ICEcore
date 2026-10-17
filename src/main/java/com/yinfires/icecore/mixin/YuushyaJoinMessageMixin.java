package com.yinfires.icecore.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.yuushya.Yuushya", remap = false)
public abstract class YuushyaJoinMessageMixin {
    @Inject(method = "lambda$init$4(Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void icecore$silenceDeclaration(CallbackInfo ci) {
        ci.cancel();
    }
}
