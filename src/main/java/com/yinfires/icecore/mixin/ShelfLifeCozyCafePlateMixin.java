package com.yinfires.icecore.mixin;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep the occupied hand slot until native food and dirty-plate delivery finish. */
@Pseudo
@Mixin(targets = "io.github.chakyl.cozycafe.item.ServingPlateItem", remap = false)
public abstract class ShelfLifeCozyCafePlateMixin {
    @Redirect(method = "m_7203_(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResultHolder;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;m_41774_(I)V"))
    private void icecore$deferConsumption(ItemStack plate, int amount) { }

    @Inject(method = "m_7203_(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResultHolder;", at = @At("RETURN"))
    private void icecore$consumeAfterDelivery(net.minecraft.world.level.Level level,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand,
            CallbackInfoReturnable<net.minecraft.world.InteractionResultHolder<ItemStack>> cir) {
        if (!level.isClientSide && cir.getReturnValue().getResult().consumesAction())
            cir.getReturnValue().getObject().shrink(1);
    }
}
