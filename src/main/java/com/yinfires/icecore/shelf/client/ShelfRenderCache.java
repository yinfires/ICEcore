package com.yinfires.icecore.shelf.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.yinfires.icecore.shelf.ShelfBlockEntity;
import net.blay09.mods.cookingforblockheads.client.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import java.util.*;

final class ShelfRenderCache {
    private static final int MAX_ENTRIES=4096,MAX_BYTES=32*1024*1024;
    private static final LinkedHashMap<Key,Entry> ENTRIES=new LinkedHashMap<>(64,.75f,true);
    private static final Map<ShelfBlockEntity,Snapshot> SNAPSHOTS=new WeakHashMap<>();
    private static int bytes,builds,generation;
    private static boolean nativeRequired;
    static void frame() { builds=0;nativeRequired=ShelfShaderCompatibility.nativeRequired(); ShelfSceneCache.frame(); ShelfRenderStats.frame(); }
    static boolean nativeRequired() { return nativeRequired; }
    static void clear() { for(Entry entry:ENTRIES.values()) { entry.resident=false;entry.mesh=null; } ENTRIES.clear(); SNAPSHOTS.clear(); bytes=0; builds=0; generation++; }
    static final class Key {
        final CompoundTag data; final int epoch,hash;
        Key(CompoundTag data) { this.data=data;epoch=generation;hash=31*data.hashCode()+epoch; }
        @Override public int hashCode() { return hash; }
        @Override public boolean equals(Object value) { return value instanceof Key key && epoch==key.epoch && data.equals(key.data); }
        static Key of(ItemStack stack) { return new Key(stack.serializeNBT()); }
    }
    static final class Slot {
        final int index; final ItemStack stack; final Key key; Entry entry;
        Slot(int index,ItemStack stack) { this.index=index;this.stack=stack;key=Key.of(stack); }
    }
    record Snapshot(long revision,Slot[] slots) {}
    static Slot[] slots(ShelfBlockEntity shelf) {
        Snapshot current=SNAPSHOTS.get(shelf);
        if(current==null || current.revision!=shelf.renderRevision()) {
            List<Slot> slots=new ArrayList<>();
            for(int i=0;i<72;i++) if(!shelf.getItem(i).isEmpty()) slots.add(new Slot(i,shelf.getItem(i).copy()));
            current=new Snapshot(shelf.renderRevision(),slots.toArray(Slot[]::new)); SNAPSHOTS.put(shelf,current);
        }
        return current.slots;
    }
    static final class Entry {
        ShelfItemMesh mesh; final int bytes; Key key; boolean resident;
        Entry(ShelfItemMesh mesh,int bytes) { this.mesh=mesh;this.bytes=bytes; }
        int bytes() { return bytes; }
    }
    static ShelfItemMesh mesh(Slot slot,ShelfBlockEntity shelf) {
        if(nativeRequired) return null;
        if(slot.entry!=null && slot.entry.resident) {
            ENTRIES.get(slot.entry.key); // Identity key updates LRU without comparing NBT.
            return slot.entry.mesh;
        }
        Entry existing=ENTRIES.get(slot.key);
        if(existing!=null) { slot.entry=existing;return existing.mesh; }
        if(builds>=2) return null;
        Entry entry=lookup(slot.key,()->build(slot,shelf));
        slot.entry=entry;return entry==null?null:entry.mesh;
    }
    static Entry lookup(Key key,java.util.function.Supplier<Entry> builder) {
        Entry entry=ENTRIES.get(key);
        if(entry!=null) return entry;
        if(builds>=2) return null;
        builds++;
        entry=builder.get();
        if(entry.bytes()>MAX_BYTES) return entry;
        entry.key=key;entry.resident=true;ENTRIES.put(key,entry);bytes+=entry.bytes();
        while(ENTRIES.size()>MAX_ENTRIES || bytes>MAX_BYTES) {
            var iterator=ENTRIES.entrySet().iterator();var oldest=iterator.next();bytes-=oldest.getValue().bytes();oldest.getValue().resident=false;oldest.getValue().mesh=null;iterator.remove();
        }
        return entry;
    }
    private static Entry build(Slot slot,ShelfBlockEntity shelf) {
        ItemStack stack=slot.stack;
        var renderer=Minecraft.getInstance().getItemRenderer();
        BakedModel model=renderer.getModel(stack,shelf.getLevel(),null,0);
        ShelfRenderStats.model();
        ShelfItemMesh mesh=null;
        if(eligible(stack,model)) {
            ShelfItemMesh capture=new ShelfItemMesh(); PoseStack local=new PoseStack();
            local.mulPose(Axis.YP.rotationDegrees(45)); local.scale(.3f,.3f,.3f);
            try {
                RenderUtils.renderItem(stack,0,local,capture,shelf.getLevel());
                ShelfRenderStats.model();
                if(ShelfRenderStats.ENABLED) for(var part:capture.parts()) ShelfRenderStats.layer(part.type,part.size/12);
                capture.finish(); mesh=capture;
            } catch(ShelfItemMesh.TooLarge ignored) { /* bounded capture; native fallback */ }
        }
        int cost=(mesh==null?0:mesh.bytes())+slot.key.data.sizeInBytes()+128;
        if(cost>ShelfItemMesh.MAX_BYTES) return new Entry(null,slot.key.data.sizeInBytes()+128);
        return new Entry(mesh,cost);
    }
    private static boolean eligible(ItemStack stack,BakedModel model) {
        if(!BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("minecraft")
                || stack.hasFoil() || model.getClass()!=SimpleBakedModel.class
                || model.isCustomRenderer() || (model.getOverrides().getClass()!=ItemOverrides.class || !model.getOverrides().getOverrides().isEmpty())) return false;
        // Accept only exact vanilla item implementations. Unknown tint/model behavior stays live.
        Class<?> type=stack.getItem().getClass();
        if(type!=Item.class && type!=BlockItem.class) return false;
        var random=net.minecraft.util.RandomSource.create(42);
        for(int side=0;side<7;side++) {
            random.setSeed(42);
            for(var quad:model.getQuads(null,side==6?null:net.minecraft.core.Direction.values()[side],random))
                if(quad.isTinted()) return false;
        }
        boolean direct=!(stack.getItem() instanceof BlockItem blockItem)
                || (!(blockItem.getBlock() instanceof net.minecraft.world.level.block.HalfTransparentBlock)
                && !(blockItem.getBlock() instanceof net.minecraft.world.level.block.StainedGlassPaneBlock));
        var passes=model.getRenderPasses(stack,direct);
        return passes.size()==1 && passes.get(0)==model;
    }
    private ShelfRenderCache() {}
}
