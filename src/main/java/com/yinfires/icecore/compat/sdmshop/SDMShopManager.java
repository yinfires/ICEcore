package com.yinfires.icecore.compat.sdmshop;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.server.MinecraftServer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/** Server-side named shop registry. SDM is optional and all calls are guarded. */
public final class SDMShopManager extends SavedData {
    public static final String FILE_ID = "icecore_sdmshops";
    private static final String DEFAULT_UUID = "85564763-17ad-4523-b0a3-37f887071b69";
    private static SDMShopManager instance;
    private final LinkedHashMap<String, Entry> entries = new LinkedHashMap<>();
    private final HashMap<UUID, CompoundTag> snapshots = new HashMap<>();
    private MinecraftServer server;

    public record Entry(String id, UUID uuid, String displayName) {}
    private SDMShopManager() {}
    public static SDMShopManager load(CompoundTag tag) {
        SDMShopManager m = new SDMShopManager();
        var list = tag.getList("Shops", 10);
        for (int i=0;i<list.size();i++) { CompoundTag t=list.getCompound(i); try {
            String id=t.getString("Id"); UUID u=t.getUUID("Uuid"); if (!id.isBlank()) m.entries.put(id,new Entry(id,u,t.getString("Name")));
            if (t.contains("Data",10)) m.snapshots.put(u,t.getCompound("Data").copy());
        } catch (Exception ignored) {} }
        return m;
    }
    @Override public CompoundTag save(CompoundTag out) {
        var list = new net.minecraft.nbt.ListTag();
        for (Entry e: entries.values()) { CompoundTag t=new CompoundTag(); t.putString("Id",e.id()); t.putUUID("Uuid",e.uuid()); t.putString("Name",e.displayName()); CompoundTag s=snapshots.get(e.uuid()); if(s!=null)t.put("Data",s.copy()); list.add(t); }
        out.put("Shops",list); return out;
    }
    public static SDMShopManager get(MinecraftServer server) {
        if (instance == null || instance.server != server) { instance = server.overworld().getDataStorage().computeIfAbsent(SDMShopManager::load, SDMShopManager::new, FILE_ID); instance.server=server; instance.migrate(); }
        return instance;
    }
    public Collection<Entry> entries(){ return List.copyOf(entries.values()); }
    public Entry find(String id){ return entries.get(id); }
    public boolean create(String id,String name){ if(entries.containsKey(id)||!id.matches("[a-z0-9_-]+"))return false; UUID u=UUID.randomUUID(); entries.put(id,new Entry(id,u,name.isBlank()?id:name)); snapshots.put(u,new CompoundTag()); setDirty(); return true; }
    public boolean rename(String id,String name){ Entry e=entries.get(id); if(e==null||name.isBlank())return false; entries.put(id,new Entry(id,e.uuid(),name)); setDirty(); return true; }
    public boolean delete(String id){ if(entries.size()<=1)return false; Entry e=entries.remove(id); if(e==null)return false; snapshots.remove(e.uuid()); setDirty(); return true; }
    public boolean open(ServerPlayer player,String id,boolean edit){ Entry e=entries.get(id); if(e==null)return false; SDMShopCompat.markActive(this,e.uuid()); if(!SDMShopCompat.openShop(player,e.uuid(),snapshots.get(e.uuid()),edit))return false; return true; }
    /** Stores the latest SDM shop NBT for an entry after an in-game edit. */
    public void updateSnapshot(UUID entryUuid,CompoundTag data){ if(entryUuid==null||data==null)return; boolean known=entries.values().stream().anyMatch(e->e.uuid().equals(entryUuid)); if(!known)return; snapshots.put(entryUuid,data); setDirty(); }
    private void migrate(){ if(!entries.isEmpty()||!SDMShopCompat.isLoaded())return; try { Object shop=field("net.sixik.sdmshoprework.common.shop.ShopBase","SERVER").get(null); if(shop==null)return; UUID u=(UUID)field(shop.getClass(),"shopUUID").get(shop); String n=((net.minecraft.network.chat.Component)field(shop.getClass(),"shopName").get(shop)).getString(); entries.put("default",new Entry("default",u,n.isBlank()?"default":n)); snapshots.put(u,((CompoundTag)shop.getClass().getMethod("serializeNBT").invoke(shop)).copy()); setDirty(); } catch(Exception ignored){} }
    static Field field(Class<?> c,String n)throws Exception{Field f=c.getField(n);f.setAccessible(true);return f;}
    static Field field(String c,String n)throws Exception{return field(Class.forName(c),n);}
}
