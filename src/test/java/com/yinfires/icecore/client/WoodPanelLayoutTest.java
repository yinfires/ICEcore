package com.yinfires.icecore.client;

import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

final class WoodPanelLayoutTest {
    @Test void actualTextureInteriorHasBalancedMarginsAtAllSizesAndScales() throws Exception {
        var texture=ImageIO.read(Path.of("src/main/resources/assets/icecore/textures/gui/wood_panel.png").toFile());
        int fill=texture.getRGB(13,13);
        // Measure the flat interior, not the outside alpha rectangle (which includes bevels).
        int first=0,last=texture.getWidth()-1;
        while(texture.getRGB(first,13)!=fill) first++;
        while(texture.getRGB(last,13)!=fill) last--;
        assertEquals(first,texture.getWidth()-1-last);
        for(int width:List.of(0,1,10,37,128,303)) for(int height:List.of(0,8,20,74))
            for(double scale:List.of(.75,1.,1.25,1.5,2.,3.,4.)) {
                checkAxis(width,scale,first);
                checkAxis(height,scale,first);
            }
    }

    private static void checkAxis(int size,double scale,int inset) {
        int start=WoodPanelLayout.outerStart(30),end=start+WoodPanelLayout.outerSize(size);
        long left=Math.round(30*scale)-Math.round((start+inset)*scale);
        long right=Math.round((end-inset)*scale)-Math.round((30+size)*scale);
        assertTrue(Math.abs(left-right)<=1,"unbalanced final pixel margins");
    }

    @Test void screenEdgesAndOversizedScrollingUseTheSameFrame() {
        for(int screen:List.of(80,320,854)) for(int size:List.of(1,10,37,68)) {
            for(int desired:List.of(-20,0,6,31,1000)) {
                int content=WoodPanelLayout.constrain(desired,size,screen);
                assertTrue(WoodPanelLayout.outerStart(content)>=0);
                assertTrue(WoodPanelLayout.outerStart(content)+WoodPanelLayout.outerSize(size)<=screen);
                assertEquals(content,WoodPanelLayout.constrain(content,size,screen));
            }
        }
        assertEquals(6,WoodPanelLayout.constrain(-100,500,80));
        // Native scroll remains an independent translation of both frame and content.
        for(float scroll:List.of(0f,.25f,40f,432f))
            assertEquals(6f,(6f-scroll)-(WoodPanelLayout.outerStart(6)-scroll));
    }

    @Test void allAdaptersUseSharedGeometryWithoutUnconditionalShadowPadding() throws Exception {
        Path root=Path.of("src/main/java/com/yinfires/icecore/mixin");
        for(String adapter:List.of("WoodTooltipBackgroundMixin.java","WoodModernTooltipMixin.java")) {
            String source=Files.readString(root.resolve(adapter));
            assertTrue(source.contains("WoodPanel.drawAround("));
            assertFalse(source.contains("+13"));
        }
        for(String adapter:List.of("WoodTooltipLayoutMixin.java","WoodModernTooltipMixin.java"))
            assertTrue(Files.readString(root.resolve(adapter)).contains("WoodPanelLayout.constrain("));
        String jade=Files.readString(root.resolve("WoodJadeLayoutMixin.java"));
        assertTrue(jade.contains("Arrays.fill(padding,WoodPanelLayout.BORDER)"));
        assertFalse(jade.contains("padding[1]="));
        assertFalse(Files.readString(root.resolve("WoodModernTooltipMixin.java")).contains("Math.floor(x)"));
    }
}
