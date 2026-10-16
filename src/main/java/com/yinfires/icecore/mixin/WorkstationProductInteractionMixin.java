package com.yinfires.icecore.mixin;

import com.yinfires.icecore.workstation.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets = {
    "com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.PotBlockEntity",
    "com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.StockpotBlockEntity"
}, remap = false)
public abstract class WorkstationProductInteractionMixin {
    @Inject(method = "takeOutProduct(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void icecore$sharedProduct(Level level, LivingEntity user, ItemStack held, CallbackInfoReturnable<Boolean> cir) {
        if (!(user instanceof net.minecraft.world.entity.player.Player)) return;
        var entity = (BlockEntity)(Object)this;
        if (entity instanceof com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.StockpotBlockEntity pot && pot.hasLid()) { cir.setReturnValue(false); return; }
        WorkstationAdapter adapter = ExternalWorkstationAdapters.create(entity);
        if (adapter.output().isEmpty()) { cir.setReturnValue(false); return; }
        if (!level.isClientSide && user instanceof ServerPlayer player) WorkstationInteractionService.interact(adapter, player, InteractionHand.MAIN_HAND);
        cir.setReturnValue(true);
    }
}
