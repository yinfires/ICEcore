package com.yinfires.icecore.mixin;

import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopecookery.item.TeapotItem", remap = false)
public abstract class WorkstationTeapotItemMixin extends BlockItem {
    protected WorkstationTeapotItemMixin(Block block, Item.Properties properties) { super(block, properties); }
    @Inject(method = "m_6225_(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;", at = @At("HEAD"), cancellable = true)
    private void icecore$place(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) { cir.setReturnValue(super.useOn(context)); }
}
