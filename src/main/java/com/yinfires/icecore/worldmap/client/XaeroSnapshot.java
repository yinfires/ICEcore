package com.yinfires.icecore.worldmap.client;

import com.mojang.logging.LogUtils;
import com.yinfires.icecore.worldmap.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import xaero.map.*;
import xaero.map.gui.*;
import xaero.map.file.export.*;
import java.awt.image.BufferedImage;
import java.awt.*;
import javax.imageio.ImageIO;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;

/** Uses Xaero's actual framebuffer exporter, one bounded tile per render opportunity. */
final class XaeroSnapshot {
    private static Job job;
    static int[] missingChunk;
    private XaeroSnapshot(){}
    static boolean busy(){return job!=null;}
    static String progress(){return job==null?"":job.cursor+"/"+job.total;}
    static void cancel(){if(job!=null){job.cancelled=true;if(job.worker!=null)job.worker.cancel(true);job=null;}}
    static void begin(MapDefinition d){
        if(job!=null)throw new IllegalStateException("export running");
        var mc=Minecraft.getInstance();var session=WorldMapSession.getCurrentSession();
        if(!MapClient.operator||session==null||!session.isUsable()||!mc.level.dimension().location().toString().equals(d.dimension))throw new IllegalArgumentException("export session/dimension");
        // Xaero selections use inclusive chunk coordinates. Keep exact block bounds in the manifest.
        d.validate();missingChunk=null;job=new Job(d,session.getMapProcessor(),MapClient.revision);
    }
    static void renderStep(){
        Job j=job;if(j==null||j.cancelled||j.processing||System.nanoTime()<j.next)return;
        var mc=Minecraft.getInstance();
        if(mc.level==null||!mc.level.dimension().location().toString().equals(j.definition.dimension)||WorldMapSession.getCurrentSession()==null||WorldMapSession.getCurrentSession().getMapProcessor()!=j.processor){cancel();return;}
        try{j.step();}catch(Exception ex){LogUtils.getLogger().error("Map snapshot failed; previous atlas preserved",ex);MapClient.feedback("capture_failed");cancel();}
    }
    private static final class Job {
        final MapDefinition definition;final MapProcessor processor;final long revision;
        final Path root,tiles;final int firstX,firstZ,columns,rows,total;
        int cursor;long next,loadDeadline;boolean processing;volatile boolean cancelled;Future<?> worker;
        MapCapturePlan coverage;
        Job(MapDefinition d,MapProcessor p,long revision){
            definition=d;processor=p;this.revision=revision;d.atlas="atlas_"+UUID.randomUUID().toString().replace("-","");
            root=net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("icecore/world_map_atlases").resolve(d.atlas);tiles=root.resolve("assets/icecore/world_map").resolve(d.atlas);
            firstX=Math.floorDiv(d.minX,512);firstZ=Math.floorDiv(d.minZ,512);columns=Math.floorDiv(d.maxX-1,512)-firstX+1;rows=Math.floorDiv(d.maxZ-1,512)-firstZ+1;total=columns*rows;
            processing=true;worker=MapClient.WORK.submit(()->{
                try{
                    var plan=MapCapturePlan.build(d);
                    if(plan.count()==0)throw new IllegalArgumentException("no region terrain to export");
                    Minecraft.getInstance().execute(()->{if(job==this&&!cancelled){coverage=plan;processing=false;}});
                }catch(Exception ex){Minecraft.getInstance().execute(()->{if(job==this){LogUtils.getLogger().error("Map capture coverage failed",ex);MapClient.feedback("capture_failed");cancel();}});}
            });
        }
        void step() throws Exception {
            if(MapClient.revision!=revision)throw new IllegalStateException("map changed during export");
            if(cursor>=total){finish();return;}
            int tx=firstX+cursor%columns,tz=firstZ+cursor/columns;
            int x0=Math.max(tx*512,definition.minX),z0=Math.max(tz*512,definition.minZ),x1=Math.min((tx+1)*512,definition.maxX),z1=Math.min((tz+1)*512,definition.maxZ);
            if(!coverage.intersects(x0,z0,x1,z1)){process(null,tx,tz,x0,z0,x1,z1);processing=true;return;}
            int layer=processor.getCurrentCaveLayer();if(layer!=Integer.MAX_VALUE)throw new IllegalStateException("select surface map before export");
            var region=processor.getLeafMapRegion(layer,tx,tz,true);
            if(region==null)throw new IllegalStateException("no Xaero region at "+tx+","+tz);
            if(region.getLoadState()!=2){
                if(loadDeadline==0){loadDeadline=System.nanoTime()+30000000000L;processor.getMapSaveLoad().requestLoad(region,"icecore atlas");}
                if(System.nanoTime()>loadDeadline)throw new IllegalStateException("Xaero cache not loaded at "+tx+","+tz);
                next=System.nanoTime()+100000000L;return;
            }
            MapTileSelection selection=new MapTileSelection(x0>>4,z0>>4);selection.setEnd((x1-1)>>4,(z1-1)>>4);
            Path raw=root.resolve("raw").resolve(""+cursor);Files.createDirectories(raw);
            var registries=Minecraft.getInstance().level.registryAccess();
            processing=true;
            // Match MapSaveLoad.exportPNG: serialize against Xaero's map runner, execute GL
            // on the Minecraft thread, release the runner even on cancellation/failure.
            WorldMap.mapRunner.addTask(new xaero.map.task.MapRunnerTask(){
                @Override public void run(MapProcessor current){
                    CompletableFuture<Void> done=new CompletableFuture<>();
                    Minecraft.getInstance().execute(()->{
                        try{
                            if(cancelled||job!=Job.this||current!=processor)return;
                            // A disk-loaded tile has isLoaded=true but wasWrittenOnce=false.
                            // Match Xaero's own MapSaveLoad completeness criterion, under its runner.
                            for(int cz=z0>>4;cz<=(z1-1)>>4;cz++)for(int cx=x0>>4;cx<=(x1-1)>>4;cx++){
                                if(!coverage.required(cx,cz))continue;
                                var chunk=region.getChunk((cx&31)>>2,(cz&31)>>2);var tile=chunk==null?null:chunk.getTile(cx&3,cz&3);
                                if(tile==null||!tile.isLoaded()){
                                    missingChunk=new int[]{cx,cz};
                                    throw new IllegalStateException("missing recorded chunk "+cx+","+cz);
                                }
                            }
                            var result=new PNGExporter(raw).export(processor,registries.registryOrThrow(Registries.BIOME),registries.registryOrThrow(Registries.DIMENSION_TYPE),selection,processor.getMapSaveLoad().getOldFormatSupport());
                            if(!result.getType().isSuccess())throw new IllegalStateException("Xaero export: "+result.getType());
                            process(raw,tx,tz,x0,z0,x1,z1);
                        }catch(Exception ex){LogUtils.getLogger().error("Xaero atlas export failed",ex);if(missingChunk!=null)com.yinfires.icecore.feedback.PlayerFeedback.showLocal(Minecraft.getInstance().player,net.minecraft.network.chat.Component.translatable("icecore.map.capture_missing",missingChunk[0],missingChunk[1]));else MapClient.feedback("capture_failed");cancel();}
                        finally{done.complete(null);}
                    });
                    try{done.get(30,TimeUnit.SECONDS);}catch(Exception ex){cancelled=true;Minecraft.getInstance().execute(()->{if(job==Job.this){MapClient.feedback("capture_failed");cancel();}});}
                }
            });
        }
        void process(Path raw,int tx,int tz,int x0,int z0,int x1,int z1){
            worker=MapClient.WORK.submit(()->{
                try{
                    BufferedImage source=null;int chunkX=x0>>4<<4,chunkZ=z0>>4<<4;
                    if(raw!=null){
                        Path image;try(var paths=Files.walk(raw)){var list=paths.filter(f->f.toString().endsWith(".png")).toList();if(list.size()!=1)throw new IllegalStateException("unexpected Xaero image count");image=list.get(0);}
                        source=ImageIO.read(image.toFile());
                        if(source==null||source.getWidth()!=(((x1-1)>>4)+1)*16-chunkX||source.getHeight()!=(((z1-1)>>4)+1)*16-chunkZ)throw new IllegalStateException("Xaero export scale must be 1:1");
                    }
                    BufferedImage out=new BufferedImage(512,512,BufferedImage.TYPE_INT_RGB);Graphics2D g=out.createGraphics();g.setColor(new Color(128,128,128));g.fillRect(0,0,512,512);if(source!=null)g.drawImage(source,chunkX-tx*512,chunkZ-tz*512,null);
                    // Gray out unused chunks before downsampling so absent terrain cannot bleed black.
                    for(int cz=0;cz<32;cz++)for(int cx=0;cx<32;cx++)if(!coverage.required(tx*32+cx,tz*32+cz))g.fillRect(cx*16,cz*16,16,16);
                    g.dispose();Files.createDirectories(tiles.resolve("0"));ImageIO.write(out,"png",tiles.resolve("0/"+tx+"_"+tz+".png").toFile());if(source!=null)source.flush();out.flush();
                    Minecraft.getInstance().execute(()->{if(job==this&&!cancelled){cursor++;processing=false;loadDeadline=0;next=System.nanoTime()+100000000L;}});
                }catch(Exception ex){Minecraft.getInstance().execute(()->{if(job==this){LogUtils.getLogger().error("Map tile failed",ex);MapClient.feedback("capture_failed");cancel();}});}
            });
        }
        void finish(){
            processing=true;worker=MapClient.WORK.submit(()->{
                try{
                    for(int level=1;level<=5;level++){
                        int span=512<<level;Path folder=tiles.resolve(""+level);Files.createDirectories(folder);
                        for(int z=Math.floorDiv(definition.minZ,span);z<=Math.floorDiv(definition.maxZ-1,span);z++)for(int x=Math.floorDiv(definition.minX,span);x<=Math.floorDiv(definition.maxX-1,span);x++){
                            if(cancelled||Thread.currentThread().isInterrupted())return;
                            BufferedImage out=new BufferedImage(512,512,BufferedImage.TYPE_INT_RGB);Graphics2D g=out.createGraphics();g.setColor(new Color(128,128,128));g.fillRect(0,0,512,512);g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                            for(int dz=0;dz<2;dz++)for(int dx=0;dx<2;dx++){Path child=tiles.resolve((level-1)+"/"+(x*2+dx)+"_"+(z*2+dz)+".png");if(Files.exists(child)){var image=ImageIO.read(child.toFile());g.drawImage(image,dx*256,dz*256,256,256,null);image.flush();}}
                            g.dispose();ImageIO.write(out,"png",folder.resolve(x+"_"+z+".png").toFile());out.flush();
                        }
                    }
                    Files.writeString(root.resolve("pack.mcmeta"),"{\"pack\":{\"pack_format\":15,\"description\":\"ICEcore map atlas\"}}",StandardCharsets.UTF_8);
                    Files.writeString(root.resolve("manifest.json"),MapDefinition.JSON.toJson(definition),StandardCharsets.UTF_8);
                    Path packDir=Minecraft.getInstance().gameDirectory.toPath().resolve("resourcepacks");Files.createDirectories(packDir);String name="ICEcore-"+definition.atlas+".zip";
                    Path staging=root.resolve("pack.zip");try(var zip=new ZipOutputStream(Files.newOutputStream(staging));var files=Files.walk(root.resolve("assets"))){
                        for(Path f:files.filter(Files::isRegularFile).toList()){if(cancelled)return;zip.putNextEntry(new ZipEntry(root.relativize(f).toString().replace('\\','/')));Files.copy(f,zip);zip.closeEntry();}
                        zip.putNextEntry(new ZipEntry("pack.mcmeta"));Files.copy(root.resolve("pack.mcmeta"),zip);zip.closeEntry();
                    }
                    if(cancelled)return;Files.move(staging,packDir.resolve(name),StandardCopyOption.REPLACE_EXISTING);
                    Minecraft.getInstance().execute(()->{
                        if(job!=this||cancelled)return;var mc=Minecraft.getInstance();var repository=mc.getResourcePackRepository();repository.reload();var previous=new ArrayList<>(repository.getSelectedIds());var selected=new ArrayList<>(previous);
                        // Keep the currently published atlas available until the server commits
                        // the new definition. At most the current and the candidate pack remain.
                        String active="file/ICEcore-"+MapClient.definition.atlas+".zip";
                        selected.removeIf(s->s.startsWith("file/ICEcore-atlas_")&&!s.equals(active));selected.add("file/"+name);repository.setSelected(selected);
                        if(!repository.getSelectedIds().contains("file/"+name)){repository.setSelected(previous);MapClient.feedback("pack_failed");job=null;return;}
                        mc.reloadResourcePacks().whenComplete((v,error)->mc.execute(()->{
                            if(job!=this||cancelled)return;
                            var probe=new net.minecraft.resources.ResourceLocation("icecore","world_map/"+definition.atlas+"/0/"+firstX+"_"+firstZ+".png");
                            if(error==null&&MapClient.revision==revision&&mc.getResourceManager().getResource(probe).isPresent()){mc.options.updateResourcePacks(repository);if(mc.screen instanceof IslandMapScreen editor)editor.submittedSnapshot(definition);MapClient.request("save","",MapDefinition.JSON.toJson(definition));MapClient.feedback("snapshot_ready");}else{repository.setSelected(previous);mc.reloadResourcePacks();MapClient.feedback("pack_failed");}job=null;
                        }));
                    });
                }catch(Exception ex){Minecraft.getInstance().execute(()->{if(job==this){LogUtils.getLogger().error("Map pyramid failed",ex);MapClient.feedback("failed");cancel();}});}
            });
        }
    }
}
