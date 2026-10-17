package com.yinfires.icecore.mixin;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.TeapotBlockEntity;
import com.yinfires.icecore.workstation.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.TeapotBlockEntity", remap = false)
public abstract class WorkstationTeapotIngredientPickupMixin {
    @Inject(method = "removeIngredient(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void icecore$inventory(Level level, LivingEntity user, CallbackInfoReturnable<Boolean> cir) {
        if (!(user instanceof Player player)) return;
        var self = (TeapotBlockEntity)(Object)this;
        cir.setReturnValue(false);
        if (level.isClientSide || self.getStatus() != 0 || self.getInput().isEmpty() || !player.getMainHandItem().isEmpty()) return;
        if (!WorkstationInventoryTransfer.giveOne(player, self.getInput())) return;
        self.getInput().shrink(1); ExternalWorkstation.call(self, "refresh"); cir.setReturnValue(true);
    }
}
