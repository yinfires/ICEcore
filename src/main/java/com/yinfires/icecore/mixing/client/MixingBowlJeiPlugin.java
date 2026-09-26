package com.yinfires.icecore.mixing.client;

import com.yinfires.icecore.ICECore;
import com.yinfires.icecore.mixing.MixingBowlRecipe;
import com.yinfires.icecore.mixing.ModMixing;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public final class MixingBowlJeiPlugin implements IModPlugin {
    public static final RecipeType<MixingBowlRecipe> TYPE = new RecipeType<>(
            ResourceLocation.fromNamespaceAndPath(ICECore.MOD_ID, "mixing_bowl"), MixingBowlRecipe.class);

    @Override public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(ICECore.MOD_ID, "mixing_bowl_jei");
    }

    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new Category(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override public void registerRecipes(IRecipeRegistration registration) {
        if (Minecraft.getInstance().level != null) {
            registration.addRecipes(TYPE, Minecraft.getInstance().level.getRecipeManager()
                    .getAllRecipesFor(ModMixing.MIXING_BOWL_RECIPE.get()));
        }
    }

    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModMixing.MIXING_BOWL_ITEM.get(), TYPE);
    }

    private static final class Category implements IRecipeCategory<MixingBowlRecipe> {
        private final IDrawable icon;
        private final IDrawable background;
        private Category(IGuiHelper helper) {
            icon = helper.createDrawableItemStack(new ItemStack(ModMixing.MIXING_BOWL_ITEM.get()));
            background = helper.createDrawable(
                    ResourceLocation.fromNamespaceAndPath("youkaisfeasts", "textures/gui/ferment.png"),
                    0, 0, 144, 54);
        }
        @Override public RecipeType<MixingBowlRecipe> getRecipeType() { return TYPE; }
        @Override public Component getTitle() { return Component.translatable("block.icecore.mixing_bowl"); }
        @Override public IDrawable getBackground() { return background; }
        @Override public IDrawable getIcon() { return icon; }
        @Override public int getWidth() { return 144; }
        @Override public int getHeight() { return 54; }
        @Override public void setRecipe(IRecipeLayoutBuilder layout, MixingBowlRecipe recipe, IFocusGroup focuses) {
            java.util.ArrayList<ItemStack> carrierItems = new java.util.ArrayList<>();
            int inputIndex = 0;
            for (; inputIndex < recipe.getIngredients().size(); inputIndex++) {
                int x = 1 + (inputIndex % 3) * 18, y = 1 + (inputIndex / 3) * 18;
                layout.addSlot(RecipeIngredientRole.INPUT, x, y)
                        .addIngredients(recipe.getIngredients().get(inputIndex));
            }
            if (!recipe.fluid().isEmpty()) {
                int x = 1 + (inputIndex % 3) * 18, y = 1 + (inputIndex / 3) * 18;
                layout.addSlot(RecipeIngredientRole.INPUT, x, y)
                        .addItemStack(recipe.fluid().display());
            }
            for (int i = 0; i < recipe.results().size(); i++) {
                int x = 91 + (i % 3) * 18, y = 1 + (i / 3) * 18;
                layout.addSlot(RecipeIngredientRole.OUTPUT, x, y)
                        .addItemStack(recipe.results().get(i).stack());
                if (!recipe.results().get(i).effectiveCarrier().isEmpty()) {
                    carrierItems.addAll(java.util.List.of(recipe.results().get(i).effectiveCarrier().getItems()));
                }
            }
            if (!carrierItems.isEmpty()) {
                layout.addSlot(RecipeIngredientRole.INPUT, 64, 1).addItemStacks(carrierItems);
            }
        }
    }
}
