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
        private final Matrix4f lastPose=new Matrix4f();
        private final Matrix3f lastNormal=new Matrix3f();
        private int lastLight;
        private boolean prepared;
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
            // Encode invariant attributes once into the existing staging allocation.
            Matrix4f identity=new Matrix4f();Matrix3f normalIdentity=new Matrix3f();
            for(int i=0;i<size;i+=12) pack(staging,data,i,identity,normalIdentity,light);
            staging.flip();
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
            Object writer=ShelfBulkWriter.resolveWriter(consumer);
            if(writer!=null) {
                prepare(pose.pose(),pose.normal(),light);
                ShelfBulkWriter.writeResolved(consumer,writer,staging,size/12);
            } else {
                ShelfRenderStats.reason(ShelfRenderStats.Reason.BULK_WRAPPER);
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
        boolean prepare(Matrix4f m,Matrix3f n,int light) {
            boolean position=!prepared || !lastPose.equals(m);
            boolean normals=!prepared || !lastNormal.equals(n);
            boolean lighting=!prepared || lastLight!=light;
            if(position || normals || lighting) {
                // staging is a live native allocation sized once to size/12*STRIDE.
                // Independent passes avoid per-vertex mode branches and ByteBuffer bounds checks.
                long base=MemoryUtil.memAddress(staging,0);
                if(position) {
                    float m00=m.m00(),m10=m.m10(),m20=m.m20(),m30=m.m30();
                    float m01=m.m01(),m11=m.m11(),m21=m.m21(),m31=m.m31();
                    float m02=m.m02(),m12=m.m12(),m22=m.m22(),m32=m.m32();
                    for(int i=0;i<size;i+=12,base+=STRIDE) {
                        float x=data[i],y=data[i+1],z=data[i+2];
                        MemoryUtil.memPutFloat(base,m00*x+m10*y+m20*z+m30);
                        MemoryUtil.memPutFloat(base+4,m01*x+m11*y+m21*z+m31);
                        MemoryUtil.memPutFloat(base+8,m02*x+m12*y+m22*z+m32);
                    }
                }
                if(lighting) {
                    base=MemoryUtil.memAddress(staging,0)+28;
                    for(int i=11;i<size;i+=12,base+=STRIDE)
                        MemoryUtil.memPutInt(base,light(light,Float.floatToRawIntBits(data[i])));
                }
                if(normals) {
                    base=MemoryUtil.memAddress(staging,0)+32;
                    float n00=n.m00(),n10=n.m10(),n20=n.m20();
                    float n01=n.m01(),n11=n.m11(),n21=n.m21();
                    float n02=n.m02(),n12=n.m12(),n22=n.m22();
                    for(int i=8;i<size;i+=12,base+=STRIDE) {
                        float x=data[i],y=data[i+1],z=data[i+2];
                        MemoryUtil.memPutByte(base,normal(n00*x+n10*y+n20*z));
                        MemoryUtil.memPutByte(base+1,normal(n01*x+n11*y+n21*z));
                        MemoryUtil.memPutByte(base+2,normal(n02*x+n12*y+n22*z));
                    }
                }
                lastPose.set(m);lastNormal.set(n);lastLight=light;prepared=true;
            }
            staging.position(0);staging.limit(size/12*STRIDE);
            return position || normals || lighting;
        }
        @Override public void close() { if(gpu!=null) { gpu.close();gpu=null; } if(staging!=null) { MemoryUtil.memFree(staging);staging=null; } }
    }
}
