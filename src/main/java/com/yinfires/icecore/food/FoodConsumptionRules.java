package com.yinfires.icecore.food;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.MilkBucketItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;

import javax.annotation.Nullable;

public final class FoodConsumptionRules {
    private static final TagKey<Item> RAW_SKEWERS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("kaleidoscope_grilling", "raw_skewers"));
    private static final TagKey<Item> GRILLED_SKEWERS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("kaleidoscope_grilling", "grilled_skewers"));
    private FoodConsumptionRules() {
    }

    public static boolean isFood(ItemStack stack, @Nullable LivingEntity entity) {
        if (stack.isEmpty()) return false;
        if (stack.getFoodProperties(entity) != null) return true;
        if (stack.getItem() instanceof MilkBucketItem || stack.getItem() instanceof PotionItem) return true;
        if (!com.yinfires.icecore.food.FoodConsumptionClientState.modCompatibility()
                && entity != null && entity.level().isClientSide) return false;
        if (!com.yinfires.icecore.config.ICECoreConfig.enableModCompatibility()
                && (entity == null || !entity.level().isClientSide)) return false;
        if (stack.is(RAW_SKEWERS) || stack.is(GRILLED_SKEWERS)) return true;
        String className = stack.getItem().getClass().getName();
        return className.endsWith("TeacupItem")
                || className.endsWith("DrinkBlockItem")
                || className.endsWith("CocktailBlockItem")
                || className.endsWith("JuiceBucketItem")
                || className.endsWith("BaseJuiceBucketItem");
    }

}
