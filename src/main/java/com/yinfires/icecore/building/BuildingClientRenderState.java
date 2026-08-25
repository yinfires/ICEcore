package com.yinfires.icecore.building;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Coordinates hidden from chunk meshes while a dismantle preview is visible.
 * Workers build the requested mesh off-thread; the visible preview changes
 * only after every affected section has uploaded.
 */
public final class BuildingClientRenderState {
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final Object TRANSACTION_LOCK = new Object();
    private static final ThreadLocal<MeshSnapshot> WORKER_SNAPSHOT = new ThreadLocal<>();

    private static volatile MeshSnapshot buildingSnapshot = MeshSnapshot.EMPTY;
    private static volatile long committedGeneration;
    private static long nextGeneration;
    private static long activeGeneration;
    private static volatile long preparedGeneration;
    private static Phase phase = Phase.IDLE;
    private static long[] committedPositionKeys = new long[0];
    private static long[] desiredPositionKeys = new long[0];
    private static Set<Long> pendingSectionKeys = Collections.emptySet();
    private static volatile long[] preparedSectionKeys = new long[0];
    private static volatile long[] restoredSectionKeys = new long[0];

    private BuildingClientRenderState() {
    }

    /** Block entities must follow the target that is currently visible. */
    public static boolean isHidden(BlockPos position) {
        long key = position.asLong();
        long sectionKey = sectionKey(position);
        boolean oldVisible = Arrays.binarySearch(committedPositionKeys, key) >= 0
                && Arrays.binarySearch(restoredSectionKeys, sectionKey) < 0;
        boolean newVisible = Arrays.binarySearch(desiredPositionKeys, key) >= 0
                && Arrays.binarySearch(preparedSectionKeys, sectionKey) >= 0;
        return oldVisible || newVisible;
    }

    public static boolean isHidden(int x, int y, int z) {
        return workerSnapshot().contains(x, y, z);
    }

    public static BlockState hiddenState() {
        return AIR;
    }

    public static MeshSnapshot captureMeshSnapshot() {
        return buildingSnapshot;
    }

    public static void installWorkerSnapshot(MeshSnapshot snapshot) {
        WORKER_SNAPSHOT.set(snapshot);
    }

    public static MeshSnapshot workerSnapshot() {
        MeshSnapshot snapshot = WORKER_SNAPSHOT.get();
        return snapshot == null ? buildingSnapshot : snapshot;
    }

    public static long workerGeneration() {
        return workerSnapshot().generation();
    }

