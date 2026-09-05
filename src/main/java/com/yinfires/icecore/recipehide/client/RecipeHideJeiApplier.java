package com.yinfires.icecore.recipehide.client;

import com.mojang.logging.LogUtils;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.gui.bookmarks.BookmarkList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies the current {@link RecipeHideClientState} to the live JEI runtime. Rebuilt to reconcile the
 * <em>current</em> runtime to the target state on every call, with no cross-runtime bookkeeping:
 *
 * <ul>
 *   <li><b>Recipes</b>: for every {@link RecipeType}, fetch the current runtime's recipe instances and
 *       explicitly {@code hideRecipes}/{@code unhideRecipes} both directions. Because the instances come
 *       from the current runtime, identity matching (JEI stores hidden recipes in an IdentityHashMap)
 *       always holds — fixing "recipe not viewable after show" across JEI reloads.</li>
 *   <li><b>Items/fluids</b>: session-only bookkeeping of what we removed, invalidated when the runtime
 *       changes. Re-add last removals (same runtime), then remove the current target set. Non-empty
 *       checks guard every call so {@code ErrorUtil.checkNotEmpty} never throws.</li>
 *   <li><b>Catalyst items</b> (crafting table, furnace, ...) are removed like any other item so they
 *       leave the item list. Removing a catalyst makes it invisible in every JEI context, and JEI hides
 *       any recipe category whose catalysts are all invisible — which would make a "shown" item's
 *       recipes unviewable. JEI 15.49.0.188 has no per-context hide API, so we publish the removed
 *       catalyst items to {@link RecipeHideCatalystVisibility}; {@code JeiRecipeManagerInternalMixin}
 *       reads it to keep those categories viewable while the catalyst stays out of the list.</li>
 *   <li><b>No swallowed exceptions</b>: failures are logged, so a bad state can't silently corrupt JEI's
 *       search index.</li>
 *   <li><b>Exempt mode</b>: a client toggle; when on, everything is shown (recipes unhidden, items
 *       re-added) regardless of hide state. Server-side crafting ban is unaffected.</li>
 * </ul>
 */
public final class RecipeHideJeiApplier {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static IJeiRuntime runtime;
    // Bookkeeping is tied to a specific runtime; a new runtime starts fully shown so old records are void.
    private static IJeiRuntime bookkeepingRuntime;
    @SuppressWarnings("rawtypes")
    private static final Map<IIngredientType, List> removedIngredients = new LinkedHashMap<>();
    // The EXACT recipe instances we hid, per type. JEI's hidden set is identity-based and
    // createRecipeLookup().get() returns fresh instances across calls, so we must unhide these exact
    // objects — not freshly-fetched ones (which would fail to match and leave recipes stuck hidden).
    @SuppressWarnings("rawtypes")
    private static final Map<RecipeType, List> hiddenRecipeInstances = new LinkedHashMap<>();

    private RecipeHideJeiApplier() {
    }

    public static void setRuntime(IJeiRuntime jeiRuntime) {
        if (jeiRuntime != bookkeepingRuntime) {
            // New (or cleared) runtime starts fully shown; old records don't apply to it.
            removedIngredients.clear();
            hiddenRecipeInstances.clear();
            RecipeHideCatalystVisibility.set(Set.of());
            bookkeepingRuntime = jeiRuntime;
        }
        runtime = jeiRuntime;
    }

    public static boolean hasRuntime() {
        return runtime != null;
    }

    /** Reconciles the current runtime to the target state. Call on runtime-available and any change. */
    public static void reapply() {
        IJeiRuntime current = runtime;
        if (current == null) {
            return;
        }
        boolean exempt = RecipeHideClientState.exemptMode();
        boolean hideAll = !exempt && RecipeHideClientState.hideAll();
        Set<ResourceLocation> recipeIds = RecipeHideClientState.recipeIds();
        Set<Item> shownItems = resolveItems(RecipeHideClientState.shownItemIds());
        Set<Item> hiddenItems = resolveItems(RecipeHideClientState.hiddenItemIds());

        applyRecipes(current.getRecipeManager(), exempt, hideAll, recipeIds);
        applyItems(current, exempt, hideAll, shownItems, hiddenItems);
    }

