package com.yinfires.icecore.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import snownee.jade.api.view.FluidView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep the real fluid bar, using the vanilla milk name without a mod-origin suffix. */
@Mixin(value=FluidView.class,remap=false)
public abstract class WorkstationJuicerMilkJadeMixin {
    @Inject(method="readDefault(Lnet/minecraft/nbt/CompoundTag;)Lsnownee/jade/api/view/FluidView;",at=@At("RETURN"))
    private static void icecore$milk(CompoundTag tag,CallbackInfoReturnable<FluidView> cir) {
        if(cir.getReturnValue()!=null && tag.getString("fluid").equals("kaleidoscope_fragrantorchard:milk_still"))
            cir.getReturnValue().fluidName=Component.translatable("fluid_type.minecraft.milk");
    }
}
