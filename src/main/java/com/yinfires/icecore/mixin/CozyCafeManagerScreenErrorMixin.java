package com.yinfires.icecore.mixin;

import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds ICEcore's menu-seat failure to CozyCafe's existing manager HUD. */
@Pseudo
@Mixin(targets = "io.github.chakyl.cozycafe.gui.CafeManagerScreen", remap = false)
public abstract class CozyCafeManagerScreenErrorMixin {
    @Shadow private Component errorMessage;
    @Shadow private int errorDisplayTicks;

    @Inject(method = "setErrorMessage(Ljava/lang/Byte;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void icecore$validMenuSeatHud(Byte code, CallbackInfo callback) {
        if (code != null && code.byteValue() == 5) {
            errorMessage = Component.translatable("icecore.cozycafe.no_valid_menu_seat");
            errorDisplayTicks = 240;
            callback.cancel();
        }
    }
}
