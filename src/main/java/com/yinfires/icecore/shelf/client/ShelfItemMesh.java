package com.yinfires.icecore.shelf.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import java.util.*;

/** Captures the native item pipeline, then replays through the caller's ordinary buffers. */
final class ShelfItemMesh implements MultiBufferSource {
    static final int MAX_BYTES=256*1024;
    private final List<Part> parts=new ArrayList<>();
    private int vertices;
    List<Part> parts() { return parts; }
    int bytes() { return vertices*48+parts.size()*64; }
    @Override public VertexConsumer getBuffer(RenderType type) {
        // Preserve buffer request order, including repeated layers.
        Part part=new Part(type); parts.add(part); return part;
    }
    void finish() {
        float bottom=Float.POSITIVE_INFINITY;
        for(Part part:parts) for(int i=0;i<part.size;i+=12) bottom=Math.min(bottom,part.data[i+1]);
        if(!Float.isFinite(bottom)) bottom=0;
        for(Part part:parts) {
            part.data=Arrays.copyOf(part.data,part.size);
            for(int i=0;i<part.size;i+=12) part.data[i+1]-=bottom;
        }
    }
    void replay(PoseStack.Pose pose,MultiBufferSource buffers,int light) {
        Matrix4f m=pose.pose(); Matrix3f n=pose.normal();
        for(Part part:parts) {
            VertexConsumer out=buffers.getBuffer(part.type);
            for(int i=0;i<part.size;i+=12) {
                float[] d=part.data; float x=d[i],y=d[i+1],z=d[i+2];
                out.vertex(m.m00()*x+m.m10()*y+m.m20()*z+m.m30(),m.m01()*x+m.m11()*y+m.m21()*z+m.m31(),m.m02()*x+m.m12()*y+m.m22()*z+m.m32());
                int color=Float.floatToRawIntBits(d[i+3]);
                out.color(color&255,(color>>>8)&255,(color>>>16)&255,color>>>24).uv(d[i+4],d[i+5]);
                int bakedLight=Float.floatToRawIntBits(d[i+11]);
                out.overlayCoords(Float.floatToRawIntBits(d[i+6]),Float.floatToRawIntBits(d[i+7]))
                        .uv2(Math.max(light&65535,bakedLight&65535),Math.max(light>>>16,bakedLight>>>16));
                x=d[i+8]; y=d[i+9]; z=d[i+10];
                out.normal(n.m00()*x+n.m10()*y+n.m20()*z,n.m01()*x+n.m11()*y+n.m21()*z,n.m02()*x+n.m12()*y+n.m22()*z);
                out.endVertex();
            }
        }
    }
    static final class TooLarge extends RuntimeException {
        @Override public synchronized Throwable fillInStackTrace() { return this; }
    }
    final class Part implements VertexConsumer {
        final RenderType type;
        float[] data=new float[192]; int size;
        float x,y,z,u,v,nx,ny,nz; int color=-1,overlayU,overlayV,light;
        Part(RenderType type) { this.type=type; }
        @Override public VertexConsumer vertex(double x,double y,double z) { this.x=(float)x;this.y=(float)y;this.z=(float)z;return this; }
        @Override public VertexConsumer color(int r,int g,int b,int a) { color=r|(g<<8)|(b<<16)|(a<<24);return this; }
        @Override public VertexConsumer uv(float u,float v) { this.u=u;this.v=v;return this; }
        @Override public VertexConsumer overlayCoords(int u,int v) { overlayU=u;overlayV=v;return this; }
        @Override public VertexConsumer uv2(int u,int v) { light=u|(v<<16);return this; }
        @Override public VertexConsumer normal(float x,float y,float z) { nx=x;ny=y;nz=z;return this; }
        @Override public void endVertex() {
            if((vertices+1)*48+parts.size()*64>MAX_BYTES) throw new TooLarge();
            if(size+12>data.length) data=Arrays.copyOf(data,data.length*2);
            data[size++]=x;data[size++]=y;data[size++]=z;data[size++]=Float.intBitsToFloat(color);
            data[size++]=u;data[size++]=v;data[size++]=Float.intBitsToFloat(overlayU);data[size++]=Float.intBitsToFloat(overlayV);
            data[size++]=nx;data[size++]=ny;data[size++]=nz;data[size++]=Float.intBitsToFloat(light);vertices++;
        }
        @Override public void defaultColor(int r,int g,int b,int a) { color(r,g,b,a); }
        @Override public void unsetDefaultColor() { color=-1; }
    }
}
