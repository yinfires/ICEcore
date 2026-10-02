package com.yinfires.icecore.item;

import com.yinfires.icecore.ICECore;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceLocation;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ICECore.MOD_ID);
    public static final RegistryObject<Item> WRENCH = ITEMS.register("wrench", () -> new WrenchItem(new Item.Properties().stacksTo(1)));
    public static final SeedBagDefinition WHEAT_DEFINITION = new SeedBagDefinition("wheat", ResourceLocation.fromNamespaceAndPath(ICECore.MOD_ID, "wheat_seed_bag"), Items.WHEAT, Blocks.WHEAT,
            state -> state.getValue(CropBlock.AGE) >= 7);
    public static final SeedBagDefinition CARROT_DEFINITION = new SeedBagDefinition("carrot", ResourceLocation.fromNamespaceAndPath(ICECore.MOD_ID, "carrot_seed_bag"), Items.CARROT, Blocks.CARROTS,
            state -> state.getValue(CropBlock.AGE) >= 7);
    public static final RegistryObject<Item> WHEAT_SEED_BAG = ITEMS.register("wheat_seed_bag",
            () -> new SeedBagItem(new Item.Properties(), WHEAT_DEFINITION));
    public static final RegistryObject<Item> CARROT_SEED_BAG = ITEMS.register("carrot_seed_bag",
            () -> new SeedBagItem(new Item.Properties(), CARROT_DEFINITION));

    public static final java.util.List<SeedBagDefinition> SEED_BAG_DEFINITIONS = java.util.List.of(WHEAT_DEFINITION, CARROT_DEFINITION);

    public static java.util.List<SeedBagDefinition> seedBagDefinitions() { return SEED_BAG_DEFINITIONS; }

    private ModItems() {
    }
}
