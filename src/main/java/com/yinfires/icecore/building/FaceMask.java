package com.yinfires.icecore.building;

import net.minecraft.core.Direction;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/** The six world faces used by the optional placement direction restriction. */
public final class FaceMask {
    private FaceMask() {
    }

    public static int bit(Direction direction) {
        return 1 << direction.ordinal();
    }

    public static int sideMask() {
        return bit(Direction.NORTH) | bit(Direction.SOUTH) | bit(Direction.EAST) | bit(Direction.WEST);
    }

    public static int allMask() {
        int mask = 0;
        for (Direction direction : Direction.values()) {
            mask |= bit(direction);
        }
        return mask;
    }

    public static boolean allows(int mask, Direction direction) {
        return mask == 0 || (mask & bit(direction)) != 0;
    }

    public static Set<String> toNames(int mask) {
        Set<String> names = new java.util.LinkedHashSet<>();
        for (Direction direction : Direction.values()) {
            if ((mask & bit(direction)) != 0) {
                names.add(direction.getSerializedName());
            }
        }
        return names;
    }

    public static int fromNames(Iterable<String> names) {
        int mask = 0;
        for (String raw : names) {
            String name = raw.toLowerCase(Locale.ROOT);
            if (name.equals("side")) {
                mask |= sideMask();
                continue;
            }
            if (name.equals("all")) {
                throw new IllegalArgumentException("all is not a persisted face value");
            }
            Direction direction = Direction.byName(name);
            if (direction == null) {
                throw new IllegalArgumentException("unknown face: " + raw);
            }
            mask |= bit(direction);
        }
        return mask;
    }

    public static EnumSet<Direction> toDirections(int mask) {
        EnumSet<Direction> result = EnumSet.noneOf(Direction.class);
        for (Direction direction : Direction.values()) {
            if ((mask & bit(direction)) != 0) {
                result.add(direction);
            }
        }
        return result;
    }
}
