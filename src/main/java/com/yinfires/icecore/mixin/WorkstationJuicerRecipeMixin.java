package com.yinfires.icecore.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

@Mixin(targets = "com.bmt.kaleidoscope_fragrantorchard.crafting.JuicerRecipe", remap = false)
public abstract class WorkstationJuicerRecipeMixin {
    @Shadow @Final private ResourceLocation id;
    @Shadow @Final private List<ItemStack> inputItems;
    @Shadow @Final private FluidStack requiredInputFluid;
    @Inject(method = "matchesMixedMode(Lnet/minecraftforge/fluids/FluidStack;Lnet/minecraft/core/NonNullList;)Z",at=@At("HEAD"),cancellable=true)
    private void icecore$exactBatch(FluidStack fluid, NonNullList<ItemStack> items, CallbackInfoReturnable<Boolean> cir) {
        if(!id.getNamespace().equals("icecore")||!id.getPath().startsWith("recipe_manager/"))return;
        if(requiredInputFluid.isEmpty() ? !fluid.isEmpty() : !fluid.isFluidEqual(requiredInputFluid)||fluid.getAmount()!=requiredInputFluid.getAmount()) {cir.setReturnValue(false);return;}
        int total=0,expected=0;
        for(ItemStack required:inputItems){
            int found=0,requiredTotal=0;expected+=required.getCount();
            // The native serializer supplies item IDs, not NBT predicates. Match the same
            // identity used by native finishCrafting; acquired food carries shelf-life NBT.
            for(ItemStack other:inputItems)if(ItemStack.isSameItem(required,other))requiredTotal+=other.getCount();
            for(ItemStack actual:items)if(ItemStack.isSameItem(required,actual))found+=actual.getCount();
            if(found!=requiredTotal){cir.setReturnValue(false);return;}
        }
        for(ItemStack actual:items){if(!actual.isEmpty()&&actual.getCount()!=1){cir.setReturnValue(false);return;}total+=actual.getCount();}cir.setReturnValue(total==expected);
    }
}
