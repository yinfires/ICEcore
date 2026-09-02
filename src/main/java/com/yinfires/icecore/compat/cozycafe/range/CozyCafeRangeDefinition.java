package com.yinfires.icecore.compat.cozycafe.range;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Objects;

/** A custom, inclusive recognition range bound to one CozyCafe manager position. */
public final class CozyCafeRangeDefinition {
    public static final long MAX_SCAN_VOLUME = 65_536L;
    private String dimension;
    private int[] computer;
    private int[] pos1;
    private int[] pos2;
    private String spawnRegion;

    public CozyCafeRangeDefinition() {
    }

    public CozyCafeRangeDefinition(ResourceKey<Level> dimension, BlockPos computer) {
        this.dimension = Objects.requireNonNull(dimension).location().toString();
        this.computer = coordinates(computer);
    }

    public String dimension() {
        return dimension;
    }

    public int[] computer() {
        return computer == null ? null : computer.clone();
    }

    public int[] pos1() {
        return pos1 == null ? null : pos1.clone();
    }

    public int[] pos2() {
        return pos2 == null ? null : pos2.clone();
    }

    public String spawnRegion() {
        return spawnRegion;
    }

    public void setSpawnRegion(String spawnRegion) {
        this.spawnRegion = spawnRegion == null || spawnRegion.isBlank() ? null : spawnRegion;
    }

    public void setFirst(BlockPos position) {
        pos1 = coordinates(position);
    }

    public void beginSelection(BlockPos position) {
        setFirst(position);
        pos2 = null;
    }

    public void setSecond(BlockPos position) {
        pos2 = coordinates(position);
    }

    public boolean canSetSecond(BlockPos position) {
        return pos1 == null || withinScanLimit(position(pos1), position);
    }

    public boolean isComplete() {
        return validCoordinates(pos1) && validCoordinates(pos2);
    }

    public BlockPos firstPosition() {
        return isComplete() ? position(pos1) : null;
    }

    public BlockPos secondPosition() {
        return isComplete() ? position(pos2) : null;
    }

    public String key() {
        return key(dimension, computer);
    }

    public static String key(ResourceKey<Level> dimension, BlockPos computer) {
        return key(Objects.requireNonNull(dimension).location().toString(), coordinates(computer));
    }

    public static String key(String dimension, int[] computer) {
        if (dimension == null || ResourceLocation.tryParse(dimension) == null || !validCoordinates(computer)) {
            throw new IllegalArgumentException("invalid CozyCafe computer location");
        }
        return dimension + "|" + computer[0] + "," + computer[1] + "," + computer[2];
    }

    public void validate() {
        key();
        if (pos1 != null && !validCoordinates(pos1)) {
            throw new IllegalArgumentException("invalid CozyCafe range pos1");
        }
        if (pos2 != null && !validCoordinates(pos2)) {
            throw new IllegalArgumentException("invalid CozyCafe range pos2");
        }
        if (isComplete() && !withinScanLimit(position(pos1), position(pos2))) {
            throw new IllegalArgumentException("CozyCafe range exceeds scan volume limit");
        }
        if (spawnRegion != null && spawnRegion.isBlank()) {
            throw new IllegalArgumentException("invalid CozyCafe spawn region");
        }
    }

    private static boolean withinScanLimit(BlockPos first, BlockPos second) {
        long sizeX = Math.abs((long) first.getX() - second.getX()) + 1L;
        long sizeY = Math.abs((long) first.getY() - second.getY()) + 1L;
        long sizeZ = Math.abs((long) first.getZ() - second.getZ()) + 1L;
        if (sizeX > MAX_SCAN_VOLUME || sizeY > MAX_SCAN_VOLUME || sizeZ > MAX_SCAN_VOLUME) {
            return false;
        }
        long xy = sizeX * sizeY;
        return xy <= MAX_SCAN_VOLUME && sizeZ <= MAX_SCAN_VOLUME / xy;
    }

    private static int[] coordinates(BlockPos position) {
        Objects.requireNonNull(position);
        return new int[]{position.getX(), position.getY(), position.getZ()};
    }

    private static BlockPos position(int[] coordinates) {
        return new BlockPos(coordinates[0], coordinates[1], coordinates[2]);
    }

    private static boolean validCoordinates(int[] coordinates) {
        return coordinates != null && coordinates.length == 3;
    }
}
