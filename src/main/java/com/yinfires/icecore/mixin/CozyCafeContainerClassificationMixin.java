package com.yinfires.icecore.mixin;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep native bowl classification independent of the serving permission decision. */
@Mixin(targets="io.github.chakyl.cozycafe.util.FoodClassificationUtils",remap=false)
public abstract class CozyCafeContainerClassificationMixin {
    @org.spongepowered.asm.mixin.Unique private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> icecore$managed=
        net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
            net.minecraft.resources.ResourceLocation.parse("icecore:recipe_manager/1790175849511"));
    @org.spongepowered.asm.mixin.Unique private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> icecore$base=
        net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
            net.minecraft.resources.ResourceLocation.parse("icecore:recipe_manager/food_base_carrier"));
    @Inject(method="dropsBowl(Lnet/minecraft/world/item/ItemStack;)Z",at=@At("HEAD"),cancellable=true)
    private static void icecore$vessel(ItemStack stack,CallbackInfoReturnable<Boolean> cir){
        if(stack.is(icecore$managed) && stack.is(icecore$base))
            cir.setReturnValue(false);
    }
}
