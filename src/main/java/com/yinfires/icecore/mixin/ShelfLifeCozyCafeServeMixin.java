package com.yinfires.icecore.mixin;

import com.yinfires.icecore.spoilage.ShelfLife;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "io.github.chakyl.cozycafe.blockentities.CafeMenuBlockEntity", remap = false)
public abstract class ShelfLifeCozyCafeServeMixin {
    @Inject(method = "handleServe", at = @At("HEAD"))
    private void icecore$serve(net.minecraft.core.BlockPos pos, net.minecraft.world.entity.player.Player player,
                               net.minecraft.world.item.ItemStack stack, CallbackInfo ci) { ShelfLife.settle(stack); }
}
