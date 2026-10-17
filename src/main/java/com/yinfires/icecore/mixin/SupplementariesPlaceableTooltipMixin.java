package com.yinfires.icecore.mixin;

import com.yinfires.icecore.config.ICECoreClientConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Supplier;

@Pseudo
@Mixin(targets = "net.mehvahdjukaar.supplementaries.common.events.ClientEvents", remap = false)
public abstract class SupplementariesPlaceableTooltipMixin {
    @Redirect(method = "onItemTooltip(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/TooltipFlag;Ljava/util/List;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/function/Supplier;get()Ljava/lang/Object;", ordinal = 1), remap = false)
    private static Object icecore$hidePlaceableHint(Supplier<?> setting) {
        return ICECoreClientConfig.hideOriginalDetails() ? Boolean.FALSE : setting.get();
    }
}
