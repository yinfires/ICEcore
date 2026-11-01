package com.yinfires.icecore.worldmap.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import java.util.List;

/** Flush each native field as one unit, preserving Modern UI's EditBox rendering and tooltip. */
final class MapEditorWidgets {
    private MapEditorWidgets(){}
    static void render(GuiGraphics graphics,List<EditBox> fields,int mouseX,int mouseY,float partial){
        graphics.flush();
        for(var field:fields){
            graphics.enableScissor(field.getX()-1,field.getY()-1,field.getX()+field.getWidth()+1,field.getY()+field.getHeight()+1);
            try{field.render(graphics,mouseX,mouseY,partial);graphics.flush();}
            finally{graphics.disableScissor();}
        }
    }
}
