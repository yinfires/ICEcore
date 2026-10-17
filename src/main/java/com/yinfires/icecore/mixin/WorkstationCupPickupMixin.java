package com.yinfires.icecore.mixin;

import com.github.ysbbbbbb.kaleidoscopecookery.block.drink.*;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems;
import com.yinfires.icecore.workstation.WorkstationInventoryTransfer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets = {"com.github.ysbbbbbb.kaleidoscopecookery.block.drink.TeacupBlock",
        "com.github.ysbbbbbb.kaleidoscopecookery.block.drink.EmptyCupBlock"}, remap = false)
public abstract class WorkstationCupPickupMixin {
    @Inject(method = "m_6227_(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;", at = @At("HEAD"), cancellable = true)
    private void icecore$inventory(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (hand != InteractionHand.MAIN_HAND || !player.getMainHandItem().isEmpty()) return;
        cir.setReturnValue(InteractionResult.sidedSuccess(level.isClientSide));
        if (level.isClientSide) return;
        if (state.getBlock() instanceof TeacupBlock cup) {
            int cups = state.getValue(cup.getCupCountProperty()), tea = state.getValue(cup.getTeaCountProperty());
            boolean empty = cups > tea;
            if (cups <= 0 || !WorkstationInventoryTransfer.giveOne(player,
                    new ItemStack(empty ? ModItems.EMPTY_CUP.get() : cup.asItem()))) return;
            BlockState next = cups == 1 ? Blocks.AIR.defaultBlockState()
                    : state.setValue(cup.getCupCountProperty(), cups - 1).setValue(cup.getTeaCountProperty(), empty ? tea : tea - 1);
            level.setBlockAndUpdate(pos, next);
        } else {
            int cups = state.getValue(EmptyCupBlock.CUP_COUNT);
            if (cups <= 0 || !WorkstationInventoryTransfer.giveOne(player, new ItemStack(ModItems.EMPTY_CUP.get()))) return;
            level.setBlockAndUpdate(pos, cups == 1 ? Blocks.AIR.defaultBlockState() : state.setValue(EmptyCupBlock.CUP_COUNT, cups - 1));
        }
        level.playSound(null, pos, state.getSoundType(level, pos, player).getBreakSound(), net.minecraft.sounds.SoundSource.BLOCKS, 1F, 1F);
    }
}
