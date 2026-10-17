package com.yinfires.icecore.interaction;

import com.yinfires.icecore.ICECore;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ICECore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class InteractionEvents {
    @SubscribeEvent public static void start(ServerStartingEvent event) { InteractionManager.INSTANCE.start(event.getServer()); }
    @SubscribeEvent public static void stop(ServerStoppingEvent event) { InteractionManager.INSTANCE.stop(); }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) InteractionManager.INSTANCE.send(player);
    }
    @SubscribeEvent public static void tags(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) InteractionManager.INSTANCE.tagsChanged();
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent event) { InteractionCommands.register(event.getDispatcher()); }
    public static boolean blocked(Level level, Player player, BlockPos pos) {
        if (player == null) return false;
        if (level.isClientSide) return InteractionClientState.blocked(level, player, pos);
        return player instanceof ServerPlayer serverPlayer && serverPlayer.gameMode.getGameModeForPlayer() == GameType.ADVENTURE
                && InteractionManager.INSTANCE.snapshot().blocked(level, pos);
    }
}
