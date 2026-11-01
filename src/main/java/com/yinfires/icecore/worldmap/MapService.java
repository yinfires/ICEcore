package com.yinfires.icecore.worldmap;

import com.yinfires.icecore.ICECore;
import com.yinfires.icecore.building.BuildingDataManager;
import com.yinfires.icecore.feedback.PlayerFeedback;
import com.yinfires.icecore.island.*;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraftforge.event.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

@Mod.EventBusSubscriber(modid=ICECore.MOD_ID)
public final class MapService {
    private static MinecraftServer server;
    private static MapDefinition definition=new MapDefinition();
    private static MapSavedData state;
    private static long revision;
    private static boolean writing;
    private static Capture capture;
    private static final Map<ServerPlayer,Long> lastRequest=new WeakHashMap<>();
    private static final ExecutorService IO=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"icecore-map-save");t.setDaemon(true);return t;});
    private MapService(){}
    static Path file(){return FMLPaths.CONFIGDIR.get().resolve("icecore/world_map.json");}
    @SubscribeEvent public static void start(ServerStartedEvent e){
        server=e.getServer();state=server.overworld().getDataStorage().computeIfAbsent(MapSavedData::load,MapSavedData::new,"icecore_map");
        definition=new MapDefinition();revision=state.revision;
        try{if(Files.exists(file()))definition=MapDefinition.parse(Files.readString(file(),StandardCharsets.UTF_8));pruneUnlocks();}catch(Exception ex){LogUtils.getLogger().error("Cannot load ICEcore world map; keeping map disabled",ex);}
    }
    @SubscribeEvent public static void stop(ServerStoppingEvent e){capture=null;server=null;state=null;writing=false;lastRequest.clear();MapNetwork.clearTransfers();definition=new MapDefinition();}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p && state!=null)send(p,false);}
    public static boolean setUnlocked(String id,boolean unlocked){
        if(server==null||!server.isSameThread())throw new IllegalStateException("Map unlocks must run on server thread");
        if(definition.region(id)==null)return false;
        if(state.change(id,unlocked)){revision++;broadcast();}return true;
    }
    static void send(ServerPlayer p,boolean editor){
        if(state==null)return;
        boolean op=p.hasPermissions(2);
        List<String> sources=new ArrayList<>();
        if(op){for(String s:IslandManager.get().names())sources.add("island:"+s);for(String s:BuildingDataManager.get().data().regions().keySet())sources.add("region:"+s);Collections.sort(sources);}
        var visible=state.unlocked.stream().filter(id->definition.region(id)!=null).collect(java.util.stream.Collectors.toUnmodifiableSet());
        MapNetwork.send(p,new MapNetwork.Snapshot(revision,MapDefinition.JSON.toJson(definition),visible,op,editor,sources.stream().filter(s->s.length()<=128).limit(512).toList()));
    }
    private static void broadcast(){if(server!=null)for(ServerPlayer p:server.getPlayerList().getPlayers())send(p,false);}
    static void request(ServerPlayer p,MapNetwork.Request r){
        if(state==null)return;
        long now=System.nanoTime();Long last=lastRequest.put(p,now);if(last!=null&&now-last<100000000L)return;
        if(r.action().equals("open")){send(p,false);return;}
        if(!p.hasPermissions(2))return;
        if(r.action().equals("editor")){send(p,true);return;}
        if(r.revision()!=revision||writing||capture!=null){feedback(p,"stale");send(p,false);return;}
        try{
            if(r.action().equals("save")){MapDefinition d=MapDefinition.parse(r.payload());MapPolygonEditor.validateChanged(definition,d);persist(d,p);}
            else if(r.action().equals("bind"))beginCapture(p,r.id(),r.payload());
        }catch(Exception ex){feedback(p,"invalid");LogUtils.getLogger().warn("Rejected map edit: {}",ex.getMessage());}
    }
    private static void persist(MapDefinition d,ServerPlayer player){
        String json=MapDefinition.JSON.toJson(d);if(json.length()>900000)throw new IllegalArgumentException("definition size");
        writing=true;MinecraftServer owner=server;
        CompletableFuture.runAsync(()->{
            try{Files.createDirectories(file().getParent());Path temp=Files.createTempFile(file().getParent(),"world-map-",".json");Files.writeString(temp,json,StandardCharsets.UTF_8);
                try{Files.move(temp,file(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temp,file(),StandardCopyOption.REPLACE_EXISTING);}
            }catch(Exception ex){throw new CompletionException(ex);}
        },IO).whenComplete((ok,error)->owner.execute(()->{
            if(server!=owner)return;writing=false;
            if(error==null){definition=d;pruneUnlocks();revision++;broadcast();feedback(player,"saved");}else{LogUtils.getLogger().error("Map save failed",error);feedback(player,"failed");}
        }));
    }
    private static void feedback(ServerPlayer p,String key){if(p!=null)PlayerFeedback.show(p,Component.translatable("icecore.map."+key));}
    private static void pruneUnlocks(){if(state.unlocked.removeIf(id->definition.region(id)==null)){state.revision++;state.setDirty();}}
    @SubscribeEvent public static void commands(RegisterCommandsEvent event){
        var root=Commands.literal("map").requires(s->s.hasPermission(2));
        for(boolean unlock:new boolean[]{true,false})root.then(Commands.literal(unlock?"unlock":"lock").then(Commands.argument("id",StringArgumentType.word())
            .suggests((c,b)->c.getSource().hasPermission(2)?SharedSuggestionProvider.suggest(definition.regions.stream().map(r->r.id),b):b.buildFuture())
            .executes(c->{boolean ok=setUnlocked(StringArgumentType.getString(c,"id"),unlock);PlayerFeedback.show(c.getSource(),Component.translatable(ok?"icecore.map.changed":"icecore.map.invalid"));return ok?1:0;})));
        root.then(Commands.literal("editor").executes(c->{send(c.getSource().getPlayerOrException(),true);return 1;}));
        root.then(Commands.literal("reload").executes(c->{
            if(writing||capture!=null)return 0;MinecraftServer owner=server;writing=true;
            CompletableFuture.supplyAsync(()->{try{return MapDefinition.parse(Files.readString(file(),StandardCharsets.UTF_8));}catch(Exception ex){throw new CompletionException(ex);}},IO)
                .whenComplete((d,error)->owner.execute(()->{if(server!=owner)return;writing=false;if(error==null){definition=d;pruneUnlocks();revision++;broadcast();}PlayerFeedback.show(c.getSource(),Component.translatable(error==null?"icecore.map.saved":"icecore.map.failed"));}));return 1;
        }));
        event.getDispatcher().register(Commands.literal("icecore").then(root));
    }
    private static void beginCapture(ServerPlayer p,String id,String source){
        if(definition.region(id)==null)throw new IllegalArgumentException("save region before binding");
        IslandManager.Entry island=null;ServerLevel level;int x,y,z,sx,sy,sz;
        if(source.startsWith("island:")){
            island=IslandManager.get().entry(source.substring(7));if(island==null)throw new IllegalArgumentException("island");
            level=server.getLevel(island.dimension());var min=island.min();x=min.getX();y=min.getY();z=min.getZ();sx=island.data().sizeX();sy=island.data().sizeY();sz=island.data().sizeZ();
        }else if(source.startsWith("region:")){
            var r=BuildingDataManager.get().data().regions().get(source.substring(7));if(r==null||!r.isComplete())throw new IllegalArgumentException("region");
            level=server.getLevel(ResourceKey.create(Registries.DIMENSION,new ResourceLocation(r.dimension())));var a=r.pos1();var b=r.pos2();
            x=Math.min(a[0],b[0]);y=Math.min(a[1],b[1]);z=Math.min(a[2],b[2]);sx=Math.abs(a[0]-b[0])+1;sy=Math.abs(a[1]-b[1])+1;sz=Math.abs(a[2]-b[2])+1;
        }else throw new IllegalArgumentException("binding type");
        if(level==null||!level.dimension().location().toString().equals(definition.dimension)||x<definition.minX||z<definition.minZ||(long)x+sx>definition.maxX||(long)z+sz>definition.maxZ||(long)sx*sz>16777216||(long)sx*sy*sz>268435456)throw new IllegalArgumentException("capture bounds");
        capture=new Capture(p,id,source,level,island==null?null:island.data(),x,y,z,sx,sy,sz);
    }
    @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ServerTickEvent e){
        if(e.phase!=net.minecraftforge.event.TickEvent.Phase.END)return;
        if(server!=null&&server.getTickCount()%20==0)MapNetwork.expireTransfers();
        if(capture!=null){Capture active=capture;try{active.tick();}catch(Exception ex){feedback(active.player,"capture_failed");LogUtils.getLogger().warn("Map footprint capture failed: {}",ex.getMessage());capture=null;}}
    }
    private static final class Capture {
        final ServerPlayer player;final String id,source;final ServerLevel level;final IslandData data;
        final int x,y,z,sx,sy,sz;final BitSet columns;final long deadline=System.nanoTime()+120000000000L;
        int column,vertical;
        final BlockPos.MutableBlockPos pos=new BlockPos.MutableBlockPos();
        Capture(ServerPlayer p,String id,String source,ServerLevel level,IslandData data,int x,int y,int z,int sx,int sy,int sz){this.player=p;this.id=id;this.source=source;this.level=level;this.data=data;this.x=x;this.y=y;this.z=z;this.sx=sx;this.sy=sy;this.sz=sz;columns=new BitSet(sx*sz);vertical=sy-1;}
        void tick(){
            if(System.nanoTime()>deadline||player.isRemoved())throw new IllegalStateException("capture cancelled/timed out");
            long end=System.nanoTime()+2000000;int checks=0;
            while(column<sx*sz&&checks++<8192&&System.nanoTime()<end){
                int lx=column%sx,lz=column/sx;boolean solid;
                if(data!=null)solid=!data.stateAt(lx,vertical,lz).isAir();
                else{var chunk=level.getChunkSource().getChunkNow((x+lx)>>4,(z+lz)>>4);if(chunk==null)throw new IllegalStateException("unloaded chunk");solid=!chunk.getBlockState(pos.set(x+lx,y+vertical,z+lz)).isAir();}
                if(solid)columns.set(column);
                if(solid||--vertical<0){column++;vertical=sy-1;}
            }
            if(column<sx*sz)return;
            MinecraftServer owner=server;MapDefinition next=definition.copy();capture=null;writing=true;
            CompletableFuture.supplyAsync(()->{
                List<Integer> runs=new ArrayList<>();for(int row=0;row<sz;row++){int c=0;while(c<sx){int start=columns.nextSetBit(row*sx+c);if(start<0||start>=(row+1)*sx)break;int stop=Math.min((row+1)*sx,columns.nextClearBit(start));runs.add(x+start-row*sx);runs.add(z+row);runs.add(stop-start);if(runs.size()>180000)throw new IllegalArgumentException("footprint complexity limit");c=stop-row*sx;}}
                var r=next.region(id);r.binding=source;r.runs=runs.stream().mapToInt(Integer::intValue).toArray();next.validate();return next;
            },IO).whenComplete((d,error)->owner.execute(()->{if(server!=owner)return;writing=false;if(error==null)persist(d,player);else feedback(player,"capture_failed");}));
        }
    }
}
