package com.yinfires.icecore.interaction;

import com.yinfires.icecore.building.BuildingStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import java.util.*;

/** Already-expanded rules. No registry/tag/rule iteration occurs during interaction. */
public record InteractionSnapshot(Set<Block> denied, Set<Block> allowed, InteractionExceptions exceptions) {
    public InteractionSnapshot { denied = Set.copyOf(denied); allowed = Set.copyOf(allowed); }
    public static InteractionSnapshot empty() { return new InteractionSnapshot(Set.of(), Set.of(), InteractionExceptions.empty()); }
    public boolean blocked(Level level, BlockPos pos) {
        Block block = level.getBlockState(pos).getBlock();
        if (!denied.contains(block) || allowed.contains(block)) return false;
        if (exceptions.entries().isEmpty()) return true;
        // State-only normalization avoids reading unloaded neighbours or scanning adjacent blocks.
        BlockPos primary = BuildingStructure.primaryPosition(level.getBlockState(pos), pos);
        String exception = exceptions.entries().get(location(level, primary));
        return !BuiltInRegistries.BLOCK.getKey(block).toString().equals(exception);
    }
    public static InteractionExceptions.Location location(Level level, BlockPos pos) {
        return new InteractionExceptions.Location(level.dimension().location().toString(), pos.getX(), pos.getY(), pos.getZ());
    }
}
