package com.yinfires.icecore.mixin;

import com.yinfires.icecore.item.ModItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Youkai's Feasts tea stops at its leaf stage. Lowering getMaxAge stops random-tick, bonemeal and
 * daily growth there and makes CropBlock#isMaxAge (used by Jade's progress line) report mature.
 */
@Pseudo
@Mixin(targets = "dev.xkmc.youkaishomecoming.content.block.plant.TeaCropBlock", remap = false)
public abstract class SeedBagCompatTeaMixin {
    @Inject(method = "m_7419_()I", at = @At("HEAD"), cancellable = true, remap = false)
    private void icecore$leafStageIsMax(CallbackInfoReturnable<Integer> callback) {
        callback.setReturnValue(ModItems.TEA_HARVEST_AGE);
    }
}
