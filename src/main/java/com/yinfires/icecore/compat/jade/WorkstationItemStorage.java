package com.yinfires.icecore.compat.jade;

import com.yinfires.icecore.workstation.ExternalWorkstation;
import com.yinfires.icecore.fridge.FridgePairing;
import net.blay09.mods.cookingforblockheads.tile.FridgeBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.ItemStackHandler;
import snownee.jade.api.*;
import snownee.jade.api.view.*;
import snownee.jade.addon.universal.ItemCollector;
import snownee.jade.addon.universal.ItemIterator;
import java.util.*;

/** Read-only projection into Jade's standard item storage UI. */
public enum WorkstationItemStorage implements IServerExtensionProvider<Object, ItemStack>, IClientExtensionProvider<ItemStack, ItemView> {
    INSTANCE;
    public ResourceLocation getUid() { return ResourceLocation.fromNamespaceAndPath("icecore", "workstation_items"); }
    public List<ClientViewGroup<ItemView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<ItemStack>> groups) {
        return ClientViewGroup.map(groups, ItemView::new, null);
    }
    public List<ViewGroup<ItemStack>> getGroups(ServerPlayer player, ServerLevel level, Object target, boolean details) {
        if (target instanceof FridgeBlockEntity fridge && FridgePairing.integrated(fridge.getBlockState())) {
            // Native pairing resolves either half to the same upper + lower inventory.
            Container container = fridge.getCombinedContainer();
            return new ItemCollector<Container>(new ItemIterator.ContainerItemIterator(0))
                    .update(container, level.getGameTime());
        }
        if (!(target instanceof BlockEntity entity) || !WorkstationJadeProvider.supported(entity)) return null;
        CompoundTag data = WorkstationJadeProvider.snapshot(entity);
        List<ItemStack> items = new ArrayList<>();
        var products = data.getList("Products", 10);
        if (!products.isEmpty()) {
            for (int i = 0; i < products.size(); i++) items.add(ItemStack.of(products.getCompound(i)));
        } else {
            Object contents = switch(entity.getClass().getSimpleName()) {
                case "PotBlockEntity", "StockpotBlockEntity" -> ExternalWorkstation.call(entity, "getInputs");
                case "CuisineBoardBlockEntity" -> ExternalWorkstation.get(entity, "contents");
                case "FermentationTankBlockEntity" -> ExternalWorkstation.get(entity, "items");
                case "FreezerBlockEntity" -> ExternalWorkstation.get(entity, "inputInventory");
                case "SteamerBlockEntity" -> ExternalWorkstation.call(entity, "getItems");
                case "BarrelBlockEntity" -> ExternalWorkstation.get(entity, "ingredient");
                case "OvenBlockEntity" -> entity;
                default -> null;
            };
            if (contents instanceof Container container) {
                for (int i = 0; i < container.getContainerSize(); i++) items.add(container.getItem(i).copy());
            }
            else if (contents instanceof ItemStackHandler handler) {
                for (int i = 0; i < handler.getSlots(); i++) items.add(handler.getStackInSlot(i).copy());
            } else if (contents instanceof List<?> list) {
                for (Object item : list) if (item instanceof ItemStack stack) items.add(stack.copy());
            } else {
                var inputs = data.getList("Inputs", 10);
                for (int i = 0; i < inputs.size(); i++) items.add(ItemStack.of(inputs.getCompound(i)));
            }
        }
        return List.of(new ViewGroup<>(items));
    }
}
