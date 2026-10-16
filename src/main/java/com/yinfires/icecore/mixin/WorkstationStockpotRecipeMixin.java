package com.yinfires.icecore.mixin;

import com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.StockpotRecipe;
import com.google.gson.JsonObject;
import com.yinfires.icecore.workstation.WorkstationContainerCompat;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopecookery.crafting.serializer.StockpotRecipeSerializer", remap = false)
public abstract class WorkstationStockpotRecipeMixin {
    @Inject(method = "fromJson(Lnet/minecraft/resources/ResourceLocation;Lcom/google/gson/JsonObject;)Lcom/github/ysbbbbbb/kaleidoscopecookery/crafting/recipe/StockpotRecipe;", at = @At("RETURN"), cancellable = true)
    private void icecore$deriveCarrier(ResourceLocation id, JsonObject json, CallbackInfoReturnable<StockpotRecipe> cir) {
        if (json.has("carrier") || json.has("empty_carrier") && json.get("empty_carrier").getAsBoolean()) return;
        StockpotRecipe r = cir.getReturnValue();
        cir.setReturnValue(new StockpotRecipe(r.id(), r.ingredients(), r.soupBase(), r.result(), r.time(), WorkstationContainerCompat.resolveOutputCarrier(r.result(), null), r.visuals()));
    }
}
