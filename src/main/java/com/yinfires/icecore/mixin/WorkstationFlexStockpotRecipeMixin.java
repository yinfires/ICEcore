package com.yinfires.icecore.mixin;

import com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.FlexStockpotRecipe;
import com.google.gson.JsonObject;
import com.yinfires.icecore.workstation.WorkstationContainerCompat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopecookery.crafting.serializer.FlexStockpotRecipeSerializer", remap = false)
public abstract class WorkstationFlexStockpotRecipeMixin {
    @Inject(method = "fromJson(Lnet/minecraft/resources/ResourceLocation;Lcom/google/gson/JsonObject;)Lcom/github/ysbbbbbb/kaleidoscopecookery/crafting/recipe/FlexStockpotRecipe;", at = @At("RETURN"), cancellable = true)
    private void icecore$deriveCarrier(ResourceLocation id, JsonObject json, CallbackInfoReturnable<FlexStockpotRecipe> cir) {
        if (json.has("carrier") && !(json.has("empty_carrier") && json.get("empty_carrier").getAsBoolean())) return;
        FlexStockpotRecipe r = cir.getReturnValue();
        Ingredient carrier = json.has("empty_carrier") && json.get("empty_carrier").getAsBoolean() ? Ingredient.EMPTY : WorkstationContainerCompat.resolveOutputCarrier(r.result(), null);
        cir.setReturnValue(new FlexStockpotRecipe(r.id(), r.ingredients(), r.soupBase(), r.result(), r.time(), carrier, r.visuals()));
    }
}
