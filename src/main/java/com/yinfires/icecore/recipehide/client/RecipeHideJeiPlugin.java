package com.yinfires.icecore.recipehide.client;

import com.yinfires.icecore.ICECore;
import com.yinfires.icecore.recipehide.RecipeHideRegistry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

/** Independent JEI plugin that applies ICEcore's recipe-hide state to the runtime. */
@JeiPlugin
public final class RecipeHideJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(ICECore.MOD_ID, "recipe_hide");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    /**
     * JEI is (re)building its runtime. It gathers recipes via {@code getAllRecipesFor} → {@code byType},
     * which our mixin filters — so an active hide state at build time would make JEI register only the
     * unfiltered subset, and recipes it never registered could never be shown again without a relog.
     * Open the registry bypass here (JEI runs every plugin's registerCategories before any plugin's
     * registerRecipes) and close it in {@link #onRuntimeAvailable}; visibility is then enforced purely
     * at runtime by {@link RecipeHideJeiApplier}. The server thread keeps filtering throughout.
     */
    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        RecipeHideRegistry.setJeiBuildBypass(true);
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        RecipeHideRegistry.setJeiBuildBypass(false);
        RecipeHideJeiApplier.setRuntime(runtime);
        RecipeHideJeiApplier.reapply();
    }

    @Override
    public void onRuntimeUnavailable() {
        RecipeHideJeiApplier.setRuntime(null);
    }
}
