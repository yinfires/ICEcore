package com.yinfires.icecore.workstation;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.StockpotBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

public final class StockpotProducts {
    private StockpotProducts() {}
    private static final java.lang.reflect.Field RECIPE_ID = ExternalWorkstation.field(StockpotBlockEntity.class, "recipeId");
    public static Ingredient carrier(StockpotBlockEntity pot) {
        ResourceLocation id;
        try { id = (ResourceLocation)RECIPE_ID.get(pot); }
        catch (IllegalAccessException e) { throw new IllegalStateException(e); }
        var recipe = id == null ? null : pot.getLevel().getRecipeManager().byKey(id).orElse(null);
        if (recipe instanceof StockpotRecipe r) return r.carrier();
        if (recipe instanceof FlexStockpotRecipe r) return r.carrier();
        return WorkstationContainerCompat.resolveOutputCarrier(pot.getResult(), null);
    }
}
