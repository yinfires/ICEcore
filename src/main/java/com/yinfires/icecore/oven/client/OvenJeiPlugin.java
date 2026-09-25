package com.yinfires.icecore.oven.client;

import com.yinfires.icecore.ICECore;
import com.yinfires.icecore.oven.ModOven;
import com.yinfires.icecore.oven.OvenRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;

import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public final class OvenJeiPlugin implements IModPlugin {
    public static final RecipeType<OvenRecipe> TYPE = new RecipeType<>(
            ResourceLocation.fromNamespaceAndPath(ICECore.MOD_ID, "oven"), OvenRecipe.class);
    @Override public ResourceLocation getPluginUid() { return ResourceLocation.fromNamespaceAndPath(ICECore.MOD_ID, "oven_jei"); }
    @Override public void registerCategories(IRecipeCategoryRegistration r) { r.addRecipeCategories(new Category(r.getJeiHelpers().getGuiHelper())); }
    @Override public void registerRecipes(IRecipeRegistration r) { if (Minecraft.getInstance().level != null) r.addRecipes(TYPE, Minecraft.getInstance().level.getRecipeManager().getAllRecipesFor(ModOven.OVEN_RECIPE.get())); }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration r) { r.addRecipeCatalyst(ModOven.OVEN_ITEM.get(), TYPE); }

    private static final class Category implements IRecipeCategory<OvenRecipe> {
        private final mezz.jei.api.gui.drawable.IDrawable icon;
        private final mezz.jei.api.gui.drawable.IDrawable background;
        
        private Category(IGuiHelper h) { icon = h.createDrawableItemStack(new ItemStack(ModOven.OVEN_ITEM.get())); background = h.createDrawable(ResourceLocation.fromNamespaceAndPath(ICECore.MOD_ID, "textures/gui/oven.png"), 0, 0, 144, 54); }
        @Override public RecipeType<OvenRecipe> getRecipeType() { return TYPE; }
        @Override public Component getTitle() { return Component.translatable("block.icecore.oven"); }
        @Override public mezz.jei.api.gui.drawable.IDrawable getIcon() { return icon; }
        @Override public mezz.jei.api.gui.drawable.IDrawable getBackground() { return background; }
        @Override public int getWidth() { return 144; }
        @Override public int getHeight() { return 54; }
        @Override public void setRecipe(IRecipeLayoutBuilder l, OvenRecipe r, IFocusGroup f) {
            for (int i=0;i<r.getIngredients().size()&&i<9;i++) l.addSlot(RecipeIngredientRole.INPUT,1+(i%3)*18,1+(i/3)*18).addIngredients(r.getIngredients().get(i)).addTooltipCallback((v,t)->{if(r.batch()) t.add(Component.translatable("icecore.oven.batch_supported").withStyle(ChatFormatting.GRAY));});
            for (int i=0;i<r.results().size()&&i<9;i++) l.addSlot(RecipeIngredientRole.OUTPUT,91+(i%3)*18,1+(i/3)*18).addItemStack(r.results().get(i));
        }

        @Override public void draw(OvenRecipe r, mezz.jei.api.gui.ingredient.IRecipeSlotsView v, GuiGraphics g, double x, double y) { if (!r.batch()) return; g.pose().pushPose(); g.pose().translate(0,0,200); for(int i=0;i<r.getIngredients().size()&&i<9;i++){int sx=1+(i%3)*18,sy=1+(i/3)*18; int c=0xFFFFFFFF; g.fill(sx+1,sy+1,sx+2,sy+2,c);g.fill(sx+3,sy+1,sx+4,sy+2,c);g.fill(sx+2,sy+2,sx+3,sy+3,c);g.fill(sx+1,sy+3,sx+2,sy+4,c);g.fill(sx+3,sy+3,sx+4,sy+4,c); } g.pose().popPose(); }
    }
}



