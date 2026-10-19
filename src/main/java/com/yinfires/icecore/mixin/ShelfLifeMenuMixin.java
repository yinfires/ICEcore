package com.yinfires.icecore.mixin;

import com.yinfires.icecore.spoilage.ShelfLife;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(AbstractContainerMenu.class)
public abstract class ShelfLifeMenuMixin {
    @Inject(method = "m_246200_(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/inventory/ClickAction;Lnet/minecraft/world/inventory/Slot;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void icecore$manual(Player player, ClickAction action, Slot slot, ItemStack target, ItemStack source, CallbackInfoReturnable<Boolean> cir) {
        if (ShelfLife.manual(target, source, slot, player, action == ClickAction.SECONDARY)) cir.setReturnValue(true);
    }
    @Inject(method = "m_150399_(IILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V", at = @At("HEAD"))
    private void icecore$before(int slot, int button, ClickType type, Player player, CallbackInfo ci) {
        ShelfLife.player(((AbstractContainerMenu)(Object)this).getCarried(), player);
    }
}
