package com.yinfires.icecore.recipehide.client;

import net.minecraft.world.item.Item;

import java.util.Set;

/**
 * Client-side registry of catalyst items (crafting table, furnace, ...) that {@link RecipeHideJeiApplier}
 * physically removed from the JEI item list so they no longer show up there.
 *
 * <p>Removing a catalyst makes it invisible in every JEI context. JEI 15.49.0.188's
 * {@code RecipeManagerInternal.isCategoryHidden} then hides a whole recipe category when all of its
 * catalysts are invisible — which would make a "shown" item's recipes unviewable. This version of JEI
 * has no per-context {@code hideIngredients} API, so {@code JeiRecipeManagerInternalMixin} redirects
 * that check and consults this set: a category whose catalyst we deliberately hid is treated as still
 * having a visible catalyst, keeping its recipes viewable while the catalyst stays out of the list.
 *
 * <p>Empty when the feature is inactive, so JEI behaves exactly as stock.
 */
public final class RecipeHideCatalystVisibility {
    private static volatile Set<Item> hidden = Set.of();

    private RecipeHideCatalystVisibility() {
    }

    /** Replaces the set of catalyst items currently hidden from the item list. */
    public static void set(Set<Item> items) {
        hidden = items.isEmpty() ? Set.of() : Set.copyOf(items);
    }

    /** True when at least one catalyst is hidden; lets the mixin short-circuit to stock behavior. */
    public static boolean anyHidden() {
        return !hidden.isEmpty();
    }

    /** True when {@code item} is a catalyst we deliberately hid from the item list. */
    public static boolean isHidden(Item item) {
        return hidden.contains(item);
    }
}
