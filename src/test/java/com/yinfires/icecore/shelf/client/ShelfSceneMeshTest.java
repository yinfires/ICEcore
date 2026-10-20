package com.yinfires.icecore.shelf.client;

import org.junit.jupiter.api.Test;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.nio.*;
import static org.junit.jupiter.api.Assertions.*;

final class ShelfSceneMeshTest {
    @Test void nativeNewEntityPackingPreservesEveryAttribute() {
        float[] data={1,2,3,Float.intBitsToFloat(0xAABBCCDD),.25f,.75f,
                Float.intBitsToFloat(4),Float.intBitsToFloat(5),1,0,0,Float.intBitsToFloat(0x00F00020)};
        ByteBuffer out=ByteBuffer.allocate(36).order(ByteOrder.nativeOrder());
        Matrix4f transform=new Matrix4f().translation(10,20,30).rotateY((float)Math.PI/2);
        Matrix3f normal=new Matrix3f().rotationY((float)Math.PI/2);
        ShelfSceneMesh.pack(out,data,0,transform,normal,0x00300090);out.flip();
        assertEquals(13,out.getFloat(),.00001);assertEquals(22,out.getFloat(),.00001);assertEquals(29,out.getFloat(),.00001);
        assertEquals(0xAABBCCDD,out.getInt());assertEquals(.25f,out.getFloat());assertEquals(.75f,out.getFloat());
        assertEquals(4,out.getShort());assertEquals(5,out.getShort());assertEquals(0x00F00090,out.getInt());
        assertEquals(0,out.get());assertEquals(0,out.get());assertEquals(-127,out.get());assertEquals(0,out.get());assertFalse(out.hasRemaining());
    }
    @Test void preparedBufferMatchesNativePackingAndInvalidatesExactly() {
        var layer=new ShelfSceneMesh.Layer(null);
        float[] data={1,2,3,Float.intBitsToFloat(0xAABBCCDD),.25f,.75f,
                Float.intBitsToFloat(4),Float.intBitsToFloat(5),1,0,0,Float.intBitsToFloat(0x00F00020)};
        layer.append(data,12,0,0,0);layer.finish(0,false);
        try {
            Matrix4f m=new Matrix4f().translation(10,20,30).rotateY(.7f);
            Matrix3f n=new Matrix3f().rotationY(.7f);
            for(int step=0;step<4;step++) {
                int light=step>=2?0x00300090:0;
                if(step==1) m.translate(.001f,0,0);
                if(step==3) n.rotateX(.2f);
                assertTrue(layer.prepare(m,n,light));
                ByteBuffer expected=ByteBuffer.allocate(36).order(ByteOrder.nativeOrder());
                ShelfSceneMesh.pack(expected,data,0,m,n,light);expected.flip();
                assertEquals(expected,layer.staging);
                assertFalse(layer.prepare(new Matrix4f(m),new Matrix3f(n),light));
                assertEquals(expected,layer.staging);
            }
        } finally { layer.close(); }
    }
    @Test void movingNativeWritesMatchPackingForEveryVertexAndAttribute() {
        var layer=new ShelfSceneMesh.Layer(null);var random=new java.util.Random(42);
        float[] data=new float[12*256];
        for(int i=0;i<data.length;i+=12) {
            for(int j=0;j<12;j++) data[i+j]=random.nextFloat()*2-1;
            data[i+3]=Float.intBitsToFloat(0xAABBCCDD);data[i+6]=Float.intBitsToFloat(4);
            data[i+7]=Float.intBitsToFloat(5);data[i+11]=Float.intBitsToFloat(0x00F00020);
        }
        layer.append(data,data.length,0,0,0);layer.finish(0,false);
        try {
            for(int frame=0;frame<50;frame++) {
                var m=new Matrix4f().translation(frame*.01f,frame*.003f,-frame*.007f).rotateY(frame*.03f);
                var n=new Matrix3f().rotationY(frame*.03f);int light=frame%2==0?0x00300090:0;
                assertTrue(layer.prepare(m,n,light));
                ByteBuffer expected=ByteBuffer.allocate(256*36).order(ByteOrder.nativeOrder());
                for(int i=0;i<data.length;i+=12) ShelfSceneMesh.pack(expected,data,i,m,n,light);
                expected.flip();assertEquals(expected,layer.staging);
            }
        } finally {layer.close();}
    }
    @Test void unknownConsumerReplaysWithoutPreparingBulkBuffer() {
        var layer=new ShelfSceneMesh.Layer(null);
        layer.append(new float[]{1,2,3,0,0,0,0,0,0,1,0,0},12,0,0,0);layer.finish(0,false);
        try {
            var consumer=org.mockito.Mockito.mock(com.mojang.blaze3d.vertex.VertexConsumer.class,org.mockito.Mockito.RETURNS_SELF);
            var buffers=org.mockito.Mockito.mock(net.minecraft.client.renderer.MultiBufferSource.class);
            org.mockito.Mockito.when(buffers.getBuffer(null)).thenReturn(consumer);
            var pose=new com.mojang.blaze3d.vertex.PoseStack();pose.translate(10,20,30);
            layer.submit(pose.last(),buffers,0);
            assertEquals(1,layer.staging.getFloat(0));
            org.mockito.Mockito.verify(consumer).vertex(11d,22d,33d);
            org.mockito.Mockito.verify(consumer).endVertex();
            org.mockito.Mockito.verify(buffers).getBuffer(null);
        } finally { layer.close(); }
    }
    @Test void localLightTransformationMatchesNativeDiffuseForAllShelfFacings() {
        Vector3f light=new Vector3f(.2f,1,.7f).normalize();
        for(int facing=0;facing<4;facing++) {
            Matrix3f normal=new Matrix3f().rotationXYZ(.4f,facing*(float)Math.PI/2,.1f);
            Vector3f localLight=new Vector3f(light).mul(new Matrix3f(normal).transpose());
            for(Vector3f vertexNormal:new Vector3f[]{new Vector3f(1,0,0),new Vector3f(0,1,0),new Vector3f(0,0,-1)})
                assertEquals(light.dot(new Vector3f(vertexNormal).mul(normal)),localLight.dot(vertexNormal),.000001);
        }
    }
    @Test void lightUpdatesRemainIndependentFromStaticOpaqueAttributes() {
        int baked=0x00F00020;
        assertEquals(0x00F00090,ShelfSceneMesh.light(0x00300090,baked));
        assertEquals(baked,ShelfSceneMesh.light(0,baked));
    }
    @Test void layerMergeRetainsSlotAnchorsWithoutMutatingSharedCapture() {
        float[] source={1,2,3,0,0,0,0,0,0,1,0,0};
        var layer=new ShelfSceneMesh.Layer(null);
        layer.append(source,12,10,20,30);layer.append(source,12,40,50,60);
        assertEquals(24,layer.size);assertEquals(11,layer.data[0]);assertEquals(22,layer.data[1]);assertEquals(33,layer.data[2]);
        assertEquals(41,layer.data[12]);assertEquals(52,layer.data[13]);assertEquals(63,layer.data[14]);assertEquals(1,source[0]);
    }
    @Test void onlyAdjacentStaticLayersMergeAcrossNativeFallbacks() {
        var mesh=new ShelfSceneMesh();
        var first=mesh.nextLayer(null);
        assertSame(first,mesh.nextLayer(null));
        Object nativeSlot=new Object();mesh.ordered.add(nativeSlot);
        var second=mesh.nextLayer(null);
        assertNotSame(first,second);assertSame(second,mesh.nextLayer(null));
        assertEquals(java.util.List.of(first,nativeSlot,second),mesh.ordered);
        assertEquals(2,mesh.layers.size());mesh.close();assertTrue(mesh.ordered.isEmpty());
    }
    @Test void transparentBatchesRetainThePerItemSizeCeiling() {
        assertTrue(ShelfSceneMesh.transparentWithinItemBudget(256*1024));
        assertFalse(ShelfSceneMesh.transparentWithinItemBudget(256*1024+36));
    }
}
