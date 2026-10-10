package com.yinfires.icecore.compat.dewdrop;

import com.yinfires.icecore.farming.SeedBagCrops;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Resolves only registered seed-bag plants; climbing lookup is bounded by rootOf. */
public final class DewDropIrrigation {
    private DewDropIrrigation() {}

    public static BlockPos soil(Level level, BlockPos hit) {
        if (hit == null || !level.hasChunkAt(hit)) return null;
        BlockState state = level.getBlockState(hit);
        if (state.getBlock() instanceof FarmBlock) return hit;
        SeedBagCrops.Crop crop = SeedBagCrops.find(state.getBlock());
        if (crop == null) return null;
        BlockPos root = SeedBagCrops.rootOf(crop, level, hit, state);
        if (root == null || !level.hasChunkAt(root.below())) return null;
        return level.getBlockState(root.below()).getBlock() instanceof FarmBlock ? root.below() : null;
    }
}
