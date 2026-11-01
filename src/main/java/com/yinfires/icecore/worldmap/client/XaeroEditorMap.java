package com.yinfires.icecore.worldmap.client;

import com.yinfires.icecore.mixin.XaeroEditorAccessor;
import com.yinfires.icecore.worldmap.MapViewport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import xaero.map.WorldMapSession;
import xaero.map.gui.GuiMap;

/** Owns a native map screen; calls Minecraft's virtual Screen API, also in SRG production. */
final class XaeroEditorMap {
    private final Screen nativeMap;
    private final XaeroEditorAccessor camera;
    private int width=-1,height=-1;
    XaeroEditorMap(){
        var mc=Minecraft.getInstance();var session=WorldMapSession.getCurrentSession();
        if(session==null||!session.isUsable())throw new IllegalStateException("Xaero session not ready");
        // XaeroLib ScreenBase.init initializes its escape screen. An owner here would
        // recursively initialize IslandMapScreen before Minecraft marks it initialized.
        // This embedded map never owns navigation; both native return targets stay null.
        nativeMap=new GuiMap(null,null,session.getMapProcessor(),mc.player);
        camera=(XaeroEditorAccessor)nativeMap;
    }
    void resize(int w,int h){if(w!=width||h!=height){width=w;height=h;nativeMap.init(Minecraft.getInstance(),w,h);}}
    void render(GuiGraphics g,int mx,int my,float partial,MapViewport view){
        boolean hidden=GuiMap.hiddenUI,attached=XaeroEditorAccessor.icecore$attached();
        GuiMap.hiddenUI=true;XaeroEditorAccessor.icecore$attached(false);
        g.flush();g.pose().pushPose();
        try{nativeMap.render(g,mx,my,partial);g.flush();}finally{g.pose().popPose();GuiMap.hiddenUI=hidden;XaeroEditorAccessor.icecore$attached(attached);}
        // Xaero draws at Z~970. Restore a clean GUI depth plane for vanilla widgets/tooltips.
        g.flush();com.mojang.blaze3d.systems.RenderSystem.clear(org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT,Minecraft.ON_OSX);
        view.width=width;view.height=height;
        var window=Minecraft.getInstance().getWindow();
        view.nativeCamera(window.getWidth(),window.getHeight(),window.getGuiScale(),camera.icecore$scale(),camera.icecore$cameraX(),camera.icecore$cameraZ());
    }
    void drag(double dx,double dz,MapViewport view){camera.icecore$cameraX(camera.icecore$cameraX()-dx/view.scale);camera.icecore$cameraZ(camera.icecore$cameraZ()-dz/view.scale);}
    void zoom(double x,double y,double delta){nativeMap.mouseScrolled(x,y,delta);}
    void camera(MapViewport view){camera.icecore$cameraX(view.x);camera.icecore$cameraZ(view.z);}
    void close(){nativeMap.removed();}
}
