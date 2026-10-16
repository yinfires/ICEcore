package com.yinfires.icecore.workstation;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.*;
import com.github.ysbbbbbb.kaleidoscopecookery.init.tag.TagMod;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import java.util.List;

public final class ExternalWorkstationAdapters {
    private ExternalWorkstationAdapters() {}
    public static WorkstationAdapter create(BlockEntity entity) {
        String type = entity.getClass().getName();
        if (!(entity instanceof WorkstationAccess)) return null;
        if (type.endsWith(".FermentationTankBlockEntity")) return new Inventory(entity, (Container)ExternalWorkstation.get(entity, "items"), null,
                (IFluidHandler)ExternalWorkstation.get(entity, "fluids"));
        if (type.endsWith(".FreezerBlockEntity")) return new Inventory(entity, null,
                (ItemStackHandler)ExternalWorkstation.get(entity, "inputInventory"), (IFluidHandler)ExternalWorkstation.get(entity, "tank"));
        if (net.minecraftforge.fml.ModList.get().isLoaded("kaleidoscope_cookery")) {
            if (entity instanceof PotBlockEntity pot) return new Cookery(entity, pot.getInputs());
            if (entity instanceof StockpotBlockEntity pot) return new Cookery(entity, pot.getInputs());
        }
        return null;
    }
    private abstract static class Base implements WorkstationAdapter {
        final BlockEntity entity;
        Base(BlockEntity entity) { this.entity = entity; }
        public WorkstationSlotState state() { return ((WorkstationAccess)entity).icecore$slots(); }
        public boolean allowsInput() { return true; }
        public boolean allowsOutput() { return true; }
        public void changed() { ExternalWorkstation.changed(entity); }
    }
    private static final class Inventory extends Base {
        final Container inventory; final ItemStackHandler handler; final IFluidHandler fluid;
        Inventory(BlockEntity entity, Container inventory, ItemStackHandler handler, IFluidHandler fluid) {
            super(entity); this.inventory = inventory; this.handler = handler; this.fluid = fluid;
        }
        public int size() { return inventory == null ? handler.getSlots() : inventory.getContainerSize(); }
        public ItemStack item(int i) { return inventory == null ? handler.getStackInSlot(i) : inventory.getItem(i); }
        public void item(int i, ItemStack stack) { if (inventory == null) handler.setStackInSlot(i, stack); else inventory.setItem(i, stack); }
        public IFluidHandler fluid() { return fluid; }
        public boolean accepts(ItemStack stack) { return inventory != null || handler.isItemValid(0, stack); }
        public ItemStack output() {
            if (inventory != null || ExternalWorkstation.number(entity, "outputCount") <= 0) return ItemStack.EMPTY;
            Object recipe = ExternalWorkstation.get(entity, "recipe");
            return recipe instanceof net.minecraft.world.item.crafting.Recipe<?> r ? r.getResultItem(entity.getLevel().registryAccess()).copyWithCount(ExternalWorkstation.number(entity, "outputCount")) : ItemStack.EMPTY;
        }
        public Ingredient carrier() { return inventory == null ? (Ingredient)ExternalWorkstation.call(ExternalWorkstation.get(entity, "recipe"), "getExtractIngredient") : Ingredient.EMPTY; }
        public void takeOutput() {
            ExternalWorkstation.set(entity, "outputCount", ExternalWorkstation.number(entity, "outputCount") - 1);
            ExternalWorkstation.call(entity, "checkForMatchingRecipe");
        }
        public void changed() {
            if (inventory != null) {
                ExternalWorkstation.set(entity, "recipeProgress", 0);
                ExternalWorkstation.call(entity, "notifyTile");
            }
            super.changed();
        }
    }
    private static final class Cookery extends Base {
        final List<ItemStack> items;
        Cookery(BlockEntity entity, List<ItemStack> items) { super(entity); this.items = items; }
        public int size() { return items.size(); }
        public ItemStack item(int i) { return items.get(i); }
        public void item(int i, ItemStack stack) { items.set(i, stack); }
        public boolean allowsInput() { return entity instanceof PotBlockEntity p ? p.getStatus() == 0 : ((StockpotBlockEntity)entity).getStatus() == 1; }
        public boolean accepts(ItemStack stack) { return !stack.is(TagMod.INGREDIENT_BLOCKLIST) && !com.github.ysbbbbbb.kaleidoscopecookery.compat.tetra.TetraCompat.isModularItem(stack); }
        public boolean allowsOutput() { return allowsInput() || !output().isEmpty(); }
        public ItemStack output() {
            if (entity instanceof PotBlockEntity p) return p.getStatus() == 2 || p.getStatus() == 3 ? p.getResult() : ItemStack.EMPTY;
            StockpotBlockEntity p = (StockpotBlockEntity)entity;
            return p.getStatus() == 3 && p.getTakeoutCount() > 0 ? p.getResult().copyWithCount(p.getTakeoutCount()) : ItemStack.EMPTY;
        }
        public Ingredient carrier() { return entity instanceof PotBlockEntity ? (Ingredient)ExternalWorkstation.get(entity, "carrier") : StockpotProducts.carrier((StockpotBlockEntity)entity); }
        public void takeOutput() {
            if (entity instanceof PotBlockEntity p) {
                ItemStack result = p.getResult(); result.shrink(1);
                if (result.isEmpty()) p.reset();
            } else {
                StockpotBlockEntity p = (StockpotBlockEntity)entity;
                int remaining = p.getTakeoutCount() - 1; ExternalWorkstation.set(p, "takeoutCount", remaining);
                if (remaining <= 0) {
                    p.setStatus(0); items.clear(); ExternalWorkstation.set(p, "result", ItemStack.EMPTY);
                    ExternalWorkstation.set(p, "recipeId", com.github.ysbbbbbb.kaleidoscopecookery.crafting.serializer.StockpotRecipeSerializer.EMPTY_ID);
                    ExternalWorkstation.set(p, "soupBaseId", com.github.ysbbbbbb.kaleidoscopecookery.init.ModSoupBases.WATER);
                    ExternalWorkstation.set(p, "currentTick", -1); p.renderEntity = null;
                }
            }
        }
        public void changed() { ExternalWorkstation.call(entity, "refresh"); }
    }
}
