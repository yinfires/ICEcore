package com.yinfires.icecore.time;

import com.yinfires.icecore.building.BuildingDataManager;
import com.yinfires.icecore.feedback.PlayerFeedback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

import java.util.*;

public final class TimeVoteManager {
    private static final Map<UUID, Confirmation> CONFIRMED=new HashMap<>();
    private static final Set<UUID> ACKS=new HashSet<>();
    private static final Map<UUID,Long> LAST_INTERACTION=new HashMap<>();
    private static MinecraftServer server;
    private static long revision;
    private static long sequence;
    private static Running running;
    private static Set<UUID> lastPopulation=Set.of();
    private TimeVoteManager(){}
    public static void start(MinecraftServer value){server=value;clear();}
    public static void stop(){abort();server=null;}
    public static boolean isRunning(){return running!=null;}
    public static boolean isFastForwarding(){return running!=null && running.elapsed>=running.fastForwardStart() && running.elapsed<running.summaryStart();}

    public static boolean handle(ServerPlayer player, net.minecraft.core.BlockPos hit) {
        if(server==null || running!=null || player.gameMode.getGameModeForPlayer()!=GameType.ADVENTURE || player.level().dimension()!=Level.OVERWORLD)return false;
        var match=TimeRules.match(player.level(),hit,TimeConfigManager.get().data(),BuildingDataManager.get().data());
        if(match==null)return false;
        long tick=player.level().getGameTime();Long previousTick=LAST_INTERACTION.put(player.getUUID(),tick);if(previousTick!=null&&previousTick==tick)return true;
        if(!TimeConfigManager.get().data().cameras().containsKey(match.region())){PlayerFeedback.show(player,Component.translatable("icecore.time.camera.missing",match.region()));return true;}
        Confirmation old=CONFIRMED.get(player.getUUID());
        if(old!=null && old.region.equals(match.region()) && old.position.equals(match.primary())) CONFIRMED.remove(player.getUUID());
        else CONFIRMED.put(player.getUUID(),new Confirmation(match.region(),match.primary()));
        changed();
        tryStart();
        return true;
    }
    public static void tick(){
        if(server==null)return;
        if(running!=null){tickRunning();return;}
        Set<UUID> population=server.overworld().players().stream().map(ServerPlayer::getUUID).collect(java.util.stream.Collectors.toUnmodifiableSet());
        if(!population.equals(lastPopulation)){lastPopulation=population;changed();}
        boolean changed=false;
        Iterator<Map.Entry<UUID,Confirmation>> it=CONFIRMED.entrySet().iterator();
        while(it.hasNext()){
            var e=it.next();ServerPlayer p=server.getPlayerList().getPlayer(e.getKey());var region=BuildingDataManager.get().data().regions().get(e.getValue().region);
            if(p==null||p.level().dimension()!=Level.OVERWORLD||region==null||!region.contains(Level.OVERWORLD,p.blockPosition())){it.remove();changed=true;}
        }
        if(changed)changed();
        tryStart();
    }
    private static void tryStart(){
        if(server==null||running!=null)return;
        List<ServerPlayer> participants=server.overworld().players();
        if(participants.isEmpty()||CONFIRMED.size()!=participants.size()||participants.stream().anyMatch(p->!CONFIRMED.containsKey(p.getUUID())))return;
        long from=server.overworld().getDayTime(),target=TimeCalendar.nextDaySix(from);
        Map<UUID,TimeCamera> cameras=new HashMap<>();Map<UUID,PlayerOrigin> origins=new HashMap<>();
        for(ServerPlayer p:participants){TimeCamera c=TimeConfigManager.get().data().cameras().get(CONFIRMED.get(p.getUUID()).region);if(c==null)return;cameras.put(p.getUUID(),c);origins.put(p.getUUID(),new PlayerOrigin(p.getX(),p.getY(),p.getZ(),p.getYRot(),p.getXRot(),p.isInvulnerable()));p.setInvulnerable(true);}
        TimeTimings t=TimeConfigData.gson().fromJson(TimeConfigData.gson().toJson(TimeConfigManager.get().data().timings()),TimeTimings.class);
        t.set("fastForward",fastForwardDurationTicks(from,target,t.fastForwardTicks()));
        running=new Running(++sequence,List.copyOf(participants),cameras,origins,from,target,t,0);ACKS.clear();
        for(ServerPlayer p:participants)TimeNetworking.sendCutscene(p,new ClientBoundCutscenePacket(sequence,false,cameras.get(p.getUUID()),from,target,TimeService.currentIncome(),server.overworld().getGameTime(),t));
    }
    private static void tickRunning(){
        Running r=running;if(r==null)return;r.elapsed++;
        for(ServerPlayer p:r.participants){if(p.connection!=null)p.setDeltaMovement(0,0,0);}
        if(r.elapsed>=r.fastForwardStart()&&r.elapsed<=r.summaryStart()){
            double x=Math.min(1D,(r.elapsed-r.fastForwardStart())/(double)Math.max(1,r.timings.fastForwardTicks()));
            double eased=1D-Math.pow(1D-x,3D);TimeService.setDayTimeInternal(server.overworld(),r.fromTime+Math.round((r.targetTime-r.fromTime)*eased));
        }
        if(ACKS.containsAll(r.participants.stream().map(ServerPlayer::getUUID).filter(id->server.getPlayerList().getPlayer(id)!=null).toList()) || r.elapsed>r.timeoutTicks())finish();
    }
    public static void ack(ServerPlayer p,long seq){if(running!=null&&running.sequence==seq)ACKS.add(p.getUUID());}
    public static boolean isParticipant(ServerPlayer player){return running!=null&&running.origins.containsKey(player.getUUID());}
    public static void participantLeaving(ServerPlayer player){if(isParticipant(player))abort();else revalidate();}
    public static void abort(){if(running!=null){for(ServerPlayer p:running.participants){restorePlayer(p,running.origins.get(p.getUUID()));TimeNetworking.sendCutscene(p,new ClientBoundCutscenePacket(running.sequence,true,running.cameras.get(p.getUUID()),0,0,0,0,TimeConfigManager.get().data().timings()));}}running=null;clear();}
    private static void finish(){if(running==null)return;TimeService.setDayTimeInternal(server.overworld(),running.targetTime);for(ServerPlayer p:running.participants)restorePlayer(p,running.origins.get(p.getUUID()));running=null;clear();TimeNetworking.broadcastSnapshot();}
    private static void restorePlayer(ServerPlayer player,PlayerOrigin origin){if(origin==null)return;player.setInvulnerable(origin.invulnerable);player.setDeltaMovement(0,0,0);if(player.connection!=null&&player.level().dimension()==Level.OVERWORLD)player.connection.teleport(origin.x,origin.y,origin.z,origin.yaw,origin.pitch);}
    public static void onExternalTimeSet(){if(running!=null)abort();TimeService.resetFraction();}
    public static void revalidate(){if(running!=null)return;boolean changed=CONFIRMED.entrySet().removeIf(e->{var r=BuildingDataManager.get().data().regions().get(e.getValue().region);return r==null||!TimeConfigManager.get().data().regions().contains(e.getValue().region);});if(changed)changed();}
    public static void clear(){CONFIRMED.clear();ACKS.clear();LAST_INTERACTION.clear();lastPopulation=server==null?Set.of():server.overworld().players().stream().map(ServerPlayer::getUUID).collect(java.util.stream.Collectors.toUnmodifiableSet());changed();}
    private static void changed(){revision++;broadcastVote();}
    private static void broadcastVote(){if(server==null)return;List<ClientBoundVotePacket.PlayerEntry> entries=CONFIRMED.keySet().stream().map(server.getPlayerList()::getPlayer).filter(Objects::nonNull).sorted(Comparator.comparing(p->p.getGameProfile().getName())).map(p->new ClientBoundVotePacket.PlayerEntry(p.getUUID(),p.getGameProfile().getName())).toList();var packet=new ClientBoundVotePacket(revision,server.overworld().players().size(),entries);for(var p:server.getPlayerList().getPlayers())TimeNetworking.sendVote(p,packet);}
    static int fastForwardDurationTicks(long from,long target,int ticksPerGameHour){return Math.max(1,(int)Math.ceil((target-from)*ticksPerGameHour/1000.0D));}
    private record Confirmation(String region,net.minecraft.core.BlockPos position){}
    private static final class Running{
        final long sequence;final List<ServerPlayer> participants;final Map<UUID,TimeCamera> cameras;final Map<UUID,PlayerOrigin> origins;final long fromTime,targetTime;final TimeTimings timings;int elapsed;
        Running(long s,List<ServerPlayer> p,Map<UUID,TimeCamera> c,Map<UUID,PlayerOrigin> o,long f,long t,TimeTimings timings,int e){sequence=s;participants=p;cameras=c;origins=o;fromTime=f;targetTime=t;this.timings=timings;elapsed=e;}
        int fastForwardStart(){return timings.fadeToCameraTicks()+timings.revealCameraTicks();}
        int summaryStart(){return fastForwardStart()+timings.fastForwardTicks();}
        int timeoutTicks(){return summaryStart()+512*timings.typewriterTicksPerCharacter()+timings.summaryHoldTicks()+timings.summaryFadeTicks()+timings.fadeToPlayerTicks()+timings.revealPlayerTicks()+100;}
    }
    private record PlayerOrigin(double x,double y,double z,float yaw,float pitch,boolean invulnerable){}
}
