package com.yinfires.icecore.shelf.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

final class RackItemBatchTest {
    @Test void oneNativeCaptureCanReplayDifferentPlacementsWithoutChangingOriginOrOverlay() {
        var mesh=new ShelfItemMesh();
        mesh.getBuffer(null).vertex(1,-2,3).color(12,34,56,255).overlayCoords(7,8).uv2(0,0).normal(0,1,0).endVertex();
        mesh.finish(false);
        var out=mock(VertexConsumer.class,RETURNS_SELF);
        var first=new PoseStack();first.translate(10,20,30);
        var second=new PoseStack();second.translate(40,50,60);
        mesh.replay(first.last(),type->out,0xE00090);
        mesh.replay(second.last(),type->out,0xE00090);
        verify(out).vertex(11,18,33);verify(out).vertex(41,48,63);
        verify(out,times(2)).overlayCoords(7,8);verify(out,times(2)).uv2(0x90,0xE0);
        assertEquals(-2,mesh.parts().get(0).data[1]);
    }
    @Test void compiledBatchUsesSharedBoundedCacheAndPreservesPerRenderLifecycle() throws Exception {
        var node=new ClassNode();
        try(var in=RackItemBatch.class.getResourceAsStream("RackItemBatch.class")) {new ClassReader(in).accept(node,0);}
        var render=node.methods.stream().filter(m->m.name.equals("render")).findFirst().orElseThrow();
        int cache=0,nativeCalls=0,replay=0,slot=0;
        for(var i:render.instructions)if(i instanceof MethodInsnNode c) {
            if(c.owner.endsWith("/ShelfRenderCache")&&c.name.equals("mesh"))cache++;
            if(c.owner.endsWith("/ItemRenderer")&&c.name.equals("renderStatic"))nativeCalls++;
            if(c.owner.endsWith("/ShelfItemMesh")&&c.name.equals("replay"))replay++;
            if(c.owner.endsWith("/ShelfRenderCache$Slot")&&c.name.equals("<init>"))slot++;
        }
        assertEquals(1,cache);assertEquals(2,nativeCalls);assertEquals(1,replay);assertEquals(1,slot);
        assertTrue(java.util.Arrays.stream(RackItemBatch.class.getDeclaredFields()).anyMatch(f->f.getType()==ShelfRenderCache.Slot[].class));
        var source=java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/yinfires/icecore/shelf/client/RackDisplayCache.java"));
        assertTrue(source.contains("if(recording!=null || batch!=null) return false"));
        assertTrue(source.contains("finally { batch=null; }"));
        assertTrue(source.contains("ShelfShaderCompatibility.acceleratedRenderingPresent()) return false"));
    }
}
