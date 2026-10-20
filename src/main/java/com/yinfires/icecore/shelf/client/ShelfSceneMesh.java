package com.yinfires.icecore.shelf.client;

import com.mojang.blaze3d.vertex.*;
import com.yinfires.icecore.shelf.ShelfLayout;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.joml.Matrix4f;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import java.nio.*;
import java.util.*;

/** Shelf-local geometry, merged by the real layer returned by the native item renderer. */
final class ShelfSceneMesh implements AutoCloseable {
    static final int STRIDE=36,MAX_BYTES=32*1024*1024;
    // BufferBuilder owns native memory and has no close API in 1.20.1. Reuse one,
    // rather than leaking a native allocation on each upload/rebuild.
    private static BufferBuilder uploadBuilder;
    private static boolean uploadActive;
    final List<Layer> layers=new ArrayList<>();
    final List<ShelfRenderCache.Slot> fallback=new ArrayList<>();
    final List<Object> ordered=new ArrayList<>();
    int bytes,staticSlots;
    static boolean transparentWithinItemBudget(int bytes) { return bytes<=ShelfItemMesh.MAX_BYTES; }
    static boolean opaque(RenderType type) {
        var atlas=TextureAtlas.LOCATION_BLOCKS;
        return type==net.minecraft.client.renderer.Sheets.solidBlockSheet() || type==net.minecraft.client.renderer.Sheets.cutoutBlockSheet()
                || type==RenderType.entitySolid(atlas) || type==RenderType.entityCutout(atlas)
                || type==RenderType.entityCutoutNoCull(atlas) || type==RenderType.entityCutoutNoCull(atlas,false);
    }
    static boolean transparent(RenderType type) {
        var atlas=TextureAtlas.LOCATION_BLOCKS;
        return type==net.minecraft.client.renderer.Sheets.translucentCullBlockSheet()
                || type==RenderType.itemEntityTranslucentCull(atlas) || type==RenderType.entityTranslucentCull(atlas)
                || type==RenderType.entityTranslucent(atlas) || type==RenderType.entityTranslucent(atlas,false);
    }
    static boolean supported(RenderType type) {
        return type.format()==DefaultVertexFormat.NEW_ENTITY && type.mode()==VertexFormat.Mode.QUADS
                && (opaque(type)||transparent(type));
    }
    void add(ShelfRenderCache.Slot slot,ShelfItemMesh mesh) {
        for(var part:mesh.parts()) if(!supported(part.type) || part.size%48!=0 || (!opaque(part.type) && !transparentWithinItemBudget(part.size/12*STRIDE))) {
            fallback.add(slot);ordered.add(slot);ShelfRenderStats.reason(ShelfRenderStats.Reason.UNKNOWN_LAYER);return;
        }
        float x=slot.rack?0:(float)(ShelfLayout.x(slot.index)/16),y=slot.rack?0:(float)(ShelfLayout.y(slot.index)/16),z=slot.rack?0:(float)(ShelfLayout.z(slot.index)/16);
        // Bound the entire mesh for the no-fog proof, including resource-pack geometry.
        for(var part:mesh.parts()) for(int i=0;i<part.size;i+=12) {
            float px=part.data[i]+x,py=part.data[i+1]+y,pz=part.data[i+2]+z;
            if(slot.rack) { var point=new Vector3f(px,py,pz).mulPosition(slot.placement.pose());px=point.x;py=point.y;pz=point.z; }
            if(!Float.isFinite(px+py+pz) || px*px+py*py+pz*pz>9) {
                fallback.add(slot);ordered.add(slot);ShelfRenderStats.reason(ShelfRenderStats.Reason.UNKNOWN_LAYER);return;
            }
        }
        // Conservatively include retained floats, staging bytes and GPU index storage.
        int required=mesh.parts().stream().mapToInt(p->p.size/12*90).sum();
        if(bytes+required>MAX_BYTES) { fallback.add(slot);ordered.add(slot);ShelfRenderStats.reason(ShelfRenderStats.Reason.MEMORY);return; }
        for(var part:mesh.parts()) {
            Layer layer=nextLayer(part.type);
            if(slot.rack) layer.appendTransformed(part.data,part.size,slot.placement);
            else layer.append(part.data,part.size,x,y,z);
        }
        bytes+=required;staticSlots++;
    }
    Layer nextLayer(RenderType type) {
        if(!ordered.isEmpty() && ordered.get(ordered.size()-1) instanceof Layer previous && previous.type==type) return previous;
        Layer layer=new Layer(type);layers.add(layer);ordered.add(layer);return layer;
    }
    void finish(int light,boolean direct) { for(var layer:layers) layer.finish(light,direct); }
    @Override public void close() { for(var layer:layers) layer.close();layers.clear();ordered.clear(); }
    static int light(int current,int baked) {
        return Math.max(current&65535,baked&65535)|(Math.max(current>>>16,baked>>>16)<<16);
    }
    static byte normal(float value) { return (byte)((int)(Math.max(-1,Math.min(1,value))*127)); }
    static void pack(ByteBuffer out,float[] d,int i,Matrix4f m,Matrix3f n,int light) {
        float x=d[i],y=d[i+1],z=d[i+2];
        out.putFloat(m.m00()*x+m.m10()*y+m.m20()*z+m.m30());
        out.putFloat(m.m01()*x+m.m11()*y+m.m21()*z+m.m31());
        out.putFloat(m.m02()*x+m.m12()*y+m.m22()*z+m.m32());
        out.putInt(Float.floatToRawIntBits(d[i+3])).putFloat(d[i+4]).putFloat(d[i+5]);
        out.putShort((short)Float.floatToRawIntBits(d[i+6])).putShort((short)Float.floatToRawIntBits(d[i+7]));
        out.putInt(light(light,Float.floatToRawIntBits(d[i+11])));
        x=d[i+8];y=d[i+9];z=d[i+10];
        out.put(normal(n.m00()*x+n.m10()*y+n.m20()*z));
        out.put(normal(n.m01()*x+n.m11()*y+n.m21()*z));
        out.put(normal(n.m02()*x+n.m12()*y+n.m22()*z)).put((byte)0);
    }
    static final class Layer implements AutoCloseable {
        final RenderType type;
        float[] data=new float[0];int size;
        ByteBuffer staging;
        VertexBuffer gpu;
        Layer(RenderType type) { this.type=type; }
        void append(float[] source,int length,float x,float y,float z) {
            if(size+length>data.length) data=Arrays.copyOf(data,java.lang.Math.max(size+length,java.lang.Math.max(192,data.length*2)));
            System.arraycopy(source,0,data,size,length);
            for(int i=size;i<size+length;i+=12) { data[i]+=x;data[i+1]+=y;data[i+2]+=z; }
            size+=length;
        }
        void appendTransformed(float[] source,int length,PoseStack.Pose pose) {
            int begin=size;append(source,length,0,0,0);
            Vector3f v=new Vector3f();
            for(int i=begin;i<size;i+=12) {
                v.set(data[i],data[i+1],data[i+2]).mulPosition(pose.pose());
                data[i]=v.x;data[i+1]=v.y;data[i+2]=v.z;
                v.set(data[i+8],data[i+9],data[i+10]).mul(pose.normal());
                data[i+8]=v.x;data[i+9]=v.y;data[i+10]=v.z;
            }
        }
        void finish(int light,boolean direct) {
            data=Arrays.copyOf(data,size);
            staging=MemoryUtil.memAlloc(size/12*STRIDE);
            if(direct && opaque(type)) {
                staging.clear();Matrix4f m=new Matrix4f();Matrix3f n=new Matrix3f();
                for(int i=0;i<size;i+=12) pack(staging,data,i,m,n,light);
                staging.flip();
                if(uploadBuilder==null) uploadBuilder=new BufferBuilder(256);
                BufferBuilder builder=uploadBuilder;
                if(uploadActive) throw new IllegalStateException("Reentrant shelf mesh upload");
                uploadActive=true;
                builder.begin(type.mode(),type.format());builder.putBulkData(staging);
                gpu=new VertexBuffer(VertexBuffer.Usage.STATIC);
                try { gpu.bind();gpu.upload(builder.end()); } finally { VertexBuffer.unbind();builder.discard();uploadActive=false;MemoryUtil.memFree(staging);staging=null; }
                // Opaque CPU geometry is unnecessary after upload.
                data=null;
            }
        }
        void submit(PoseStack.Pose pose,MultiBufferSource buffers,int light) {
            VertexConsumer consumer=buffers.getBuffer(type);ShelfRenderStats.buffer();
            ShelfRenderStats.type("consumer",consumer);
            staging.clear();
            for(int i=0;i<size;i+=12) pack(staging,data,i,pose.pose(),pose.normal(),light);
            staging.flip();
            if(!ShelfBulkWriter.write(consumer,staging,size/12)) {
                // Unknown wrappers retain every native VertexConsumer hook.
                for(int i=0;i<size;i+=12) {
                    float x=data[i],y=data[i+1],z=data[i+2];var m=pose.pose();var n=pose.normal();
                    int color=Float.floatToRawIntBits(data[i+3]);int packed=light(light,Float.floatToRawIntBits(data[i+11]));
                    consumer.vertex(m.m00()*x+m.m10()*y+m.m20()*z+m.m30(),m.m01()*x+m.m11()*y+m.m21()*z+m.m31(),m.m02()*x+m.m12()*y+m.m22()*z+m.m32())
                            .color(color&255,(color>>>8)&255,(color>>>16)&255,color>>>24).uv(data[i+4],data[i+5])
                            .overlayCoords(Float.floatToRawIntBits(data[i+6]),Float.floatToRawIntBits(data[i+7])).uv2(packed&65535,packed>>>16);
                    x=data[i+8];y=data[i+9];z=data[i+10];
                    consumer.normal(n.m00()*x+n.m10()*y+n.m20()*z,n.m01()*x+n.m11()*y+n.m21()*z,n.m02()*x+n.m12()*y+n.m22()*z);consumer.endVertex();
                }
            }
            ShelfRenderStats.draw(size/12,false);
        }
        @Override public void close() { if(gpu!=null) { gpu.close();gpu=null; } if(staging!=null) { MemoryUtil.memFree(staging);staging=null; } }
    }
}
