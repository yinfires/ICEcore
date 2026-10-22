package com.yinfires.icecore.mixin;

import com.yinfires.icecore.workstation.ExternalWorkstation;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.*;
import net.minecraft.world.item.*;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Bucket filling is native FluidUtil; these slots only document that extraction. */
@Mixin(targets="com.bmt.kaleidoscope_fragrantorchard.compat.jei.JuicerCategory",remap=false)
public abstract class WorkstationJuicerBucketJeiMixin {
    @Inject(method="setRecipe(Lmezz/jei/api/gui/builder/IRecipeLayoutBuilder;Lcom/bmt/kaleidoscope_fragrantorchard/crafting/JuicerRecipe;Lmezz/jei/api/recipe/IFocusGroup;)V",at=@At("TAIL"))
    private void icecore$bucket(IRecipeLayoutBuilder builder,@Coerce Object recipe,IFocusGroup focus,CallbackInfo ci) {
        var id=((net.minecraft.world.item.crafting.Recipe<?>)recipe).getId();
        if(!id.getNamespace().equals("icecore")||!id.getPath().startsWith("recipe_manager/")
            ||(Boolean)ExternalWorkstation.call(recipe,"isMixedMode")
            ||!((ItemStack)ExternalWorkstation.call(recipe,"getExtractItem")).isEmpty())return;
        var fluid=(FluidStack)ExternalWorkstation.call(recipe,"getResultFluid");
        Item bucket=fluid.getFluid().getBucket();
        if(bucket==Items.AIR)return;
        builder.addSlot(RecipeIngredientRole.CATALYST,109,44).addItemStack(new ItemStack(Items.BUCKET));
        builder.addSlot(RecipeIngredientRole.OUTPUT,128,44).addItemStack(new ItemStack(bucket));
    }
}
