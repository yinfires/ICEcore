package com.yinfires.icecore.farming;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.List;

public final class SeedBagLoot {
    private SeedBagLoot() {}

    /** Vanilla BlockBehaviour#getDrops, for crops whose upstream override bypasses loot tables. */
    public static List<ItemStack> lootTableDrops(Block block, BlockState state, LootParams.Builder builder) {
        ResourceLocation table = block.getLootTable();
        if (table == BuiltInLootTables.EMPTY) return List.of();
        LootParams params = builder.withParameter(LootContextParams.BLOCK_STATE, state).create(LootContextParamSets.BLOCK);
        return params.getLevel().getServer().getLootData().getLootTable(table).getRandomItems(params);
    }
}
