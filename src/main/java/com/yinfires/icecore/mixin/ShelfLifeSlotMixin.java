package com.yinfires.icecore.mixin;

import com.yinfires.icecore.spoilage.ShelfLife;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Slot.class)
public abstract class ShelfLifeSlotMixin {
    @Inject(method = "m_7993_()Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
    private void icecore$get(CallbackInfoReturnable<ItemStack> cir) {
        var container = ((Slot)(Object)this).container;
        if (container instanceof net.minecraft.world.entity.player.Inventory inventory) ShelfLife.player(cir.getReturnValue(), inventory.player);
        else if (container instanceof com.yinfires.icecore.spoilage.PreservedContainer preserved && preserved.icecore$preserved()) ShelfLife.freeze(cir.getReturnValue());
        else if (!(container instanceof net.blay09.mods.balm.api.container.DefaultContainer)
                && !(container instanceof net.blay09.mods.balm.api.container.CombinedContainer)) ShelfLife.release(cir.getReturnValue());
    }
}
