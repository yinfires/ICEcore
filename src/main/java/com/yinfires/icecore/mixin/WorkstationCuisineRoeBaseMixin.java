package com.yinfires.icecore.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Completed roe rolls reuse the native California base and its subsequent topping stages. */
@Mixin(targets="dev.xkmc.youkaishomecoming.content.pot.table.food.FoodModelHelper",remap=false)
public abstract class WorkstationCuisineRoeBaseMixin {
    @Inject(method="find(Lnet/minecraft/world/item/ItemStack;)Ldev/xkmc/youkaishomecoming/content/pot/table/food/FoodTableItemHolder;",
        at=@At("HEAD"),cancellable=true)
    private static void icecore$preparedBase(ItemStack stack,CallbackInfoReturnable<Object> cir) {
        if(!ResourceLocation.parse("youkaisfeasts:roe_california_roll").equals(ForgeRegistries.ITEMS.getKey(stack.getItem())))return;
        var base=ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("youkaisfeasts:california_roll"));
        if(base!=null)cir.setReturnValue(com.yinfires.icecore.workstation.ExternalWorkstation.invokeStatic(
            "dev.xkmc.youkaishomecoming.content.pot.table.food.FoodModelHelper","find",new Class<?>[]{ItemStack.class},base.getDefaultInstance()));
    }
}
