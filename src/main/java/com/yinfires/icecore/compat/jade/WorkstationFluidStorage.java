package com.yinfires.icecore.compat.jade;

import com.yinfires.icecore.workstation.ExternalWorkstation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.capability.IFluidHandler;
import snownee.jade.api.*;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.view.*;
import java.util.*;

/** Display-only tea volume; real tanks retain their real capacity and contents. */
public enum WorkstationFluidStorage implements IServerExtensionProvider<Object, CompoundTag>, IClientExtensionProvider<CompoundTag, FluidView> {
    INSTANCE;
    public ResourceLocation getUid() { return ResourceLocation.fromNamespaceAndPath("icecore", "workstation_fluids"); }
    public List<ClientViewGroup<FluidView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<CompoundTag>> groups) {
        return ClientViewGroup.map(groups, FluidView::readDefault, null);
    }
    public List<ViewGroup<CompoundTag>> getGroups(ServerPlayer player, ServerLevel level, Object target, boolean details) {
        if (!(target instanceof BlockEntity entity) || !WorkstationJadeProvider.supported(entity)) return null;
        String type = entity.getClass().getSimpleName();
        List<CompoundTag> fluids = new ArrayList<>();
        if (type.equals("TeapotBlockEntity")) {
            if (ExternalWorkstation.number(entity, "status") == 2) return List.of();
            var id = (ResourceLocation)ExternalWorkstation.get(entity, "teaFluidId");
            var fluid = net.minecraftforge.registries.ForgeRegistries.FLUIDS.getValue(id);
            if (fluid == null) fluid = net.minecraft.world.level.material.Fluids.EMPTY;
            int amount = fluid == net.minecraft.world.level.material.Fluids.EMPTY ? 0 : 1000;
            fluids.add(FluidView.writeDefault(JadeFluidObject.of(fluid, amount, null), 1000));
        } else {
            Object tank = switch(type) {
                case "FermentationTankBlockEntity" -> ExternalWorkstation.get(entity, "fluids");
                case "FreezerBlockEntity" -> ExternalWorkstation.get(entity, "tank");
                case "BarrelBlockEntity" -> ExternalWorkstation.get(entity, "fluid");
                default -> null;
            };
            if (tank instanceof IFluidHandler handler) for (int i = 0; i < handler.getTanks(); i++) {
                var fluid = handler.getFluidInTank(i);
                fluids.add(FluidView.writeDefault(JadeFluidObject.of(fluid.getFluid(), fluid.getAmount(), fluid.getTag()), handler.getTankCapacity(i)));
            }
        }
        return fluids.isEmpty() ? List.of() : List.of(new ViewGroup<>(fluids));
    }
}
