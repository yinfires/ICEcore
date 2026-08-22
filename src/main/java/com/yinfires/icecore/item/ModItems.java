package com.yinfires.icecore.item;

import com.yinfires.icecore.ICECore;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ICECore.MOD_ID);
    public static final RegistryObject<Item> WRENCH = ITEMS.register("wrench", () -> new WrenchItem(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }
}
