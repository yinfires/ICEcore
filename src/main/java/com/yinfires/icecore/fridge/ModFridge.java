package com.yinfires.icecore.fridge;

import com.yinfires.icecore.ICECore;
import net.blay09.mods.cookingforblockheads.tile.ModBlockEntities;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import java.util.HashSet;
import java.util.Set;

public final class ModFridge {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ICECore.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ICECore.MOD_ID);
    public static final RegistryObject<IntegratedFridgeBlock> FRIDGE = BLOCKS.register("fridge", IntegratedFridgeBlock::new);
    public static final RegistryObject<Item> FRIDGE_ITEM = ITEMS.register("fridge", () -> new IntegratedFridgeItem(FRIDGE.get()));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        bus.addListener(ModFridge::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            FridgeBlockTypeAccess type = (FridgeBlockTypeAccess) ModBlockEntities.fridge.get();
            Set<Block> blocks = new HashSet<>(type.icecore$getValidBlocks());
            blocks.add(FRIDGE.get());
            type.icecore$setValidBlocks(Set.copyOf(blocks));
        });
    }

    private ModFridge() {}
}
