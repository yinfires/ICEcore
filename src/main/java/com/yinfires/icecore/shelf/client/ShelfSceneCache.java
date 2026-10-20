package com.yinfires.icecore.shelf.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.yinfires.icecore.shelf.ShelfBlockEntity;
import com.yinfires.icecore.shelf.ShelfBlock;
import net.minecraft.client.renderer.*;
import org.joml.Matrix4f;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import java.lang.ref.WeakReference;
import java.util.*;

/** Render-thread-owned shelf VBOs. No world/inventory ownership and no stale-revision draws. */
final class ShelfSceneCache {
    static final int MAX_SHELVES=256,MAX_BYTES=32*1024*1024;
    private static final LinkedHashMap<WeakReference<ShelfBlockEntity>,Entry> ENTRIES=new LinkedHashMap<>(32,.75f,true);
    private static final WeakHashMap<ShelfBlockEntity,WeakReference<ShelfBlockEntity>> KEYS=new WeakHashMap<>();
    private static int bytes,builds,sweep;
    private record Entry(long revision,int light,net.minecraft.core.Direction facing,ShelfSceneMesh mesh) {}
    private static final Matrix4f MODEL_VIEW=new Matrix4f();
    private static final Matrix3f TRANSPOSE=new Matrix3f();
    private static final Vector3f LIGHT0=new Vector3f(),LIGHT1=new Vector3f(),LOCAL0=new Vector3f(),LOCAL1=new Vector3f();
    private static final Vector3f SAVED0=new Vector3f(),SAVED1=new Vector3f();
    static boolean staticVboEligible(int bytes,int shelves) { return bytes>=0 && bytes<=MAX_BYTES && shelves<=MAX_SHELVES; }
    static void frame() {
        builds=0;
        // Chunk unload releases immediately; this bounded sweep handles GC and broken shelves.
        if(++sweep<120) return;
        sweep=0;
        var iterator=ENTRIES.entrySet().iterator();
        while(iterator.hasNext()) {
            var next=iterator.next();var shelf=next.getKey().get();
            if(shelf==null || shelf.isRemoved() || shelf.getLevel()==null) {
                bytes-=next.getValue().mesh.bytes;next.getValue().mesh.close();iterator.remove();
                if(shelf!=null) KEYS.remove(shelf);
            }
        }
    }
    static void unload(net.minecraft.world.level.LevelAccessor level,net.minecraft.world.level.ChunkPos chunk) {
        if(!RenderSystem.isOnRenderThread()) { RenderSystem.recordRenderCall(()->unload(level,chunk));return; }
        var iterator=ENTRIES.entrySet().iterator();
        while(iterator.hasNext()) {
            var next=iterator.next();var shelf=next.getKey().get();
            if(shelf==null || (shelf.getLevel()==level && (shelf.getBlockPos().getX()>>4)==chunk.x && (shelf.getBlockPos().getZ()>>4)==chunk.z)) {
                bytes-=next.getValue().mesh.bytes;next.getValue().mesh.close();iterator.remove();
                if(shelf!=null) KEYS.remove(shelf);
            }
        }
    }
    static void clear() {
        if(!RenderSystem.isOnRenderThread()) { RenderSystem.recordRenderCall(ShelfSceneCache::clear);return; }
        for(var entry:ENTRIES.values()) entry.mesh.close();ENTRIES.clear();KEYS.clear();bytes=builds=0;
    }
    static ShelfRenderCache.Slot[] render(ShelfBlockEntity shelf,PoseStack.Pose pose,MultiBufferSource buffers,int light) {
        ShelfRenderStats.shelf();
        if(ShelfRenderCache.nativeRequired()) { ShelfRenderStats.reason(ShelfRenderStats.Reason.SHADERS);return null; }
        // Direct GPU draws cannot honor arbitrary MultiBufferSource interception.
        if(buffers.getClass()!=MultiBufferSource.BufferSource.class) { ShelfRenderStats.reason(ShelfRenderStats.Reason.WRAPPER);return null; }
        // Native entity fog expects CPU-transformed vertices. In fog, use that exact path.
        float distance=(float)Math.sqrt(pose.pose().m30()*pose.pose().m30()+pose.pose().m31()*pose.pose().m31()+pose.pose().m32()*pose.pose().m32());
        if(distance+4>=RenderSystem.getShaderFogStart()) { ShelfRenderStats.reason(ShelfRenderStats.Reason.FOG);return null; }
        var key=KEYS.computeIfAbsent(shelf,WeakReference::new);
        Entry current=ENTRIES.get(key);
        var facing=shelf.getBlockState().getValue(ShelfBlock.FACING);
        if(current!=null && (current.revision!=shelf.renderRevision() || current.light!=light || current.facing!=facing)) {
            ENTRIES.remove(key);bytes-=current.mesh.bytes;current.mesh.close();current=null;
        }
        if(current==null) {
            if(builds>=1) { ShelfRenderStats.reason(ShelfRenderStats.Reason.BUILD_BUDGET);return null; }
            var slots=ShelfRenderCache.slots(shelf);
            // Warm at most two shared item captures per frame before allocating a whole mesh.
            for(var slot:slots) {
                ShelfRenderCache.mesh(slot,shelf);
            }
            for(var slot:slots) if(slot.entry==null || !slot.entry.resident) { ShelfRenderStats.reason(ShelfRenderStats.Reason.ITEM_PENDING);return null; }
            builds++;long start=ShelfRenderStats.start();ShelfSceneMesh mesh=new ShelfSceneMesh();
            try {
                for(var slot:slots) {
                    if(slot.entry.mesh==null) { mesh.fallback.add(slot);ShelfRenderStats.reason(ShelfRenderStats.Reason.DYNAMIC); }
                    else mesh.add(slot,slot.entry.mesh);
                }
                if(!ShelfSceneMesh.preservesLayerOrder(mesh.layers.size(),mesh.fallback.size())) {
                    mesh.close();mesh.bytes=0;mesh.fallback.clear();Collections.addAll(mesh.fallback,slots);
                }
                while(!ENTRIES.isEmpty() && (ENTRIES.size()>=MAX_SHELVES || bytes+mesh.bytes>MAX_BYTES)) {
                    var iterator=ENTRIES.entrySet().iterator();var oldest=iterator.next();
                    bytes-=oldest.getValue().mesh.bytes;oldest.getValue().mesh.close();iterator.remove();
                }
                if(!staticVboEligible(bytes+mesh.bytes,ENTRIES.size()+1)) { mesh.close();ShelfRenderStats.reason(ShelfRenderStats.Reason.MEMORY);return null; }
                mesh.finish(light);
                current=new Entry(shelf.renderRevision(),light,facing,mesh);ENTRIES.put(key,current);bytes+=mesh.bytes;
            } catch(RuntimeException|Error failure) { mesh.close();throw failure; }
            finally { ShelfRenderStats.build(start); }
        } else ShelfRenderStats.hit();
        if(!ShelfSceneMesh.preservesLayerOrder(current.mesh.layers.size(),current.mesh.fallbackSlots.length)) {
            ShelfRenderStats.reason(ShelfRenderStats.Reason.UNKNOWN_LAYER);return null;
        }
        for(var layer:current.mesh.layers) if(layer.gpu==null && !ShelfBulkWriter.supported(buffers.getBuffer(layer.type))) {
            ShelfRenderStats.reason(ShelfRenderStats.Reason.BULK_WRAPPER);return null;
        }
        long start=ShelfRenderStats.start();
        for(var layer:current.mesh.layers) {
            if(layer.gpu!=null) {
                try { draw(layer,pose); } catch(NativeDrawUnavailable unavailable) { ShelfRenderStats.reason(ShelfRenderStats.Reason.UNKNOWN_LAYER);return null; }
            } else layer.submit(pose,buffers,light);
        }
        ShelfRenderStats.submit(start);
        return current.mesh.fallbackSlots;
    }
    private static void readLight(com.mojang.blaze3d.shaders.Uniform uniform,Vector3f out) {
        var values=uniform.getFloatBuffer();out.set(values.get(0),values.get(1),values.get(2));
    }
    private static void draw(ShelfSceneMesh.Layer layer,PoseStack.Pose pose) {
        var previousShader=RenderSystem.getShader();
        float fogStart=RenderSystem.getShaderFogStart(),fogEnd=RenderSystem.getShaderFogEnd();
        boolean lights=false,setup=false;
        try {
            layer.type.setupRenderState();setup=true;
            ShaderInstance shader=RenderSystem.getShader();
            if(shader==null || shader.LIGHT0_DIRECTION==null || shader.LIGHT1_DIRECTION==null || shader.MODEL_VIEW_MATRIX==null || shader.PROJECTION_MATRIX==null) { layer.type.clearRenderState();setup=false;throw new NativeDrawUnavailable(); }
            if(shader.LIGHT0_DIRECTION!=null && shader.LIGHT1_DIRECTION!=null) {
                RenderSystem.setupShaderLights(shader);
                readLight(shader.LIGHT0_DIRECTION,LIGHT0);readLight(shader.LIGHT1_DIRECTION,LIGHT1);
                SAVED0.set(LIGHT0);SAVED1.set(LIGHT1);
                // dot(L,N*n) == dot(transpose(N)*L,n). The shelf transform is rigid.
                TRANSPOSE.set(pose.normal()).transpose();
                LOCAL0.set(LIGHT0).mul(TRANSPOSE);LOCAL1.set(LIGHT1).mul(TRANSPOSE);
                RenderSystem.setShaderLights(LOCAL0,LOCAL1);lights=true;
                RenderSystem.setupShaderLights(shader);
            }
            // Eligibility above proves the native vertices are entirely before the fog start.
            RenderSystem.setShaderFogStart(Float.MAX_VALUE/4);RenderSystem.setShaderFogEnd(Float.MAX_VALUE/2);
            MODEL_VIEW.set(RenderSystem.getModelViewMatrix()).mul(pose.pose());
            layer.gpu.bind();layer.gpu.drawWithShader(MODEL_VIEW,RenderSystem.getProjectionMatrix(),shader);
            ShelfRenderStats.draw(layer.size/12,true);
        } finally {
            VertexBuffer.unbind();
            RenderSystem.setShaderFogStart(fogStart);RenderSystem.setShaderFogEnd(fogEnd);
            if(setup) layer.type.clearRenderState();
            RenderSystem.setShader(()->previousShader);
            if(lights) { RenderSystem.setShaderLights(SAVED0,SAVED1);if(previousShader!=null) RenderSystem.setupShaderLights(previousShader); }
        }
    }
    static final class NativeDrawUnavailable extends RuntimeException { @Override public synchronized Throwable fillInStackTrace() { return this; } }
    private ShelfSceneCache() {}
}
