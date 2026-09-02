package com.yinfires.icecore.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.text.NumberFormat;
import java.util.Locale;

/** Uses the same dollar marker and grouping as ICEcore's currency HUD. */
@Pseudo
@Mixin(targets = "net.sixik.sdmshoprework.SDMShopRework", remap = false)
public abstract class SDMShopMoneyFormatMixin {
    @Inject(method = "moneyString(J)Ljava/lang/String;", at = @At("HEAD"), cancellable = true, remap = false)
    private static void icecore$formatAmount(long amount, CallbackInfoReturnable<String> callbackInfo) {
        callbackInfo.setReturnValue("$" + NumberFormat.getIntegerInstance(Locale.US).format(amount));
    }

    @Inject(method = "moneyString(Ljava/lang/String;)Ljava/lang/String;", at = @At("HEAD"), cancellable = true,
            remap = false)
    private static void icecore$formatText(String amount, CallbackInfoReturnable<String> callbackInfo) {
        callbackInfo.setReturnValue("$" + amount);
    }
}
