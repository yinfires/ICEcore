package com.yinfires.icecore.compat.cozycafe.customer;

import net.minecraft.nbt.*;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

public final class CafeCustomerData extends SavedData {
    public final Map<String,Integer> reputation=new LinkedHashMap<>();
    public long lastOpenedDay=Long.MIN_VALUE, previousTime;
    public boolean active;
    public String dimension="";
    public long manager;
    public UUID session=UUID.randomUUID();
    public int quota,admitted,countdown;
    public int openingReputation,openingDishes;
    public double timeBase;
    public final List<Long> seats=new ArrayList<>();
    public final Set<String> attraction=new HashSet<>();
    public final Map<String,List<Long>> retainedSeats=new LinkedHashMap<>();
    public java.math.BigInteger pendingRevenue=java.math.BigInteger.ZERO;
    public final LinkedHashSet<String> settledOrders=new LinkedHashSet<>();
    public boolean mayOpen(long time){return !active&&Math.floorDiv(time,24000)>lastOpenedDay;}
    public void advance(long time){
        if(!active)return;
        if(com.yinfires.icecore.compat.cozycafe.CozyCafeHours.isClosed(time)
                ||com.yinfires.icecore.compat.cozycafe.CozyCafeHours.crossedClosing(previousTime,time)||admitted>=quota)active=false;
        if(previousTime!=time||!active){previousTime=time;setDirty();}
    }
    public boolean admit(){if(!active||admitted>=quota)return false;admitted++;setDirty();return true;}
    public boolean claim(String order){
        if(order.isEmpty()||!settledOrders.add(order))return false;
        while(settledOrders.size()>8192)settledOrders.remove(settledOrders.iterator().next());
        setDirty();return true;
    }
    public static CafeCustomerData load(CompoundTag tag) {
        CafeCustomerData d=new CafeCustomerData();
        CompoundTag reps=tag.getCompound("Reputation");
        for(String id:reps.getAllKeys()) d.reputation.put(id,Math.max(0,reps.getInt(id)));
        if(tag.contains("LastOpenedDay")) d.lastOpenedDay=tag.getLong("LastOpenedDay");
        d.previousTime=tag.getLong("PreviousTime"); d.active=tag.getBoolean("Active");
        d.dimension=tag.getString("Dimension"); d.manager=tag.getLong("Manager");
        if(tag.hasUUID("Session")) d.session=tag.getUUID("Session");
        d.quota=Math.max(0,tag.getInt("Quota")); d.admitted=Math.max(0,tag.getInt("Admitted"));
        d.openingReputation=tag.getInt("OpeningReputation");d.openingDishes=tag.getInt("OpeningDishes");
        d.countdown=Math.max(0,tag.getInt("Countdown")); d.timeBase=Math.max(0,tag.getDouble("TimeBase"));
        for(long pos:tag.getLongArray("Seats")) d.seats.add(pos);
        for(Tag t:tag.getList("Attraction",Tag.TAG_STRING)) d.attraction.add(t.getAsString());
        for(Tag t:tag.getList("RetainedScenes",Tag.TAG_COMPOUND)){
            CompoundTag scene=(CompoundTag)t;List<Long> seats=new ArrayList<>();for(long pos:scene.getLongArray("Seats"))seats.add(pos);
            d.retainedSeats.put(scene.getString("Key"),seats);
        }
        if(tag.contains("PendingRevenue"))try{d.pendingRevenue=new java.math.BigInteger(tag.getString("PendingRevenue")).max(java.math.BigInteger.ZERO);}catch(NumberFormatException invalid){throw new IllegalArgumentException("Invalid cafe revenue ledger",invalid);}
        for(Tag t:tag.getList("SettledOrders",Tag.TAG_STRING))d.settledOrders.add(t.getAsString());
        return d;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        tag.putInt("Schema",1); // A separate ledger deliberately does not migrate CozyCafe reputation.
        CompoundTag reps=new CompoundTag(); reputation.forEach(reps::putInt); tag.put("Reputation",reps);
        tag.putLong("LastOpenedDay",lastOpenedDay); tag.putLong("PreviousTime",previousTime); tag.putBoolean("Active",active);
        tag.putString("Dimension",dimension); tag.putLong("Manager",manager); tag.putUUID("Session",session);
        tag.putInt("Quota",quota); tag.putInt("Admitted",admitted); tag.putInt("Countdown",countdown); tag.putDouble("TimeBase",timeBase);
        tag.putInt("OpeningReputation",openingReputation);tag.putInt("OpeningDishes",openingDishes);
        tag.putLongArray("Seats",seats); ListTag list=new ListTag(); attraction.forEach(id->list.add(StringTag.valueOf(id))); tag.put("Attraction",list);
        ListTag retained=new ListTag();retainedSeats.forEach((key,positions)->{CompoundTag scene=new CompoundTag();scene.putString("Key",key);scene.putLongArray("Seats",positions);retained.add(scene);});tag.put("RetainedScenes",retained);
        tag.putString("PendingRevenue",pendingRevenue.toString());
        ListTag settled=new ListTag();settledOrders.forEach(id->settled.add(StringTag.valueOf(id)));tag.put("SettledOrders",settled);
        return tag;
    }
}
