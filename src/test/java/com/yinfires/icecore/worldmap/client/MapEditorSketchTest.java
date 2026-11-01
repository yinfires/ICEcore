package com.yinfires.icecore.worldmap.client;

import com.yinfires.icecore.worldmap.*;
import net.minecraft.client.gui.GuiGraphics;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.mockito.Mockito.*;

class MapEditorSketchTest {
    @Test void selectingOrAcceptingSharedEdgeNeverRendersExistingYellowVertices(){
        var g=mock(GuiGraphics.class);var v=new MapViewport();v.width=200;v.height=100;
        var points=List.of(new MapDefinition.Point(0,0),new MapDefinition.Point(20,0));
        MapEditorSketch.vertices(g,v,points,MapEditMode.SHARED_EDGE);
        MapEditorSketch.vertices(g,v,points,MapEditMode.SHARED_EDGE.afterAcceptingEdge());verifyNoInteractions(g);
        MapEditorSketch.vertices(g,v,points,MapEditMode.POINTS);
        verify(g).fill(98,48,103,53,0xffffff00);verify(g).fill(118,48,123,53,0xffffff00);verifyNoMoreInteractions(g);
    }
}
