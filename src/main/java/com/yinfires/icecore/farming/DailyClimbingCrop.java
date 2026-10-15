package com.yinfires.icecore.farming;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

public interface DailyClimbingCrop {
    void icecore$postGrowth(ServerLevel level, BlockPos pos, RandomSource random);
}
