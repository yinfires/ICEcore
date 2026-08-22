package com.yinfires.icecore.building;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Objects;

public final class RegionDefinition {
    private String name;
    private String dimension;
    private int[] pos1;
    private int[] pos2;

    public RegionDefinition() {
    }

    public RegionDefinition(String name) {
        this.name = name;
    }

    public RegionDefinition(String name, ResourceKey<Level> dimension, BlockPos pos1, BlockPos pos2) {
        this.name = name;
        setBounds(dimension, pos1, pos2);
    }

    public String name() {
        return name;
    }

    public String dimension() {
        return dimension;
    }

    public int[] pos1() {
        return pos1 == null ? null : pos1.clone();
    }

    public int[] pos2() {
        return pos2 == null ? null : pos2.clone();
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setBounds(ResourceKey<Level> dimension, BlockPos first, BlockPos second) {
        this.dimension = Objects.requireNonNull(dimension).location().toString();
        this.pos1 = new int[]{first.getX(), first.getY(), first.getZ()};
        this.pos2 = new int[]{second.getX(), second.getY(), second.getZ()};
    }

    public void setFirst(ResourceKey<Level> dimension, BlockPos position) {
        ensureDimension(dimension);
        this.pos1 = new int[]{position.getX(), position.getY(), position.getZ()};
    }

    public void setSecond(ResourceKey<Level> dimension, BlockPos position) {
        ensureDimension(dimension);
        this.pos2 = new int[]{position.getX(), position.getY(), position.getZ()};
    }

    public void clearBounds() {
        dimension = null;
        pos1 = null;
        pos2 = null;
    }

    public boolean isComplete() {
        return dimension != null && pos1 != null && pos2 != null && pos1.length == 3 && pos2.length == 3;
    }

    public boolean contains(ResourceKey<Level> level, BlockPos position) {
        if (!isComplete() || !dimension.equals(level.location().toString())) {
            return false;
        }
        int minX = Math.min(pos1[0], pos2[0]);
        int minY = Math.min(pos1[1], pos2[1]);
        int minZ = Math.min(pos1[2], pos2[2]);
        int maxX = Math.max(pos1[0], pos2[0]);
        int maxY = Math.max(pos1[1], pos2[1]);
        int maxZ = Math.max(pos1[2], pos2[2]);
        return position.getX() >= minX && position.getX() <= maxX
                && position.getY() >= minY && position.getY() <= maxY
                && position.getZ() >= minZ && position.getZ() <= maxZ;
    }

    private void ensureDimension(ResourceKey<Level> level) {
        if (dimension == null) {
            dimension = level.location().toString();
            return;
        }
        if (!dimension.equals(level.location().toString())) {
            throw new IllegalArgumentException("region coordinate dimension mismatch");
        }
    }
}
