package com.yinfires.icecore.island;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.Clearable;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Cross-tick, budgeted placement / clearing queue. Each job walks the volume
 * bottom-up (the prototype "grow from the ground" wavefront) and mutates a
 * bounded number of blocks per server tick so a kilometre island never stalls
 * the server thread in one tick.
 *
 * <p>Neighbour updates are suppressed ({@link Block#UPDATE_KNOWN_SHAPE},
 * {@link Block#UPDATE_SUPPRESS_DROPS}) so placing/clearing never triggers water
 * flow, falling sand or redstone cascades. Container block entities are cleared
 * before removal so hiding a chest never spills its contents. Client + light
 * updates stay on so the result is visually and light-correct.
 *
 * <p><b>Show is delayed then batched</b>: during the entrance window (the client
 * cutscene camera + screen warp, see island/IslandEntranceClient) the server places
 * nothing; when the window ends it grows the volume bottom-up over as many ticks as
 * its size needs, a bounded slice per tick, so a kilometre island never stalls one
 * server tick. Suppressed neighbour updates keep placement from re-flowing fluids or
 * triggering cascades while the wavefront runs. Hide is batched the same way. Batched
 * low-level relight for kilometre islands is a later optimisation.
 */
public final class IslandPlacer {
    /** Clients + known-shape (no neighbour shape updates) + suppress drops. */
    public static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    /** Hide removal budget per tick; keeps a large island from stalling one tick. */
    private static final int HIDE_BLOCKS_PER_TICK = 8192;
    /** Show placement budget per tick; bounds the per-tick main-thread cost so a kilometre island never stalls one tick. */
    private static final int SHOW_BLOCKS_PER_TICK = 8192;

    private static final Deque<Job> JOBS = new ArrayDeque<>();

    private IslandPlacer() {
    }

    /**
     * Enqueues a show whose blocks are placed in a single tick only after
     * {@code delayTicks} have elapsed (the client-side animation window). Captured
     * entities spawn in that same landing tick.
     */
    public static void enqueueShow(ServerLevel level, BlockPos min, IslandData data, int delayTicks, Runnable onComplete) {
        JOBS.add(Job.show(level, min, data, Math.max(0, delayTicks), onComplete));
    }

    public static void enqueueHide(ServerLevel level, BlockPos min, IslandData data, int durationTicks, Runnable onComplete) {
        JOBS.add(Job.hide(level, min, data, onComplete));
    }

    /** Server-thread pump. Processes one budget slice of the head job per call. */
    public static void tick() {
        Job job = JOBS.peek();
        if (job == null) {
            return;
        }
        if (job.step()) {
            JOBS.poll();
            if (job.onComplete != null) {
                job.onComplete.run();
            }
        }
    }

    public static void clear() {
        JOBS.clear();
    }

    public static boolean idle() {
        return JOBS.isEmpty();
    }

    /**
     * Server ticks a show will spend placing a volume of this size, excluding the
     * pre-placement delay. Placement is deterministic ({@link #SHOW_BLOCKS_PER_TICK}
     * per tick), so this is exact — the client uses it to pace the entrance warp so
     * its peak lines up with the actual reveal.
     */
    public static int estimatePlaceTicks(long volume) {
        if (volume <= 0) {
            return 1;
        }
        return (int) Math.min(Integer.MAX_VALUE, (volume + SHOW_BLOCKS_PER_TICK - 1) / SHOW_BLOCKS_PER_TICK);
    }

    private enum Mode {
        SHOW,
        HIDE
    }

    private static final class Job {
        private final ServerLevel level;
        private final BlockPos min;
        private final IslandData data;
        private final Mode mode;
        private final Runnable onComplete;
        /** SHOW: remaining animation-window ticks before the one-tick landing burst. */
        private int delayTicks;
        /** HIDE: flat cursor over the volume; layout ((y*sizeZ)+z)*sizeX + x. */
        private long cursor;

        private Job(ServerLevel level, BlockPos min, IslandData data, Mode mode, int delayTicks, Runnable onComplete) {
            this.level = level;
            this.min = min;
            this.data = data;
            this.mode = mode;
            this.delayTicks = delayTicks;
            this.onComplete = onComplete;
        }

        private static Job show(ServerLevel level, BlockPos min, IslandData data, int delayTicks, Runnable onComplete) {
            return new Job(level, min, data, Mode.SHOW, delayTicks, onComplete);
        }

        private static Job hide(ServerLevel level, BlockPos min, IslandData data, Runnable onComplete) {
            return new Job(level, min, data, Mode.HIDE, 0, onComplete);
        }

        /** Returns true when the job is finished. */
        private boolean step() {
            return mode == Mode.SHOW ? stepShow() : stepHide();
        }

        /**
         * Waits out the delay window, then places the volume bottom-up over as many
         * ticks as its size needs, a bounded {@link #SHOW_BLOCKS_PER_TICK} slice per
         * tick so a kilometre island never stalls one server tick. The whole placement
         * is hidden behind the client entrance effect (the island only becomes visible
         * on the flash reveal after onComplete), so the wavefront is never seen.
         * Entities spawn once the last block lands.
         */
        private boolean stepShow() {
            if (delayTicks > 0) {
                delayTicks--;
                return false;
            }
            long volume = data.volume();
            int sizeX = data.sizeX();
            int sizeZ = data.sizeZ();
            BlockPos.MutableBlockPos world = new BlockPos.MutableBlockPos();
            int processed = 0;
            while (cursor < volume && processed < SHOW_BLOCKS_PER_TICK) {
                int lx = (int) (cursor % sizeX);
                int rest = (int) (cursor / sizeX);
                int lz = rest % sizeZ;
                int ly = rest / sizeZ;
                world.set(min.getX() + lx, min.getY() + ly, min.getZ() + lz);
                placeOne(world, lx, ly, lz);
                cursor++;
                processed++;
            }
            if (cursor >= volume) {
                spawnEntities();
                return true;
            }
            return false;
        }

        /** Batched removal; fluid fidelity does not apply to clearing. */
        private boolean stepHide() {
            long volume = data.volume();
            int sizeX = data.sizeX();
            int sizeZ = data.sizeZ();
            if (cursor == 0) {
                removeEntities();
            }
            BlockPos.MutableBlockPos world = new BlockPos.MutableBlockPos();
            int processed = 0;
            while (cursor < volume && processed < HIDE_BLOCKS_PER_TICK) {
                int lx = (int) (cursor % sizeX);
                int rest = (int) (cursor / sizeX);
                int lz = rest % sizeZ;
                int ly = rest / sizeZ;
                world.set(min.getX() + lx, min.getY() + ly, min.getZ() + lz);
                clearContainer(world);
                level.setBlock(world, Blocks.AIR.defaultBlockState(), FLAGS);
                cursor++;
                processed++;
            }
            return cursor >= volume;
        }

        /**
         * Empties a container-style block entity before its block is removed so
         * vanilla's onRemove drop logic finds nothing to spill. UPDATE_SUPPRESS_DROPS
         * only covers the block's own loot, not held inventory.
         */
        private void clearContainer(BlockPos world) {
            BlockEntity blockEntity = level.getBlockEntity(world);
            if (blockEntity instanceof Clearable clearable) {
                clearable.clearContent();
            }
        }

        /** Discards non-player entities in the island volume so a hidden island leaves nothing behind. */
        private void removeEntities() {
            net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(
                    min.getX(), min.getY(), min.getZ(),
                    min.getX() + data.sizeX(), min.getY() + data.sizeY(), min.getZ() + data.sizeZ());
            for (Entity entity : level.getEntities((Entity) null, box,
                    e -> !(e instanceof net.minecraft.world.entity.player.Player))) {
                entity.discard();
            }
        }

        /** Recreates captured entities at their original positions when the island finishes showing. */
        private void spawnEntities() {
            for (CompoundTag tag : data.entities()) {
                EntityType.loadEntityRecursive(tag, level, entity -> {
                    level.addFreshEntity(entity);
                    return entity;
                });
            }
        }

        private void placeOne(BlockPos world, int lx, int ly, int lz) {
            BlockState state = data.stateAt(lx, ly, lz);
            level.setBlock(world, state, FLAGS);
            if (state.hasBlockEntity()) {
                CompoundTag nbt = data.blockEntityAt(new BlockPos(lx, ly, lz));
                if (nbt != null) {
                    BlockEntity blockEntity = level.getBlockEntity(world);
                    if (blockEntity != null) {
                        blockEntity.load(nbt);
                        blockEntity.setChanged();
                    }
                }
            }
        }
    }
}
