package com.yinfires.icecore.interaction;

import com.yinfires.icecore.ICECore;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class InteractionClientState {
    private static InteractionSnapshot snapshot = InteractionSnapshot.empty();
    private static long revision = -1;
    public static InteractionSnapshot snapshot() { return snapshot; }
    public static boolean blocked(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player,
                                  net.minecraft.core.BlockPos pos) {
        var minecraft = net.minecraft.client.Minecraft.getInstance();
        return minecraft.player == player && minecraft.gameMode != null
                && minecraft.gameMode.getPlayerMode() == net.minecraft.world.level.GameType.ADVENTURE
                && snapshot.blocked(level, pos);
    }
    public static void accept(InteractionSnapshot value, long version) { if (version >= revision) { snapshot = value; revision = version; } }
    @Mod.EventBusSubscriber(modid = ICECore.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class Events {
        @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { snapshot = InteractionSnapshot.empty(); revision = -1; }
    }
}
