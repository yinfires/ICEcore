package com.yinfires.icecore.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class BlockListDefinition {
    private String name;
    private final List<String> entries = new ArrayList<>();
    private final Set<String> regions = new LinkedHashSet<>();
    private final Set<String> supports = new LinkedHashSet<>();
    private final Set<String> allowedFaces = new LinkedHashSet<>();

    public BlockListDefinition() {
    }

    public BlockListDefinition(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> entries() {
        return entries;
    }

    public Set<String> regions() {
        return regions;
    }

    public Set<String> supports() {
        return supports;
    }

    public Set<String> allowedFaces() {
        return allowedFaces;
    }

    public int faceMask() {
        return FaceMask.fromNames(allowedFaces);
    }

    public boolean matches(BlockState state, Registry<Block> registry) {
        return matches(state.getBlock(), registry);
    }

    public boolean matches(Block block, Registry<Block> registry) {
        for (String entry : entries) {
            if (matchesEntry(block, entry, registry)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesEntry(Block block, String entry, Registry<Block> registry) {
        String value = entry.startsWith("#") ? entry.substring(1) : entry;
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) {
            return false;
        }
        if (entry.startsWith("#")) {
            TagKey<Block> tag = TagKey.create(net.minecraft.core.registries.Registries.BLOCK, id);
            return registry.getResourceKey(block).flatMap(registry::getHolder).map(holder -> holder.is(tag)).orElse(false);
        }
        ResourceLocation registered = registry.getKey(block);
        return id.equals(registered);
    }

    public boolean hasBaseRestriction(BuildingData data) {
        boolean region = regions.stream().map(data.regions()::get).anyMatch(RegionDefinition::isComplete);
        boolean support = supports.stream().map(data.blockLists()::get)
                .anyMatch(list -> list != null);
        return region || support;
    }

    public boolean matchesRegion(BuildingData data, ResourceLocation dimension, BlockPos position) {
        return regions.stream()
                .map(data.regions()::get)
                .filter(region -> region != null)
                .anyMatch(region -> region.contains(net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.DIMENSION, dimension), position));
    }

    public boolean matchesSupport(BuildingData data, BlockState clicked, Registry<Block> registry) {
        return supports.stream()
                .map(data.blockLists()::get)
                .filter(list -> list != null)
                .anyMatch(list -> list.matches(clicked, registry));
    }

    public boolean allowsFace(Direction face) {
        return FaceMask.allows(faceMask(), face);
    }
}
