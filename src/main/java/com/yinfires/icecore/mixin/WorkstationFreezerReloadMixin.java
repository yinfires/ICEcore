package com.yinfires.icecore.mixin;

import com.yinfires.icecore.workstation.ExternalWorkstation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Captures inputs before native freezing consumes them; failed reloads return the whole batch. */
@Mixin(targets="com.bmt.kaleidoscope_world_liquor.block.entity.FreezerBlockEntity",remap=false)
public abstract class WorkstationFreezerReloadMixin {
    @Unique private CompoundTag icecore$batch;
    @Unique private CompoundTag icecore$pending;
    @Unique private Object icecore$observedRecipe;
    @Unique private boolean icecore$observed;
    @Inject(method="tryStartCrafting()Z",at=@At("HEAD"),cancellable=true)
    private void icecore$capture(CallbackInfoReturnable<Boolean> cir) {
        Object cached=ExternalWorkstation.get(this,"cachedRecipe");
        var level=((BlockEntity)(Object)this).getLevel();
        if(level!=null && cached instanceof Recipe<?> old && old.getId().getNamespace().equals("icecore")
                && old.getId().getPath().startsWith("recipe_manager/")) {
            var current=level.getRecipeManager().byKey(old.getId()).orElse(null);
            if(current==null || current.getClass()!=cached.getClass()){cir.setReturnValue(false);return;}
            var items=(ItemStackHandler)ExternalWorkstation.get(this,"inputInventory");
            var inputs=NonNullList.withSize(4,ItemStack.EMPTY);
            for(int i=0;i<4;i++)inputs.set(i,items.getStackInSlot(i));
            var fluid=((FluidTank)ExternalWorkstation.get(this,"tank")).getFluid();
            if(!(Boolean)ExternalWorkstation.invoke(current,"matches",new Class<?>[]{FluidStack.class,NonNullList.class,Level.class},fluid,inputs,level)) {
                cir.setReturnValue(false);return;
            }
            ExternalWorkstation.set(this,"cachedRecipe",current);
        }
        icecore$pending=new CompoundTag();
        icecore$pending.put("items",((ItemStackHandler)ExternalWorkstation.get(this,"inputInventory")).serializeNBT());
        icecore$pending.put("fluid",((FluidTank)ExternalWorkstation.get(this,"tank")).writeToNBT(new CompoundTag()));
    }
    @Inject(method="tryStartCrafting()Z",at=@At("RETURN"))
    private void icecore$started(CallbackInfoReturnable<Boolean> cir) {
        if(cir.getReturnValue()){icecore$batch=icecore$pending;icecore$observedRecipe=ExternalWorkstation.get(this,"recipe");icecore$observed=true;}
        icecore$pending=null;
    }
    @Inject(method="tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lcom/bmt/kaleidoscope_world_liquor/block/entity/FreezerBlockEntity;)V",at=@At("HEAD"),cancellable=true)
    private static void icecore$reload(Level level,BlockPos pos,BlockState state,@Coerce Object entity,CallbackInfo ci) {
        if(!level.isClientSide && ((WorkstationFreezerReloadMixin)entity).icecore$refresh(level,state))ci.cancel();
    }
    @Unique private boolean icecore$refresh(Level level,BlockState state) {
        Object stored=ExternalWorkstation.get(this,"recipe");
        if(!(stored instanceof Recipe<?> old) || !old.getId().getNamespace().equals("icecore")
                || !old.getId().getPath().startsWith("recipe_manager/"))return false;
        Recipe<?> current=level.getRecipeManager().byKey(old.getId()).orElse(null);
        if(icecore$observed && current==icecore$observedRecipe)return false;
        icecore$observed=true;icecore$observedRecipe=current;
        if(current==old)return false;
        if(ExternalWorkstation.number(this,"outputCount")>0) {
            if(current!=null && current.getClass()==stored.getClass())ExternalWorkstation.set(this,"recipe",current);
            return false;
        }
        if(icecore$batch==null || (Boolean)ExternalWorkstation.call(this,"isWorking")==false)return false; // Legacy/completed saves keep their consumed batch.
        var items=new ItemStackHandler(4);items.deserializeNBT(icecore$batch.getCompound("items"));
        var liquid=new FluidTank(1000);liquid.readFromNBT(icecore$batch.getCompound("fluid"));
        var inputs=NonNullList.withSize(4,ItemStack.EMPTY);
        for(int i=0;i<4;i++)inputs.set(i,items.getStackInSlot(i));
        if(current!=null && current.getClass()==stored.getClass() && (Boolean)ExternalWorkstation.invoke(current,"matches",
                new Class<?>[]{FluidStack.class,NonNullList.class,Level.class},liquid.getFluid(),inputs,level)) {
            ExternalWorkstation.set(this,"recipe",current);
            ExternalWorkstation.set(this,"maxProgress",ExternalWorkstation.call(current,"getCraftTime"));return false;
        }
        var entity=(BlockEntity)(Object)this;
        for(var property:state.getProperties())if(property instanceof BooleanProperty working && property.getName().equals("working")) {
            level.setBlock(entity.getBlockPos(),state.setValue(working,false),3);break;
        }
        ExternalWorkstation.set(this,"progress",0);ExternalWorkstation.set(this,"maxProgress",0);
        var restored=icecore$batch;icecore$batch=null;
        ((ItemStackHandler)ExternalWorkstation.get(this,"inputInventory")).deserializeNBT(restored.getCompound("items"));
        ((FluidTank)ExternalWorkstation.get(this,"tank")).readFromNBT(restored.getCompound("fluid"));
        ExternalWorkstation.call(this,"checkForMatchingRecipe");
        ExternalWorkstation.changed(entity);return true;
    }
    @Inject(method="m_183515_(Lnet/minecraft/nbt/CompoundTag;)V",at=@At("TAIL"))
    private void icecore$save(CompoundTag tag,CallbackInfo ci){if(icecore$batch!=null)tag.put("icecore_batch_inputs",icecore$batch.copy());}
    @Inject(method="m_142466_(Lnet/minecraft/nbt/CompoundTag;)V",at=@At("TAIL"))
    private void icecore$load(CompoundTag tag,CallbackInfo ci){icecore$batch=tag.contains("icecore_batch_inputs")?tag.getCompound("icecore_batch_inputs").copy():null;icecore$observed=false;}
}
