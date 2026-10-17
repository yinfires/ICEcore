package com.yinfires.icecore.mixin;

import com.yinfires.icecore.workstation.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets = "vectorwing.farmersdelight.common.block.CuttingBoardBlock", remap = false)
public abstract class WorkstationCuttingBoardBlockMixin {
    @Inject(method = "m_6227_(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;", at = @At("HEAD"), cancellable = true)
    private void icecore$interact(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        var entity = level.getBlockEntity(pos);
        if (!(entity instanceof CuttingBoardAccess)) return;
        if (hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer serverPlayer)
            CuttingBoardInteraction.interact(entity, serverPlayer, hand);
        cir.setReturnValue(InteractionResult.sidedSuccess(level.isClientSide));
    }
    @Inject(method = "m_6810_(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)V", at = @At("HEAD"))
    private void icecore$dropProducts(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving, CallbackInfo ci) {
        if (level.isClientSide || state.is(next.getBlock())) return;
        if (level.getBlockEntity(pos) instanceof CuttingBoardAccess board) {
            for (var item : board.icecore$products()) if (!item.isEmpty()) Containers.dropItemStack(level, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, item);
            board.icecore$products().clear();
        }
    }
}
