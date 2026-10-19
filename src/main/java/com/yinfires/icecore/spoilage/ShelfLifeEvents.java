package com.yinfires.icecore.spoilage;

import com.yinfires.icecore.ICECore;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.server.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

@Mod.EventBusSubscriber(modid = ICECore.MOD_ID)
public final class ShelfLifeEvents {
    private static long syncedDay = Long.MIN_VALUE;
    private ShelfLifeEvents() {}
    @SubscribeEvent public static void start(ServerAboutToStartEvent event) { syncedDay = Long.MIN_VALUE; ShelfLife.start(event.getServer()); }
    @SubscribeEvent public static void ready(ServerStartedEvent event) { ShelfLife.reload(); }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { ShelfLife.stop(); }
    @SubscribeEvent public static void reload(net.minecraftforge.event.AddReloadListenerEvent event) {
        event.addListener(new net.minecraft.server.packs.resources.SimplePreparableReloadListener<Boolean>() {
            protected Boolean prepare(net.minecraft.server.packs.resources.ResourceManager resources, net.minecraft.util.profiling.ProfilerFiller profiler) { return true; }
            protected void apply(Boolean value, net.minecraft.server.packs.resources.ResourceManager resources, net.minecraft.util.profiling.ProfilerFiller profiler) { ShelfLife.reload(); }
        });
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        // Vanilla already visits its inventory; only settle carried state here.
        ShelfLife.player(player.containerMenu.getCarried(), player);
    }
    @SubscribeEvent public static void pickup(net.minecraftforge.event.entity.player.EntityItemPickupEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ShelfLife.player(event.getItem().getItem(), player);
    }
    @SubscribeEvent public static void crafted(PlayerEvent.ItemCraftedEvent event) { ShelfLife.player(event.getCrafting(), event.getEntity()); }
    @SubscribeEvent public static void smelted(PlayerEvent.ItemSmeltedEvent event) { ShelfLife.player(event.getSmelting(), event.getEntity()); }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            com.yinfires.icecore.network.ICECoreNetwork.shelfLifeDay(ShelfLife.day(), player);
            for (var stack : player.getInventory().items) ShelfLife.player(stack, player);
        }
    }
    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !ShelfLife.active()) return;
        long day = ShelfLife.day();
        if (day == syncedDay) return;
        syncedDay = day;
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            com.yinfires.icecore.network.ICECoreNetwork.shelfLifeDay(day, player);
            for (ItemStack stack : player.getInventory().items) ShelfLife.player(stack, player);
        }
    }
    @SubscribeEvent public static void drop(net.minecraftforge.event.entity.item.ItemTossEvent event) { ShelfLife.release(event.getEntity().getItem()); }
}
