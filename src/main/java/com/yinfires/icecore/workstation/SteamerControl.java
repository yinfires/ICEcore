package com.yinfires.icecore.workstation;

import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.SteamerBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.SteamerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class SteamerControl {
    private SteamerControl() {}
    private static final java.util.Map<Level, LidCache> CACHES = new java.util.WeakHashMap<>();
    private static final class LidCache {
        long tick = Long.MIN_VALUE;
        final java.util.Map<BlockPos, Boolean> positions = new java.util.HashMap<>();
    }
    public static void invalidate(Level level) { CACHES.remove(level); }
    public static void lidChanged(Level level, BlockPos position) {
        invalidate(level);
        if (level.isClientSide) return;
        BlockPos.MutableBlockPos pos = position.mutable();
        while (pos.getY() >= level.getMinBuildHeight() && level.hasChunkAt(pos)) {
            if (!(level.getBlockEntity(pos) instanceof SteamerBlockEntity steamer)) break;
            if (!closed(level, pos)) resetUnfinished(steamer);
            pos.move(net.minecraft.core.Direction.DOWN);
        }
    }
    /** Bound the native contiguous stack by world height; no chunks are loaded. */
    public static boolean closed(Level level, BlockPos position) {
        LidCache cache = CACHES.computeIfAbsent(level, ignored -> new LidCache());
        if (cache.tick != level.getGameTime()) { cache.tick = level.getGameTime(); cache.positions.clear(); }
        Boolean known = cache.positions.get(position); if (known != null) return known;
        BlockPos.MutableBlockPos pos = position.mutable();
        java.util.List<BlockPos> visited = new java.util.ArrayList<>();
        boolean closed = false;
        while (pos.getY() < level.getMaxBuildHeight() && level.hasChunkAt(pos)) {
            known = cache.positions.get(pos);
            if (known != null) { closed = known; break; }
            var state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof SteamerBlock)) break;
            visited.add(pos.immutable());
            if (state.getValue(SteamerBlock.HAS_LID)) { closed = true; break; }
            if (state.getValue(SteamerBlock.HALF)) break;
            pos.move(net.minecraft.core.Direction.UP);
        }
        if (cache.positions.size() + visited.size() > 4096) cache.positions.clear();
        for (BlockPos part : visited) cache.positions.put(part, closed);
        return closed;
    }
    public static void resetUnfinished(SteamerBlockEntity entity) {
        int[] progress = entity.getCookingProgress(), time = entity.getCookingTime();
        boolean changed = false;
        for (int i = 0; i < progress.length; i++) if (time[i] > 0 && progress[i] != 0) { progress[i] = 0; changed = true; }
        if (changed) ExternalWorkstation.changed(entity);
    }
    public static void uniformBatch(SteamerBlockEntity entity) {
        int[] time = entity.getCookingTime(), progress = entity.getCookingProgress();
        for (int i = 0; i < time.length; i++) if (time[i] > 0) { time[i] = 600; progress[i] = 0; }
        ExternalWorkstation.changed(entity);
    }
}
