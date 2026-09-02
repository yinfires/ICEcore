package com.yinfires.icecore.mixin;

import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.deco.TextBlockEntity;
import com.yinfires.icecore.compat.cozycafe.board.CozyCafeBoardService;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {"com.github.ysbbbbbb.kaleidoscopetavern.block.deco.ChalkboardBlock",
        "com.github.ysbbbbbb.kaleidoscopetavern.block.deco.SandwichBoardBlock"}, remap = false)
public abstract class TavernBoardPlacementMixin {
    @Inject(method = "m_6402_(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("RETURN"), remap = false)
    private void icecore$placeBoundBoard(Level level, BlockPos pos, BlockState state, LivingEntity placer,
                                         ItemStack stack, CallbackInfo ci) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof TextBlockEntity board) CozyCafeBoardService.place(level, pos, stack, board);
    }
}
