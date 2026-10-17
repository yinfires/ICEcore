package com.yinfires.icecore.fridge;

import net.blay09.mods.cookingforblockheads.block.FridgeBlock;
import net.blay09.mods.cookingforblockheads.tile.FridgeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.core.Direction;

/** Native refrigerator behavior with an inseparable, explicitly paired two-block shell. */
public final class IntegratedFridgeBlock extends FridgeBlock {
    public IntegratedFridgeBlock() {
        registerDefaultState(defaultBlockState().setValue(MODEL_TYPE, FridgeModelType.LARGE_LOWER));
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context).setValue(MODEL_TYPE, FridgeModelType.LARGE_LOWER);
    }

    // The native fridge supports floating placement; exclude its independent stacking limits.
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return !level.isOutsideBuildHeight(pos);
    }

    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState adjacent,
            LevelAccessor level, BlockPos pos, BlockPos adjacentPos) {
        // Do not return AIR here: removing during shape propagation bypasses the drop transaction.
        return state;
    }

    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState previous, boolean moving) {
        if (!level.isClientSide && !level.restoringBlockSnapshots) level.scheduleTick(pos, this, 1);
    }

    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor,
            BlockPos neighborPos, boolean moving) {
        if (!level.isClientSide && !level.restoringBlockSnapshots) level.scheduleTick(pos, this, 1);
    }

    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!FridgePairing.matches(state, level.getBlockState(FridgePairing.partnerPos(state, pos)))) {
            level.destroyBlock(pos, true);
        }
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        // BlockStateTag must not turn the lower half into SMALL/UPPER or desynchronize orientation.
        BlockState lower = state.setValue(MODEL_TYPE, FridgeModelType.LARGE_LOWER);
        level.setBlock(pos, lower, 2);
        level.setBlock(pos.above(), lower.setValue(MODEL_TYPE, FridgeModelType.LARGE_UPPER), 2);
        super.setPlacedBy(level, pos, lower, placer, stack);
        super.setPlacedBy(level, pos.above(), lower.setValue(MODEL_TYPE, FridgeModelType.LARGE_UPPER), placer, stack);
    }

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (state.is(replacement.getBlock())) return;
        if (!level.isClientSide && !level.restoringBlockSnapshots) {
            BlockPos partner = FridgePairing.partnerPos(state, pos);
            BlockState other = level.getBlockState(partner);
            // Drop this half's own 27 slots, never getCombinedContainer(). Raw upgrade flags avoid
            // upper-half getters resolving to the same base and returning upgrades twice.
            if (level.getBlockEntity(pos) instanceof FridgeBlockEntity fridge) {
                if (level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS))
                    fridge.dropItems(level, pos);
                fridge.getContainer().clearContent();
                if (level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS)) {
                    if (fridge.hasIceUpgrade) Block.popResource(level, pos,
                            new ItemStack(net.blay09.mods.cookingforblockheads.item.ModItems.iceUnit));
                    if (fridge.hasPreservationUpgrade) Block.popResource(level, pos,
                            new ItemStack(net.blay09.mods.cookingforblockheads.item.ModItems.preservationChamber));
                }
                fridge.hasIceUpgrade = false;
                fridge.hasPreservationUpgrade = false;
            }
            // The caller handles this half's block loot. Remove the partner without a second block
            // loot call; its onRemove still drops its own inventory and removes its block entity.
            if (FridgePairing.matches(state, other)) level.setBlock(partner, Blocks.AIR.defaultBlockState(), 3);
            level.updateNeighbourForOutputSignal(pos, this);
        }
        // Deliberately bypass native onRemove, which downgrades neighbours and drops upgrades again.
        level.removeBlockEntity(pos);
    }

    @Override public PushReaction getPistonPushReaction(BlockState state) { return PushReaction.BLOCK; }
}
