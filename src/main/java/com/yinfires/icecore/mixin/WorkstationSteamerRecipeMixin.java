package com.yinfires.icecore.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.SteamerRecipe", remap = false)
public abstract class WorkstationSteamerRecipeMixin {
    @Inject(method = "getCookTick()I", at = @At("HEAD"), cancellable = true)
    private void icecore$thirtySeconds(CallbackInfoReturnable<Integer> cir) { cir.setReturnValue(600); }
}
