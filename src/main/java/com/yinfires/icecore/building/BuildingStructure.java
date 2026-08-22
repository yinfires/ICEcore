package com.yinfires.icecore.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Small, state-driven description of the two-position structures used by block
 * placement and dismantling.  The implementation deliberately does not test
 * concrete vanilla block classes: modded blocks commonly reuse the same
 * {@code half}/{@code part} properties and can therefore use the same preview
 * and wrench path.
 */
public final class BuildingStructure {
    private static final ConcurrentHashMap<Block, Optional<StructureProperty>> DISCOVERY_CACHE = new ConcurrentHashMap<>();

    private BuildingStructure() {
    }

    public static BlockPos primaryPosition(BlockGetter level, BlockPos position) {
        BlockState state = level.getBlockState(position);
        StructureProperty structure = find(state);
        if (structure == null || !structure.valueName(state).equals(structure.secondary())) {
            return position;
        }
        // Prefer the state-declared neighbouring primary part.  If a modded
        // block uses the opposite facing convention, the nearby-state fallback
        // below still resolves the real primary part without a block-class test.
        BlockPos expected = position.relative(structure.pairDirection(state, true));
        if (level.getBlockState(expected).getBlock() == state.getBlock()
                && structure.matchesPrimary(level.getBlockState(expected))) {
            return expected;
        }
        for (Direction direction : Direction.values()) {
            BlockPos candidate = position.relative(direction);
            BlockState candidateState = level.getBlockState(candidate);
            if (candidateState.getBlock() == state.getBlock()
                    && structure.matchesPrimary(candidateState)) {
                return candidate;
            }
        }
        return primaryPosition(state, position);
    }

    public static BlockPos primaryPosition(BlockState state, BlockPos position) {
        StructureProperty structure = find(state);
        if (structure == null) {
            return position;
        }
        String value = structure.valueName(state);
        if (value.equals(structure.secondary())) {
            return position.relative(structure.pairDirection(state, true));
        }
        return position;
    }

    public static List<BlockPos> positions(BlockState primaryState, BlockPos primary) {
        primaryState = primaryState(primaryState);
        List<BlockPos> positions = new ArrayList<>();
        positions.add(primary);
        StructureProperty structure = find(primaryState);
        if (structure == null || !structure.valueName(primaryState).equals(structure.primary())) {
            return positions;
        }
        positions.add(primary.relative(structure.pairDirection(primaryState, false)));
        return positions;
    }

    public static BlockState primaryState(BlockState state) {
        StructureProperty structure = find(state);
        if (structure == null) {
            return state;
        }
        return structure.valueName(state).equals(structure.secondary())
                ? structure.withValue(state, structure.primary()) : state;
    }

    public static BlockState stateAt(BlockState primaryState, BlockPos primary, BlockPos position) {
        primaryState = primaryState(primaryState);
        if (primary.equals(position)) {
            return primaryState;
        }
        StructureProperty structure = find(primaryState);
        if (structure == null || !structure.valueName(primaryState).equals(structure.primary())) {
            return primaryState;
        }
        if (!primary.relative(structure.pairDirection(primaryState, false)).equals(position)) {
            return primaryState;
        }
        return structure.withValue(primaryState, structure.secondary());
    }

    private static StructureProperty find(BlockState state) {
        return DISCOVERY_CACHE.computeIfAbsent(state.getBlock(), ignored -> Optional.ofNullable(discover(state))).orElse(null);
    }

    private static StructureProperty discover(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            String propertyName = property.getName().toLowerCase(Locale.ROOT);
            List<String> values = new ArrayList<>();
            for (Object possible : property.getPossibleValues()) {
                @SuppressWarnings("rawtypes")
                String value = ((Property) property).getName((Comparable) possible).toLowerCase(Locale.ROOT);
                values.add(value);
            }
            String primary = null;
            String secondary = null;
            PairKind kind = null;
            if (values.contains("lower") && values.contains("upper")) {
                primary = "lower";
                secondary = "upper";
                kind = PairKind.VERTICAL;
            } else if (values.contains("foot") && values.contains("head")) {
                primary = "foot";
                secondary = "head";
                kind = PairKind.FACING;
            } else if (values.contains("left") && values.contains("right")) {
                primary = "left";
                secondary = "right";
                kind = PairKind.CLOCKWISE;
            } else if (values.contains("front") && values.contains("back")) {
                primary = "front";
                secondary = "back";
                kind = PairKind.FACING;
            } else if (values.size() == 2 && values.contains("bottom") && values.contains("top")) {
                primary = "bottom";
                secondary = "top";
                kind = PairKind.VERTICAL;
            } else if (values.size() == 2 && values.contains("base") && values.contains("top")) {
                primary = "base";
                secondary = "top";
                kind = PairKind.VERTICAL;
            } else if (values.size() == 2 && values.contains("first") && values.contains("second")) {
                primary = "first";
                secondary = "second";
                kind = PairKind.VERTICAL;
            }
            if (primary == null || kind == null) {
                continue;
            }
            if (!propertyName.contains("half") && !propertyName.contains("part")
                    && !propertyName.contains("section") && !propertyName.contains("segment")
                    && !propertyName.contains("level") && !propertyName.contains("piece")
                    && !propertyName.contains("portion") && !propertyName.contains("type")) {
                continue;
            }
            // Stairs use a single-block half=top/bottom property.  Do not
            // mistake that orientation for a second occupied cell; custom
            // multi-block blocks can use part/section/type instead.
            if (kind == PairKind.VERTICAL && values.contains("bottom")
                    && values.contains("top") && propertyName.equals("half")) {
                continue;
            }
            return new StructureProperty(property, primary, secondary, kind);
        }
        return null;
    }

    private enum PairKind {
        VERTICAL,
        FACING,
        CLOCKWISE
    }

    private record StructureProperty(Property<?> property, String primary, String secondary,
                                     PairKind kind) {
        private String valueName(BlockState state) {
            @SuppressWarnings("rawtypes")
            String value = ((Property) property).getName(state.getValue(property));
            return value.toLowerCase(Locale.ROOT);
        }

        private boolean matchesPrimary(BlockState state) {
            return valueName(state).equals(primary);
        }

        private Direction pairDirection(BlockState state, boolean fromSecondary) {
            Direction facing = facing(state).orElse(Direction.NORTH);
            return switch (kind) {
                case VERTICAL -> fromSecondary ? Direction.DOWN : Direction.UP;
                case FACING -> fromSecondary ? facing.getOpposite() : facing;
                case CLOCKWISE -> fromSecondary ? facing.getCounterClockWise() : facing.getClockWise();
            };
        }

        private BlockState withValue(BlockState state, String valueName) {
            @SuppressWarnings("rawtypes")
            Optional value = ((Property) property).getValue(valueName);
            if (value.isEmpty()) {
                return state;
            }
            @SuppressWarnings({"rawtypes", "unchecked"})
            StateHolder<?, ?> holder = state;
            return (BlockState) holder.setValue((Property) property, (Comparable) value.get());
        }

        private static Optional<Direction> facing(BlockState state) {
            for (Property<?> property : state.getProperties()) {
                String name = property.getName().toLowerCase(Locale.ROOT);
                Object value = state.getValue(property);
                if ((name.contains("facing") || name.contains("direction") || name.contains("orientation"))
                        && value instanceof Direction direction) {
                    return Optional.of(direction);
                }
            }
            for (Property<?> property : state.getProperties()) {
                Object value = state.getValue(property);
                if (value instanceof Direction direction) {
                    return Optional.of(direction);
                }
            }
            return Optional.empty();
        }

    }
}
