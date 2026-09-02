package com.yinfires.icecore.mixin;

import com.yinfires.icecore.compat.sdmshop.SDMShopCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Installs ICEcore's shared balance after SDM Shop Rework creates its default economy module. */
@Pseudo
@Mixin(targets = "net.sixik.sdmshoprework.economy.EconomyManager", remap = false)
public abstract class SDMShopEconomyMixin {
    @Inject(method = "init()V", at = @At("RETURN"), remap = false)
    private static void icecore$installSharedCurrency(CallbackInfo callbackInfo) {
        SDMShopCompat.installEconomyModule();
    }
}
