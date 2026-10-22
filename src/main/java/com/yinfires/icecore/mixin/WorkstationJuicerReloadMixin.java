package com.yinfires.icecore.mixin;

import com.yinfires.icecore.workstation.ExternalWorkstation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native mixed recipes keep their inputs until completion, so invalid work can stop safely. */
@Mixin(targets="com.bmt.kaleidoscope_fragrantorchard.block.entity.JuicerBlockEntity",remap=false)
public abstract class WorkstationJuicerReloadMixin {
    @Unique private Object icecore$observedRecipe;
    @Unique private boolean icecore$observed;
    @Inject(method="tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lcom/bmt/kaleidoscope_fragrantorchard/block/entity/JuicerBlockEntity;)V",at=@At("HEAD"))
    private static void icecore$reload(Level level,BlockPos pos,BlockState state,
            @Coerce Object entity,CallbackInfo ci) {
        if(!level.isClientSide)((WorkstationJuicerReloadMixin)entity).icecore$refresh(level);
    }
    @Unique private void icecore$refresh(Level level) {
        Object stored=ExternalWorkstation.get(this,"recipe");
        if(!(stored instanceof Recipe<?> old) || !old.getId().getNamespace().equals("icecore")
                || !old.getId().getPath().startsWith("recipe_manager/"))return;
        Recipe<?> current=level.getRecipeManager().byKey(old.getId()).orElse(null);
        if(icecore$observed && current==icecore$observedRecipe)return;
        icecore$observed=true;icecore$observedRecipe=current;
        if(current==old)return;
        boolean completed=(Boolean)ExternalWorkstation.get(this,"isMixedResult");
        if(completed) {
            if(current!=null && current.getClass()==stored.getClass()) {
                ExternalWorkstation.set(this,"recipe",current);
                ExternalWorkstation.set(this,"mixedExtractItem",((net.minecraft.world.item.ItemStack)ExternalWorkstation.call(current,"getMixedExtractItem")).copy());
            }
        } else {
            ExternalWorkstation.set(this,"crafting",false);
            ExternalWorkstation.set(this,"progress",0);
            ExternalWorkstation.call(this,"checkForMatchingRecipe");
        }
        ExternalWorkstation.changed((net.minecraft.world.level.block.entity.BlockEntity)(Object)this);
    }
}
