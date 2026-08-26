package com.yinfires.icecore.time;

import com.yinfires.icecore.ICECore;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=ICECore.MOD_ID,bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class TimeEvents {
    private TimeEvents(){}
    @SubscribeEvent public static void started(ServerStartedEvent e){TimeConfigManager.get().start(e.getServer());TimeService.start(e.getServer());TimeVoteManager.start(e.getServer());}
    @SubscribeEvent public static void stopping(ServerStoppingEvent e){TimeVoteManager.stop();TimeService.stop();TimeConfigManager.get().stop();}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p){TimeNetworking.sendSnapshot(p);TimeVoteManager.revalidate();}}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)TimeVoteManager.participantLeaving(p);}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)TimeVoteManager.participantLeaving(p);}
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){if(e.phase==TickEvent.Phase.END){TimeService.tick();TimeVoteManager.tick();}}
    @SubscribeEvent(priority=EventPriority.HIGHEST,receiveCanceled=true) public static void right(PlayerInteractEvent.RightClickBlock e){if(e.getEntity() instanceof ServerPlayer p&&TimeVoteManager.handle(p,e.getPos())){e.setCanceled(true);e.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);e.setUseItem(net.minecraftforge.eventbus.api.Event.Result.DENY);}}
    @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true) public static void lockedBlock(PlayerInteractEvent.RightClickBlock e){if(e.getEntity() instanceof ServerPlayer p&&TimeVoteManager.isParticipant(p)){e.setCanceled(true);e.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);e.setUseItem(net.minecraftforge.eventbus.api.Event.Result.DENY);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void attack(LivingAttackEvent e){if(e.getEntity() instanceof ServerPlayer p&&TimeVoteManager.isParticipant(p))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST,receiveCanceled=true) public static void lockedRight(PlayerInteractEvent.RightClickItem e){if(e.getEntity() instanceof ServerPlayer p&&TimeVoteManager.isParticipant(p))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST,receiveCanceled=true) public static void lockedLeft(PlayerInteractEvent.LeftClickBlock e){if(e.getEntity() instanceof ServerPlayer p&&TimeVoteManager.isParticipant(p))e.setCanceled(true);}
}
