package com.yinfires.icecore.farming;

import com.yinfires.icecore.ICECore;
import com.yinfires.icecore.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModFarming {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ICECore.MOD_ID);
    public static final RegistryObject<Block> UNTILLED_DIRT = BLOCKS.register("untilled_dirt",
            () -> new UntilledDirtBlock(BlockBehaviour.Properties.copy(Blocks.DIRT)));
    public static final RegistryObject<Item> UNTILLED_DIRT_ITEM = ModItems.ITEMS.register("untilled_dirt",
            () -> new BlockItem(UNTILLED_DIRT.get(), new Item.Properties()));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }

    private ModFarming() {}
}
