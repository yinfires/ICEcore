package com.yinfires.icecore.time;

import com.yinfires.icecore.network.ICECoreNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;

public final class TimeNetworking {
    private TimeNetworking(){}
    public static void sendSnapshot(ServerPlayer player){var s=player.server.overworld(); ICECoreNetwork.sendToPlayer(new ClientBoundTimeSnapshotPacket(TimeConfigData.gson().toJson(TimeConfigManager.get().data().toJson()),s.getDayTime(),s.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)),player);}
    public static void broadcastSnapshot(){var server=TimeConfigManager.get().server();if(server!=null)for(var p:server.getPlayerList().getPlayers())sendSnapshot(p);}
    public static void sendVote(ServerPlayer p,ClientBoundVotePacket packet){ICECoreNetwork.sendToPlayer(packet,p);}
    public static void sendCutscene(ServerPlayer p,ClientBoundCutscenePacket packet){ICECoreNetwork.sendToPlayer(packet,p);}
    public static void broadcastDaySummary(long sequence,long income,TimeTimings timings){var server=TimeConfigManager.get().server();if(server!=null){var packet=new ClientBoundDaySummaryPacket(sequence,income,timings);for(var p:server.getPlayerList().getPlayers())ICECoreNetwork.sendToPlayer(packet,p);}}
    public static void ack(long sequence){ICECoreNetwork.sendToServer(new ServerBoundCutsceneAckPacket(sequence));}
}
