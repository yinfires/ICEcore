package com.yinfires.icecore.mixin;

import com.yinfires.icecore.compat.sdmshop.SDMShopCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Persists in-game shop edits back into ICEcore's per-entry snapshot. Every SDM edit packet
 * (create/edit tab and entry) funnels through {@code ShopBase.saveShopToFile}, so capturing there
 * covers all edit paths without polling.
 */
@Pseudo
@Mixin(targets = "net.sixik.sdmshoprework.common.shop.ShopBase", remap = false)
public abstract class SDMShopSaveMixin {
    @Inject(method = "saveShopToFile()V", at = @At("HEAD"), remap = false)
    private void icecore$captureSnapshot(CallbackInfo callback) {
        SDMShopCompat.captureActiveSnapshot();
    }
}
