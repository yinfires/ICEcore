package com.yinfires.icecore.mixin;

import com.yinfires.icecore.farming.DailyClimbingCrop;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "dev.xkmc.youkaishomecoming.content.block.plant.rope.RopeLoggedCropBlock", remap = false)
public interface SeedBagCompatClimbingGrowthMixin extends DailyClimbingCrop {
    @Override @Invoker(value = "postGrowth", remap = false)
    void icecore$postGrowth(ServerLevel level, BlockPos pos, RandomSource random);
}