    /**
     * Reconciles recipe visibility. First UNHIDE the exact instances we hid last time (JEI's hidden
     * set is identity-based and fresh fetches return different instances, so only the recorded objects
     * can be removed). Then hide the current target set and record those exact instances.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void applyRecipes(IRecipeManager recipeManager, boolean exempt, boolean hideAll,
                                     Set<ResourceLocation> recipeIds) {
        // 1) Unhide exactly what we hid before (recorded instances), so nothing stays stuck hidden.
        hiddenRecipeInstances.forEach((type, recipes) -> {
            if (!recipes.isEmpty()) {
                try {
                    recipeManager.unhideRecipes(type, recipes);
                } catch (Throwable e) {
                    LOGGER.warn("[recipehide] unhideRecipes failed for {}", type.getUid(), e);
                }
            }
        });
        hiddenRecipeInstances.clear();

        if (exempt) {
            return; // everything shown
        }

        // 2) Hide the current target set, recording the exact instances for next-time unhide.
        BookmarkList bookmarks = RecipeHideBookmarks.bookmarkList(runtime);
        recipeManager.createRecipeCategoryLookup().includeHidden().get().forEach(category -> {
            RecipeType type = category.getRecipeType();
            List hideList = new ArrayList();
            recipeManager.createRecipeLookup(type).includeHidden().get().forEach(recipe -> {
                boolean listed = recipe instanceof Recipe<?> vanilla && recipeIds.contains(vanilla.getId());
                // hideAll → hide all except exemptions; else → hide only listed.
                if (hideAll ? !listed : listed) {
                    hideList.add(recipe);
                }
            });
            if (!hideList.isEmpty()) {
                try {
                    recipeManager.hideRecipes(type, hideList);
                    hiddenRecipeInstances.put(type, hideList);
                } catch (Throwable e) {
                    LOGGER.warn("[recipehide] hideRecipes failed for {}", type.getUid(), e);
                }
                if (bookmarks != null) {
                    for (Object recipe : hideList) {
                        RecipeHideBookmarks.removeRecipeBookmark(bookmarks, type, recipe);
                    }
                }
            }
        });
    }

    /** Re-add prior removals (same runtime), then remove the current target set across all types. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void applyItems(IJeiRuntime runtimeRef, boolean exempt, boolean hideAll,
                                   Set<Item> shownItems, Set<Item> hiddenItems) {
        IIngredientManager ingredientManager = runtimeRef.getIngredientManager();

        // Restore whatever we previously removed on this runtime.
        removedIngredients.forEach((type, ingredients) -> {
            if (!ingredients.isEmpty()) {
                try {
                    ingredientManager.addIngredientsAtRuntime(type, new ArrayList<>(ingredients));
                } catch (Throwable e) {
                    LOGGER.warn("[recipehide] addIngredientsAtRuntime failed", e);
                }
            }
        });
        removedIngredients.clear();
        RecipeHideCatalystVisibility.set(Set.of());

        if (exempt) {
            return; // everything shown
        }

        // Catalyst items (crafting table, furnace, ...) are removed like any other hidden item so they
        // leave the item list. Removing them makes them invisible in every context; JEI would then hide
        // the whole recipe category (all catalysts invisible) and its "shown" recipes would be
        // unviewable. We record which removed items are catalysts and publish them to
        // RecipeHideCatalystVisibility, so JeiRecipeManagerInternalMixin keeps those categories viewable.
        Set<Item> catalystItems = collectCatalystItems(runtimeRef.getRecipeManager());
        Set<Item> hiddenCatalystItems = new HashSet<>();

        for (IIngredientType type : ingredientManager.getRegisteredIngredientTypes()) {
            boolean isItem = type == VanillaTypes.ITEM_STACK;
            if (!isItem && !hideAll) {
                continue; // non-item types only removed under hide-all
            }
            if (isItem && !hideAll && hiddenItems.isEmpty()) {
                continue; // nothing to remove
            }
            List toRemove = new ArrayList();
            for (Object ingredient : ingredientManager.getAllIngredients(type)) {
                if (isItem) {
                    ItemStack stack = (ItemStack) ingredient;
                    Item item = stack.getItem();
                    boolean shouldHide = hideAll ? !shownItems.contains(item) : hiddenItems.contains(item);
                    if (!shouldHide) {
                        continue;
                    }
                    toRemove.add(ingredient);
                    if (catalystItems.contains(item)) {
                        hiddenCatalystItems.add(item); // keep its recipe category viewable via the mixin
                    }
                } else {
                    toRemove.add(ingredient); // hide-all removes every non-item ingredient
                }
            }
            if (!toRemove.isEmpty()) {
                try {
                    ingredientManager.removeIngredientsAtRuntime(type, toRemove);
                    removedIngredients.put(type, toRemove);
                } catch (Throwable e) {
                    LOGGER.warn("[recipehide] removeIngredientsAtRuntime failed", e);
                }
            }
        }

        // Publish before/after removal is fine: the mixin only consults it when JEI recomputes category
        // visibility, which happens lazily on the next lookup.
        RecipeHideCatalystVisibility.set(hiddenCatalystItems);

        // Clear favorites for every physically-removed item (catalysts included).
        BookmarkList bookmarks = RecipeHideBookmarks.bookmarkList(runtime);
        if (bookmarks != null) {
            Set<Item> removedItems = new HashSet<>();
            List items = removedIngredients.get(VanillaTypes.ITEM_STACK);
            if (items != null) {
                for (Object stack : items) {
                    removedItems.add(((ItemStack) stack).getItem());
                }
            }
            if (!removedItems.isEmpty()) {
                RecipeHideBookmarks.removeItemBookmarks(bookmarks, removedItems);
            }
        }
    }

    /** Every item registered as a recipe-category catalyst, across all categories. Best-effort. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Set<Item> collectCatalystItems(IRecipeManager recipeManager) {
        Set<Item> catalysts = new HashSet<>();
        try {
            recipeManager.createRecipeCategoryLookup().includeHidden().get().forEach(category -> {
                RecipeType type = category.getRecipeType();
                try {
                    recipeManager.createRecipeCatalystLookup(type).includeHidden()
                            .get(VanillaTypes.ITEM_STACK)
                            .forEach(stack -> catalysts.add(((ItemStack) stack).getItem()));
                } catch (Throwable e) {
                    LOGGER.warn("[recipehide] catalyst lookup failed for {}", type.getUid(), e);
                }
            });
        } catch (Throwable e) {
            LOGGER.warn("[recipehide] catalyst enumeration failed", e);
        }
        return catalysts;
    }

    private static Set<Item> resolveItems(Set<ResourceLocation> ids) {
        Set<Item> items = new HashSet<>();
        for (ResourceLocation id : ids) {
            Item item = BuiltInRegistries.ITEM.get(id);
            if (item != Items.AIR) {
                items.add(item);
            }
        }
        return items;
    }
}
