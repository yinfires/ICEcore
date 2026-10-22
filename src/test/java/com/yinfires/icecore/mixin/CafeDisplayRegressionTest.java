package com.yinfires.icecore.mixin;

import com.yinfires.icecore.compat.cozycafe.client.CafeModelBounds;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class CafeDisplayRegressionTest {
    @Test void handheldPlateCacheIsSharedAndClearedOnReload() {
        var plate=mock(BakedModel.class); var food=mock(BakedModel.class);
        var key=new com.yinfires.icecore.compat.cozycafe.client.CafePlateItemCache.Key(plate,food,null,Direction.UP);
        com.yinfires.icecore.compat.cozycafe.client.CafePlateItemCache.clear();
        var quads=List.of(mock(BakedQuad.class));
        com.yinfires.icecore.compat.cozycafe.client.CafePlateItemCache.put(key,quads);
        assertSame(quads,com.yinfires.icecore.compat.cozycafe.client.CafePlateItemCache.get(
            new com.yinfires.icecore.compat.cozycafe.client.CafePlateItemCache.Key(plate,food,null,Direction.UP)));
        assertNull(com.yinfires.icecore.compat.cozycafe.client.CafePlateItemCache.get(
            new com.yinfires.icecore.compat.cozycafe.client.CafePlateItemCache.Key(plate,food,null,Direction.DOWN)));
        com.yinfires.icecore.compat.cozycafe.client.CafePlateItemCache.clear();
        assertNull(com.yinfires.icecore.compat.cozycafe.client.CafePlateItemCache.get(key));
    }
    @Test void plateFootprintAndMenuRemainders() {
        for(double bottom:new double[]{-.25,0,.125,.5})for(double[] place:new double[][]{{.1,.125},{1.11,1.15}}) {
            double offset=com.yinfires.icecore.compat.cozycafe.CafeDisplayRules.plateOffset(place[0],place[1],bottom,.75);
            assertEquals(place[1]+.001,place[0]+offset+bottom*.75,1e-9);
        }
        for(double size:new double[]{.25,.8,1,1.5,2}) {
            float scale=com.yinfires.icecore.compat.cozycafe.CafeDisplayRules.plateScale(size,size);
            assertTrue(size*scale<=.750001);
            assertTrue(scale<=.9);
        }
        assertTrue(com.yinfires.icecore.compat.cozycafe.CafeDisplayRules.hideMenu(false,true,false,false));
        assertTrue(com.yinfires.icecore.compat.cozycafe.CafeDisplayRules.hideMenu(false,true,false,true));
        assertFalse(com.yinfires.icecore.compat.cozycafe.CafeDisplayRules.hideMenu(true,true,false,false));
        assertFalse(com.yinfires.icecore.compat.cozycafe.CafeDisplayRules.hideMenu(false,false,true,true));
        assertFalse(com.yinfires.icecore.compat.cozycafe.CafeDisplayRules.hideMenu(false,false,false,false));
    }
    @Test void physicalPlateKeepsCenterForEveryNativeCourse() {
        for(int course=0;course<=4;course++) {
            assertEquals(2,com.yinfires.icecore.compat.cozycafe.CafeDisplayRules.course(true,false,course));
            assertEquals(1,com.yinfires.icecore.compat.cozycafe.CafeDisplayRules.course(false,true,course));
            assertEquals(course,com.yinfires.icecore.compat.cozycafe.CafeDisplayRules.course(false,false,course));
        }
    }
    @org.junit.jupiter.api.BeforeAll static void bootstrap() {
        net.minecraft.SharedConstants.tryDetectVersion();
        try(var hooks=mockStatic(net.minecraftforge.network.NetworkHooks.class)) {
            net.minecraft.server.Bootstrap.bootStrap();
        }
    }
    @Test void platingMatrixUsesRecognitionDrinkAndRealVesselTogether() {
        for(String name:List.of("bread","cake","skewer","bowl_food","cup_food","bottle_food","drink","unknown")) {
            boolean recognized=!name.equals("unknown"), drink=name.equals("drink"), vessel=name.endsWith("_food");
            assertEquals(List.of("bread","cake","skewer").contains(name),com.yinfires.icecore.compat.cozycafe.CafeServingRules.requiresPlate(recognized,drink,vessel),name);
            assertFalse(com.yinfires.icecore.compat.cozycafe.CafeServingRules.requiresPlate(recognized,true,vessel),name);
        }
    }
    @Test void transformedLowestVertexAndCacheLifecycle() {
        BakedModel model=mock(BakedModel.class);
        when(model.isGui3d()).thenReturn(true);
        when(model.applyTransform(any(),any(),anyBoolean())).thenAnswer(call->{
            com.mojang.blaze3d.vertex.PoseStack pose=call.getArgument(1);
            pose.translate(0,.3,0); pose.scale(1,.6F,1); return model;
        });
        BakedQuad quad=mock(BakedQuad.class);
        int[] vertices=new int[32];
        for(int v=0;v<4;v++)vertices[v*8+1]=Float.floatToIntBits(v<2 ? .2F : .8F);
        when(quad.getVertices()).thenReturn(vertices);
        when(model.getQuads(isNull(),nullable(Direction.class),any(),any(),isNull())).thenReturn(List.of(quad));
        CafeModelBounds.clear();
        // ItemRenderer centers vertices before the display transform: .3 + .6*(.2-.5).
        assertEquals(.12F,CafeModelBounds.minimumY(model,ItemDisplayContext.FIXED,false),1e-6);
        CafeModelBounds.minimumY(model,ItemDisplayContext.FIXED,false);
        verify(model,times(1)).applyTransform(any(),any(),anyBoolean());
        assertEquals(.2F,CafeModelBounds.minimumY(model,ItemDisplayContext.NONE,true),1e-6);
        CafeModelBounds.clear();
        CafeModelBounds.minimumY(model,ItemDisplayContext.FIXED,false);
        verify(model,times(2)).applyTransform(any(),any(),anyBoolean());
    }
}
