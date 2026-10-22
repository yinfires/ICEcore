package com.yinfires.icecore.mixin;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import dev.xkmc.youkaishomecoming.content.pot.ferment.FermentationDummyContainer;
import com.yinfires.icecore.workstation.ExternalWorkstation;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Managed quantities describe the whole sealed batch, including one-ingredient recipes. */
@Mixin(targets="dev.xkmc.youkaishomecoming.content.pot.ferment.SimpleFermentationRecipe",remap=false)
public abstract class WorkstationFermentationBatchMixin {
    @Shadow public FluidStack inputFluid;

    @Inject(method="matches(Ldev/xkmc/youkaishomecoming/content/pot/ferment/FermentationDummyContainer;Lnet/minecraft/world/level/Level;)Z",
            at=@At("HEAD"),cancellable=true)
    private void icecore$exactBatch(FermentationDummyContainer container, Level level,
                                    CallbackInfoReturnable<Boolean> cir) {
        var id=((Recipe<?>)(Object)this).getId();
        if(!id.getNamespace().equals("icecore") || !id.getPath().startsWith("recipe_manager/"))return;
        var tank=(IFluidHandler)ExternalWorkstation.call(container,"fluids");
        var actual=tank.getFluidInTank(0);
        if(inputFluid.isEmpty()!=actual.isEmpty() || !inputFluid.isEmpty()
                && (!inputFluid.isFluidEqual(actual) || inputFluid.getAmount()!=actual.getAmount())) {
            cir.setReturnValue(false);
            return;
        }
        var items=ExternalWorkstation.call(container,"items");
        @SuppressWarnings("unchecked")
        var stacks=(java.util.List<ItemStack>)ExternalWorkstation.call(items,"getAsList");
        for(var stack:stacks)
            if(!stack.isEmpty() && stack.getCount()!=1) {cir.setReturnValue(false);return;}
    }

    @Inject(method="isSimple()Z",at=@At("HEAD"),cancellable=true)
    private void icecore$wholeBatch(CallbackInfoReturnable<Boolean> cir) {
        var id=((Recipe<?>)(Object)this).getId();
        if(id.getNamespace().equals("icecore") && id.getPath().startsWith("recipe_manager/"))
            cir.setReturnValue(false);
    }
}
