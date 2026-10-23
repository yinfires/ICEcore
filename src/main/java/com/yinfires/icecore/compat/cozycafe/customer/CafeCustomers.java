package com.yinfires.icecore.compat.cozycafe.customer;

import com.yinfires.icecore.compat.cozycafe.*;
import com.yinfires.icecore.compat.cozycafe.seating.*;
import com.yinfires.icecore.compat.cozycafe.spawn.CozyCafeSpawnRegionService;
import com.yinfires.icecore.time.TimeConfigManager;
import com.yinfires.icecore.currency.CurrencyService;
import io.github.chakyl.cozycafe.blockentities.*;
import io.github.chakyl.cozycafe.data.*;
import io.github.chakyl.cozycafe.entities.CustomerEntity;
import io.github.chakyl.cozycafe.registry.CozyRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import java.nio.file.*;
import java.util.*;

public final class CafeCustomers {
    private static final Logger LOGGER=LogUtils.getLogger();
    private static MinecraftServer server;
    private static CafeCustomerData data;
    private static CafeCustomerConfig config=CafeCustomerConfig.empty();
    private static long revision;
    private static int retry;
    private static int routeFailures;
    private static final Map<CafeManagerBlockEntity,Map<CafeMenuItem.MenuItemCategory,List<ItemStack>>> MENUS=new LinkedHashMap<>(32,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<CafeManagerBlockEntity,Map<CafeMenuItem.MenuItemCategory,List<ItemStack>>> eldest){return size()>32;}
    };
    private CafeCustomers() {}
    public static CafeCustomerConfig config(){return config;}
    public static CafeCustomerData data(){return data;}
    public static Path file(){return FMLPaths.CONFIGDIR.get().resolve("icecore/cafe_customers.json");}
    public static void start(MinecraftServer value) {
        server=value;
        data=value.overworld().getDataStorage().computeIfAbsent(CafeCustomerData::load,CafeCustomerData::new,"icecore_cafe_customers");
        reload();
    }
    public static void stop(){server=null;data=null;config=CafeCustomerConfig.empty();retry=0;routeFailures=0;MENUS.clear();}
    public static void invalidateMenus(){MENUS.clear();}
    public static void invalidateMenu(CafeManagerBlockEntity manager){if(manager.getLevel()!=null&&!manager.getLevel().isClientSide)MENUS.remove(manager);}
    public static boolean reload() {
        try {
            CafeCustomerConfig loaded=CafeCustomerConfig.read(file());
            // Validate file skins before replacing the running definition set.
            CafeCustomerSnapshot.validateSkins(loaded,file().getParent());
            config=loaded;
            MENUS.clear();
            for(var t:config.types()) data.reputation.compute(t.id(),(id,n)->CafeCustomerRules.clampReputation(n==null?0:n,t.cap()));
            data.setDirty(); revision++;if(server!=null)for(ServerPlayer p:server.getPlayerList().getPlayers())send(p);return true;
        } catch(Exception invalid) { LOGGER.warn("Cannot reload cafe customer definitions: {}",invalid.getMessage()); return false; }
    }
    public static int reputation(String id){return data==null?0:data.reputation.getOrDefault(id,0);}
    public static int total(){
        if(data==null)return 0;
        long total=0;for(var t:config.types())total+=reputation(t.id());
        return (int)Math.min(Integer.MAX_VALUE,total);
    }
    public static boolean set(String id,long amount) {
        var t=config.type(id);if(data==null||t==null)return false;
        int next=CafeCustomerRules.clampReputation(amount,t.cap());
        if(next!=reputation(id)){data.reputation.put(id,next);data.setDirty();broadcast();}
        return true;
    }
    public static void change(String id,int delta){set(id,(long)reputation(id)+delta);}
    public static void broadcast(){
        revision++;
        if(server!=null)for(ServerPlayer p:server.getPlayerList().getPlayers())com.yinfires.icecore.network.ICECoreNetwork.sendToPlayer(new CafeCustomerSnapshot(revision,config,data.reputation,false),p);
    }
    public static void send(ServerPlayer p){
        com.yinfires.icecore.network.ICECoreNetwork.sendToPlayer(new CafeCustomerSnapshot(revision,config,data==null?Map.of():data.reputation,true),p);
    }
    public static int count(ItemStack stack,List<String> tags){
        int n=0;for(String id:tags)if(stack.is(TagKey.create(Registries.ITEM,new ResourceLocation(id))))n++;
        return n;
    }
    public static List<ItemStack> menu(CafeManagerBlockEntity manager,CafeMenuItem.MenuItemCategory category){
        var cached=MENUS.get(manager);if(cached!=null&&category!=null)return cached.getOrDefault(category,List.of());
        if(manager.getMenu()==null)return List.of();
        Set<Item> seen=new HashSet<>();List<ItemStack> result=new ArrayList<>();
        Map<CafeMenuItem.MenuItemCategory,List<ItemStack>> categories=new EnumMap<>(CafeMenuItem.MenuItemCategory.class);
        for(ItemStack stack:manager.getMenu()) {
            CafeMenuItem entry=CafeMenuItemRegistry.INSTANCE.getForItem(stack.getItem());
            if(!stack.isEmpty() && entry!=null && seen.add(stack.getItem())){
                ItemStack item=stack.copyWithCount(1);result.add(item);categories.computeIfAbsent(entry.category(),ignored->new ArrayList<>()).add(item);
            }
        }
        categories.replaceAll((key,items)->List.copyOf(items));if(result.size()<=4096)MENUS.put(manager,categories);
        return category==null?result:categories.getOrDefault(category,List.of());
    }
    public static byte openingFailure(CafeManagerBlockEntity manager){
        if(!(manager.getLevel() instanceof ServerLevel level)||data==null)return 7;
        if(CozyCafeHours.isClosed(level.getServer().overworld().getDayTime()))return 6;
        if(!data.mayOpen(level.getServer().overworld().getDayTime()))return 3;
        if(config.types().isEmpty())return 7;
        if(menu(manager,CafeMenuItem.MenuItemCategory.MAIN).isEmpty())return 8;
        boolean region=CozyCafeSpawnRegionService.hasValidRegion(level,manager.getBlockPos());
        BlockPos sign=manager.getLinkedSign();
        if(!region && (sign==null||!level.hasChunkAt(sign)||!(level.getBlockEntity(sign) instanceof CafeSignBlockEntity)))return 0;
        return -1;
    }
    public static void opened(CafeManagerBlockEntity manager){
        if(!(manager.getLevel() instanceof ServerLevel level)||data==null)return;
        data.lastOpenedDay=Math.floorDiv(server.overworld().getDayTime(),24000);
        data.previousTime=server.overworld().getDayTime();data.active=true;
        data.dimension=level.dimension().location().toString();data.manager=manager.getBlockPos().asLong();data.session=UUID.randomUUID();
        List<ItemStack> foods=menu(manager,null);
        data.openingReputation=total();data.openingDishes=foods.size();
        data.quota=CafeCustomerRules.quota(data.openingReputation,data.openingDishes);data.admitted=0;
        double multiplier=TimeConfigManager.get().data().dayDurationMultiplier();
        data.timeBase=CafeCustomerRules.paceBase(data.previousTime,multiplier);
        data.seats.clear();data.seats.addAll(CozyCafeSeatingService.snapshotMenus(level,manager.getBlockPos()).stream().map(BlockPos::asLong).toList());
        String key=data.dimension+"@"+data.manager;
        Set<Long> retained=new LinkedHashSet<>(data.retainedSeats.getOrDefault(key,List.of()));
        retained.removeIf(pos->{BlockPos p=BlockPos.of(pos);return level.hasChunkAt(p)&&(!(level.getBlockEntity(p) instanceof CafeMenuBlockEntity table)||!occupied(table));});
        retained.addAll(data.seats);data.retainedSeats.put(key,List.copyOf(retained));
        data.attraction.clear();
        for(var t:config.types())if(foods.stream().anyMatch(s->count(s,t.likes())>0&&count(s,t.dislikes())==0))data.attraction.add(t.id());
        data.countdown=nextInterval(1);data.setDirty();retry=0;routeFailures=0;
    }
    public static void closed(CafeManagerBlockEntity manager){
        if(owns(manager)){data.active=false;data.setDirty();}
    }
    public static boolean owns(CafeManagerBlockEntity manager){
        return data!=null && manager.getLevel()!=null && data.manager==manager.getBlockPos().asLong()
                && data.dimension.equals(manager.getLevel().dimension().location().toString());
    }
    public static boolean hasUnfinished(CafeManagerBlockEntity manager){
        if(!(manager.getLevel() instanceof ServerLevel level))return false;
        List<BlockPos> seats=CozyCafeSeatingService.snapshotMenus(level,manager.getBlockPos());
        if(data!=null)seats=data.retainedSeats.getOrDefault(level.dimension().location()+"@"+manager.getBlockPos().asLong(),owns(manager)?data.seats:List.of()).stream().map(BlockPos::of).toList();
        for(BlockPos pos:seats){
            if(!level.hasChunkAt(pos))return true;
            if(level.getBlockEntity(pos) instanceof CafeMenuBlockEntity table && occupied(table))return true;
        }
        return false;
    }
    private static boolean occupied(CafeMenuBlockEntity table){return table.getHasCustomer()||((CozyCafeMenuSeatAccess)table).icecore$getCustomerUuid()!=null||((CafeTableAccess)table).icecore$hasContainerContent();}
    public static void globalTick(){
        if(data==null)return;
        data.advance(server.overworld().getDayTime());
        if(server.getTickCount()%20==0&&data.pendingRevenue.signum()>0){
            long amount=data.pendingRevenue.min(java.math.BigInteger.valueOf(Long.MAX_VALUE-CurrencyService.balance())).longValueExact();
            if(amount>0&&CurrencyService.add(amount,"cozycafe","cozycafe",server.createCommandSourceStack()).success()){
                data.pendingRevenue=data.pendingRevenue.subtract(java.math.BigInteger.valueOf(amount));data.setDirty();
            }
        }
    }
    private static int nextInterval(double free){return CafeCustomerRules.interval(data.timeBase,data.quota,free,server.overworld().random.nextDouble(),
            config.minIntervalTicks(),config.maxIntervalTicks(),config.jitter(),config.vacancyBoost());}
    public static void seatChanged(net.minecraft.world.level.Level level,BlockPos pos){
        if(data!=null&&data.active&&data.countdown==0&&data.dimension.equals(level.dimension().location().toString())&&data.seats.contains(pos.asLong()))retry=0;
    }
    public static void tick(CafeManagerBlockEntity manager){
        if(!(manager.getLevel() instanceof ServerLevel level))return;
        if(data!=null)data.advance(level.getServer().overworld().getDayTime());
        if(!owns(manager)||!data.active){if(manager.isOpen())manager.setOpen(false,false);return;}
        if(!manager.isOpen()){data.active=false;data.setDirty();return;}
        if(data.countdown>0){data.countdown--;data.setDirty();return;}
        if(retry>0){retry--;return;}
        // Restore routes before availability checks: an unloaded route is not evidence of a full cafe.
        if(!CozyCafeSeatingService.hasRouteSnapshot(level,manager.getBlockPos())) {
            ((CafeCustomerManagerAccess)manager).icecore$restoreCustomerRoutes();retry=Math.min(1200,20<<Math.min(6,routeFailures++));return;
        }
        // Bounded opening snapshot, checked at most once per second while waiting for a seat.
        List<CafeMenuBlockEntity> free=new ArrayList<>();
        boolean readyRoute=false;
        for(long pos:data.seats){
            BlockPos p=BlockPos.of(pos);
            if(!level.hasChunkAt(p)||!(level.getBlockEntity(p) instanceof CafeMenuBlockEntity m))continue;
            if(!occupied(m)&&!manager.getBlockPos().equals(m.getCafeManager()))m.setCafeManager(manager.getBlockPos());
            readyRoute|=CozyCafeSeatingService.hasReadyRoute(level,manager.getBlockPos(),p);
            if(m.canReceiveNewCustomer()&&!((CafeTableAccess)m).icecore$hasContainerContent())free.add(m);
        }
        if(readyRoute)routeFailures=0;
        if(free.isEmpty()){
            if(!readyRoute&&!CozyCafeSeatingService.isRouteWarming(manager)){
                ((CafeCustomerManagerAccess)manager).icecore$restoreCustomerRoutes();retry=Math.min(1200,20<<Math.min(6,routeFailures++));
            }else retry=200;
            return;
        }
        int available=free.size();
        int batch=Math.min(Math.min(CafeCustomerRules.sample(CafeCustomerRules.groups(data.quota),level.random.nextDouble())+1,free.size()),data.quota-data.admitted);
        int successes=0;
        for(int i=0;i<batch;i++){
            int index=level.random.nextInt(free.size());CafeMenuBlockEntity table=free.remove(index);
            table.setCafeManager(manager.getBlockPos());
            BlockPos spawn=CozyCafeSeatingService.routeEntrance(table,level.random);
            if(spawn==null)continue;
            CustomerEntity customer=CozyRegistry.EntityRegistry.CUSTOMER.get().create(level);
            if(customer==null||!CozyCafeSeatingService.reserveIfReachable(table,customer,spawn)){if(customer!=null)customer.discard();continue;}
            CafeCustomerConfig.Type type=chooseType(level);
            if(type==null){CozyCafeSeatingService.release(table);customer.discard();continue;}
            CompoundTag visit=visit(type,manager,level);
            ((CafeCustomerVisit)table).icecore$visit(visit);
            ((CafeCustomerVisit)customer).icecore$visit(visit.copy());
            customer.setCustomName(Component.translatable(type.nameKey()));customer.setCustomNameVisible(false);
            // An empty username selects the synchronous native path without online profile lookup.
            io.github.chakyl.cozycafe.util.CustomerEntityUtils.spawnCustomerAndTarget(spawn,level,"",table.getBlockPos(),customer,io.github.chakyl.cozycafe.util.CustomerTarget.MENU);
            if(level.getEntity(customer.getUUID())!=customer) {CozyCafeSeatingService.release(table);table.setCustomerTravelTime(-1);customer.discard();continue;}
            ((CozyCafeMenuSeatAccess)table).icecore$setSeatingStage("walking");
            table.setCustomerTravelTime(-1);data.admit();successes++;
        }
        data.setDirty();
        if(data.admitted>=data.quota)manager.setOpen(false,false);
        else if(successes>0)data.countdown=nextInterval((double)(available-successes)/Math.max(1,data.seats.size()));
        else retry=20;
    }
    private static CafeCustomerConfig.Type chooseType(ServerLevel level){
        if(config.types().isEmpty())return null;
        double[] w=new double[config.types().size()];
        for(int i=0;i<w.length;i++){var t=config.types().get(i);w[i]=t.weight()*(data.attraction.contains(t.id())?1+config.menuBoost():1)*(1+config.reputationBoost()*reputation(t.id())/t.cap());}
        return config.types().get(CafeCustomerRules.sample(w,level.random.nextDouble()));
    }
    private static CompoundTag visit(CafeCustomerConfig.Type type,CafeManagerBlockEntity manager,ServerLevel level){
        CompoundTag v=new CompoundTag();v.putString("Type",type.id());v.putString("Name",type.nameKey());v.putUUID("Session",data.session);
        ListTag likes=new ListTag(),dislikes=new ListTag();type.likes().forEach(s->likes.add(StringTag.valueOf(s)));type.dislikes().forEach(s->dislikes.add(StringTag.valueOf(s)));
        v.put("Likes",likes);v.put("Dislikes",dislikes);v.putDouble("PreferenceChance",config.preferenceChance());v.putDouble("TipChance",config.tipChance());v.putInt("Penalty",config.timeoutPenalty());
        boolean drink=!menu(manager,CafeMenuItem.MenuItemCategory.DRINK).isEmpty(),dessert=!menu(manager,CafeMenuItem.MenuItemCategory.DESSERT).isEmpty();
        int pattern=level.random.nextInt(CafeCustomerRules.courseCount(drink,dessert));
        int mask=CafeCustomerRules.courses(drink,dessert,pattern,level.random.nextBoolean());
        v.putInt("Courses",mask);v.putBoolean("Settled",true);
        if(!type.skins().isEmpty()){
            double[] weights=type.skins().stream().mapToDouble(CafeCustomerConfig.Skin::weight).toArray();
            var skin=type.skins().get(CafeCustomerRules.sample(weights,level.random.nextDouble()));
            v.putString("Skin",type.id()+"/"+skin.id());v.putString("Resource",skin.resource());v.putBoolean("Slim",skin.slim());
        }
        return v;
    }
    public static List<String> tags(CompoundTag visit,String key){return visit.getList(key,Tag.TAG_STRING).stream().map(Tag::getAsString).toList();}
    public static void chooseCourse(CafeManagerBlockEntity manager,CafeMenuBlockEntity table){
        if(!(table.getLevel() instanceof ServerLevel))return;
        CompoundTag v=((CafeCustomerVisit)table).icecore$visit();
        if(v.isEmpty()){table.closeMenu(true);return;}
        int course=table.getCurrentCourse();
        for(;course<3;course++) {
            if((v.getInt("Courses")&(1<<course))==0)continue;
            var category=course==0?CafeMenuItem.MenuItemCategory.DRINK:course==1?CafeMenuItem.MenuItemCategory.MAIN:CafeMenuItem.MenuItemCategory.DESSERT;
            List<ItemStack> all=menu(manager,category);if(all.isEmpty())continue;
            List<String> likes=tags(v,"Likes"),dislikes=tags(v,"Dislikes");
            var random=table.getLevel().random;
            int[] liked=new int[all.size()],disliked=new int[all.size()];
            for(int i=0;i<all.size();i++){liked[i]=count(all.get(i),likes);disliked[i]=count(all.get(i),dislikes);}
            int selected=CafeCustomerRules.chooseFood(liked,disliked,v.getDouble("PreferenceChance"),random.nextDouble(),random.nextDouble());
            ItemStack chosen=all.get(selected);
            table.setCurrentCourse(course,false);
            v.putUUID("Order",UUID.randomUUID());v.putBoolean("Settled",false);
            v.putInt("Liked",liked[selected]);v.putInt("Disliked",disliked[selected]);
            v.putInt("Patience",CafeMenuBlockEntity.getMaxWaitTime());
            table.setRequestedItem(chosen.copyWithCount(1));table.setWaitTime(-1);table.setChanged();return;
        }
        table.closeMenu(true);
    }
    public static long settle(CafeMenuBlockEntity table,int price,int waited){
        if(!(table.getLevel() instanceof ServerLevel))return 0;
        CompoundTag v=((CafeCustomerVisit)table).icecore$visit();
        if(v.isEmpty()||v.getBoolean("Settled"))return 0;
        v.putBoolean("Settled",true);table.setChanged();
        String order=v.hasUUID("Order")?v.getUUID("Order").toString():"";
        if(data==null||!data.claim(order))return 0;
        int speed=CafeCustomerRules.speed(waited,Math.max(1,v.getInt("Patience")));
        change(v.getString("Type"),speed+v.getInt("Liked")-v.getInt("Disliked"));
        long tips=0;
        if(speed==2&&table.getLevel().random.nextDouble()<v.getDouble("TipChance"))tips+=CafeCustomerRules.tip(price);
        if(v.getInt("Liked")>0&&table.getLevel().random.nextDouble()<v.getDouble("TipChance"))tips+=CafeCustomerRules.tip(price);
        long amount=Math.max(0L,(long)price)+tips;
        if(amount>0&&!CurrencyService.add(amount,"cozycafe","cozycafe",CurrencyService.sourceAt((ServerLevel)table.getLevel(),table.getBlockPos().getCenter(),null)).success())data.pendingRevenue=data.pendingRevenue.add(java.math.BigInteger.valueOf(amount));
        data.setDirty();return tips;
    }
    public static void timeout(CafeMenuBlockEntity table){
        if(!(table.getLevel() instanceof ServerLevel))return;
        CompoundTag v=((CafeCustomerVisit)table).icecore$visit();
        if(!v.isEmpty()&&!v.getBoolean("Settled")){
            v.putBoolean("Settled",true);table.setChanged();
            if(data!=null&&v.hasUUID("Order")&&data.claim(v.getUUID("Order").toString()))change(v.getString("Type"),-v.getInt("Penalty"));
        }
    }
}
