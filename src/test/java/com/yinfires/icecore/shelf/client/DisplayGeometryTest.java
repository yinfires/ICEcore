package com.yinfires.icecore.shelf.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

final class DisplayGeometryTest {
    @Test void staticBehaviorAcceptsAnyItemIdentityButRejectsDynamicHooks() throws Exception {
        assertNull(ShelfRenderCache.behaviorRejection(false,true,false,true,true));
        assertEquals("foil",ShelfRenderCache.behaviorRejection(true,true,false,true,true));
        assertEquals("model_class",ShelfRenderCache.behaviorRejection(false,false,false,true,true));
        assertEquals("custom_renderer",ShelfRenderCache.behaviorRejection(false,true,true,true,true));
        assertEquals("overrides",ShelfRenderCache.behaviorRejection(false,true,false,false,true));
        assertEquals("client_extension",ShelfRenderCache.behaviorRejection(false,true,false,true,false));
        String source=java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/yinfires/icecore/shelf/client/ShelfRenderCache.java"));
        assertFalse(source.contains("getNamespace()"));
        assertFalse(source.contains("type!=Item.class"));
    }
    @Test void realGeneratedLayerZeroHasTintIndexWithoutRequiringDynamicColor() {
        SpriteContents sprite=mock(SpriteContents.class);
        when(sprite.width()).thenReturn(16);when(sprite.height()).thenReturn(16);
        when(sprite.getUniqueFrames()).thenAnswer(i->IntStream.of(0));
        var elements=new ItemModelGenerator().processFrames(0,"layer0",sprite);
        assertFalse(elements.isEmpty());
        for(var element:elements) for(var face:element.faces.values()) {
            assertEquals(0,face.tintIndex);
            var quad=new BakedQuad(new int[32],face.tintIndex,Direction.NORTH,null,true);
            assertTrue(quad.isTinted());
            assertFalse(ShelfRenderCache.tintRequiresLiveColor(quad.isTinted(),false));
            assertTrue(ShelfRenderCache.tintRequiresLiveColor(quad.isTinted(),true));
        }
    }
    @Test void rackCaptureRetainsOriginalOriginAndTransformsNormals() {
        ShelfItemMesh mesh=new ShelfItemMesh();
        mesh.getBuffer(null).vertex(1,-2,3).normal(0,1,0).endVertex();mesh.finish(false);
        assertEquals(-2,mesh.parts().get(0).data[1]);
        PoseStack pose=new PoseStack();pose.translate(4,5,6);pose.scale(.25f,.25f,.25f);
        var layer=new ShelfSceneMesh.Layer(null);
        layer.appendTransformed(mesh.parts().get(0).data,12,pose.last());
        assertEquals(4.25f,layer.data[0]);assertEquals(4.5f,layer.data[1]);assertEquals(6.75f,layer.data[2]);
        assertEquals(-2,mesh.parts().get(0).data[1]);
    }
}
