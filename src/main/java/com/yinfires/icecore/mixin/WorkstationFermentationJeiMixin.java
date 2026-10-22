package com.yinfires.icecore.mixin;

import dev.xkmc.youkaishomecoming.content.pot.ferment.SimpleFermentationRecipe;
import com.yinfires.icecore.workstation.WorkstationContainerCompat;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets="dev.xkmc.youkaishomecoming.compat.jei.FermentRecipeCategory",remap=false)
public abstract class WorkstationFermentationJeiMixin {
    @Inject(method="setRecipe(Lmezz/jei/api/gui/builder/IRecipeLayoutBuilder;Ldev/xkmc/youkaishomecoming/content/pot/ferment/SimpleFermentationRecipe;Lmezz/jei/api/recipe/IFocusGroup;)V",at=@At("TAIL"))
    private void icecore$containers(IRecipeLayoutBuilder builder,SimpleFermentationRecipe recipe,IFocusGroup focus,CallbackInfo ci) {
        var id=((net.minecraft.world.item.crafting.Recipe<?>)(Object)recipe).getId();
        if(!id.getNamespace().equals("icecore")||!id.getPath().startsWith("recipe_manager/"))return;
        var containers=new java.util.ArrayList<ItemStack>();
        for(var result:recipe.results) WorkstationContainerCompat.resolveEmptyContainer(result).ifPresent(empty->{
            if(containers.stream().noneMatch(s->ItemStack.isSameItemSameTags(s,empty)))containers.add(empty.copyWithCount(1));
        });
        if(!containers.isEmpty())builder.addSlot(RecipeIngredientRole.CATALYST,64,1).addItemStacks(containers);
    }
}
