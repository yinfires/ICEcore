package com.yinfires.icecore.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BuildingRules {
    private BuildingRules() {
    }

    public static boolean canPlace(Level level, BlockPos target, BlockState placing, BlockState support, Direction face) {
        List<BlockListDefinition> lists = BuildingRuntimeCache.listsFor(placing);
        if (lists.isEmpty()) {
            return false;
        }
        Registry<net.minecraft.world.level.block.Block> registry = level.registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.BLOCK);
        ResourceLocation dimension = level.dimension().location();
        for (BlockListDefinition list : lists) {
            boolean region = list.regions().isEmpty() || list.matchesRegion(BuildingRuntimeCache.data(), dimension, target);
            boolean supportAllowed = list.supports().isEmpty()
                    || list.matchesSupport(BuildingRuntimeCache.data(), support, registry);
            if (!region || !supportAllowed || !list.allowsFace(face)) {
                return false;
            }
        }
        return true;
    }

    /** Compatibility overload for callers that only have the clicked state. */
    public static boolean canPlace(Level level, BlockPos target, BlockState support, Direction face) {
        return canPlace(level, target, level.getBlockState(target), support, face);
    }

    public static boolean canBreak(Level level, BlockPos keyPosition, BlockState state) {
        // Do not rely on the block-identity cache for the dismantle path.  The
        // cache is an optimisation for placement and can be stale for one tick
        // while a rule snapshot is being rebuilt.  Removal must evaluate the
        // authoritative world data directly so the wrench cannot be blocked by
        // an unrelated cache miss.
        List<BlockListDefinition> lists = matchingBreakLists(level, state);
        if (lists.isEmpty()) {
            return false;
        }
        ResourceLocation dimension = level.dimension().location();
        for (BlockListDefinition list : lists) {
            if (list.regions().isEmpty()) {
                continue;
            }
            if (!list.matchesRegion(BuildingRuntimeCache.data(), dimension, keyPosition)) {
                return false;
            }
        }
        // Every valid placement list also grants dismantling permission. Region
        // bindings remain effective as additional area restrictions; support and
        // face bindings are placement-only constraints and do not block removal.
        return true;
    }

    private static List<BlockListDefinition> matchingBreakLists(Level level, BlockState state) {
        BuildingData data = BuildingRuntimeCache.data();
        var registry = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BLOCK);
        List<BlockListDefinition> matches = new ArrayList<>();
        for (BlockListDefinition list : data.blockLists().values()) {
            if (list.matches(state, registry) && list.hasBaseRestriction(data)) {
                matches.add(list);
            }
        }
        return matches.isEmpty() ? Collections.emptyList() : List.copyOf(matches);
    }
}
