package com.yinfires.icecore.worldmap.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.yinfires.icecore.worldmap.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;

/** At most 48 resident 512px textures (48 MiB), four bounded in-flight decodes. */
final class MapAtlas implements AutoCloseable {
    private final MapDefinition definition;
    private final Set<String> unlocked;
    private final LinkedHashMap<String,ResourceLocation> resident=new LinkedHashMap<>(64,.75f,true);
    private final Set<String> pending=new HashSet<>(),missing=new HashSet<>();
    private final String[][][] keys=new String[6][][];
    private final int[] firstX=new int[6],firstZ=new int[6];
    private Future<?> fogTask;
    private volatile MapFog fog;
    private volatile boolean closed;
    private boolean failed;
    private boolean viewReady;
    private int capacity=48;
    private static int inFlight;
    void capacity(int value){capacity=value;trim();}
    private void trim(){var textures=Minecraft.getInstance().getTextureManager();while(resident.size()>capacity){var it=resident.entrySet().iterator();textures.release(it.next().getValue());it.remove();}}
    boolean ready(){return viewReady;}
    void prepare(MapViewport view){draw(null,view);}
    MapAtlas(MapDefinition d,Set<String> unlocked){
        definition=d;this.unlocked=Set.copyOf(unlocked);
        if(d.atlas.equals("unpublished")){failed=true;MapClient.feedback("not_published");return;}
        for(int level=0;level<6;level++){
            int span=512<<level;firstX[level]=Math.floorDiv(d.minX,span);firstZ[level]=Math.floorDiv(d.minZ,span);
            int columns=Math.floorDiv(d.maxX-1,span)-firstX[level]+1,rows=Math.floorDiv(d.maxZ-1,span)-firstZ[level]+1;
            keys[level]=new String[rows][columns];for(int z=0;z<rows;z++)for(int x=0;x<columns;x++)keys[level][z][x]=level+"/"+(firstX[level]+x)+"_"+(firstZ[level]+z);
        }
        fogTask=MapClient.WORK.submit(()->{try{fog=MapFog.build(d,this.unlocked);}catch(Exception e){if(!closed){LogUtils.getLogger().error("Map fog failed",e);Minecraft.getInstance().execute(()->{failed=true;MapClient.feedback("failed");});}}});
    }
    void draw(GuiGraphics g,MapViewport v){
        viewReady=false;if(closed||failed||fog==null)return;
        int clipX0=Math.max(0,(int)Math.ceil(v.screenX(definition.minX))),clipZ0=Math.max(0,(int)Math.ceil(v.screenZ(definition.minZ)));
        int clipX1=Math.min(v.width,(int)Math.floor(v.screenX(definition.maxX))),clipZ1=Math.min(v.height,(int)Math.floor(v.screenZ(definition.maxZ)));
        if(clipX1<=clipX0||clipZ1<=clipZ0)return;
        if(g!=null)g.enableScissor(clipX0,clipZ0,clipX1,clipZ1);
        int level=Math.max(0,Math.min(5,(int)Math.floor(Math.log(1/v.scale)/Math.log(2))));int span=512<<level;
        int x0=Math.max(Math.floorDiv(definition.minX,span),Math.floorDiv((int)Math.floor(v.worldX(0)),span));
        int z0=Math.max(Math.floorDiv(definition.minZ,span),Math.floorDiv((int)Math.floor(v.worldZ(0)),span));
        int x1=Math.min(Math.floorDiv(definition.maxX-1,span),Math.floorDiv((int)Math.ceil(v.worldX(v.width)),span));
        int z1=Math.min(Math.floorDiv(definition.maxZ-1,span),Math.floorDiv((int)Math.ceil(v.worldZ(v.height)),span));
        while((x1-x0+1)*(z1-z0+1)>Math.min(capacity,24)&&level<5){level++;span=512<<level;x0=Math.max(firstX[level],Math.floorDiv((int)Math.floor(v.worldX(0)),span));z0=Math.max(firstZ[level],Math.floorDiv((int)Math.floor(v.worldZ(0)),span));x1=Math.min(firstX[level]+keys[level][0].length-1,Math.floorDiv((int)Math.ceil(v.worldX(v.width)),span));z1=Math.min(firstZ[level]+keys[level].length-1,Math.floorDiv((int)Math.ceil(v.worldZ(v.height)),span));}
        boolean complete=true;
        for(int z=z0;z<=z1;z++)for(int x=x0;x<=x1;x++){
            String key=keys[level][z-firstZ[level]][x-firstX[level]];ResourceLocation texture=resident.get(key);
            if(texture==null){complete=false;queue(key,level,x,z);continue;}
            if(g==null)continue;
            int left=(int)Math.floor(v.screenX((double)x*span)),top=(int)Math.floor(v.screenZ((double)z*span));
            int right=(int)Math.ceil(v.screenX((double)(x+1)*span)),bottom=(int)Math.ceil(v.screenZ((double)(z+1)*span));
            g.blit(texture,left,top,right-left,bottom-top,0,0,512,512,512,512);
        }
        if(g!=null)g.disableScissor();viewReady=complete;
    }
    private void queue(String key,int level,int x,int z){
        if(inFlight>=4||pending.contains(key)||missing.contains(key))return;pending.add(key);inFlight++;
        ResourceLocation resource=new ResourceLocation("icecore","world_map/"+definition.atlas+"/"+key+".png");
        var manager=Minecraft.getInstance().getResourceManager();MapFog field=fog;
        MapClient.WORK.execute(()->{
            if(closed){Minecraft.getInstance().execute(()->{pending.remove(key);inFlight--;});return;}
            NativeImage image=null;Exception error=null;
            try(var stream=manager.open(resource)){
                var source=ImageIO.read(stream);if(source==null||source.getWidth()!=512||source.getHeight()!=512)throw new IllegalArgumentException("atlas tile must be 512x512");
                image=new NativeImage(512,512,false);int span=512<<level;
                for(int pz=0;pz<512;pz++)for(int px=0;px<512;px++){
                    if(px==0&&closed)throw new CancellationException();
                    double wx=(double)x*span+(px+.5)*(1<<level),wz=(double)z*span+(pz+.5)*(1<<level);
                    int a=wx<definition.minX||wx>=definition.maxX||wz<definition.minZ||wz>=definition.maxZ?255:field.alpha(wx,wz);
                    int color=source.getRGB(px,pz),r=((color>>16&255)*(255-a)+128*a)/255,gr=((color>>8&255)*(255-a)+128*a)/255,b=((color&255)*(255-a)+128*a)/255;
                    image.setPixelRGBA(px,pz,0xff000000|b<<16|gr<<8|r);
                }
            }catch(Exception ex){error=ex;if(image!=null){image.close();image=null;}}
            NativeImage result=image;Exception problem=error;
            Minecraft.getInstance().execute(()->{
                pending.remove(key);inFlight--;if(closed){if(result!=null)result.close();return;}
                if(problem!=null){missing.add(key);if(missing.size()==1)MapClient.feedback("missing_tiles");return;}
                var textures=Minecraft.getInstance().getTextureManager();ResourceLocation id=textures.register("icecore/map",new DynamicTexture(result));resident.put(key,id);
                trim();
            });
        });
    }
    @Override public void close(){closed=true;if(fogTask!=null)fogTask.cancel(true);var textures=Minecraft.getInstance().getTextureManager();resident.values().forEach(textures::release);resident.clear();}
}
