package com.yinfires.icecore.food;

import com.yinfires.icecore.ICECore;
import com.yinfires.icecore.config.ICECoreConfig;
import com.yinfires.icecore.network.ICECoreNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ICECore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FoodConsumptionEvents {
    private FoodConsumptionEvents() {
    }

    @SubscribeEvent
    public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        boolean disabled = player.level().isClientSide
                ? FoodConsumptionClientState.disabled()
                : ICECoreConfig.disableFoodConsumption();
        if (!disabled) {
            return;
        }
        if (FoodConsumptionRules.isFood(event.getItem(), player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ICECoreNetwork.sendToPlayer(
                    new ClientBoundFoodConsumptionConfigPacket(ICECoreConfig.disableFoodConsumption(),
                            ICECoreConfig.enableModCompatibility()), player);
        }
    }
}
