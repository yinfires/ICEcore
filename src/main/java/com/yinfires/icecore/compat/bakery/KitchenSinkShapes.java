package com.yinfires.icecore.compat.bakery;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Four immutable native orientations, filled only below the unchanged basin. */
public final class KitchenSinkShapes {
    private static final VoxelShape BASE = Block.box(0, 0, 0, 16, 12, 16);
    private static final Map<Direction, VoxelShape> SHAPES = new ConcurrentHashMap<>();
    private KitchenSinkShapes() {}

    public static VoxelShape extend(VoxelShape original, Direction facing) {
        VoxelShape cached = SHAPES.get(facing);
        if (cached != null) return cached;
        VoxelShape expanded = Shapes.or(original, BASE).optimize();
        VoxelShape previous = SHAPES.putIfAbsent(facing, expanded);
        return previous == null ? expanded : previous;
    }
}
