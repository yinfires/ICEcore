package com.yinfires.icecore.oven;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.yinfires.icecore.workstation.BatchProcessingService;
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

public final class OvenRecipe implements Recipe<Container> {
    private final ResourceLocation id; private final NonNullList<Ingredient> ingredients;
    private final NonNullList<ItemStack> results; private final int cookingTime; private final boolean batch; private final int maxBatches;
    public OvenRecipe(ResourceLocation id, NonNullList<Ingredient> ingredients, NonNullList<ItemStack> results, int cookingTime, boolean batch, int maxBatches) {
        this.id=id; this.ingredients=ingredients; this.results=results; this.cookingTime=cookingTime; this.batch=batch; this.maxBatches=maxBatches;
    }
    public int cookingTime(){return cookingTime;} public NonNullList<ItemStack> results(){return results;}
    public boolean batch(){return batch;} public int maxBatches(){return maxBatches;}
    public BatchProcessingService.Match match(Container c, int limit){return BatchProcessingService.match(c, ingredients, batch ? Math.min(limit, maxBatches) : 1);}
    @Override public boolean matches(Container c, Level l){return match(c, 1).batches()>0;}
    @Override public ItemStack assemble(Container c, RegistryAccess a){return results.get(0).copy();}
    @Override public boolean canCraftInDimensions(int w,int h){return w*h>=ingredients.size();}
    @Override public ItemStack getResultItem(RegistryAccess a){return results.get(0).copy();}
    @Override public ResourceLocation getId(){return id;}
    @Override public RecipeSerializer<?> getSerializer(){return ModOven.OVEN_SERIALIZER.get();}
    @Override public RecipeType<?> getType(){return ModOven.OVEN_RECIPE.get();}
    @Override public NonNullList<Ingredient> getIngredients(){return ingredients;}
    public static final class Serializer implements RecipeSerializer<OvenRecipe> {
        @Override public OvenRecipe fromJson(ResourceLocation id, JsonObject json){
            JsonArray in=GsonHelper.getAsJsonArray(json,"ingredients"); JsonArray out=GsonHelper.getAsJsonArray(json,"results");
            if(in.size()<1||in.size()>9||out.size()<1||out.size()>9) throw new JsonParseException("Oven supports 1-9 inputs and outputs");
            NonNullList<Ingredient> ingredients=NonNullList.create(); in.forEach(v->ingredients.add(Ingredient.fromJson(v)));
            NonNullList<ItemStack> results=NonNullList.create(); out.forEach(v->{ItemStack stack=CraftingHelper.getItemStack(v.getAsJsonObject(),true,false); if(stack.getCount()!=1) throw new JsonParseException("Oven results must be single items"); results.add(stack);});
            int time=GsonHelper.getAsInt(json,"cooking_time",400); if(time<1) throw new JsonParseException("cooking_time must be positive");
            boolean batch=false; int maxBatches=1;
            if (json.has("processing")) {
                JsonObject processing=GsonHelper.getAsJsonObject(json,"processing");
                String mode=GsonHelper.getAsString(processing,"mode","single");
                if (!mode.equals("single") && !mode.equals("batch")) throw new JsonParseException("processing.mode must be single or batch");
                batch=mode.equals("batch"); maxBatches=GsonHelper.getAsInt(processing,"max_batches",9);
                if (batch && (maxBatches<1 || maxBatches>9)) throw new JsonParseException("processing.max_batches must be 1-9");
            }
            return new OvenRecipe(id,ingredients,results,time,batch,maxBatches);
        }
        @Override public OvenRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf b){int n=b.readVarInt(); NonNullList<Ingredient> in=NonNullList.create(); for(int i=0;i<n;i++)in.add(Ingredient.fromNetwork(b)); int m=b.readVarInt(); NonNullList<ItemStack> out=NonNullList.create(); for(int i=0;i<m;i++)out.add(b.readItem()); return new OvenRecipe(id,in,out,b.readVarInt(),b.readBoolean(),b.readVarInt());}
        @Override public void toNetwork(FriendlyByteBuf b,OvenRecipe r){b.writeVarInt(r.ingredients.size());r.ingredients.forEach(i->i.toNetwork(b));b.writeVarInt(r.results.size());r.results.forEach(b::writeItem);b.writeVarInt(r.cookingTime);b.writeBoolean(r.batch);b.writeVarInt(r.maxBatches);}
    }
}





