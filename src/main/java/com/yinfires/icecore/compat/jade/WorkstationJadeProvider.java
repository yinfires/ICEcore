package com.yinfires.icecore.compat.jade;

import com.yinfires.icecore.workstation.*;
import com.yinfires.icecore.oven.OvenBlockEntity;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.ItemStackHandler;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

/** Only serializes the workstation currently requested by Jade; no world/tick scans. */
public enum WorkstationJadeProvider implements IServerDataProvider<BlockAccessor>, IBlockComponentProvider {
    INSTANCE;
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("icecore", "workstation");
    private static final String KEY = "ICEcoreWorkstation";
    public static boolean supported(BlockEntity entity) {
        if (entity == null) return false;
        return switch (entity.getClass().getSimpleName()) {
            case "PotBlockEntity", "StockpotBlockEntity", "FermentationTankBlockEntity", "CuisineBoardBlockEntity",
                 "FreezerBlockEntity", "TeapotBlockEntity", "SteamerBlockEntity", "BarrelBlockEntity", "OvenBlockEntity" -> true;
            default -> false;
        };
    }
    @Override public void appendServerData(CompoundTag root, BlockAccessor accessor) {
        BlockEntity entity = accessor.getBlockEntity();
        // The tavern barrel occupies several blocks; all parts resolve to its origin.
        var id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(accessor.getBlock());
        if (id != null && id.toString().equals("kaleidoscope_tavern:barrel")) {
            entity = com.yinfires.icecore.compat.jade.TavernJadeAccess.origin(accessor);
        }
        if (!supported(entity)) return;
        root.put(KEY, snapshot(entity));
    }
    public static CompoundTag snapshot(BlockEntity entity) {
        CompoundTag data = new CompoundTag(); ListTag products = new ListTag(); ListTag inputs = new ListTag();
        String type = entity.getClass().getSimpleName(); data.putString("Type", type);
        switch (type) {
            case "PotBlockEntity" -> {
                int status = ExternalWorkstation.number(entity, "status");
                if (status >= 2) add(products, (ItemStack)ExternalWorkstation.get(entity, "result"));
                if (status == 1) timer(data, "cooking", ExternalWorkstation.number(entity, "currentTick"));
                data.putString("Status", "tip.kaleidoscope_cookery.pot." + switch(status) {
                    case 0 -> entity.getBlockState().getValue(com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.PotBlock.HAS_OIL) ? "add_ingredient" : "need_oil"; case 1 -> "need_stir_fry"; case 2 -> "done"; default -> "done";
                });
            }
            case "StockpotBlockEntity" -> {
                int status = ExternalWorkstation.number(entity, "status");
                if (status == 3) add(products, ((ItemStack)ExternalWorkstation.get(entity, "result")).copyWithCount(ExternalWorkstation.number(entity, "takeoutCount")));
                if (status == 2) timer(data, "cooking", ExternalWorkstation.number(entity, "currentTick"));
            }
            case "FermentationTankBlockEntity" -> {
                if (!entity.getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN)) {
                    int total = ExternalWorkstation.number(entity, "totalTime");
                    if (total > 0) timer(data, "fermenting", total - ExternalWorkstation.number(entity, "recipeProgress"));
                }
                Container items = (Container)ExternalWorkstation.get(entity, "items");
                for (int i = 0; i < items.getContainerSize(); i++) add(inputs, items.getItem(i));
            }
            case "CuisineBoardBlockEntity" -> {
                Object model = ExternalWorkstation.call(entity, "getModel");
                var result = (java.util.Optional<?>)ExternalWorkstation.invoke(model, "complete", new Class<?>[]{net.minecraft.world.level.Level.class}, entity.getLevel());
                result.ifPresent(item -> add(products, (ItemStack)item));
            }
            case "FreezerBlockEntity" -> {
                int count = ExternalWorkstation.number(entity, "outputCount");
                Object recipe = ExternalWorkstation.get(entity, "recipe");
                if (count > 0 && recipe instanceof net.minecraft.world.item.crafting.Recipe<?> r) add(products, r.getResultItem(entity.getLevel().registryAccess()).copyWithCount(count));
                if ((Boolean)ExternalWorkstation.call(entity, "isWorking")) timer(data, "freezing", ExternalWorkstation.number(entity, "maxProgress") - ExternalWorkstation.number(entity, "progress"));
            }
            case "TeapotBlockEntity" -> {
                int status = ExternalWorkstation.number(entity, "status");
                if (status == 2) add(products, (ItemStack)ExternalWorkstation.get(entity, "result"));
                else add(inputs, (ItemStack)ExternalWorkstation.get(entity, "input"));
                ResourceLocation fluid = (ResourceLocation)ExternalWorkstation.get(entity, "teaFluidId");
                var registered = net.minecraftforge.registries.ForgeRegistries.FLUIDS.getValue(fluid);
                data.putString("Fluid", registered == null ? "minecraft:empty" : fluid.toString());
                data.putInt("Capacity", 1000); data.putInt("Amount", registered == null || registered == net.minecraft.world.level.material.Fluids.EMPTY ? 0 : 1000);
                if (status == 1) timer(data, "boiling", ExternalWorkstation.number(entity, "currentTick"));
                if (status == 2) data.putString("Status", "tooltip.kaleidoscope_cookery.teapot.progress.finished");
                if (status == 2) { data.remove("Capacity"); data.remove("Amount"); data.remove("Fluid"); }
            }
            case "SteamerBlockEntity" -> {
                if (SteamerControl.closed(entity.getLevel(), entity.getBlockPos())) {
                    int[] progress = (int[])ExternalWorkstation.get(entity, "cookingProgress"), times = (int[])ExternalWorkstation.get(entity, "cookingTime");
                    int remaining = -1;
                    for (int i = 0; i < times.length; i++) if (times[i] > 0) remaining = Math.max(remaining, times[i] - progress[i]);
                    if (remaining >= 0) timer(data, "steaming", remaining);
                }
            }
            case "BarrelBlockEntity" -> {
                ItemStackHandler output = (ItemStackHandler)ExternalWorkstation.call(entity, "getOutput");
                for (int i = 0; i < output.getSlots(); i++) add(products, output.getStackInSlot(i));
                ItemStackHandler items = (ItemStackHandler)ExternalWorkstation.get(entity, "ingredient");
                for (int i = 0; i < items.getSlots(); i++) add(inputs, items.getStackInSlot(i));
                IFluidHandler tank = (IFluidHandler)ExternalWorkstation.get(entity, "fluid");
                var fluid = tank.getFluidInTank(0); data.put("FluidStack", fluid.writeToNBT(new CompoundTag()));
                data.putInt("Capacity", 4000); data.putInt("Amount", fluid.getAmount());
            }
            case "OvenBlockEntity" -> {
                OvenBlockEntity oven = (OvenBlockEntity)entity;
                if (oven.baking()) {
                    var recipe = (com.yinfires.icecore.oven.OvenRecipe)ExternalWorkstation.get(oven, "active");
                    timer(data, "baking", recipe.cookingTime() - oven.progress());
                }
            }
        }
        data.put("Products", products); data.put("Inputs", inputs); return data;
    }
    private static void timer(CompoundTag tag, String operation, int ticks) { tag.putString("Operation", operation); tag.putInt("Remaining", Math.max(0, ticks)); }
    private static void add(ListTag list, ItemStack stack) { if (!stack.isEmpty()) list.add(stack.save(new CompoundTag())); }
    @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag root = accessor.getServerData(); if (!root.contains(KEY)) return;
        CompoundTag data = root.getCompound(KEY);
        if (data.contains("Operation")) {
            int seconds = data.getInt("Remaining") / 20;
            tooltip.add(Component.translatable("icecore.workstation." + data.getString("Operation"), String.format(java.util.Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60)));
        } else if (data.contains("Status")) tooltip.add(Component.translatable(data.getString("Status")));
    }
    @Override public ResourceLocation getUid() { return UID; }
    @Override public int getDefaultPriority() { return 10000; }
    @Override public boolean isRequired() { return true; }
}
