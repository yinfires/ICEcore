package com.yinfires.icecore.workstation;

import com.yinfires.icecore.feedback.PlayerFeedback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

public final class WorkstationInteractionService {
    private WorkstationInteractionService() {}
    public static boolean interact(WorkstationAdapter adapter, ServerPlayer player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        var state = adapter.state();
        state.reconcile(adapter.size(), adapter::item);
        IFluidHandler tank = adapter.fluid();
        if (tank != null && !held.isEmpty()) {
            var filled = WorkstationContainerCompat.fillContainer(held, tank.getFluidInTank(0));
            if (filled.isPresent()) {
                var transfer = filled.get();
                if (tank.drain(transfer.amount(), IFluidHandler.FluidAction.SIMULATE).getAmount() == transfer.amount()) {
                    tank.drain(transfer.amount(), IFluidHandler.FluidAction.EXECUTE);
                    WorkstationContainerInteraction.exchange(player, hand, held, transfer.stack()); adapter.changed();
                    sound(player, WorkstationContainerCompat.fillSound(transfer.category())); return true;
                }
            }
        }
        if (adapter.allowsOutput() && !adapter.output().isEmpty()) {
            ItemStack output = adapter.output().copyWithCount(1);
            var carrier = adapter.carrier();
            if (carrier.isEmpty() ? held.isEmpty() : carrier.test(held)) {
                if (!give(player, hand, held, output, !carrier.isEmpty())) return true;
                adapter.takeOutput(); adapter.changed();
                sound(player, SoundEvents.ITEM_FRAME_REMOVE_ITEM); return true;
            }
            if (held.isEmpty() && !carrier.isEmpty()) need(player, carrier.getItems()[0]);
            return true;
        }
        int last = state.last(adapter::item);
        if (adapter.allowsOutput() && last >= 0 && (held.isEmpty() || WorkstationContainerCompat.isKnownEmptyContainer(held))) {
            ItemStack required = state.container(last);
            if (WorkstationContainerInteraction.canTake(required, held)) {
                ItemStack item = adapter.item(last).copyWithCount(1);
                if (!give(player, hand, held, item, !required.isEmpty())) return true;
                ItemStack remainder = adapter.item(last).copy(); remainder.shrink(1); adapter.item(last, remainder);
                state.reconcile(adapter.size(), adapter::item); adapter.changed();
                sound(player, SoundEvents.ITEM_FRAME_REMOVE_ITEM); return true;
            }
            if (held.isEmpty() && !required.isEmpty()) { need(player, required); return true; }
        }
        if (tank != null && !held.isEmpty()) {
            var entry = WorkstationContainerCompat.describe(held);
            if (entry.isPresent() && entry.get().behavior() == WorkstationContainerCompat.Behavior.FLUID) {
                var content = entry.get();
                if (tank.fill(content.fluid(), IFluidHandler.FluidAction.SIMULATE) == content.fluid().getAmount()) {
                    tank.fill(content.fluid(), IFluidHandler.FluidAction.EXECUTE);
                    WorkstationContainerInteraction.exchange(player, hand, held, content.empty()); adapter.changed();
                    sound(player, WorkstationContainerCompat.emptySound(content.category()));
                }
                return true;
            }
            if (!tank.getFluidInTank(0).isEmpty() && WorkstationContainerCompat.isFluidContainer(held)) return true;
        }
        if (!adapter.allowsInput() || held.isEmpty() || !adapter.accepts(held)) return false;
        for (int i = 0; i < adapter.size(); i++) if (adapter.item(i).isEmpty()) {
            ItemStack input = held.copyWithCount(1);
            ItemStack empty = WorkstationContainerInteraction.requiredEmptyContainer(input,
                    tank == null ? WorkstationContainerInteraction.FluidContainerMode.STORED_AS_ITEM : WorkstationContainerInteraction.FluidContainerMode.REAL_FLUID_STORAGE);
            adapter.item(i, input); state.inserted(i, input, empty); adapter.changed();
            if (!empty.isEmpty()) WorkstationContainerInteraction.exchange(player, hand, held, empty);
            else if (!player.getAbilities().instabuild) held.shrink(1);
            sound(player, SoundEvents.ITEM_FRAME_ADD_ITEM); return true;
        }
        return true;
    }
    private static boolean give(ServerPlayer player, InteractionHand hand, ItemStack held, ItemStack output, boolean consume) {
        if (consume) { WorkstationContainerInteraction.exchange(player, hand, held, output); return true; }
        return WorkstationInventoryTransfer.giveOne(player, output);
    }
    private static void sound(ServerPlayer player, net.minecraft.sounds.SoundEvent sound) {
        player.level().playSound(null, player.blockPosition(), sound, SoundSource.BLOCKS, .8F, 1.1F);
    }
    public static void need(ServerPlayer player, ItemStack container) { PlayerFeedback.show(player, Component.translatable("icecore.workstation.need_container", container.getHoverName())); }
}
