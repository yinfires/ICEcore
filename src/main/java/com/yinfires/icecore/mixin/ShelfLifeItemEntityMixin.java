package com.yinfires.icecore.mixin;

import com.yinfires.icecore.spoilage.ShelfLife;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(ItemEntity.class)
public abstract class ShelfLifeItemEntityMixin {
    @Inject(method = "m_32045_(Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"))
    private void icecore$set(ItemStack stack, CallbackInfo ci) { ShelfLife.release(stack); }
}
