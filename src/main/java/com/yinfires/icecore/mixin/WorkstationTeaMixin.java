package com.yinfires.icecore.mixin;

import com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems;
import com.github.ysbbbbbb.kaleidoscopecookery.item.TeacupItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopecookery.block.drink.TeacupBlock", remap = false)
public abstract class WorkstationTeaMixin {
    @Inject(method = "m_6227_(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;", at = @At("HEAD"), cancellable = true)
    private void icecore$noHeldPickup(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        var held = player.getMainHandItem();
        if (held.isEmpty()) return;
        if (hand == InteractionHand.MAIN_HAND) {
            var cup = (com.github.ysbbbbbb.kaleidoscopecookery.block.drink.TeacupBlock)state.getBlock();
            int cups = state.getValue(cup.getCupCountProperty());
            if (held.is(ModItems.TEAPOT.get())) {
                var tea = com.github.ysbbbbbb.kaleidoscopecookery.item.TeapotItem.getPourOut(held);
                if (!tea.isEmpty() && tea.getItem() == cup.asItem() && state.getValue(cup.getTeaCountProperty()) < cups) return;
            }
            if (held.is(ModItems.EMPTY_CUP.get()) && cups < cup.getMaxCount()) return;
            if (held.getItem() instanceof TeacupItem item && item.getBlock() == cup) return;
        }
        cir.setReturnValue(InteractionResult.CONSUME);
    }
}
