package com.yinfires.icecore.compat.bakery;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** Refurbished Furniture 1.0.20's corner precedence, restricted to this kitchen set. */
public final class KitchenConnections {
    public static final ResourceLocation COUNTER = new ResourceLocation("bakery", "brick_counter");
    public static final ResourceLocation SINK = new ResourceLocation("bakery", "kitchen_sink");
    public static final ResourceLocation STOVE = new ResourceLocation("farmersdelight", "stove");

    public enum Shape { DEFAULT, INSIDE_CORNER_LEFT, INSIDE_CORNER_RIGHT, OUTSIDE_CORNER_LEFT, OUTSIDE_CORNER_RIGHT }

    private KitchenConnections() {}

    public static boolean available() { return Availability.ENABLED; }

    private static final class Availability {
        private static final boolean ENABLED = net.minecraftforge.fml.ModList.get().isLoaded("bakery")
                && net.minecraftforge.fml.ModList.get().isLoaded("farmersdelight")
                && net.minecraftforge.fml.ModList.get().isLoaded("refurbished_furniture");
    }

    public static Direction direction(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (!COUNTER.equals(id) && !STOVE.equals(id) && !(SINK.equals(id)
                && state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER)) return null;
        return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING) : null;
    }

    public static Shape shape(BlockGetter level, BlockPos pos, Direction facing) {
        Direction behind = direction(level.getBlockState(pos.relative(facing.getOpposite())));
        // Inner corners take precedence, exactly as KitchenCabinetryBlock#getShape.
        Shape inside = choose(facing, behind, null);
        return inside != Shape.DEFAULT ? inside
                : choose(facing, null, direction(level.getBlockState(pos.relative(facing))));
    }

    public static Shape choose(Direction facing, Direction behind, Direction ahead) {
        if (behind == facing.getClockWise()) return Shape.INSIDE_CORNER_RIGHT;
        if (behind == facing.getCounterClockWise()) return Shape.INSIDE_CORNER_LEFT;
        if (ahead == facing.getClockWise()) return Shape.OUTSIDE_CORNER_LEFT;
        if (ahead == facing.getCounterClockWise()) return Shape.OUTSIDE_CORNER_RIGHT;
        return Shape.DEFAULT;
    }
}
