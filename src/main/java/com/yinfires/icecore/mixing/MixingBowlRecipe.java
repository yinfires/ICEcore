package com.yinfires.icecore.mixing;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import com.yinfires.icecore.workstation.WorkstationContainerCompat;

import java.util.ArrayList;
import java.util.List;

public final class MixingBowlRecipe implements Recipe<Container> {
    public record Result(ItemStack stack, Ingredient carrier) {}
    public record FluidInput(FluidStack stack, ItemStack display) {
        public static final FluidInput EMPTY = new FluidInput(FluidStack.EMPTY, ItemStack.EMPTY);
        public boolean isEmpty() { return stack.isEmpty(); }
    }
    private final ResourceLocation id;
    private final NonNullList<Ingredient> ingredients;
    private final List<Result> results;
    private final FluidInput fluid;

    public MixingBowlRecipe(ResourceLocation id, NonNullList<Ingredient> ingredients, FluidInput fluid, List<Result> results) {
        this.id = id;
        this.ingredients = ingredients;
        this.fluid = fluid;
        this.results = List.copyOf(results);
    }

    public List<Result> results() { return results; }
    public FluidInput fluid() { return fluid; }

    public boolean matches(Container container, FluidStack actualFluid, Level level) {
        if (fluid.isEmpty() != actualFluid.isEmpty()) return false;
        if (!fluid.isEmpty() && (!fluid.stack().isFluidEqual(actualFluid)
                || fluid.stack().getAmount() != actualFluid.getAmount())) return false;
        return matches(container, level);
    }

    @Override public boolean matches(Container container, Level level) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < container.getContainerSize(); i++) if (!container.getItem(i).isEmpty()) stacks.add(container.getItem(i));
        if (stacks.size() != ingredients.size()) return false;
        boolean[] used = new boolean[stacks.size()];
        return matchIngredient(0, stacks, used);
    }

    private boolean matchIngredient(int index, List<ItemStack> stacks, boolean[] used) {
        if (index == ingredients.size()) return true;
        Ingredient ingredient = ingredients.get(index);
        for (int i = 0; i < stacks.size(); i++) {
            if (!used[i] && ingredient.test(stacks.get(i))) {
                used[i] = true;
                if (matchIngredient(index + 1, stacks, used)) return true;
                used[i] = false;
            }
        }
        return false;
    }

    @Override public ItemStack assemble(Container container, RegistryAccess access) { return results.get(0).stack().copy(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= ingredients.size(); }
    @Override public ItemStack getResultItem(RegistryAccess access) { return results.get(0).stack().copy(); }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return ModMixing.MIXING_BOWL_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ModMixing.MIXING_BOWL_RECIPE.get(); }
    @Override public NonNullList<Ingredient> getIngredients() { return ingredients; }

    public static final class Serializer implements RecipeSerializer<MixingBowlRecipe> {
        @Override public MixingBowlRecipe fromJson(ResourceLocation id, JsonObject json) {
            JsonArray inputJson = GsonHelper.getAsJsonArray(json, "ingredients");
            JsonArray outputJson = GsonHelper.getAsJsonArray(json, "results");
            boolean hasFluid = json.has("fluid");
            if (inputJson.size() + (hasFluid ? 1 : 0) < 1 || inputJson.size() + (hasFluid ? 1 : 0) > 9
                    || outputJson.size() < 1 || outputJson.size() > 9)
                throw new JsonParseException("Mixing bowl recipes require 1-9 combined item/fluid inputs and 1-9 results");
            NonNullList<Ingredient> inputs = NonNullList.create();
            inputJson.forEach(value -> {
                Ingredient ingredient = Ingredient.fromJson(value);
                if (ingredient.isEmpty()) throw new JsonParseException("Empty mixing bowl ingredient");
                for (ItemStack option : ingredient.getItems()) {
                    var container = WorkstationContainerCompat.describe(option);
                    if (container.isPresent() && container.get().behavior() == WorkstationContainerCompat.Behavior.FLUID)
                        throw new JsonParseException("Fluid containers must be declared in the mixing bowl fluid field");
                }
                inputs.add(ingredient);
            });
            FluidInput fluid = hasFluid ? parseFluid(json.getAsJsonObject("fluid")) : FluidInput.EMPTY;
            List<Result> outputs = new ArrayList<>();
            outputJson.forEach(value -> {
                JsonObject entry = value.getAsJsonObject();
                ItemStack stack = CraftingHelper.getItemStack(entry, true, false);
                if (stack.getCount() != 1) throw new JsonParseException("Mixing bowl result count must be 1");
                Ingredient carrier = entry.has("carrier") ? Ingredient.fromJson(entry.get("carrier")) : Ingredient.EMPTY;
                outputs.add(new Result(stack, carrier));
            });
            return new MixingBowlRecipe(id, inputs, fluid, outputs);
        }

        private static FluidInput parseFluid(JsonObject json) {
            ResourceLocation fluidId = ResourceLocation.tryParse(GsonHelper.getAsString(json, "fluid"));
            var registered = fluidId == null ? null : ForgeRegistries.FLUIDS.getValue(fluidId);
            int amount = GsonHelper.getAsInt(json, "amount");
            if (registered == null || amount < 1 || amount > 1000)
                throw new JsonParseException("Mixing bowl fluid requires a registered fluid and 1-1000 mB");
            ItemStack display = CraftingHelper.getItemStack(GsonHelper.getAsJsonObject(json, "display"), true, false);
            if (display.isEmpty() || display.getCount() != 1)
                throw new JsonParseException("Mixing bowl fluid display item must contain exactly one item");
            return new FluidInput(new FluidStack(registered, amount), display);
        }

        @Override public MixingBowlRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            int inputCount = buffer.readVarInt();
            NonNullList<Ingredient> inputs = NonNullList.create();
            for (int i = 0; i < inputCount; i++) inputs.add(Ingredient.fromNetwork(buffer));
            FluidInput fluid = buffer.readBoolean()
                    ? new FluidInput(buffer.readFluidStack(), buffer.readItem()) : FluidInput.EMPTY;
            int outputCount = buffer.readVarInt();
            List<Result> outputs = new ArrayList<>();
            for (int i = 0; i < outputCount; i++) outputs.add(new Result(buffer.readItem(), Ingredient.fromNetwork(buffer)));
            return new MixingBowlRecipe(id, inputs, fluid, outputs);
        }

        @Override public void toNetwork(FriendlyByteBuf buffer, MixingBowlRecipe recipe) {
            buffer.writeVarInt(recipe.ingredients.size());
            recipe.ingredients.forEach(value -> value.toNetwork(buffer));
            buffer.writeBoolean(!recipe.fluid.isEmpty());
            if (!recipe.fluid.isEmpty()) {
                buffer.writeFluidStack(recipe.fluid.stack());
                buffer.writeItem(recipe.fluid.display());
            }
            buffer.writeVarInt(recipe.results.size());
            recipe.results.forEach(value -> { buffer.writeItem(value.stack()); value.carrier().toNetwork(buffer); });
        }
    }
}
