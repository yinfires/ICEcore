package com.yinfires.icecore.mixin;

import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Filter JEI's final builder, after category callbacks, without touching item tooltips elsewhere. */
@Mixin(targets="mezz.jei.common.gui.JeiTooltip",remap=false)
public abstract class WorkstationJeiTooltipMixin {
    @Inject(method="add(Lnet/minecraft/network/chat/FormattedText;)V",at=@At("HEAD"),cancellable=true)
    private void icecore$details(FormattedText text,CallbackInfo ci) {
        if(com.yinfires.icecore.tooltip.JeiRecipeDetails.hidden(text))ci.cancel();
    }
}
