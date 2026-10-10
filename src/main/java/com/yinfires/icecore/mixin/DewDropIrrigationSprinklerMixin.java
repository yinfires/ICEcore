package com.yinfires.icecore.mixin;

import com.yinfires.icecore.compat.dewdrop.DewDropSprinklers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Run after Forge commits its placement snapshots, never inside a cancelable transaction. */
@Mixin(targets = "net.minecraftforge.common.ForgeHooks", remap = false)
public abstract class DewDropIrrigationSprinklerMixin {
    @Inject(method = "onPlaceItemIntoWorld(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;", at = @At("HEAD"), remap = false)
    private static void icecore$capturePlacement(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        DewDropSprinklers.begin(context);
    }
    @Inject(method = "onPlaceItemIntoWorld(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;", at = @At("RETURN"), remap = false)
    private static void icecore$waterOnPlacement(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        DewDropSprinklers.placed(context, cir.getReturnValue().consumesAction());
    }
}
