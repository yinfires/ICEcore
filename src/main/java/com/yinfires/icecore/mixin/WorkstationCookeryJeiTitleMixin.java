package com.yinfires.icecore.mixin;

import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets="com.github.ysbbbbbb.kaleidoscopecookery.compat.jei.category.StockpotRecipeCategory",remap=false)
public abstract class WorkstationCookeryJeiTitleMixin {
    @Inject(method="getTitle()Lnet/minecraft/network/chat/Component;",at=@At("HEAD"),cancellable=true)
    private void icecore$title(CallbackInfoReturnable<Component> cir) {
        cir.setReturnValue(Component.translatable("block.kaleidoscope_cookery.stockpot"));
    }
    @Redirect(method="draw(Lcom/github/ysbbbbbb/kaleidoscopecookery/crafting/recipe/StockpotRecipe;Lmezz/jei/api/gui/ingredient/IRecipeSlotsView;Lnet/minecraft/client/gui/GuiGraphics;DD)V",at=@At(value="INVOKE",target="Lnet/minecraft/network/chat/Component;m_237115_(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;"))
    private net.minecraft.network.chat.MutableComponent icecore$description(String key) {
        return key.equals("jei.kaleidoscope_cookery.strict_recipe")?Component.empty():Component.translatable(key);
    }
}
