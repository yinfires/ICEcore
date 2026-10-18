package com.yinfires.icecore.mixin;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.SteamerBlockEntity;
import com.yinfires.icecore.workstation.SteamerControl;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.SteamerBlockEntity", remap = false)
public abstract class WorkstationSteamerMixin {
    @Unique private boolean icecore$wasClosed;
    @Unique private boolean icecore$initialized;
    @Unique private boolean icecore$closed(Level level) {
        var self = (SteamerBlockEntity)(Object)this;
        return SteamerControl.closed(level, self.getBlockPos());
    }
    @Inject(method = "tick(Lnet/minecraft/world/level/Level;)V", at = @At("HEAD"))
    private void icecore$lidTransitions(Level level, CallbackInfo ci) {
        if (level.isClientSide) return;
        var self = (SteamerBlockEntity)(Object)this;
        boolean closed = icecore$closed(level);
        if (!icecore$initialized) {
            icecore$wasClosed = closed; icecore$initialized = true;
            int[] times = self.getCookingTime();
            for (int i = 0; i < times.length; i++) if (times[i] > 0) times[i] = 600;
        }
        if (!closed) SteamerControl.resetUnfinished(self);
        else if (!icecore$wasClosed) SteamerControl.uniformBatch(self);
        icecore$wasClosed = closed;
    }
    @Inject(method = "cookingTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lcom/github/ysbbbbbb/kaleidoscopecookery/blockentity/kitchen/SteamerBlockEntity;)V", at = @At("HEAD"), cancellable = true)
    private void icecore$closedOnly(Level level, BlockPos pos, BlockState state, SteamerBlockEntity steamer, CallbackInfo ci) {
        ci.cancel();
        if (!icecore$closed(level)) return;
        int[] times = steamer.getCookingTime(), progress = steamer.getCookingProgress();
        var items = steamer.getItems();
        boolean changed = false;
        for (int i = 0; i < times.length; i++) {
            if (times[i] <= 0 || items.get(i).isEmpty()) continue;
            progress[i]++;
            if (progress[i] < times[i]) continue;
            var input = new net.minecraft.world.SimpleContainer(items.get(i));
            var recipe = steamer.getSteamerRecipe(level, items.get(i));
            if (recipe.isPresent()) items.set(i, recipe.get().assemble(input, level.registryAccess()));
            times[i] = -1; changed = true;
        }
        if (changed) com.yinfires.icecore.workstation.ExternalWorkstation.changed(steamer);
    }
    @Inject(method = "cooldownTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lcom/github/ysbbbbbb/kaleidoscopecookery/blockentity/kitchen/SteamerBlockEntity;)V", at = @At("HEAD"), cancellable = true)
    private void icecore$pauseWithoutHeat(Level level, BlockPos pos, BlockState state, SteamerBlockEntity steamer, CallbackInfo ci) { ci.cancel(); }
    @Inject(method = "placeFood(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void icecore$noClosedInput(Level level, LivingEntity user, ItemStack item, CallbackInfoReturnable<Boolean> cir) {
        SteamerControl.invalidate(level);
        if (icecore$closed(level)) cir.setReturnValue(false);
    }
    @Inject(method = "takeFood(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void icecore$noClosedOutput(Level level, LivingEntity user, CallbackInfoReturnable<Boolean> cir) {
        SteamerControl.invalidate(level);
        if (icecore$closed(level)) { cir.setReturnValue(false); return; }
        if (!(user instanceof net.minecraft.world.entity.player.Player player)) return;
        cir.setReturnValue(false);
        if (!player.getMainHandItem().isEmpty() || level.isClientSide) return;
        var self = (SteamerBlockEntity)(Object)this;
        BlockPos pos = self.getBlockPos(), above = pos.above();
        if (level.getBlockState(above).isFaceSturdy(level, above, net.minecraft.core.Direction.DOWN)) return;
        var state = self.getBlockState();
        boolean half = state.getValue(com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.SteamerBlock.HALF);
        int end = half ? 4 : 8;
        boolean changed = false;
        var items = self.getItems();
        for (int i = 0; i < end; i++) {
            ItemStack stored = items.get(i);
            if (stored.isEmpty()) continue;
            ItemStack remainder = com.yinfires.icecore.workstation.WorkstationInventoryTransfer.insert(player, stored, false);
            int moved = stored.getCount() - remainder.getCount();
            if (moved <= 0) continue;
            stored.shrink(moved); changed = true;
            if (stored.isEmpty()) { items.set(i, ItemStack.EMPTY); self.getCookingTime()[i] = 0; self.getCookingProgress()[i] = 0; }
        }
        if (changed) { com.yinfires.icecore.workstation.ExternalWorkstation.changed(self); cir.setReturnValue(true); }
    }
}
