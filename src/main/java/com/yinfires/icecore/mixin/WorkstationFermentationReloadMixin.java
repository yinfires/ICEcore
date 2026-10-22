package com.yinfires.icecore.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.yinfires.icecore.workstation.ExternalWorkstation;

/** Re-enter the native recipe search once when a loaded recipe object is replaced. */
@Mixin(targets="dev.xkmc.youkaishomecoming.content.pot.base.TimedRecipeBlockEntity",remap=false)
public abstract class WorkstationFermentationReloadMixin {
    @Shadow protected ResourceLocation recipeId;
    @Shadow private boolean doRecipeSearch;
    @Unique private Object icecore$observedRecipe;
    @Unique private boolean icecore$observed;

    @Inject(method="tick()V",at=@At("HEAD"))
    private void icecore$reload(CallbackInfo ci) {
        var level=((BlockEntity)(Object)this).getLevel();
        if(level==null || level.isClientSide || recipeId==null)return;
        Recipe<?> current=level.getRecipeManager().byKey(recipeId).orElse(null);
        if(icecore$observed && current==icecore$observedRecipe)return;
        icecore$observed=true;
        icecore$observedRecipe=current;
        if(ExternalWorkstation.get(this,"recipe")!=current)doRecipeSearch=true;
    }
}
