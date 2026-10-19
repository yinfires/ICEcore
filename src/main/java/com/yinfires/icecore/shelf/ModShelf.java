package com.yinfires.icecore.shelf;

import com.yinfires.icecore.ICECore;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

public final class ModShelf {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ICECore.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ICECore.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ICECore.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, ICECore.MOD_ID);
    public static final RegistryObject<ShelfBlock> SHELF = BLOCKS.register("shelf", ShelfBlock::new);
    public static final RegistryObject<Item> ITEM = ITEMS.register("shelf", () -> new ShelfItem(SHELF.get()));
    public static final RegistryObject<BlockEntityType<ShelfBlockEntity>> ENTITY = ENTITIES.register("shelf",
            () -> BlockEntityType.Builder.of(ShelfBlockEntity::new, SHELF.get()).build(null));
    public static final RegistryObject<MenuType<ShelfMenu>> MENU = MENUS.register("shelf",
            () -> IForgeMenuType.create((id, inventory, data) -> new ShelfMenu(id, inventory)));
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); MENUS.register(bus);
    }
    private ModShelf() {}
}
