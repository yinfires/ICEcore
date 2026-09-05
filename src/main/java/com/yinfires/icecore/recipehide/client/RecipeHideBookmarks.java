package com.yinfires.icecore.recipehide.client;

import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IBookmarkOverlay;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.gui.bookmarks.BookmarkList;
import mezz.jei.gui.bookmarks.IBookmark;
import mezz.jei.gui.overlay.elements.IElement;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Isolated, best-effort clearing of JEI favorites (bookmarks). JEI exposes no public API to remove
 * bookmarks and does not drop them when a recipe/ingredient is hidden, so this reaches the internal
 * {@link BookmarkList} by reflecting {@code BookmarkOverlay.bookmarkList}. This deliberately couples
 * to JEI 15.49 internals; every path is guarded so a JEI change only means favorites aren't cleared,
 * never a crash.
 */
public final class RecipeHideBookmarks {
    private static Field bookmarkListField;
    private static boolean fieldResolved;

    private RecipeHideBookmarks() {
    }

    /** Best-effort access to the internal bookmark list; null if unavailable. */
    static BookmarkList bookmarkList(IJeiRuntime runtime) {
        try {
            IBookmarkOverlay overlay = runtime.getBookmarkOverlay();
            if (overlay == null) {
                return null;
            }
            if (!fieldResolved) {
                fieldResolved = true;
                Field field = overlay.getClass().getDeclaredField("bookmarkList");
                field.setAccessible(true);
                bookmarkListField = field;
            }
            if (bookmarkListField == null) {
                return null;
            }
            Object value = bookmarkListField.get(overlay);
            return value instanceof BookmarkList list ? list : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Removes any item favorites whose stack's Item is in {@code hiddenItems}. */
    static void removeItemBookmarks(BookmarkList bookmarks, Set<Item> hiddenItems) {
        try {
            List<IBookmark> toRemove = new ArrayList<>();
            for (IElement<?> element : bookmarks.getElements()) {
                element.getBookmark().ifPresent(bookmark -> {
                    ItemStack stack = element.getTypedIngredient().getItemStack().orElse(ItemStack.EMPTY);
                    if (!stack.isEmpty() && hiddenItems.contains(stack.getItem())) {
                        toRemove.add(bookmark);
                    }
                });
            }
            for (IBookmark bookmark : toRemove) {
                bookmarks.remove(bookmark);
            }
        } catch (Throwable ignored) {
        }
    }

    /** Removes the favorite for a specific recipe, if present. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    static void removeRecipeBookmark(BookmarkList bookmarks, RecipeType type, Object recipe) {
        try {
            IBookmark bookmark = bookmarks.getMatchingBookmark(type, recipe);
            if (bookmark != null) {
                bookmarks.remove(bookmark);
            }
        } catch (Throwable ignored) {
        }
    }
}
