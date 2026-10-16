package com.yinfires.icecore.mixin;

import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.PotBlockEntity", remap = false)
public abstract class WorkstationPotMixin {
    @Shadow private int currentTick;
    @Shadow private int status;
    @Shadow private ItemStack result;
    @Shadow private Ingredient carrier;
    @Redirect(method = "tickPutIngredient(Lnet/minecraft/world/level/Level;Lnet/minecraft/util/RandomSource;)V",
        at = @At(value = "INVOKE", target = "Lcom/github/ysbbbbbb/kaleidoscopecookery/blockentity/kitchen/PotBlockEntity;startCooking(Lnet/minecraft/world/level/Level;)V"), remap = false)
    private void icecore$manualStart(com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.PotBlockEntity pot, Level level) { /* Only onShovelHit may start cooking. */ }
    @Inject(method = "tickBurnt(Lnet/minecraft/world/level/Level;Lnet/minecraft/util/RandomSource;)V", at = @At("HEAD"), cancellable = true)
    private void icecore$keepCharcoal(Level level, RandomSource random, CallbackInfo ci) {
        if (currentTick != 0) return;
        result = new ItemStack(Items.CHARCOAL, 1 + random.nextInt(3)); carrier = Ingredient.EMPTY;
        status = 2; currentTick = -1;
        com.yinfires.icecore.workstation.ExternalWorkstation.call(this, "refresh"); ci.cancel();
    }
}
