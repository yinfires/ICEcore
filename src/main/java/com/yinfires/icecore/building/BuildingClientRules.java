package com.yinfires.icecore.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.Registries;

public final class BuildingClientRules {
    private BuildingClientRules() {
    }

    public static boolean canPlace(Level level, BlockPos target, BlockState placing, BlockState support, Direction face) {
        var data = BuildingClientState.data();
        var registry = level.registryAccess().registryOrThrow(Registries.BLOCK);
        var lists = data.blockLists().values().stream().filter(list -> list.matches(placing, registry)
                && list.hasBaseRestriction(data)).toList();
        if (lists.isEmpty()) return false;
        for (var list : lists) {
            boolean inRegion = list.regions().isEmpty() || list.matchesRegion(data, level.dimension().location(), target);
            boolean onSupport = list.supports().isEmpty() || list.matchesSupport(data, support, registry);
            if (!inRegion || !onSupport || !list.allowsFace(face)) return false;
        }
        return true;
    }

    public static boolean isManaged(BlockState placing) {
        var data = BuildingClientState.data();
        var client = net.minecraft.client.Minecraft.getInstance();
        if (client.level == null) return false;
        var registry = client.level.registryAccess().registryOrThrow(Registries.BLOCK);
        return data.blockLists().values().stream().anyMatch(list -> list.matches(placing, registry)
                && list.hasBaseRestriction(data));
    }

    public static boolean canBreak(Level level, BlockPos target, BlockState state) {
        var data = BuildingClientState.data();
        var registry = level.registryAccess().registryOrThrow(Registries.BLOCK);
        // Evaluate the synchronized snapshot directly.  A client cache is not
        // used here because a newly received rule revision must immediately be
        // usable by the wrench preview, even while other client state catches up.
        boolean matched = false;
        for (BlockListDefinition list : data.blockLists().values()) {
            if (!list.matches(state, registry) || !list.hasBaseRestriction(data)) {
                continue;
            }
            matched = true;
            if (list.regions().isEmpty()) continue;
            if (!list.matchesRegion(data, level.dimension().location(), target)) return false;
        }
        if (!matched) return false;
        // Every valid placement list is dismantlable. Region bindings still
        // restrict the target area; support and face bindings affect placement only.
        return true;
    }
}
