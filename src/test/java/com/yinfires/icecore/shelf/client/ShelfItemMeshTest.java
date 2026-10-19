package com.yinfires.icecore.shelf.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

final class ShelfItemMeshTest {
    @Test void capturedVerticesPreserveAttributesAndUseCurrentLightAndTransform() {
        ShelfItemMesh mesh=new ShelfItemMesh();
        RenderType layer=null;
        VertexConsumer capture=mesh.getBuffer(layer);
        capture.vertex(1,-2,3).color(12,34,56,255).uv(.2f,.7f).overlayCoords(4,5).uv2(0,0).normal(0,1,0).endVertex();
        capture.vertex(2,-1,4).color(1,2,3,255).uv(.4f,.8f).overlayCoords(6,7).normal(1,0,0).endVertex();
        mesh.finish();
        
        VertexConsumer output=mock(VertexConsumer.class,RETURNS_SELF);
        MultiBufferSource buffers=type->{ assertSame(layer,type); return output; };
        PoseStack pose=new PoseStack();pose.translate(10,20,30);
        mesh.replay(pose.last(),buffers,0x00E00090);
        verify(output).vertex(11,20,33);verify(output).vertex(12,21,34);
        verify(output).color(12,34,56,255);verify(output).uv(.2f,.7f);
        verify(output).overlayCoords(4,5);verify(output,times(2)).uv2(0x90,0xE0);
        verify(output).normal(0,1,0);verify(output,times(2)).endVertex();
    }
    @Test void captureCannotExceedIndividualMemoryBudget() {
        ShelfItemMesh mesh=new ShelfItemMesh();VertexConsumer capture=mesh.getBuffer(null);
        assertThrows(ShelfItemMesh.TooLarge.class,()->{ for(int i=0;i<10000;i++) capture.vertex(0,0,0).endVertex(); });
        assertTrue(mesh.bytes()<=ShelfItemMesh.MAX_BYTES);
    }
    @Test void replayKeepsBakedEmissionAndRotatesNormals() {
        ShelfItemMesh mesh=new ShelfItemMesh();
        mesh.getBuffer(null).vertex(1,0,0).color(255,255,255,255).uv2(240,240).normal(1,0,0).endVertex();
        mesh.finish();
        VertexConsumer output=mock(VertexConsumer.class,RETURNS_SELF);
        PoseStack pose=new PoseStack();pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));
        mesh.replay(pose.last(),type->output,0);
        verify(output).uv2(240,240);
        var normal=org.mockito.ArgumentCaptor.forClass(Float.class);
        verify(output).normal(normal.capture(),normal.capture(),normal.capture());
        assertEquals(0,normal.getAllValues().get(0),.00001);
        assertEquals(0,normal.getAllValues().get(1),.00001);
        assertEquals(-1,normal.getAllValues().get(2),.00001);
    }
}
