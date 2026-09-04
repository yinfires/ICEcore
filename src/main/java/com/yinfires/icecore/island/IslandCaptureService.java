package com.yinfires.icecore.island;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads a rectangular volume from a loaded {@link ServerLevel} into an exact
 * {@link IslandData} snapshot. Block states carry orientation, waterlogged and
 * fluid information directly; block entities are captured with full metadata so
 * chest contents, sign text, etc. round-trip losslessly.
 */
public final class IslandCaptureService {
    private IslandCaptureService() {
    }

    public static IslandData capture(ServerLevel level, BlockPos min, BlockPos max) {
        int minX = Math.min(min.getX(), max.getX());
        int minY = Math.min(min.getY(), max.getY());
        int minZ = Math.min(min.getZ(), max.getZ());
        int maxX = Math.max(min.getX(), max.getX());
        int maxY = Math.max(min.getY(), max.getY());
        int maxZ = Math.max(min.getZ(), max.getZ());
        int sizeX = maxX - minX + 1;
        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;

        List<BlockState> palette = new ArrayList<>();
        Map<BlockState, Integer> paletteIndex = new HashMap<>();
        int[] indices = new int[sizeX * sizeY * sizeZ];
        Map<BlockPos, CompoundTag> blockEntities = new HashMap<>();
        List<CompoundTag> entities = captureEntities(level, minX, minY, minZ, maxX, maxY, maxZ);

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = 0; y < sizeY; y++) {
            for (int z = 0; z < sizeZ; z++) {
                for (int x = 0; x < sizeX; x++) {
                    cursor.set(minX + x, minY + y, minZ + z);
                    // Chunk-cached read avoids per-block hash lookups through the level.
                    LevelChunk chunk = level.getChunkAt(cursor);
                    BlockState state = chunk.getBlockState(cursor);
                    int index = paletteIndex.computeIfAbsent(state, s -> {
                        palette.add(s);
                        return palette.size() - 1;
                    });
                    indices[(y * sizeZ + z) * sizeX + x] = index;
                    if (state.hasBlockEntity()) {
                        BlockEntity blockEntity = chunk.getBlockEntity(cursor);
                        if (blockEntity != null) {
                            blockEntities.put(new BlockPos(x, y, z), blockEntity.saveWithFullMetadata());
                        }
                    }
                }
            }
        }
        return new IslandData(sizeX, sizeY, sizeZ, palette, indices, blockEntities, entities);
    }

    private static List<CompoundTag> captureEntities(ServerLevel level, int minX, int minY, int minZ,
                                                     int maxX, int maxY, int maxZ) {
        AABB box = new AABB(minX, minY, minZ, maxX + 1.0, maxY + 1.0, maxZ + 1.0);
        List<CompoundTag> captured = new ArrayList<>();
        // Players are never part of an island snapshot; everything else round-trips
        // via full NBT (with entity id) so it can be recreated at show time.
        for (Entity entity : level.getEntities((Entity) null, box, e -> !(e instanceof Player))) {
            CompoundTag tag = new CompoundTag();
            if (entity.save(tag)) {
                captured.add(tag);
            }
        }
        return captured;
    }
}
