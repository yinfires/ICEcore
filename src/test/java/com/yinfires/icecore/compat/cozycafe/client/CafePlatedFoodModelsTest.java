package com.yinfires.icecore.compat.cozycafe.client;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.client.model.data.ModelData;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class CafePlatedFoodModelsTest {
    @Test void finalGeometryFitsPlateForFlatItemsAndOffsetBlockModels() {
        for(boolean block:new boolean[]{true,false}) {
            BakedModel source=mock(BakedModel.class);
            when(source.applyTransform(any(),any(),anyBoolean())).thenAnswer(call->{
                com.mojang.blaze3d.vertex.PoseStack pose=call.getArgument(1);
                pose.translate(.2,.1,-.15);pose.scale(1.2F,.8F,1.1F);return source;
            });
            int[] vertices=new int[32];
            float[][] points={{-.2F,-.15F,0},{1.3F,-.15F,0},{1.3F,.9F,.2F},{-.2F,.9F,.2F}};
            for(int v=0;v<4;v++)for(int axis=0;axis<3;axis++)vertices[v*8+axis]=Float.floatToIntBits(points[v][axis]);
            BakedQuad quad=new BakedQuad(vertices,-1,Direction.NORTH,null,true);
            when(source.getQuads(isNull(),nullable(Direction.class),any(),any(),isNull())).thenAnswer(c->c.getArgument(1)==null?List.of(quad):List.of());
            BakedModel normalized=CafePlatedFoodModels.normalize(source,block);
            var bounds=CafeModelBounds.blockBounds(normalized);
            assertEquals(0,bounds.minY,1e-6);
            assertEquals(0,(bounds.minX+bounds.maxX)/2,1e-6);
            assertEquals(0,(bounds.minZ+bounds.maxZ)/2,1e-6);
            assertTrue(bounds.getXsize()<=.750001);
            assertTrue(bounds.getZsize()<=.750001);
            assertSame(normalized,normalized.getRenderPasses(null,false).get(0));
            var finalPose=new com.mojang.blaze3d.vertex.PoseStack();
            assertSame(normalized,normalized.applyTransform(ItemDisplayContext.NONE,finalPose,false));
            finalPose.translate(-.5,-.5,-.5);
            var bottom=new org.joml.Vector3f(0,(float)bounds.minY,0);
            finalPose.last().pose().transformPosition(bottom);
            assertEquals(0,bottom.y,1e-6);
            assertEquals(0,bottom.x,1e-6);
        }
    }
}
