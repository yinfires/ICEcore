package com.yinfires.icecore.building;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Client-only coordinates whose normal chunk/block-entity rendering is hidden
 * while a building preview is drawn. The immutable snapshot is safe to read
 * from chunk rebuild workers without synchronising on the render thread.
 */
public final class BuildingClientRenderState {
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static volatile long[] hiddenPositionKeys = new long[0];

    private BuildingClientRenderState() {
    }

    public static boolean isHidden(BlockPos position) {
        return isHidden(position.getX(), position.getY(), position.getZ());
    }

    /**
     * Allocation-free lookup for optimized chunk mesh builders that query a
     * section with integer coordinates instead of a BlockPos instance.
     */
    public static boolean isHidden(int x, int y, int z) {
        long key = BlockPos.asLong(x, y, z);
        return Arrays.binarySearch(hiddenPositionKeys, key) >= 0;
    }

    public static BlockState hiddenState() {
        return AIR;
    }

    public static void update(Set<BlockPos> positions) {
        Set<BlockPos> next = positions.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(new HashSet<>(positions));
        long[] nextKeys = new long[next.size()];
        int index = 0;
        for (BlockPos position : next) {
            nextKeys[index++] = position.asLong();
        }
        Arrays.sort(nextKeys);
        long[] previousKeys = hiddenPositionKeys;
        if (Arrays.equals(previousKeys, nextKeys)) {
            return;
        }
        hiddenPositionKeys = nextKeys;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.levelRenderer == null) {
            return;
        }
        // Mark only the changed coordinates dirty.  The vanilla and
        // Embeddium rebuild paths both read the immutable hidden snapshot, so
        // the old mesh cannot contribute a second copy after its section is
        // rebuilt.  Avoid synchronous compilation on cursor movement.
        for (long key : previousKeys) {
            markDirty(minecraft, BlockPos.of(key));
        }
        for (long key : nextKeys) {
            if (Arrays.binarySearch(previousKeys, key) < 0) {
                markDirty(minecraft, BlockPos.of(key));
            }
        }
    }

    private static void markDirty(Minecraft minecraft, BlockPos position) {
            minecraft.levelRenderer.setBlocksDirty(
                    position.getX() - 1, position.getY() - 1, position.getZ() - 1,
                    position.getX() + 1, position.getY() + 1, position.getZ() + 1);
    }

    public static void clear() {
        update(Collections.emptySet());
    }
}
