package com.yinfires.icecore.worldmap.client;

import com.yinfires.icecore.worldmap.*;
import net.minecraft.client.gui.GuiGraphics;
import java.util.List;

/** Vertex overlay shared by normal drawing and explicit edge-selection mode. */
final class MapEditorSketch {
    private MapEditorSketch(){}
    static void vertices(GuiGraphics g,MapViewport view,List<MapDefinition.Point> points,MapEditMode mode){
        if(!mode.showsVertices())return;
        for(var p:points){int x=(int)view.screenX(p.x()),z=(int)view.screenZ(p.z());g.fill(x-2,z-2,x+3,z+3,0xffffff00);}
    }
}
