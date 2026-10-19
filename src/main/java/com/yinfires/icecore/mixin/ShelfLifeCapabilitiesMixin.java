package com.yinfires.icecore.mixin;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.yinfires.icecore.spoilage.ExpiredFoodItem;

/** Never re-serialize the invalidated original capability dispatcher during stack copies. */
@Mixin(value = net.minecraftforge.common.capabilities.CapabilityProvider.class, remap = false)
public abstract class ShelfLifeCapabilitiesMixin {
    @Inject(method = "getCapability", at = @At("HEAD"), cancellable = true)
    private <T> void icecore$capability(net.minecraftforge.common.capabilities.Capability<T> capability,
            net.minecraft.core.Direction side,
            CallbackInfoReturnable<net.minecraftforge.common.util.LazyOptional<T>> cir) {
        if ((Object)this instanceof net.minecraft.world.item.ItemStack stack) {
            com.yinfires.icecore.spoilage.ShelfLife.observe(stack);
            if (stack.getItem() instanceof ExpiredFoodItem) cir.setReturnValue(net.minecraftforge.common.util.LazyOptional.empty());
        }
    }
    @Inject(method = "serializeCaps", at = @At("HEAD"), cancellable = true)
    private void icecore$serialize(CallbackInfoReturnable<net.minecraft.nbt.CompoundTag> cir) {
        if ((Object)this instanceof net.minecraft.world.item.ItemStack stack && stack.getItem() instanceof ExpiredFoodItem)
            cir.setReturnValue(null);
    }
}
