package com.yinfires.icecore.building;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class BuildingRuntimeCache {
    private static final Map<Block, List<BlockListDefinition>> listsByBlock = new IdentityHashMap<>();
    private static BuildingData data = BuildingData.empty();

    private BuildingRuntimeCache() {
    }

    public static synchronized void rebuild(BuildingData newData, MinecraftServer server) {
        data = newData;
        listsByBlock.clear();
        Registry<Block> registry = server.registryAccess().registryOrThrow(Registries.BLOCK);
        for (Block block : registry) {
            List<BlockListDefinition> matches = new ArrayList<>();
            for (BlockListDefinition list : data.blockLists().values()) {
                if (list.matches(block, registry) && list.hasBaseRestriction(data)) {
                    matches.add(list);
                }
            }
            if (!matches.isEmpty()) {
                listsByBlock.put(block, List.copyOf(matches));
            }
        }
    }

    public static BuildingData data() {
        return data;
    }

    public static List<BlockListDefinition> listsFor(BlockState state) {
        return listsByBlock.getOrDefault(state.getBlock(), Collections.emptyList());
    }

    public static boolean isManaged(BlockState state) {
        return !listsFor(state).isEmpty();
    }
}
