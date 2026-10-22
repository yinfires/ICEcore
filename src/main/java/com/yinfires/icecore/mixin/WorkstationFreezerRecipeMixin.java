package com.yinfires.icecore.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets="com.bmt.kaleidoscope_world_liquor.crafting.FreezerRecipe",remap=false)
public abstract class WorkstationFreezerRecipeMixin {
    @Shadow @Final private ResourceLocation id;
    @Shadow @Final private FluidStack inputFluid;
    @Inject(method="matches(Lnet/minecraftforge/fluids/FluidStack;Lnet/minecraft/core/NonNullList;Lnet/minecraft/world/level/Level;)Z",
        at=@At("HEAD"),cancellable=true)
    private void icecore$exactInputs(FluidStack fluid,NonNullList<ItemStack> items,Level level,CallbackInfoReturnable<Boolean> cir) {
        if(!id.getNamespace().equals("icecore") || !id.getPath().startsWith("recipe_manager/"))return;
        if(inputFluid.isEmpty()?!fluid.isEmpty():!inputFluid.isFluidEqual(fluid)||inputFluid.getAmount()!=fluid.getAmount()) {
            cir.setReturnValue(false);return;
        }
        for(var stack:items)if(!stack.isEmpty()&&stack.getCount()!=1){cir.setReturnValue(false);return;}
    }
}
