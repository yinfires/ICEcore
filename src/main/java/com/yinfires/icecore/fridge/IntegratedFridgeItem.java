package com.yinfires.icecore.fridge;

import net.blay09.mods.cookingforblockheads.block.FridgeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.BlockSnapshot;

public final class IntegratedFridgeItem extends BlockItem {
    public IntegratedFridgeItem(IntegratedFridgeBlock block) { super(block, new Item.Properties()); }

    @Override protected boolean canPlace(BlockPlaceContext context, BlockState lower) {
        Level level = context.getLevel();
        BlockPos upperPos = context.getClickedPos().above();
        if (level.isOutsideBuildHeight(upperPos) || !level.getWorldBorder().isWithinBounds(upperPos)) return false;
        BlockPlaceContext upperContext = BlockPlaceContext.at(context, upperPos, Direction.UP);
        Player player = context.getPlayer();
        if (player != null && (!level.mayInteract(player, context.getClickedPos())
                || !level.mayInteract(player, upperPos)
                || !player.mayUseItemAt(upperPos, Direction.UP, context.getItemInHand()))) return false;
        BlockState upper = lower.setValue(FridgeBlock.MODEL_TYPE, FridgeBlock.FridgeModelType.LARGE_UPPER);
        return level.getBlockState(upperPos).canBeReplaced(upperContext)
                && super.canPlace(context, lower) && super.canPlace(upperContext, upper);
    }

    @Override protected boolean placeBlock(BlockPlaceContext context, BlockState lower) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockSnapshot original = BlockSnapshot.create(level.dimension(), level, pos, 11);
        if (!super.placeBlock(context, lower)) return false;
        if (level.setBlock(pos.above(), lower.setValue(FridgeBlock.MODEL_TYPE,
                FridgeBlock.FridgeModelType.LARGE_UPPER), 11)) return true;
        // Forge normally captures both writes and restores them when MultiPlace is cancelled.
        // This rollback covers the earlier exceptional case where the second write itself fails.
        boolean restoring = level.restoringBlockSnapshots;
        level.restoringBlockSnapshots = true;
        try { original.restore(true, false); }
        finally { level.restoringBlockSnapshots = restoring; }
        return false;
    }
}
