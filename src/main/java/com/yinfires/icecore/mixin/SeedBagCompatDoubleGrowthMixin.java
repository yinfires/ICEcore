package com.yinfires.icecore.mixin;

import com.yinfires.icecore.farming.DailyDoubleCrop;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "dev.xkmc.youkaishomecoming.content.block.plant.DoubleCropBlock", remap = false)
public interface SeedBagCompatDoubleGrowthMixin extends DailyDoubleCrop {
    @Override @Invoker(value = "setGrowth", remap = false)
    void icecore$setGrowth(Level level, BlockPos pos, int age, int flags);
}
