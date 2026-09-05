package com.yinfires.icecore.mixin;

import com.yinfires.icecore.recipehide.RecipeHideRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Optional;

/**
 * Server-authoritative recipe hiding: filters {@link RecipeManager}'s crafting lookups so forbidden
 * recipes cannot be crafted through any vanilla mechanic (crafting table, furnace family, stonecutter,
 * smithing). Runs on both sides; the client's {@link RecipeHideRegistry} is filled from the sync
 * packet so client-side prediction matches the server.
 *
 * <p>Only {@code byType} and {@code byKey} are filtered — the actual craft path is
 * {@code getRecipeFor}→{@code byType}. {@code getRecipes()} (enumeration used by the server's own
 * item→recipe resolution) is intentionally left unfiltered. JEI builds its runtime through
 * {@code getAllRecipesFor}→{@code byType}, so while JEI is building, {@link RecipeHideRegistry}
 * bypasses filtering on non-server threads — otherwise JEI would permanently register only the
 * unfiltered subset and later "show" commands could never restore the missing recipes.
 *
 * <p>Selectors use production SRG names + full descriptors (no refmap): {@code m_44054_} = byType,
 * {@code m_44043_} = byKey.
 */
@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {
    @SuppressWarnings("unchecked")
    @Inject(method = "m_44054_(Lnet/minecraft/world/item/crafting/RecipeType;)Ljava/util/Map;",
            at = @At("RETURN"), cancellable = true)
    private void icecore$filterByType(net.minecraft.world.item.crafting.RecipeType<?> type,
                                      CallbackInfoReturnable<Map<ResourceLocation, ?>> cir) {
        Map<ResourceLocation, ?> original = cir.getReturnValue();
        Map<ResourceLocation, ?> filtered = RecipeHideRegistry.filterMap((Map<ResourceLocation, Object>) original);
        if (filtered != original) {
            cir.setReturnValue(filtered);
        }
    }

    @Inject(method = "m_44043_(Lnet/minecraft/resources/ResourceLocation;)Ljava/util/Optional;",
            at = @At("RETURN"), cancellable = true)
    private void icecore$filterByKey(ResourceLocation id,
                                     CallbackInfoReturnable<Optional<? extends Recipe<?>>> cir) {
        Optional<? extends Recipe<?>> original = cir.getReturnValue();
        Optional<? extends Recipe<?>> filtered = RecipeHideRegistry.filterOptional(original);
        if (filtered != original) {
            cir.setReturnValue(filtered);
        }
    }
}
