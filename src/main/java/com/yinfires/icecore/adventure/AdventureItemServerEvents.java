package com.yinfires.icecore.adventure;
import com.yinfires.icecore.ICECore;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=ICECore.MOD_ID, bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class AdventureItemServerEvents {
 private AdventureItemServerEvents() {}
 @SubscribeEvent public static void start(ServerStartingEvent e){AdventureItemService.start(e.getServer());}
 @SubscribeEvent public static void stop(ServerStoppingEvent e){AdventureItemService.stop();}
 @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p) AdventureItemService.restore(p);}
 @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
 public static void open(PlayerContainerEvent.Open e){if(e.getEntity() instanceof ServerPlayer p) AdventureItemService.restoreIntoOpenedMenu(p);}
 @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p) AdventureItemService.returnToInventory(p);}
 @SubscribeEvent public static void toss(ItemTossEvent e){if(e.getPlayer() instanceof ServerPlayer p && AdventureItemService.isAdventure(p)) e.setCanceled(true);}
}
