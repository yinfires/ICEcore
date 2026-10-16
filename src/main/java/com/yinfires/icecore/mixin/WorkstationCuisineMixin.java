package com.yinfires.icecore.mixin;

import com.yinfires.icecore.workstation.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(targets = "dev.xkmc.youkaishomecoming.content.pot.table.board.CuisineBoardBlock", remap = false)
public abstract class WorkstationCuisineMixin {
    // This point is after performToolAction and before the native empty-hand retrieval branch.
    @Inject(method = "onClick(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE", target = "Ldev/xkmc/youkaishomecoming/content/pot/table/board/CuisineBoardBlockEntity;addItem(Lnet/minecraft/world/item/ItemStack;)I"), cancellable = true)
    private void icecore$inputOnly(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) return; // All native retrieval remains reachable.
        // Native fire tools and fluid slip bottles are recipe steps, not ordinary ingredients.
        if ((Boolean)ExternalWorkstation.invokeStatic("dev.xkmc.youkaishomecoming.content.pot.table.item.SearHelper", "isFireSource", new Class<?>[]{ItemStack.class}, held)
                || (Boolean)ExternalWorkstation.invokeStatic("dev.xkmc.youkaishomecoming.content.item.fluid.SlipBottleItem", "isSlipContainer", new Class<?>[]{ItemStack.class}, held)) return;
        Object board = level.getBlockEntity(pos);
        int cost = (Integer)ExternalWorkstation.invoke(board, "addItem", new Class<?>[]{ItemStack.class}, held.copyWithCount(1));
        if (cost > 0) {
            if (!level.isClientSide) {
                ItemStack empty = WorkstationContainerInteraction.requiredEmptyContainer(held, WorkstationContainerInteraction.FluidContainerMode.STORED_AS_ITEM);
                if (!empty.isEmpty()) WorkstationContainerInteraction.exchange(player, hand, held, empty);
                else if (!player.getAbilities().instabuild) held.shrink(1);
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.ITEM_FRAME_ADD_ITEM, net.minecraft.sounds.SoundSource.BLOCKS, .8F, 1.1F);
            }
            cir.setReturnValue(InteractionResult.sidedSuccess(level.isClientSide));
        } else cir.setReturnValue(InteractionResult.PASS);
    }
}
