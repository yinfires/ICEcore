package com.yinfires.icecore.island;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable-ish captured snapshot of a rectangular region: every block state
 * (orientation, waterlogged, fluids all included via the block state) plus the
 * full NBT of every block entity, keyed by position relative to the minimum
 * corner. Prototype storage is a single palette + flat index array, which is
 * exact and lossless for small regions; chunk-sliced streaming for kilometre
 * islands is a later optimisation.
 */
public final class IslandData {
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final List<BlockState> palette;
    private final int[] indices;
    private final Map<BlockPos, CompoundTag> blockEntities;
    /** Full entity NBT (with id) captured at capture time, positions absolute. */
    private final List<CompoundTag> entities;

    IslandData(int sizeX, int sizeY, int sizeZ, List<BlockState> palette, int[] indices,
               Map<BlockPos, CompoundTag> blockEntities, List<CompoundTag> entities) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.palette = palette;
        this.indices = indices;
        this.blockEntities = blockEntities;
        this.entities = entities;
    }

    public List<CompoundTag> entities() {
        return entities;
    }

    public int sizeX() {
        return sizeX;
    }

    public int sizeY() {
        return sizeY;
    }

    public int sizeZ() {
        return sizeZ;
    }

    public long volume() {
        return (long) sizeX * sizeY * sizeZ;
    }

    public BlockState stateAt(int localX, int localY, int localZ) {
        return palette.get(indices[flatIndex(localX, localY, localZ)]);
    }

    public CompoundTag blockEntityAt(BlockPos local) {
        return blockEntities.get(local);
    }

    private int flatIndex(int x, int y, int z) {
        return (y * sizeZ + z) * sizeX + x;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("sizeX", sizeX);
        tag.putInt("sizeY", sizeY);
        tag.putInt("sizeZ", sizeZ);
        ListTag paletteTag = new ListTag();
        for (BlockState state : palette) {
            paletteTag.add(NbtUtils.writeBlockState(state));
        }
        tag.put("palette", paletteTag);
        tag.putIntArray("indices", indices);
        ListTag beTag = new ListTag();
        for (Map.Entry<BlockPos, CompoundTag> entry : blockEntities.entrySet()) {
            CompoundTag record = new CompoundTag();
            BlockPos local = entry.getKey();
            record.putInt("x", local.getX());
            record.putInt("y", local.getY());
            record.putInt("z", local.getZ());
            record.put("nbt", entry.getValue());
            beTag.add(record);
        }
        tag.put("blockEntities", beTag);
        ListTag entityTag = new ListTag();
        entityTag.addAll(entities);
        tag.put("entities", entityTag);
        return tag;
    }

    public static IslandData load(CompoundTag tag, HolderGetter<Block> blocks) {
        int sizeX = tag.getInt("sizeX");
        int sizeY = tag.getInt("sizeY");
        int sizeZ = tag.getInt("sizeZ");
        ListTag paletteTag = tag.getList("palette", Tag.TAG_COMPOUND);
        List<BlockState> palette = new ArrayList<>(paletteTag.size());
        for (int i = 0; i < paletteTag.size(); i++) {
            palette.add(NbtUtils.readBlockState(blocks, paletteTag.getCompound(i)));
        }
        int[] indices = tag.getIntArray("indices");
        ListTag beTag = tag.getList("blockEntities", Tag.TAG_COMPOUND);
        Map<BlockPos, CompoundTag> blockEntities = new HashMap<>(beTag.size());
        for (int i = 0; i < beTag.size(); i++) {
            CompoundTag record = beTag.getCompound(i);
            BlockPos local = new BlockPos(record.getInt("x"), record.getInt("y"), record.getInt("z"));
            blockEntities.put(local, record.getCompound("nbt"));
        }
        ListTag entityTag = tag.getList("entities", Tag.TAG_COMPOUND);
        List<CompoundTag> entities = new ArrayList<>(entityTag.size());
        for (int i = 0; i < entityTag.size(); i++) {
            entities.add(entityTag.getCompound(i));
        }
        return new IslandData(sizeX, sizeY, sizeZ, palette, indices, blockEntities, entities);
    }
}
