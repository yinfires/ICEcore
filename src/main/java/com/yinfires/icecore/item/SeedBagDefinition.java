package com.yinfires.icecore.item;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.Item;

import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;

/** Immutable rules shared by registration, planting and loot integration. */
public record SeedBagDefinition(
        String id,
        ResourceLocation seedBagId,
        Item produce,
        Block cropBlock,
        Predicate<BlockState> mature
) {}
