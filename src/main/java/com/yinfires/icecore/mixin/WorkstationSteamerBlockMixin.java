package com.yinfires.icecore.mixin;
import com.yinfires.icecore.workstation.SteamerControl;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.SteamerBlock", remap = false)
public abstract class WorkstationSteamerBlockMixin {
    @Inject(method = "m_6227_(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;", at = @At("HEAD"), cancellable = true)
    private void icecore$noStackUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (player.getItemInHand(hand).getItem() instanceof com.github.ysbbbbbb.kaleidoscopecookery.item.SteamerItem)
            cir.setReturnValue(InteractionResult.CONSUME);
    }
    // Sneaking bypasses Block.use, so reject stacking at the native placement entry too.
    @Inject(method = "m_5573_(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/level/block/state/BlockState;", at = @At("HEAD"), cancellable = true)
    private void icecore$noStackPlacement(net.minecraft.world.item.context.BlockPlaceContext context,
            CallbackInfoReturnable<BlockState> cir) {
        var block = (net.minecraft.world.level.block.Block)(Object)this;
        var pos = context.getClickedPos();
        var level = context.getLevel();
        if (level.getBlockState(pos).is(block) || level.getBlockState(pos.below()).is(block))
            cir.setReturnValue(null);
    }
    @Inject(method = "m_6227_(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;",
            at = @At(value = "INVOKE", target = "Lcom/github/ysbbbbbb/kaleidoscopecookery/api/blockentity/ISteamer;takeFood(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Z"), cancellable = true)
    private void icecore$emptyHandOnly(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (hand != InteractionHand.MAIN_HAND || !player.getItemInHand(hand).isEmpty()) cir.setReturnValue(InteractionResult.CONSUME);
    }
    @Inject(method = "m_6227_(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;", at = @At("RETURN"))
    private void icecore$invalidateLid(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) { SteamerControl.lidChanged(level, pos); }
}
