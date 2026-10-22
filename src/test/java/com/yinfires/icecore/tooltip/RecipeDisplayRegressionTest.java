package com.yinfires.icecore.tooltip;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RecipeDisplayRegressionTest {
    @Test void separatorHasReservedSpaceInBothLayouts() {
        var category=new FoodCategoryTooltip(new FoodCategoryTooltip.Data(Component.literal("食材"),40,true));
        assertTrue(category.separatorOffset()>=10);
        assertTrue(category.getHeight()>category.separatorOffset()+1);
        assertEquals(10,new FoodCategoryTooltip(new FoodCategoryTooltip.Data(Component.literal("食材"),40,false)).getHeight());
        // Native and Modern UI both advance by component height; Modern UI adds a title gap.
        for(int titleGap:new int[]{0,2})assertTrue(10+titleGap+category.separatorOffset()+1<10+titleGap+category.getHeight());
    }
    @Test void recipeAnnotationsDoNotRemoveDynamicItemDetails() {
        assertTrue(JeiRecipeDetails.hidden(Component.translatable("jei.tooltip.recipe.by","ICEcore")));
        assertTrue(JeiRecipeDetails.hidden(Component.literal("§7制作时间: §f10 秒")));
        assertTrue(JeiRecipeDetails.hidden(Component.literal("冷冻时间：90 秒")));
        assertTrue(JeiRecipeDetails.hidden(Component.translatable("jei.test.cooking_time",20)));
        assertTrue(JeiRecipeDetails.hidden(Component.translatable("jei.kaleidoscope_world_liquor.freezer.freeze_time",90)));
        for(String text:new String[]{"剩余使用次数：16/16","牛奶 1000mB","单份产出: 250 mB","保质期：1天","售价：12"})
            assertFalse(JeiRecipeDetails.hidden(Component.literal(text)));
    }
}
