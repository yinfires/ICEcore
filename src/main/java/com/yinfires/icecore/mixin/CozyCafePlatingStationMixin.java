package com.yinfires.icecore.mixin;

import com.yinfires.icecore.compat.cozycafe.CafeServingRules;
import com.yinfires.icecore.feedback.PlayerFeedback;
import com.yinfires.icecore.workstation.WorkstationInventoryTransfer;
import io.github.chakyl.cozycafe.item.ServingPlateItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep native empty-station placement and plate creation/storage, replace eligibility and pickup. */
@Mixin(targets="io.github.chakyl.cozycafe.blockentities.PlatingStationBlockEntity",remap=false)
public abstract class CozyCafePlatingStationMixin {
    @Shadow private ItemStack plateItem;
    @Shadow private void setChangedForRender() { throw new AssertionError(); }

    @Inject(method="handlePlating(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",at=@At("HEAD"),cancellable=true)
    private void icecore$plate(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (plateItem.isEmpty()) return;
        ItemStack held=player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (held.isEmpty() || !plateItem.is(io.github.chakyl.cozycafe.registry.CozyRegistry.ItemRegistry.SERVING_PLATE.get())) {
                if (WorkstationInventoryTransfer.giveOne(player,plateItem)) {
                    plateItem.shrink(1);
                    setChangedForRender();
                } else PlayerFeedback.showLocal(player,Component.translatable("message.icecore.cafe.inventory_full"));
            } else if (!CafeServingRules.requiresPlate(held,false)) {
                PlayerFeedback.showLocal(player,Component.translatable("message.icecore.cafe.no_plating"));
            } else if (!ServingPlateItem.getStoredFood(plateItem).isEmpty()) {
                PlayerFeedback.showLocal(player,Component.translatable("block.cozycafe.plating_station.already_plated"));
            } else {
                plateItem=ServingPlateItem.createPlatedFood(held.split(1));
                setChangedForRender();
            }
        }
        cir.setReturnValue(InteractionResult.sidedSuccess(level.isClientSide));
    }
}
