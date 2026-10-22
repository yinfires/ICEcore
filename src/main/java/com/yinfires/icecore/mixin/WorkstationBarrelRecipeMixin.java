package com.yinfires.icecore.mixin;

import com.github.ysbbbbbb.kaleidoscopetavern.crafting.container.BarrelRecipeContainer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets="com.github.ysbbbbbb.kaleidoscopetavern.crafting.recipe.BarrelRecipe",remap=false)
public abstract class WorkstationBarrelRecipeMixin {
    @Inject(method="matches(Lcom/github/ysbbbbbb/kaleidoscopetavern/crafting/container/BarrelRecipeContainer;Lnet/minecraft/world/level/Level;)Z",
        at=@At("HEAD"),cancellable=true)
    private void icecore$fullIngredients(BarrelRecipeContainer container,Level level,CallbackInfoReturnable<Boolean> cir) {
        var id=((Recipe<?>)(Object)this).getId();
        if(!id.getNamespace().equals("icecore") || !id.getPath().startsWith("recipe_manager/"))return;
        for(var stack:container.getItems())if(!stack.isEmpty() && stack.getCount()!=16){
            cir.setReturnValue(false);return;
        }
    }
}
