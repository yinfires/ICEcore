package com.yinfires.icecore.recipehide.client;

import com.yinfires.icecore.recipehide.RecipeHideRegistry;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Client-only holder of the latest recipe-hide snapshot received from the server, plus the local
 * exempt-mode toggle. Feeds the client-side {@link RecipeHideRegistry} (so client recipe lookups match
 * the server) and drives {@link RecipeHideJeiApplier} whenever JEI is available.
 */
public final class RecipeHideClientState {
    private static boolean hideAll;
    private static Set<ResourceLocation> recipeIds = Set.of();
    private static Set<ResourceLocation> shownItemIds = Set.of();
    private static Set<ResourceLocation> hiddenItemIds = Set.of();
    private static boolean exemptMode;

    private RecipeHideClientState() {
    }

    public static void accept(boolean all, List<ResourceLocation> recipes,
                              List<ResourceLocation> shownItems, List<ResourceLocation> hiddenItems) {
        hideAll = all;
        recipeIds = recipes.isEmpty() ? Set.of() : new LinkedHashSet<>(recipes);
        shownItemIds = shownItems.isEmpty() ? Set.of() : new LinkedHashSet<>(shownItems);
        hiddenItemIds = hiddenItems.isEmpty() ? Set.of() : new LinkedHashSet<>(hiddenItems);
        // Client registry mirrors the server: ALL (recipeIds are exemptions) or PARTIAL (hidden set).
        RecipeHideRegistry.update(all ? RecipeHideRegistry.Mode.ALL : RecipeHideRegistry.Mode.PARTIAL, recipeIds);
        RecipeHideJeiApplier.reapply();
    }

    /** Exempt mode is a client-only visual override; it does not touch the server crafting ban. */
    public static void setExemptMode(boolean exempt) {
        exemptMode = exempt;
        RecipeHideJeiApplier.reapply();
    }

    public static boolean exemptMode() {
        return exemptMode;
    }

    public static boolean hideAll() {
        return hideAll;
    }

    public static Set<ResourceLocation> recipeIds() {
        return recipeIds;
    }

    public static Set<ResourceLocation> shownItemIds() {
        return shownItemIds;
    }

    public static Set<ResourceLocation> hiddenItemIds() {
        return hiddenItemIds;
    }
}
