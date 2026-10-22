package com.yinfires.icecore.mixin;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import com.github.ysbbbbbb.kaleidoscopetavern.crafting.container.BarrelRecipeContainer;
import com.github.ysbbbbbb.kaleidoscopetavern.crafting.recipe.BarrelRecipe;
import java.util.Optional;

/** Native success callback is retained; an unmatched batch must never become vinegar. */
@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopetavern.blockentity.brew.BarrelBlockEntity", remap = false)
public abstract class TavernInvalidRecipeMixin {
    @Shadow @Final private ItemStackHandler ingredient;
    @Shadow @Final private ItemStackHandler output;
    @Shadow @Final private FluidTank fluid;
    @Shadow private ResourceLocation recipeId;
    @Shadow private int brewLevel;
    @Shadow private int brewTime;
    @Shadow private boolean open;
    @Unique private CompoundTag icecore$batch;
    @Unique private Object icecore$manager;
    @Unique private boolean icecore$invalidBatch;
    @Shadow private void transform(Level level,net.minecraft.core.BlockPos pos,
            net.minecraft.world.item.crafting.Ingredient carrier,net.minecraft.world.item.ItemStack result) {
        throw new AssertionError();
    }

    @Redirect(method = "tick(Lnet/minecraft/world/level/Level;)V", at = @At(value = "INVOKE",
            target = "Ljava/util/Optional;ifPresentOrElse(Ljava/util/function/Consumer;Ljava/lang/Runnable;)V"), remap = false)
    private <T> void icecore$validBatchOnly(Optional<T> recipe, java.util.function.Consumer<T> success, Runnable invalid) {
        recipe.ifPresent(value->{
            icecore$batch=new CompoundTag();
            icecore$batch.put("items",ingredient.serializeNBT());
            icecore$batch.put("fluid",fluid.writeToNBT(new CompoundTag()));
            if(value instanceof BarrelRecipe barrel)
                icecore$batch.putString("carrier",barrel.carrier().toJson().toString());
            icecore$invalidBatch=false;
            success.accept(value);
        });
    }

    @Inject(method="tick(Lnet/minecraft/world/level/Level;)V",at=@At("HEAD"),cancellable=true)
    private void icecore$reloadBatch(Level level,CallbackInfo ci) {
        if(level.isClientSide)return;
        if(brewLevel<1 || icecore$batch==null)return;
        var recipe=recipeId==null?null:level.getRecipeManager().byKey(recipeId).orElse(null);
        Object manager=recipe==null?level.getRecipeManager():recipe;
        if(manager==icecore$manager) {if(icecore$invalidBatch)ci.cancel();return;}
        icecore$manager=manager;
        var stored=new ItemStackHandler(4);stored.deserializeNBT(icecore$batch.getCompound("items"));
        var liquid=new FluidTank(4000);liquid.readFromNBT(icecore$batch.getCompound("fluid"));
        if(recipe instanceof BarrelRecipe barrel && barrel.matches(new BarrelRecipeContainer(stored,liquid),level)) {
            icecore$invalidBatch=false;
            icecore$batch.putString("carrier",barrel.carrier().toJson().toString());
            var result=barrel.result().copy();result.setCount(output.getStackInSlot(0).getCount());
            output.setStackInSlot(0,result);
            com.yinfires.icecore.workstation.ExternalWorkstation.call(this,"refresh");
            return;
        }
        int originalCount=16;
        for(int i=0;i<stored.getSlots();i++)if(!stored.getStackInSlot(i).isEmpty())
            originalCount=Math.min(originalCount,stored.getStackInSlot(i).getCount());
        // Partially collected old batches retain their stored product, never refund spent ingredients.
        if(output.getStackInSlot(0).getCount()!=originalCount) {
            icecore$invalidBatch=true;ci.cancel();return;
        }
        // A reload invalidated this batch: restore what was consumed and unlock the lid.
        ingredient.deserializeNBT(icecore$batch.getCompound("items"));
        fluid.readFromNBT(icecore$batch.getCompound("fluid"));
        output.setStackInSlot(0,net.minecraft.world.item.ItemStack.EMPTY);
        recipeId=null;brewLevel=0;brewTime=-1;open=true;icecore$batch=null;
        com.yinfires.icecore.workstation.ExternalWorkstation.call(this,"refresh");
    }

    @Inject(method="doTapExtract(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V",at=@At("HEAD"),cancellable=true)
    private void icecore$noReloadVinegar(Level level,net.minecraft.core.BlockPos tapPos,CallbackInfo ci) {
        // A partially collected batch cannot be refunded. Its remaining product retains
        // the last valid extraction contract, while invalid fermentation is stopped.
        if(icecore$invalidBatch && icecore$batch!=null && icecore$batch.contains("carrier")) {
            if(!output.getStackInSlot(0).isEmpty())transform(level,tapPos,
                net.minecraft.world.item.crafting.Ingredient.fromJson(com.google.gson.JsonParser.parseString(icecore$batch.getString("carrier"))),
                output.getStackInSlot(0).copyWithCount(1));
            ci.cancel();return;
        }
        if(recipeId==null || !(level.getRecipeManager().byKey(recipeId).orElse(null) instanceof BarrelRecipe))
            ci.cancel();
    }

    @Inject(method="m_183515_(Lnet/minecraft/nbt/CompoundTag;)V",at=@At("TAIL"))
    private void icecore$saveBatch(CompoundTag tag,CallbackInfo ci) {
        if(brewLevel>0 && icecore$batch!=null)tag.put("icecore_batch_inputs",icecore$batch.copy());
    }
    @Inject(method="m_142466_(Lnet/minecraft/nbt/CompoundTag;)V",at=@At("TAIL"))
    private void icecore$loadBatch(CompoundTag tag,CallbackInfo ci) {
        icecore$batch=tag.contains("icecore_batch_inputs")?tag.getCompound("icecore_batch_inputs").copy():null;
        icecore$manager=null;
        icecore$invalidBatch=false;
    }
}
