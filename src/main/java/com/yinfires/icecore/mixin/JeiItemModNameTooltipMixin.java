package com.yinfires.icecore.mixin;

import com.yinfires.icecore.config.ICECoreClientConfig;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.ingredients.IIngredientHelper;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.List;

@Mixin(targets = "mezz.jei.library.helpers.ModIdHelper", remap = false)
public abstract class JeiItemModNameTooltipMixin {
    @Inject(method = "addModNameToIngredientTooltip(Ljava/util/List;Ljava/lang/Object;Lmezz/jei/api/ingredients/IIngredientHelper;)Ljava/util/List;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private <T> void icecore$hideLegacyModName(List<Component> lines, T ingredient, IIngredientHelper<T> helper,
                                             CallbackInfoReturnable<List<Component>> cir) {
        if (ICECoreClientConfig.hideOriginalDetails()) cir.setReturnValue(lines);
    }

    @Inject(method = "addModNameToIngredientTooltip(Ljava/util/List;Lmezz/jei/api/ingredients/ITypedIngredient;)Ljava/util/List;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private <T> void icecore$hideTypedModName(List<Component> lines, ITypedIngredient<T> ingredient,
                                            CallbackInfoReturnable<List<Component>> cir) {
        if (ICECoreClientConfig.hideOriginalDetails()) cir.setReturnValue(lines);
    }
    @Inject(method = "getModNameForTooltip(Lmezz/jei/api/ingredients/ITypedIngredient;)Ljava/util/Optional;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private <T> void icecore$hideModName(ITypedIngredient<T> ingredient, CallbackInfoReturnable<Optional<?>> cir) {
        if (ICECoreClientConfig.hideOriginalDetails()) {
            cir.setReturnValue(Optional.empty());
        }
    }
}
