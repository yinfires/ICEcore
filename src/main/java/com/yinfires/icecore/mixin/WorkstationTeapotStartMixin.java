package com.yinfires.icecore.mixin;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.TeapotBlockEntity;
import com.yinfires.icecore.workstation.ExternalWorkstation;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.TeapotBlockEntity", remap = false)
public abstract class WorkstationTeapotStartMixin {
    @Unique private boolean icecore$starting;
    @Unique private long icecore$startedTick = Long.MIN_VALUE;
    @Unique private boolean icecore$start(Level level) {
        var self = (TeapotBlockEntity)(Object)this;
        if (level.isClientSide || icecore$starting || ExternalWorkstation.number(self, "status") != 0
                || ((ItemStack)ExternalWorkstation.get(self, "input")).isEmpty() || !self.hasHeatSource(level)) return false;
        var fluid = (net.minecraft.resources.ResourceLocation)ExternalWorkstation.get(self, "teaFluidId");
        if (fluid.equals(com.github.ysbbbbbb.kaleidoscopecookery.crafting.serializer.TeapotRecipeSerializer.EMPTY_TEA_FLUID)) return false;
        // Reuse the real recipe/fallback initialization, bypassing only its poll and preparation delay.
        ExternalWorkstation.set(self, "currentTick", -1);
        icecore$starting = true;
        icecore$startedTick = level.getGameTime();
        try { self.tick(level); } finally { icecore$starting = false; }
        return true;
    }
    @Inject(method = "addIngredient(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"))
    private void icecore$afterInput(Level level, LivingEntity user, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) icecore$start(level);
    }
    @Inject(method = "tick(Lnet/minecraft/world/level/Level;)V", at = @At("HEAD"), cancellable = true)
    private void icecore$onReady(Level level, CallbackInfo ci) {
        if (!icecore$starting && (icecore$startedTick == level.getGameTime() || icecore$start(level))) ci.cancel();
    }
    @Redirect(method = "tick(Lnet/minecraft/world/level/Level;)V", at = @At(value = "INVOKE", target = "Ljava/lang/Math;floorMod(JI)I", ordinal = 0))
    private int icecore$immediatePoll(long offset, int interval) { return icecore$starting ? 0 : Math.floorMod(offset, interval); }
}
