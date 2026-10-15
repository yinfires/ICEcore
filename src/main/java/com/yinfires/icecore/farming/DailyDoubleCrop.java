package com.yinfires.icecore.farming;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface DailyDoubleCrop {
    void icecore$setGrowth(Level level, BlockPos pos, int age, int flags);
}
