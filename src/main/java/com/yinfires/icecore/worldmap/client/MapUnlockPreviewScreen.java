package com.yinfires.icecore.worldmap.client;

import com.yinfires.icecore.worldmap.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.texture.DynamicTexture;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.*;
import java.util.concurrent.Future;

/** Local-only combinations over recorded Xaero terrain; never sends network writes. */
final class MapUnlockPreviewScreen extends Screen {
    private final Screen parent;
    private final MapDefinition definition;
    private final Set<String> unlocked=new HashSet<>();
    private final MapViewport view=new MapViewport();
    private XaeroEditorMap nativeMap;
    private ResourceLocation texture;
    private Future<?> task;
    private long generation;
    private boolean suspended;
    private final MapViewport initial;
    private Component selection;
    private MapFog fog;
    private double maskX0,maskZ0,maskX1,maskZ1,requestedX,requestedZ,requestedScale;
    private int maskWidth,maskHeight;
    private long lastMaskRequest;
    MapUnlockPreviewScreen(Screen parent,MapDefinition d,MapViewport initial){super(Component.translatable("icecore.map.preview"));this.parent=parent;this.initial=new MapViewport();this.initial.x=initial.x;this.initial.z=initial.z;definition=d.copy();unlocked.addAll(MapClient.unlocked);}
    @Override protected void init(){
        clearWidgets();if(nativeMap==null){nativeMap=new XaeroEditorMap();nativeMap.resize(width,height);nativeMap.camera(initial);}else nativeMap.resize(width,height);
        String[] actions={"preview_toggle","preview_all","preview_none","back_tools"};
        for(int i=0;i<actions.length;i++){
            String action=actions[i];
            addRenderableWidget(Button.builder(Component.translatable("icecore.map."+action),b->{
                if(action.equals("preview_toggle")){suspended=true;minecraft.setScreen(new MapChoiceScreen(this,definition.regions.stream().map(r->r.id).toList(),id->{if(!unlocked.remove(id))unlocked.add(id);generate();}));}
                else if(action.equals("back_tools"))onClose();
                else{unlocked.clear();if(action.equals("preview_all"))definition.regions.forEach(r->unlocked.add(r.id));generate();}
            }).tooltip(Tooltip.create(Component.translatable("icecore.map.help."+action))).bounds(i*width/4+4,height-24,width/4-8,20).build());
        }
        if(texture==null&&task==null)generate();
    }
    private void generate(){
        selection=Component.translatable("icecore.map.preview_selection",String.join(", ",new TreeSet<>(unlocked)));
        long token=++generation;if(task!=null)task.cancel(true);if(texture!=null){minecraft.getTextureManager().release(texture);texture=null;}
        fog=null;var selection=Set.copyOf(unlocked);task=MapClient.WORK.submit(()->{
            try{var result=MapFog.build(definition,selection);net.minecraft.client.Minecraft.getInstance().execute(()->{if(token==generation){fog=result;task=null;requestedScale=0;}});}
            catch(Exception ex){net.minecraft.client.Minecraft.getInstance().execute(()->{if(token==generation){task=null;if(!(ex instanceof java.util.concurrent.CancellationException))MapClient.feedback("failed");}});}
        });
    }
    private void prepareMask(){
        if(fog==null||task!=null||view.scale<=0)return;
        if(requestedScale==view.scale&&requestedX==view.x&&requestedZ==view.z&&maskWidth==Math.min(2048,width)&&maskHeight==Math.min(2048,Math.max(1,height-28)))return;
        long now=System.nanoTime();if(now-lastMaskRequest<100_000_000L)return;lastMaskRequest=now;
        requestedX=view.x;requestedZ=view.z;requestedScale=view.scale;
        double x0=view.worldX(0),z0=view.worldZ(0),x1=view.worldX(width),z1=view.worldZ(height-28);
        int w=Math.min(2048,width),h=Math.min(2048,Math.max(1,height-28));long token=generation;var field=fog;
        task=MapClient.WORK.submit(()->{
            NativeImage image=null;
            try{
                image=new NativeImage(w,h,false);
                for(int z=0;z<h;z++){if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException();for(int x=0;x<w;x++){
                    double wx=x0+(x+.5)*(x1-x0)/w,wz=z0+(z+.5)*(z1-z0)/h;
                    int alpha=wx<definition.minX||wx>=definition.maxX||wz<definition.minZ||wz>=definition.maxZ?255:field.alpha(wx,wz);
                    image.setPixelRGBA(x,z,alpha<<24|0x808080);
                }}
                NativeImage result=image;image=null;
                net.minecraft.client.Minecraft.getInstance().execute(()->{if(token!=generation){result.close();return;}if(texture!=null)minecraft.getTextureManager().release(texture);texture=minecraft.getTextureManager().register("icecore/map_preview",new DynamicTexture(result));maskX0=x0;maskZ0=z0;maskX1=x1;maskZ1=z1;maskWidth=w;maskHeight=h;task=null;});
            }catch(Exception ex){if(image!=null)image.close();net.minecraft.client.Minecraft.getInstance().execute(()->{if(token==generation){task=null;requestedScale=0;if(!(ex instanceof java.util.concurrent.CancellationException))MapClient.feedback("failed");}});}
        });
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial){
        nativeMap.render(g,x,y,partial,view);prepareMask();
        int left=(int)Math.floor(view.screenX(maskX0)),top=(int)Math.floor(view.screenZ(maskZ0));
        int right=(int)Math.ceil(view.screenX(maskX1)),bottom=(int)Math.ceil(view.screenZ(maskZ1));
        if(texture==null)g.fill(0,0,width,height,0xff808080);
        else{
            g.enableScissor(0,0,width,height-28);g.fill(0,0,width,Math.max(0,Math.min(height,top)),0xff808080);g.fill(0,Math.max(0,Math.min(height,bottom)),width,height,0xff808080);
            g.fill(0,0,Math.max(0,Math.min(width,left)),height,0xff808080);g.fill(Math.max(0,Math.min(width,right)),0,width,height,0xff808080);
            g.flush();com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            int tw=maskWidth,th=maskHeight;
            g.blit(texture,left,top,right-left,bottom-top,0,0,tw,th,tw,th);g.flush();com.mojang.blaze3d.systems.RenderSystem.disableBlend();g.disableScissor();
        }
        g.fill(0,height-28,width,height,0xff202020);super.render(g,x,y,partial);
        if(selection!=null)g.drawString(font,selection,4,4,0xffffffff);
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(y<height-28){nativeMap.drag(dx,dy,view);return true;}return super.mouseDragged(x,y,button,dx,dy);}
    @Override public boolean mouseScrolled(double x,double y,double delta){if(y<height-28){nativeMap.zoom(x,y,delta);return true;}return super.mouseScrolled(x,y,delta);}
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void removed(){if(suspended){suspended=false;return;}generation++;fog=null;if(task!=null)task.cancel(true);task=null;if(texture!=null){minecraft.getTextureManager().release(texture);texture=null;}if(nativeMap!=null){nativeMap.close();nativeMap=null;}}
    void resourceChanged(){generate();}
    void serverChanged(String previous){if(parent instanceof IslandMapScreen map)map.serverChanged(previous);}
    void disposeSession(){suspended=false;removed();parent.removed();}
}
