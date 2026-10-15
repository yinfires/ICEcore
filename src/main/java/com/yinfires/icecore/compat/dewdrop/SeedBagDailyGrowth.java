package com.yinfires.icecore.compat.dewdrop;

import com.yinfires.icecore.farming.RiceGrowth;
import com.yinfires.icecore.farming.SeedBagCrops;
import com.yinfires.icecore.item.SeedBagDefinition.Shape;
import cool.bot.dewdropfarmland.utils.CropHandlerUtils;
import cool.bot.dewdropfarmland.tag.SturdyFarmlandBlockTags;
import com.yinfires.icecore.farming.DailyDoubleCrop;
import com.yinfires.icecore.farming.DailyClimbingCrop;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;

/** Only loaded by the optional Dew hook, after its soil/season/daily-window checks. */
public final class SeedBagDailyGrowth {
    private SeedBagDailyGrowth() {}

    public static boolean grow(BlockState state, ServerLevel level, BlockPos pos, BlockState soil, RandomSource random) {
        SeedBagCrops.Crop crop = SeedBagCrops.find(state.getBlock());
        if (crop == null) return false;
        if (state.is(SturdyFarmlandBlockTags.NOT_DAILY)) return true;
        BlockPos root = SeedBagCrops.rootOf(crop, level, pos, state);
        if (root == null || !root.equals(pos)) return true;
        if (crop.definition().shape() == Shape.SPLIT) {
            growRice(crop, state, level, pos, soil);
        } else if (crop.definition().shape() == Shape.CLIMBING) {
            // Snapshot the old vine: new segments are first eligible on the next daily pass.
            BlockPos cursor = pos;
            for (int i = 0; i < 64; i++) {
                BlockState segment = level.getBlockState(cursor);
                if (!segment.is(crop.root())) break;
                BlockPos above = cursor.above();
                boolean hadNext = level.getBlockState(above).is(crop.root());
                growAge(crop, segment, level, cursor, soil, random);
                if (!hadNext) break;
                cursor = above;
            }
        } else {
            growAge(crop, state, level, pos, soil, random);
        }
        return true;
    }

    private static void growAge(SeedBagCrops.Crop crop, BlockState state, ServerLevel level,
                                BlockPos pos, BlockState soil, RandomSource random) {
        int age = state.getValue(crop.age());
        int max = crop.definition().matureAge();
        if (state.getBlock() instanceof DailyClimbingCrop vine) {
            if (age < max && ForgeHooks.onCropsGrowPre(level, pos, state, true)) {
                level.setBlock(pos, state.setValue(crop.age(), Math.min(max,
                        age + CropHandlerUtils.getFertilizerIncrease(age, max, soil))), Block.UPDATE_CLIENTS);
                ForgeHooks.onCropsGrowPost(level, pos, state);
            }
            vine.icecore$postGrowth(level, pos, random);
        } else if (age < max && state.getBlock() instanceof DailyDoubleCrop doubleCrop) {
            if (ForgeHooks.onCropsGrowPre(level, pos, state, true)) {
                doubleCrop.icecore$setGrowth(level, pos, Math.min(max,
                        age + CropHandlerUtils.getFertilizerIncrease(age, max, soil)), Block.UPDATE_CLIENTS);
                ForgeHooks.onCropsGrowPost(level, pos, state);
            }
        } else if (age < max && crop.definition().shape() == Shape.DOUBLE
                && state.getBlock() instanceof BonemealableBlock bush) {
            // Collector's Reap performs its own light/space/Forge-event and half synchronization.
            if (bush.isValidBonemealTarget(level, pos, state, false)) bush.performBonemeal(level, random, pos, state);
        } else if (age < max && ForgeHooks.onCropsGrowPre(level, pos, state, true)) {
            level.setBlock(pos, state.setValue(crop.age(), Math.min(max,
                    age + CropHandlerUtils.getFertilizerIncrease(age, max, soil))), Block.UPDATE_CLIENTS);
            ForgeHooks.onCropsGrowPost(level, pos, state);
        }
    }

    private static void growRice(SeedBagCrops.Crop crop, BlockState state, ServerLevel level, BlockPos pos, BlockState soil) {
        BlockPos top = pos.above();
        BlockState upper = level.getBlockState(top);
        int stage = RiceGrowth.stage(state.getValue(crop.age()), upper.is(crop.upper()) ? upper.getValue(crop.upperAge()) : -1);
        if (stage == RiceGrowth.MATURE || (stage == 3 && (!upper.isAir()
                || !crop.upper().defaultBlockState().canSurvive(level, top)))) return;
        if (!ForgeHooks.onCropsGrowPre(level, pos, state, true)) return;
        int next = RiceGrowth.next(stage, CropHandlerUtils.getFertilizerIncrease(state.getValue(crop.age()), 3, soil));
        if (next <= 3) level.setBlock(pos, state.setValue(crop.age(), next), Block.UPDATE_CLIENTS);
        else if (stage == 3) level.setBlock(top, crop.upper().defaultBlockState(), Block.UPDATE_ALL);
        else level.setBlock(top, upper.setValue(crop.upperAge(), next - 4), Block.UPDATE_CLIENTS);
        ForgeHooks.onCropsGrowPost(level, pos, state);
    }
}
