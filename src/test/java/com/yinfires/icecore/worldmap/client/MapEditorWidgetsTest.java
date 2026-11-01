package com.yinfires.icecore.worldmap.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.mockito.Mockito.*;

class MapEditorWidgetsTest {
    @Test void nativeFieldsRenderOnceWithSeparateFlushAndClipBoundaries(){
        var graphics=mock(GuiGraphics.class);var fields=List.of(mock(EditBox.class),mock(EditBox.class),mock(EditBox.class),mock(EditBox.class));
        for(int i=0;i<fields.size();i++){var f=fields.get(i);when(f.getX()).thenReturn(i*100);when(f.getY()).thenReturn(300);when(f.getWidth()).thenReturn(90);when(f.getHeight()).thenReturn(20);}
        MapEditorWidgets.render(graphics,fields,4,5,.5f);
        var order=inOrder(graphics,fields.get(0),fields.get(1),fields.get(2),fields.get(3));order.verify(graphics).flush();
        for(int i=0;i<fields.size();i++){
            order.verify(graphics).enableScissor(i*100-1,299,i*100+91,321);order.verify(fields.get(i)).render(graphics,4,5,.5f);
            order.verify(graphics).flush();order.verify(graphics).disableScissor();verify(fields.get(i),times(1)).render(graphics,4,5,.5f);
        }
        order.verifyNoMoreInteractions();
    }
}
