package com.yinfires.icecore.mixin;

import com.yinfires.icecore.recipehide.client.RecipeHideCatalystVisibility;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.library.recipes.RecipeManagerInternal;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.stream.Stream;

/**
 * Keeps a recipe category viewable after ICEcore's recipe-hide removes one of its catalyst items from
 * the JEI item list.
 *
 * <p>JEI 15.49.0.188's {@code isCategoryHidden} hides a whole category when it has catalysts but none
 * are visible (its second check, using {@code getRecipeCatalystStream(type, false)} — the
 * visibility-filtered stream). ICEcore hides a catalyst by physically removing it, which makes it
 * invisible in every context, so a "shown" item's recipes would become unviewable. This JEI version
 * has no per-context {@code hideIngredients} API, so we intercept that one filtered stream: catalysts
 * ICEcore deliberately hid are treated as still present, so the category is not judged hidden and its
 * recipes stay viewable — while the catalyst remains gone from the item list.
 *
 * <p>Scope is surgical: only the {@code includeHidden == false} call inside {@code isCategoryHidden} is
 * redirected (the {@code true} call, used to test "has catalysts at all", is untouched), and when no
 * catalyst is hidden the stream is returned verbatim, so JEI behaves exactly as stock.
 */
@Mixin(value = RecipeManagerInternal.class, remap = false)
public abstract class JeiRecipeManagerInternalMixin {
    @SuppressWarnings("unchecked")
    @Redirect(
            method = "isCategoryHidden",
            at = @At(value = "INVOKE",
                    target = "Lmezz/jei/library/recipes/RecipeManagerInternal;"
                            + "getRecipeCatalystStream(Lmezz/jei/api/recipe/RecipeType;Z)Ljava/util/stream/Stream;",
                    ordinal = 1))
    private Stream<ITypedIngredient<?>> icecore$keepHiddenCatalystsVisible(RecipeManagerInternal self,
                                                                          RecipeType<?> type,
                                                                          boolean includeHidden) {
        Stream<ITypedIngredient<?>> visible =
                (Stream<ITypedIngredient<?>>) (Stream<?>) self.getRecipeCatalystStream(type, includeHidden);
        if (!RecipeHideCatalystVisibility.anyHidden()) {
            return visible; // feature inactive → stock behavior
        }
        // Append catalysts we deliberately hid (invisible now) drawn from the full catalyst list, so the
        // "no visible catalyst" check sees them as present and the category stays viewable.
        Stream<ITypedIngredient<?>> hiddenOurs =
                ((Stream<ITypedIngredient<?>>) (Stream<?>) self.getRecipeCatalystStream(type, true))
                        .filter(JeiRecipeManagerInternalMixin::icecore$isOurHiddenCatalyst);
        return Stream.concat(visible, hiddenOurs);
    }

    private static boolean icecore$isOurHiddenCatalyst(ITypedIngredient<?> ingredient) {
        return ingredient.getItemStack()
                .map(ItemStack::getItem)
                .map(RecipeHideCatalystVisibility::isHidden)
                .orElse(false);
    }
}
