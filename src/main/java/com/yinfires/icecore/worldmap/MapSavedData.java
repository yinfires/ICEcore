package com.yinfires.icecore.worldmap;

import net.minecraft.nbt.*;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

public final class MapSavedData extends SavedData {
    final Set<String> unlocked=new HashSet<>();
    long revision;
    public static MapSavedData load(CompoundTag tag){
        MapSavedData d=new MapSavedData();d.revision=tag.getLong("Revision");
        var list=tag.getList("Unlocked",Tag.TAG_STRING);for(int i=0;i<list.size();i++)d.unlocked.add(list.getString(i));return d;
    }
    @Override public CompoundTag save(CompoundTag tag){
        ListTag list=new ListTag();unlocked.stream().sorted().forEach(id->list.add(StringTag.valueOf(id)));
        tag.put("Unlocked",list);tag.putLong("Revision",revision);return tag;
    }
    boolean change(String id,boolean unlock){boolean changed=unlock?unlocked.add(id):unlocked.remove(id);if(changed){revision++;setDirty();}return changed;}
}
