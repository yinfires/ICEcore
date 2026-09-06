package com.yinfires.icecore.compat.cozycafe.spawn;

import com.yinfires.icecore.building.BuildingDataManager;
import com.yinfires.icecore.building.RegionDefinition;
import com.yinfires.icecore.compat.cozycafe.range.CozyCafeRangeDataManager;
import com.yinfires.icecore.compat.cozycafe.range.CozyCafeRangeDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Resolves bounded, cached customer entrances on a named region's lowest Y layer. */
public final class CozyCafeSpawnRegionService {
    private static final int MAX_CANDIDATES = 65_536;
    private static final Map<String, Cache> CACHE = new HashMap<>();

    private CozyCafeSpawnRegionService() {}

    public static BlockPos randomPosition(ServerLevel level, BlockPos computer, RandomSource random) {
        CozyCafeRangeDefinition link = CozyCafeRangeDataManager.get().definition(level, computer);
        if (link == null || link.spawnRegion() == null) return null;
        RegionDefinition region = BuildingDataManager.get().data().regions().get(link.spawnRegion());
        if (region == null || !region.isComplete() || !level.dimension().location().toString().equals(region.dimension())) return null;
        int[] a = region.pos1(); int[] b = region.pos2();
        int minX = Math.min(a[0], b[0]), maxX = Math.max(a[0], b[0]);
        int y = Math.min(a[1], b[1]);
        int minZ = Math.min(a[2], b[2]), maxZ = Math.max(a[2], b[2]);
        long width = (long) maxX - minX + 1L;
        long depth = (long) maxZ - minZ + 1L;
        long area = width * depth;
        if (area <= 0L) return null;
        String key = level.dimension().location() + "|" + link.spawnRegion();
        long revision = BuildingDataManager.get().revision();
        Cache cache = CACHE.get(key);
        if (cache == null || cache.revision != revision) {
            int sampleCount = (int) Math.min(area, MAX_CANDIDATES);
            List<BlockPos> positions = new ArrayList<>(sampleCount);
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int sample = 0; sample < sampleCount; sample++) {
                long index = ((long) sample * area + area / 2L) / sampleCount;
                int x = (int) (minX + index / depth);
                int z = (int) (minZ + index % depth);
                cursor.set(x, y, z);
                if (isWalkable(level, cursor)) positions.add(cursor.immutable());
            }
            cache = new Cache(revision, positions);
            CACHE.put(key, cache);
        }
        if (cache.positions.isEmpty()) return null;
        for (int attempt = 0; attempt < Math.min(8, cache.positions.size()); attempt++) {
            BlockPos selected = cache.positions.get(random.nextInt(cache.positions.size()));
            if (isWalkable(level, selected)) return selected;
        }
        CACHE.remove(key);
        return null;
    }

    public static boolean hasValidRegion(ServerLevel level, BlockPos computer) {
        return randomPosition(level, computer, level.random) != null;
    }

    /** Returns a bounded, evenly distributed entrance pool for one opening-cycle route snapshot. */
    public static List<BlockPos> entranceCandidates(ServerLevel level, BlockPos computer) {
        if (randomPosition(level, computer, level.random) == null) return List.of();
        CozyCafeRangeDefinition link = CozyCafeRangeDataManager.get().definition(level, computer);
        if (link == null || link.spawnRegion() == null) return List.of();
        Cache cache = CACHE.get(level.dimension().location() + "|" + link.spawnRegion());
        if (cache == null || cache.positions.isEmpty()) return List.of();
        int count = Math.min(16, cache.positions.size());
        List<BlockPos> result = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            int source = (int) (((long) index * cache.positions.size() + cache.positions.size() / 2L) / count);
            BlockPos candidate = cache.positions.get(Math.min(source, cache.positions.size() - 1));
            if (isWalkable(level, candidate)) result.add(candidate);
        }
        return List.copyOf(result);
    }

    public static BlockPos exitSignPosition(ServerLevel level, BlockPos computer, BlockPos fallbackSign) {
        BlockPos exit = randomPosition(level, computer, level.random);
        return exit == null ? fallbackSign : exit.above();
    }

    public static void clearCache() { CACHE.clear(); }

    private static boolean isWalkable(ServerLevel level, BlockPos feet) {
        if (!level.hasChunkAt(feet)) return false;
        BlockState feetState = level.getBlockState(feet);
        BlockState headState = level.getBlockState(feet.above());
        BlockState floorState = level.getBlockState(feet.below());
        return feetState.getCollisionShape(level, feet).isEmpty()
                && headState.getCollisionShape(level, feet.above()).isEmpty()
                && !floorState.getCollisionShape(level, feet.below()).isEmpty()
                && feetState.isPathfindable(level, feet, PathComputationType.LAND);
    }

    private record Cache(long revision, List<BlockPos> positions) {}
}
