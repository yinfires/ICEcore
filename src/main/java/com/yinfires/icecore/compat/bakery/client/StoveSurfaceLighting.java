package com.yinfires.icecore.compat.bakery.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import com.yinfires.icecore.compat.bakery.KitchenConnections;

/** Render-only material lighting. Never called by the world's light engine. */
public final class StoveSurfaceLighting {
    private StoveSurfaceLighting() {}

    private static final class Target {
        // First accessed by chunk rendering, after block registration is complete.
        private static final Block STOVE = BuiltInRegistries.BLOCK.get(KitchenConnections.STOVE);
    }

    public static boolean isLitStove(BlockState state) {
        return state.getBlock() == Target.STOVE && state.hasProperty(BlockStateProperties.LIT)
                && state.getValue(BlockStateProperties.LIT);
    }

    public static int emission(BlockState state, BlockGetter world, BlockPos pos) {
        return isLitStove(state) ? 0 : state.getLightEmission(world, pos);
    }

    public static float shade(BlockState state, BlockGetter world, BlockPos pos) {
        // setValue returns an existing immutable state: no world write or new state allocation.
        return (isLitStove(state) ? state.setValue(BlockStateProperties.LIT, false) : state)
                .getShadeBrightness(world, pos);
    }
}
