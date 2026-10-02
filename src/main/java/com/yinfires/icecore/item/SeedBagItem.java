package com.yinfires.icecore.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;

/** A crop-specific replacement for direct vanilla seed planting. */
public final class SeedBagItem extends Item {
    private final SeedBagDefinition definition;

    public SeedBagItem(Properties properties, SeedBagDefinition definition) {
        super(properties);
        this.definition = definition;
    }

    public SeedBagDefinition definition() {
        return definition;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos farmlandPos = context.getClickedPos();
        BlockState farmland = level.getBlockState(farmlandPos);
        BlockPos cropPos = farmlandPos.above();
        if (!(farmland.getBlock() instanceof FarmBlock) || !level.getBlockState(cropPos).isAir()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            level.setBlock(cropPos, definition.cropBlock().defaultBlockState(), 3);
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
