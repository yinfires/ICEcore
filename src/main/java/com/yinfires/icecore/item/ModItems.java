package com.yinfires.icecore.item;

import com.yinfires.icecore.ICECore;
import com.yinfires.icecore.item.SeedBagDefinition.Shape;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ICECore.MOD_ID);
    public static final RegistryObject<Item> WRENCH = ITEMS.register("wrench", () -> new WrenchItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> EXPIRED_FOOD = ITEMS.register("expired_food", com.yinfires.icecore.spoilage.ExpiredFoodItem::new);

    /** Tea is capped at its leaf-harvest stage; Jade and growth treat this age as mature. */
    public static final int TEA_HARVEST_AGE = 6;

    /** Every seed-bag crop. Adding an entry here opts the crop into all seed-bag farming rules. */
    public static final List<SeedBagDefinition> SEED_BAG_DEFINITIONS = List.of(
            single("wheat", "minecraft:wheat", 7, "minecraft:wheat"),
            single("carrot", "minecraft:carrots", 7, "minecraft:carrot"),
            single("potato", "minecraft:potatoes", 7, "minecraft:potato"),
            single("beetroot", "minecraft:beetroots", 3, "minecraft:beetroot"),
            single("soybean", "youkaisfeasts:soybean", 7, "youkaisfeasts:soybean"),
            new SeedBagDefinition("redbean", id("youkaisfeasts:redbean"), null,
                    List.of(id("youkaisfeasts:redbean")), Shape.SINGLE, 7, true),
            single("strawberry", "farm_and_charm:strawberry_crop", 5, "farm_and_charm:strawberry"),
            single("corn", "farm_and_charm:corn_crop", 4, "farm_and_charm:corn"),
            single("sweet_potato", "kaleidoscope_grilling:sweet_potato_crop", 7, "kaleidoscope_grilling:sweet_potato"),
            single("houttuynia", "kaleidoscope_grilling:houttuynia_crop", 7, "kaleidoscope_grilling:houttuynia"),
            single("tomato", "kaleidoscope_cookery:tomato_crop", 7, "kaleidoscope_cookery:tomato"),
            single("chili", "kaleidoscope_cookery:chili_crop", 7,
                    "kaleidoscope_cookery:red_chili", "kaleidoscope_cookery:green_chili"),
            single("eggplant", "kaleidoscope_chinesefood:eggplant_crop", 7, "kaleidoscope_chinesefood:eggplant"),
            single("onion", "farmersdelight:onions", 7, "farmersdelight:onion"),
            single("cabbage", "farmersdelight:cabbages", 7, "farmersdelight:cabbage"),
            new SeedBagDefinition("cucumber", id("youkaisfeasts:cucumber"), null,
                    List.of(id("youkaisfeasts:cucumber")), Shape.CLIMBING, 7, false),
            new SeedBagDefinition("rice", id("farmersdelight:rice"), id("farmersdelight:rice_panicles"),
                    List.of(id("farmersdelight:rice_panicle")), Shape.SPLIT, 3, true),
            new SeedBagDefinition("tea", id("youkaisfeasts:tea"), null,
                    List.of(id("youkaisfeasts:tea_leaves")), Shape.DOUBLE, TEA_HARVEST_AGE, true),
            new SeedBagDefinition("lime", id("collectorsreap:lime_bush"), null,
                    List.of(id("collectorsreap:lime")), Shape.DOUBLE, 4, false)
    );

    public static final Map<String, RegistryObject<Item>> SEED_BAGS = registerSeedBags();

    public static List<SeedBagDefinition> seedBagDefinitions() { return SEED_BAG_DEFINITIONS; }

    private static Map<String, RegistryObject<Item>> registerSeedBags() {
        Map<String, RegistryObject<Item>> bags = new LinkedHashMap<>();
        for (SeedBagDefinition definition : SEED_BAG_DEFINITIONS) {
            bags.put(definition.id(), ITEMS.register(definition.seedBagId().getPath(),
                    () -> new SeedBagItem(new Item.Properties(), definition)));
        }
        return java.util.Collections.unmodifiableMap(bags);
    }

    private static SeedBagDefinition single(String name, String crop, int matureAge, String... produce) {
        return new SeedBagDefinition(name, id(crop), null,
                java.util.Arrays.stream(produce).map(ModItems::id).toList(), Shape.SINGLE, matureAge, false);
    }

    private static ResourceLocation id(String value) {
        return ResourceLocation.parse(value);
    }

    private ModItems() {
    }
}
