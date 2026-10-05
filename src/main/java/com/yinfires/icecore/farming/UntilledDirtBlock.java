package com.yinfires.icecore.farming;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.registries.ForgeRegistries;

/** Dried farmland. Plain dirt in every respect except that a hoe turns it back into farmland. */
public final class UntilledDirtBlock extends Block {
    private static final ResourceLocation FARMLAND = ResourceLocation.withDefaultNamespace("farmland");

    public UntilledDirtBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getToolModifiedState(BlockState state, UseOnContext context, ToolAction toolAction, boolean simulate) {
        if (toolAction == ToolActions.HOE_TILL
                && context.getLevel().getBlockState(context.getClickedPos().above()).isAir()) {
            // Registry lookup keeps the farmland implementation of any mod overriding minecraft:farmland.
            return ForgeRegistries.BLOCKS.getValue(FARMLAND).defaultBlockState();
        }
        return super.getToolModifiedState(state, context, toolAction, simulate);
    }
}
