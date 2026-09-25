package com.yinfires.icecore.oven;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;

/** Single source of truth for the heat sources accepted by the oven. */
public final class OvenHeatSourceCompat {
    public static final TagKey<Block> HEAT_SOURCES = TagKey.create(
            net.minecraft.core.registries.Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("farmersdelight", "heat_sources"));

    private OvenHeatSourceCompat() {}

    public static boolean isHeatSource(BlockGetter level, BlockPos ovenPos) {
        return level.getBlockState(ovenPos.below()).is(HEAT_SOURCES);
    }
}