    /** Requests a target whose preview becomes visible only after mesh upload. */
    public static long requestWrench(Set<BlockPos> positions) {
        long[] requestedKeys = sortedKeys(positions);
        synchronized (TRANSACTION_LOCK) {
            if (phase != Phase.IDLE) return activeGeneration;
            if (Arrays.equals(desiredPositionKeys, requestedKeys)) {
                return activeGeneration;
            }
            desiredPositionKeys = requestedKeys;
            activeGeneration = ++nextGeneration;
            preparedGeneration = 0L;
            preparedSectionKeys = new long[0];
            restoredSectionKeys = new long[0];

            boolean switchesVisibleTargets = committedPositionKeys.length > 0 && requestedKeys.length > 0;
            long[] firstBuildKeys = switchesVisibleTargets
                    ? unionKeys(committedPositionKeys, requestedKeys) : requestedKeys;
            buildingSnapshot = new MeshSnapshot(activeGeneration, firstBuildKeys);
            Set<Long> affectedSections = affectedSections(
                    requestedKeys.length > 0 ? requestedKeys : committedPositionKeys);
            pendingSectionKeys = affectedSections;
            phase = switchesVisibleTargets ? Phase.PREPARING : Phase.FINALIZING;
            if (affectedSections.isEmpty()) {
                commit(activeGeneration);
                return activeGeneration;
            }
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null || minecraft.levelRenderer == null) {
                commit(activeGeneration);
                return activeGeneration;
            }
            markDirty(minecraft, requestedKeys.length > 0 ? requestedKeys : committedPositionKeys);
            return activeGeneration;
        }
    }

    /** Immediate path retained for placement previews, whose target is empty space. */
    public static void update(Set<BlockPos> positions) {
        long[] requestedKeys = sortedKeys(positions);
        synchronized (TRANSACTION_LOCK) {
            if (Arrays.equals(committedPositionKeys, requestedKeys)
                    && Arrays.equals(desiredPositionKeys, requestedKeys)) return;
            long[] previousKeys = committedPositionKeys;
            long generation = ++nextGeneration;
            activeGeneration = generation;
            committedGeneration = generation;
            committedPositionKeys = requestedKeys;
            desiredPositionKeys = requestedKeys;
            buildingSnapshot = new MeshSnapshot(generation, requestedKeys);
            pendingSectionKeys = Collections.emptySet();
            phase = Phase.IDLE;
            preparedGeneration = 0L;
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level != null && minecraft.levelRenderer != null) {
                markDirty(minecraft, previousKeys, requestedKeys);
            }
        }
    }

    public static void clear() {
        update(Collections.emptySet());
    }

    public static long committedGeneration() {
        return committedGeneration;
    }

    public static long preparedGeneration() {
        return preparedGeneration;
    }

    public static boolean transactionPending() {
        synchronized (TRANSACTION_LOCK) {
            return phase != Phase.IDLE;
        }
    }

    public static boolean shouldRenderDesired(BlockPos position) {
        synchronized (TRANSACTION_LOCK) {
            if (phase == Phase.IDLE) return Arrays.binarySearch(desiredPositionKeys, position.asLong()) >= 0;
            return Arrays.binarySearch(preparedSectionKeys, sectionKey(position)) >= 0;
        }
    }

    public static boolean shouldRenderCommitted(BlockPos position) {
        return Arrays.binarySearch(restoredSectionKeys, sectionKey(position)) < 0;
    }

    /** Called after one section mesh has actually entered its renderer. */
    public static void onSectionUploaded(long generation, int sectionX, int sectionY, int sectionZ) {
        synchronized (TRANSACTION_LOCK) {
            if (generation != activeGeneration || pendingSectionKeys.isEmpty()) return;
            long sectionKey = SectionPos.asLong(sectionX, sectionY, sectionZ);
            if (!pendingSectionKeys.contains(sectionKey)) return;
            if (phase == Phase.PREPARING
                    || (phase == Phase.FINALIZING && committedPositionKeys.length == 0)) {
                preparedSectionKeys = addSorted(preparedSectionKeys, sectionKey);
            } else if (phase == Phase.FINALIZING) {
                restoredSectionKeys = addSorted(restoredSectionKeys, sectionKey);
            }
            Set<Long> remaining = new HashSet<>(pendingSectionKeys);
            remaining.remove(sectionKey);
            pendingSectionKeys = remaining.isEmpty()
                    ? Collections.emptySet() : Collections.unmodifiableSet(remaining);
            if (remaining.isEmpty()) advance(generation);
        }
    }

    /** Immediate reset is reserved for world teardown. */
    public static void reset() {
        synchronized (TRANSACTION_LOCK) {
            long generation = ++nextGeneration;
            activeGeneration = generation;
            committedGeneration = generation;
            committedPositionKeys = new long[0];
            desiredPositionKeys = new long[0];
            buildingSnapshot = new MeshSnapshot(generation, new long[0]);
            pendingSectionKeys = Collections.emptySet();
            phase = Phase.IDLE;
            preparedGeneration = 0L;
            preparedSectionKeys = new long[0];
            restoredSectionKeys = new long[0];
            WORKER_SNAPSHOT.remove();
        }
    }

    private static void commit(long generation) {
        if (generation != activeGeneration) return;
        committedPositionKeys = desiredPositionKeys;
        committedGeneration = generation;
        pendingSectionKeys = Collections.emptySet();
        phase = Phase.IDLE;
        preparedGeneration = 0L;
        preparedSectionKeys = new long[0];
        restoredSectionKeys = new long[0];
    }

    private static void advance(long generation) {
        if (generation != activeGeneration) return;
        if (phase == Phase.PREPARING) {
            preparedGeneration = generation;
            phase = Phase.FINALIZING;
            buildingSnapshot = new MeshSnapshot(generation, desiredPositionKeys);
            pendingSectionKeys = affectedSections(committedPositionKeys);
            if (pendingSectionKeys.isEmpty()) {
                commit(generation);
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null || minecraft.levelRenderer == null) {
                commit(generation);
                return;
            }
            markDirty(minecraft, committedPositionKeys);
        } else if (phase == Phase.FINALIZING) {
            commit(generation);
        }
    }

    private static long[] sortedKeys(Set<BlockPos> positions) {
        long[] keys = new long[positions.size()];
        int index = 0;
        for (BlockPos position : positions) keys[index++] = position.asLong();
        Arrays.sort(keys);
        return keys;
    }

    private static long[] unionKeys(long[] first, long[] second) {
        long[] combined = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, combined, first.length, second.length);
        Arrays.sort(combined);
        int unique = 0;
        for (long key : combined) {
            if (unique == 0 || combined[unique - 1] != key) combined[unique++] = key;
        }
        return Arrays.copyOf(combined, unique);
    }

    private static long[] addSorted(long[] keys, long key) {
        if (Arrays.binarySearch(keys, key) >= 0) return keys;
        long[] result = Arrays.copyOf(keys, keys.length + 1);
        result[keys.length] = key;
        Arrays.sort(result);
        return result;
    }

    private static long sectionKey(BlockPos position) {
        return SectionPos.asLong(
                SectionPos.blockToSectionCoord(position.getX()),
                SectionPos.blockToSectionCoord(position.getY()),
                SectionPos.blockToSectionCoord(position.getZ()));
    }

    private static Set<Long> affectedSections(long[]... positionSets) {
        Set<Long> sections = new HashSet<>();
        for (long[] positions : positionSets) addAffectedSections(sections, positions);
        return sections.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(sections);
    }

    private static void addAffectedSections(Set<Long> sections, long[] positions) {
        for (long key : positions) {
            BlockPos position = BlockPos.of(key);
            sections.add(SectionPos.asLong(
                    SectionPos.blockToSectionCoord(position.getX()),
                    SectionPos.blockToSectionCoord(position.getY()),
                    SectionPos.blockToSectionCoord(position.getZ())));
        }
    }

    private static void markDirty(Minecraft minecraft, long[]... positionSets) {
        Set<Long> changed = new HashSet<>();
        for (long[] positions : positionSets) {
            for (long key : positions) changed.add(key);
        }
        for (long key : changed) {
            BlockPos position = BlockPos.of(key);
            minecraft.levelRenderer.setBlocksDirty(
                    position.getX() - 1, position.getY() - 1, position.getZ() - 1,
                    position.getX() + 1, position.getY() + 1, position.getZ() + 1);
        }
    }

    /** Immutable, allocation-free worker lookup. */
    public record MeshSnapshot(long generation, long[] positionKeys) {
        private static final MeshSnapshot EMPTY = new MeshSnapshot(0L, new long[0]);

        public MeshSnapshot {
            positionKeys = positionKeys.clone();
        }

        public boolean contains(BlockPos position) {
            return Arrays.binarySearch(positionKeys, position.asLong()) >= 0;
        }

        public boolean contains(int x, int y, int z) {
            return Arrays.binarySearch(positionKeys, BlockPos.asLong(x, y, z)) >= 0;
        }
    }

    private enum Phase {
        IDLE,
        PREPARING,
        FINALIZING
    }
}
