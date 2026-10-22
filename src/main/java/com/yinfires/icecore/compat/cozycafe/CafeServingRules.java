package com.yinfires.icecore.compat.cozycafe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import com.yinfires.icecore.workstation.WorkstationContainerCompat;

/** Server tags and the existing vessel resolver are the shared source of plating requirements. */
public final class CafeServingRules {
    private static final TagKey<net.minecraft.world.item.Item> FOOD = TagKey.create(Registries.ITEM,
            ResourceLocation.parse("icecore:recipe_manager/1790175849511"));
    private static final TagKey<net.minecraft.world.item.Item> DRINK = TagKey.create(Registries.ITEM,
            ResourceLocation.parse("icecore:recipe_manager/food_main_drink"));
    private CafeServingRules() {}
    public static boolean requiresPlate(ItemStack stack, boolean drink) {
        if (!stack.is(FOOD) || drink || stack.is(DRINK)) return false;
        return requiresPlate(true,false,WorkstationContainerCompat.hasServingVessel(stack));
    }
    public static boolean requiresPlate(boolean recognized,boolean drink,boolean vessel) {
        return recognized && !drink && !vessel;
    }
}
