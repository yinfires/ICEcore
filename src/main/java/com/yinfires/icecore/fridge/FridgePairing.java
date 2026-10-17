package com.yinfires.icecore.fridge;

import net.blay09.mods.cookingforblockheads.block.FridgeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Pair identity is structural: the same block, opposite halves and matching orientation. */
public final class FridgePairing {
    public static boolean integrated(BlockState state) {
        return state.getBlock() instanceof IntegratedFridgeBlock;
    }

    public static boolean upper(BlockState state) {
        return state.getValue(FridgeBlock.MODEL_TYPE) == FridgeBlock.FridgeModelType.LARGE_UPPER;
    }

    public static BlockPos partnerPos(BlockState state, BlockPos pos) {
        return upper(state) ? pos.below() : pos.above();
    }

    public static boolean matches(BlockState state, BlockState other) {
        return integrated(state) && other.is(state.getBlock())
                && state.getValue(FridgeBlock.MODEL_TYPE) != FridgeBlock.FridgeModelType.SMALL
                && other.getValue(FridgeBlock.MODEL_TYPE) != FridgeBlock.FridgeModelType.SMALL
                && upper(state) != upper(other)
                && state.getValue(FridgeBlock.FACING) == other.getValue(FridgeBlock.FACING)
                && state.getValue(FridgeBlock.FLIPPED).equals(other.getValue(FridgeBlock.FLIPPED));
    }

    private FridgePairing() {}
}
