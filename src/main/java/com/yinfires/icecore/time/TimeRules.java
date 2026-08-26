package com.yinfires.icecore.time;

import com.yinfires.icecore.building.BlockListDefinition;
import com.yinfires.icecore.building.BuildingData;
import com.yinfires.icecore.building.BuildingStructure;
import com.yinfires.icecore.building.RegionDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Comparator;

public final class TimeRules {
    private TimeRules() {}
    public static Match match(Level level, BlockPos hit, TimeConfigData config, BuildingData building) {
        if (level.dimension() != Level.OVERWORLD) return null;
        BlockPos primary = BuildingStructure.primaryPosition(level, hit);
        BlockState state = level.getBlockState(primary);
        BlockListDefinition list = building.blockLists().get(config.triggerBlockList());
        if (list == null || !list.matches(state, level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BLOCK))) return null;
        RegionDefinition region = config.regions().stream().map(building.regions()::get)
                .filter(value -> value != null && value.contains(Level.OVERWORLD, primary))
                .min(Comparator.comparingLong(TimeRules::volume).thenComparing(RegionDefinition::name)).orElse(null);
        if (region == null) return null;
        var positions = BuildingStructure.positions(BuildingStructure.primaryState(state), primary);
        double x = positions.stream().mapToInt(BlockPos::getX).average().orElse(primary.getX()) + 0.5D;
        double y = positions.stream().mapToInt(BlockPos::getY).max().orElse(primary.getY()) + 1.55D;
        double z = positions.stream().mapToInt(BlockPos::getZ).average().orElse(primary.getZ()) + 0.5D;
        return new Match(region.name(), primary, x, y, z);
    }
    private static long volume(RegionDefinition region) {
        int[] a = region.pos1(), b = region.pos2();
        if (a == null || b == null) return Long.MAX_VALUE;
        try { return Math.multiplyExact(Math.multiplyExact((long)Math.abs(a[0]-b[0])+1L, (long)Math.abs(a[1]-b[1])+1L), (long)Math.abs(a[2]-b[2])+1L); }
        catch (ArithmeticException exception) { return Long.MAX_VALUE; }
    }
    public record Match(String region, BlockPos primary, double labelX, double labelY, double labelZ) {}
}
