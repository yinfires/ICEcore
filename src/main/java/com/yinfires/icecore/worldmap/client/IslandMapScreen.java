package com.yinfires.icecore.worldmap.client;

import com.yinfires.icecore.worldmap.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import org.lwjgl.glfw.GLFW;
import java.util.*;

public final class IslandMapScreen extends Screen {
    private static final List<String> SAVE_ACTIONS=List.of("save","bind","snapshot"),EDGE_ACTIONS=List.of("reverse","accept_edge");
    private final boolean editor;
    private final MapViewport view=new MapViewport();
    private MapDefinition draft;
    private MapAtlas atlas;
    private MapAtlas nextAtlas;
    private Set<String> shownUnlocks=MapClient.unlocked;
    private final MapRevealTransition reveal=new MapRevealTransition();
    private boolean viewInitialized;
    private int selected;
    private boolean drawing;
    private int editingContour=-1;
    private XaeroEditorMap nativeMap;
    private final Map<String,Button> actions=new LinkedHashMap<>();
    private boolean suspended;
    private final MapEditorForm form;
    private final List<MapDefinition.Point> points=new ArrayList<>();
    private EditBox id,expansion,feather,bounds;
    private final List<EditBox> inputs=new ArrayList<>(4);
    private String binding="";
    private long lastPanFrame;
    private boolean tools,snapping=true,linked=true,reverse;
    private MapEditMode mode=MapEditMode.POINTS;
    private MapDefinition working;
    private MapPolygonEditor.Hit edgeStart,edgeEnd,edgeHover;
    private List<MapDefinition.Point> adoptedEdge=List.of(),edgeUndoPoints=List.of(),edgeUndoPath=List.of();
    private MapDefinition edgeUndo;
    private List<MapDefinition.Point> edgePreview=List.of();
    private MapDefinition.Point vertexOrigin;
    private int hoverX=Integer.MIN_VALUE,hoverY;
    private List<String> hovered=List.of();
    private List<Component> hoverLabels=List.of();
    private Map<String,MapFootprint> footprints=Map.of();
    private java.util.concurrent.Future<?> footprintTask;
    private long footprintGeneration;
    private int footprintHash;
    public IslandMapScreen(boolean editor){super(Component.translatable("icecore.map.title"));this.editor=editor;draft=MapClient.definition.copy();form=new MapEditorForm(draft,selectedId(),MapClient.revision);}
    boolean isEditor(){return editor;}
    public void refresh(){
        if(editor)return; // Server/resource refresh must not discard the editor's unsaved work.
        if(nextAtlas!=null){nextAtlas.close();nextAtlas=null;}reveal.reset();
        draft=MapClient.definition.copy();shownUnlocks=MapClient.unlocked;if(atlas!=null)atlas.close();atlas=null;init();
    }
    void dimensionChanged(){if(editor)minecraft.setScreen(null);else refresh();}
    void serverChanged(String previous){
        if(!editor){
            boolean unlockOnly=previous.equals(MapDefinition.JSON.toJson(MapClient.definition))&&MapClient.unlocked.containsAll(shownUnlocks);
            if(unlockOnly){if(nextAtlas!=null)nextAtlas.close();atlas.capacity(24);nextAtlas=new MapAtlas(MapClient.definition.copy(),MapClient.unlocked);nextAtlas.capacity(24);reveal.reset();shownUnlocks=MapClient.unlocked;}
            else refresh();
            view.resize(width,height,MapClient.definition.bounds(MapClient.unlocked,false),!viewInitialized||view.bounds==null);viewInitialized=true;
            return;
        }
        updateHoverLabels();
        if(form.accept(MapClient.definition,MapClient.revision,draft,selectedId(),drawing)){
            draft=MapClient.definition.copy();loadFields();init();
        }
    }
    private String selectedId(){return region()==null?"":region().id;}
    private void loadFields(){form.load(draft,selectedId());}
    void submittedSnapshot(MapDefinition definition){form.submitted(definition);}
    @Override protected void init(){
        clearWidgets();actions.clear();inputs.clear();if(atlas!=null)atlas.close();
        if(nextAtlas!=null){nextAtlas.close();nextAtlas=null;}reveal.reset();
        if(editor){if(nativeMap==null)nativeMap=new XaeroEditorMap();nativeMap.resize(width,height);}
        else {atlas=new MapAtlas(draft,MapClient.unlocked);view.resize(width,height,draft.bounds(MapClient.unlocked,false),!viewInitialized);viewInitialized=true;}
        if(!editor)return;
        int y=height-124,cell=width/4;
        id=field(4,y,cell-8,form.id,"region_id");
        expansion=field(cell+4,y,cell/2-8,form.expansion,"expansion");
        feather=field(cell+cell/2+4,y,cell/2-8,form.feather,"feather");
        bounds=field(2*cell+4,y,2*cell-8,form.bounds,"bounds");
        String[] names=tools?new String[]{"snap","link","trace","reverse","accept_edge","edit_contour","preview","back_tools","point_mode","finish","undo","cancel_draw","close"}:
            new String[]{"select","new","bounds_apply","save","draw","finish","undo","remove_contour","source","bind","cancel_draw","delete","snapshot","cancel_snapshot","tools","close"};
        for(int i=0;i<names.length;i++){
            String action=names[i];int row=i/4,col=i%4;
            var button=Button.builder(action.equals("trace")||action.equals("point_mode")?Component.translatable("icecore.map.mode_button",label(action),Component.translatable((action.equals("trace")==mode.selectsEdges())?"icecore.map.mode_selected":"icecore.map.mode_switch")):label(action),b->act(action)).bounds(col*cell+4,y+23+row*23,cell-8,20).build();
            if(action.equals("source"))button.setMessage(binding.isEmpty()?label(action):Component.literal(binding));
            if(action.equals("select")&&region()!=null)button.setMessage(Component.literal(region().id));
            if(action.equals("snap")||action.equals("link"))button.setMessage(Component.translatable("icecore.map.toggle",label(action),Component.translatable((action.equals("snap")?snapping:linked)?"options.on":"options.off")));
            button.setTooltip(Tooltip.create(label("help."+action)));
            actions.put(action,button);
            addRenderableWidget(button);
        }
        updateActions();
        hoverX=Integer.MIN_VALUE;
        buildFootprints();
    }
    private void buildFootprints(){
        int hash=1;for(var r:draft.regions)hash=31*hash+r.id.hashCode()+Arrays.hashCode(r.runs);
        if(hash==footprintHash)return;footprintHash=hash;long token=++footprintGeneration;
        if(footprintTask!=null)footprintTask.cancel(true);
        var source=new HashMap<String,int[]>();for(var r:draft.regions)if(r.runs.length>0)source.put(r.id,r.runs.clone());
        footprintTask=MapClient.WORK.submit(()->{var result=new HashMap<String,MapFootprint>();for(var entry:source.entrySet()){if(Thread.currentThread().isInterrupted())return;result.put(entry.getKey(),new MapFootprint(entry.getValue()));}net.minecraft.client.Minecraft.getInstance().execute(()->{if(token==footprintGeneration)footprints=Map.copyOf(result);});});
    }
    private Component label(String key){return Component.translatable("icecore.map."+key);}
    private EditBox field(int x,int y,int w,String value,String name){
        var box=new EditBox(font,x,y,w,20,label(name));box.setMaxLength(160);box.setValue(value);
        box.setTooltip(Tooltip.create(label("help."+name)));
        box.setResponder(text->{switch(name){case "region_id"->form.id=text;case "expansion"->form.expansion=text;case "feather"->form.feather=text;case "bounds"->form.bounds=text;default->throw new IllegalArgumentException(name);}});
        inputs.add(box);addWidget(box);return box;
    }
    private MapDefinition.Region region(){return draft.regions.isEmpty()?null:draft.regions.get(Math.floorMod(selected,draft.regions.size()));}
    private void applyFields(){draft=form.apply(draft,selectedId(),minecraft.level.dimension().location().toString());}
    private void choose(List<String> values,java.util.function.Consumer<String> callback){suspended=true;minecraft.setScreen(new MapChoiceScreen(this,values,callback));}
    private void updateActions(){
        var r=region();
        for(var entry:actions.entrySet())entry.getValue().active=MapEditorRules.enabled(entry.getKey(),draft,r,id.getValue(),drawing,points.size(),XaeroSnapshot.busy(),!MapClient.sources.isEmpty(),!binding.isEmpty(),r!=null&&MapClient.definition.region(r.id)!=null);
        if(mode.selectsEdges()&&actions.containsKey("undo"))actions.get("undo").active=drawing&&(edgeStart!=null||edgeUndo!=null||!points.isEmpty());
        if(form.awaitingSave())for(String action:SAVE_ACTIONS)if(actions.containsKey(action))actions.get(action).active=false;
        for(String action:EDGE_ACTIONS)if(actions.containsKey(action))actions.get(action).active=actions.get(action).active&&edgeStart!=null&&edgeEnd!=null;
    }
    private void clearEdgeUndo(){edgeUndo=null;edgeUndoPoints=edgeUndoPath=List.of();}
    private void startDrawing(){clearEdgeUndo();edgeHover=null;adoptedEdge=List.of();dragVertex=-1;vertexOrigin=null;working=draft.copy();drawing=true;mode=MapEditMode.POINTS;edgeStart=edgeEnd=edgeHover=null;edgePreview=List.of();adoptedEdge=List.of();}
    private void act(String action){
        try{
            switch(action){
                case "select" -> {applyFields();choose(draft.regions.stream().map(r->r.id).toList(),value->{selected=0;for(int i=0;i<draft.regions.size();i++)if(draft.regions.get(i).id.equals(value))selected=i;points.clear();drawing=false;loadFields();});}
                case "new" -> {applyFields();var next=draft.copy();var r=new MapDefinition.Region();r.id=form.id;r.expansion=Integer.parseInt(form.expansion.trim());r.feather=Integer.parseInt(form.feather.trim());next.regions.add(r);next.validate();draft=next;selected=draft.regions.size()-1;startDrawing();editingContour=-1;points.clear();loadFields();init();}
                case "bounds_apply" -> {applyFields();loadFields();init();}
                case "save" -> {applyFields();form.submitted(draft);MapNetwork.request(new MapNetwork.Request("save","",MapDefinition.JSON.toJson(draft),form.revision()));}
                case "draw" -> {applyFields();startDrawing();editingContour=-1;points.clear();}
                case "finish" -> {applyFields();var next=form.apply(working,selectedId(),draft.dimension);var r=next.region(selectedId());MapEditorRules.finish(next,r,points,editingContour);MapPolygonEditor.validateChanged(draft,next);draft=next;working=null;points.clear();drawing=false;adoptedEdge=List.of();clearEdgeUndo();editingContour=-1;edgePreview=List.of();loadFields();init();}
                case "edit_contour" -> {applyFields();choose(java.util.stream.IntStream.range(0,region().polygons.size()).mapToObj(i->Integer.toString(i+1)).toList(),value->{editingContour=Integer.parseInt(value)-1;points.clear();points.addAll(region().polygons.get(editingContour));startDrawing();});}
                case "cancel_draw" -> {clearEdgeUndo();dragVertex=-1;vertexOrigin=null;drawing=false;working=null;mode=MapEditMode.POINTS;editingContour=-1;points.clear();edgeStart=edgeEnd=edgeHover=null;edgePreview=List.of();adoptedEdge=List.of();}
                case "undo" -> {if(mode.selectsEdges()&&edgeEnd!=null){edgeEnd=null;edgePreview=List.of();}else if(mode.selectsEdges()&&edgeStart!=null){edgeStart=edgeHover=null;}else if(mode.selectsEdges()&&edgeUndo!=null){working=edgeUndo;points.clear();points.addAll(edgeUndoPoints);adoptedEdge=edgeUndoPath;clearEdgeUndo();}else if(!points.isEmpty())points.remove(points.size()-1);}
                case "remove_contour" -> {applyFields();if(region()!=null){if(!region().polygons.isEmpty())region().polygons.remove(region().polygons.size()-1);else region().runs=new int[0];loadFields();init();}}
                case "source" -> {applyFields();choose(MapClient.sources,value->binding=value);}
                case "bind" -> {applyFields();if(!MapDefinition.JSON.toJson(draft).equals(MapDefinition.JSON.toJson(MapClient.definition))){MapClient.feedback("save_before_bind");return;}MapClient.request("bind",region().id,binding);}
                case "delete" -> {applyFields();if(region()!=null)draft.regions.remove(region());loadFields();init();}
                case "snapshot" -> {applyFields();if(form.revision()!=MapClient.revision)throw new IllegalArgumentException("stale editor");XaeroSnapshot.begin(draft.copy());}
                case "cancel_snapshot" -> XaeroSnapshot.cancel();
                case "close" -> onClose();
                case "tools" -> {tools=true;init();}
                case "back_tools" -> {tools=false;init();}
                case "snap" -> {snapping=!snapping;init();}
                case "link" -> {linked=!linked;init();}
                case "trace" -> {mode=MapEditMode.SHARED_EDGE;dragVertex=-1;vertexOrigin=null;edgeStart=edgeEnd=edgeHover=null;edgePreview=List.of();init();}
                case "point_mode" -> {clearEdgeUndo();mode=MapEditMode.POINTS;dragVertex=-1;vertexOrigin=null;edgeStart=edgeEnd=edgeHover=null;edgePreview=List.of();init();}
                case "reverse" -> {reverse=!reverse;edgePreview=MapPolygonEditor.boundary(working,edgeStart,edgeEnd,reverse);}
                case "accept_edge" -> {
                    if(points.size()+edgePreview.size()>512)throw new IllegalArgumentException("too many vertices");
                    var candidate=working.copy();MapPolygonEditor.insert(candidate,edgeStart);MapPolygonEditor.insert(candidate,edgeEnd);MapPolygonEditor.validateChanged(working,candidate);
                    edgeUndo=working;edgeUndoPoints=List.copyOf(points);edgeUndoPath=adoptedEdge;working=candidate;
                    for(var p:edgePreview)if(points.isEmpty()||!points.get(points.size()-1).equals(p))points.add(p);
                    mode=mode.afterAcceptingEdge();adoptedEdge=edgePreview;edgeStart=edgeEnd=edgeHover=null;edgePreview=List.of();
                }
                case "preview" -> {applyFields();suspended=true;minecraft.setScreen(new MapUnlockPreviewScreen(this,draft,view));}
                default -> throw new IllegalArgumentException();
            }
        }catch(Exception ex){MapClient.feedback("invalid");}
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        panKeyboard();
        if(editor){nativeMap.render(g,mx,my,partial,view);updateActions();}
        else g.fill(0,0,width,height,0xff808080);
        g.enableScissor(0,0,width,mapHeight());
        boolean dimension=minecraft.level!=null&&minecraft.level.dimension().location().toString().equals(draft.dimension);
        if(!editor&&dimension&&view.bounds!=null&&atlas!=null){
            atlas.draw(g,view);
            if(nextAtlas!=null){
                nextAtlas.prepare(view);
                float blend=(float)reveal.opacity(nextAtlas.ready(),System.nanoTime());
                if(nextAtlas.ready()){
                    g.flush();com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,blend);
                    try{nextAtlas.draw(g,view);g.flush();}finally{com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,1);com.mojang.blaze3d.systems.RenderSystem.disableBlend();}
                    if(blend>=1){atlas.close();atlas=nextAtlas;nextAtlas=null;atlas.capacity(48);reveal.reset();}
                }
            }
        }
        if(editor){
            var display=working==null?draft:working;
            for(var r:display.regions)for(int i=0;i<r.polygons.size();i++)if(!r.id.equals(selectedId())||!drawing||i!=editingContour)outline(g,r.polygons.get(i),true,r.id.equals(selectedId())?0xffffffff:MapClient.unlocked.contains(r.id)?0xff65c78b:0xff9999bb);
            int tx0=Math.max(Math.floorDiv(draft.minX,512),(int)Math.floor(view.worldX(0)/512)),tz0=Math.max(Math.floorDiv(draft.minZ,512),(int)Math.floor(view.worldZ(0)/512));
            int tx1=Math.min(Math.floorDiv(draft.maxX,512),(int)Math.floor(view.worldX(width)/512)),tz1=Math.min(Math.floorDiv(draft.maxZ,512),(int)Math.floor(view.worldZ(mapHeight())/512));
            for(var entry:footprints.entrySet())for(int tz=tz0;tz<=tz1;tz++)for(int tx=tx0;tx<=tx1;tx++)for(var line:entry.getValue().tile(tx,tz))segment(g,view.screenX(line.x()),view.screenZ(line.z()),view.screenX(line.endX()),view.screenZ(line.endZ()),entry.getKey().equals(selectedId())?0xffffffff:MapClient.unlocked.contains(entry.getKey())?0xff65c78b:0xff9999bb);
            outline(g,points,false,0xffffffff);
            outline(g,adoptedEdge,false,0xff55ffff);
            outline(g,edgePreview,false,0xffffaa44);
            if(drawing&&mode.selectsEdges()){
                if(edgeHover!=null){var polygon=working.region(edgeHover.region()).polygons.get(edgeHover.polygon());outline(g,List.of(polygon.get(edgeHover.edge()),polygon.get((edgeHover.edge()+1)%polygon.size())),false,0xff55ffff);edgeMarker(g,edgeHover.point(),0xff55ffff);}
                if(edgeStart!=null)edgeMarker(g,edgeStart.point(),0xffffffff);if(edgeEnd!=null)edgeMarker(g,edgeEnd.point(),0xffffaa44);
            }
            if(drawing&&mode.showsVertices()){MapEditorSketch.vertices(g,view,points,mode);if(my<mapHeight()&&!points.isEmpty())outline(g,List.of(points.get(points.size()-1),new MapDefinition.Point(view.worldX(mx),view.worldZ(my))),false,0xffffff00);}
        }
        if(editor&&XaeroSnapshot.missingChunk!=null){int[] c=XaeroSnapshot.missingChunk;int x=(int)view.screenX(c[0]*16.0),z=(int)view.screenZ(c[1]*16.0);g.fill(x,z,(int)Math.ceil(view.screenX(c[0]*16.0+16)),(int)Math.ceil(view.screenZ(c[1]*16.0+16)),0xaaff3333);g.drawString(font,Component.translatable("icecore.map.missing_chunk",c[0],c[1]),4,16,0xffffffff);}
        if(dimension&&minecraft.player!=null){
            int x=(int)Math.round(view.screenX(minecraft.player.getX()))-8,z=(int)Math.round(view.screenZ(minecraft.player.getZ()))-8;
            if(x<width&&z<mapHeight()&&x+16>=0&&z+16>=0){g.fill(x-1,z-1,x+17,z+17,0xffffffff);PlayerFaceRenderer.draw(g,minecraft.player.getSkinTextureLocation(),x,z,16,true,false);}
        }
        g.disableScissor();
        if(editor)g.fill(0,height-128,width,height,0xe0202020);
        if(editor)MapEditorWidgets.render(g,inputs,mx,my,partial);
        super.render(g,mx,my,partial);
        if(editor&&my<mapHeight()){
            if(!drawing&&!hovered.isEmpty())g.renderComponentTooltip(font,hoverLabels,mx,my);
            if(drawing)g.drawString(font,label(mode.showsVertices()?"point_status":edgeStart==null?"trace_start":edgeEnd==null?"trace_end":"trace_confirm"),4,28,0xff55ffff);
        }
        if(editor&&XaeroSnapshot.busy())g.drawString(font,Component.translatable("icecore.map.generating",XaeroSnapshot.progress()),4,4,0xffffffff);
    }
    private void edgeMarker(GuiGraphics g,MapDefinition.Point p,int color){int x=(int)view.screenX(p.x()),z=(int)view.screenZ(p.z());g.fill(x-4,z-4,x+5,z-3,color);g.fill(x-4,z+4,x+5,z+5,color);g.fill(x-4,z-3,x-3,z+4,color);g.fill(x+4,z-3,x+5,z+4,color);}
    private void outline(GuiGraphics g,List<MapDefinition.Point> p,boolean closed,int color){
        if(p.size()<2)return;
        for(int i=1;i<p.size()+(closed?1:0);i++){
            var a=p.get(i-1);var b=p.get(i%p.size());double ax=view.screenX(a.x()),az=view.screenZ(a.z()),bx=view.screenX(b.x()),bz=view.screenZ(b.z());
            segment(g,ax,az,bx,bz,color);
        }
    }
    private void segment(GuiGraphics g,double ax,double az,double bx,double bz,int color){
            if(ax<0&&bx<0||ax>width&&bx>width||az<0&&bz<0||az>mapHeight()&&bz>mapHeight())return;
            if(Math.abs(bx-ax)<.5&&Math.abs(bz-az)<.5)return;
            // Clip the segment before rasterizing so high zoom never produces unbounded work.
            double[] t={0,1};if(!clip(- (bx-ax),ax,t)||!clip(bx-ax,width-ax,t)||!clip(-(bz-az),az,t)||!clip(bz-az,mapHeight()-az,t))return;
            double x=ax+(bx-ax)*t[0],z=az+(bz-az)*t[0],dx=(bx-ax)*(t[1]-t[0]),dz=(bz-az)*(t[1]-t[0]);
            g.pose().pushPose();g.pose().translate(x,z,0);g.pose().mulPose(com.mojang.math.Axis.ZP.rotation((float)Math.atan2(dz,dx)));
            g.fill(0,0,Math.max(1,(int)Math.ceil(Math.hypot(dx,dz))),1,color);g.pose().popPose();
    }
    private boolean clip(double p,double q,double[] t){if(p==0)return q>=0;double r=q/p;if(p<0){if(r>t[1])return false;t[0]=Math.max(t[0],r);}else{if(r<t[0])return false;t[1]=Math.min(t[1],r);}return true;}
    private int mapHeight(){return editor?Math.max(1,height-128):height;}
    private void panKeyboard(){
        long now=System.nanoTime(),previous=lastPanFrame;lastPanFrame=now;
        if(previous==0||getFocused() instanceof EditBox||!minecraft.isWindowActive())return;
        long window=minecraft.getWindow().getWindow();
        int x=(com.mojang.blaze3d.platform.InputConstants.isKeyDown(window,GLFW.GLFW_KEY_RIGHT)?1:0)-(com.mojang.blaze3d.platform.InputConstants.isKeyDown(window,GLFW.GLFW_KEY_LEFT)?1:0);
        int z=(com.mojang.blaze3d.platform.InputConstants.isKeyDown(window,GLFW.GLFW_KEY_DOWN)?1:0)-(com.mojang.blaze3d.platform.InputConstants.isKeyDown(window,GLFW.GLFW_KEY_UP)?1:0);
        if(x!=0||z!=0){double seconds=(now-previous)/1_000_000_000.0,dx=-MapKeyboardPan.pixels(x,z,seconds),dz=-MapKeyboardPan.pixels(z,x,seconds);if(editor)nativeMap.drag(dx,dz,view);else view.drag(dx,dz);hovered=List.of();edgeHover=null;hoverX=Integer.MIN_VALUE;}
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        if(super.mouseClicked(x,y,button))return true;
        if(y>=mapHeight())return false;
        setFocused(null);
        if(id!=null){id.setFocused(false);expansion.setFocused(false);feather.setFocused(false);bounds.setFocused(false);}
        if(editor&&drawing&&button==0){
            if(mode.selectsEdges()){
                var choices=MapPolygonEditor.edgeChoices(working,selectedId(),view.worldX(x),view.worldZ(y),6/view.scale,1/view.scale,edgeEnd==null?edgeStart:null);
                if(choices.size()==1)pickEdge(choices.get(0));
                else if(choices.size()>1){var labels=choices.stream().map(hit->Component.translatable("icecore.map.edge_source",hit.region(),hit.polygon()+1).getString()).toList();choose(labels,value->pickEdge(choices.get(labels.indexOf(value))));}
                return true;
            }
            int nearest=-1;double distance=36;
            for(int i=0;i<points.size();i++){
                double dx=view.screenX(points.get(i).x())-x,dz=view.screenZ(points.get(i).z())-y;
                if(dx*dx+dz*dz<distance){distance=dx*dx+dz*dz;nearest=i;}
            }
            if(nearest>=0){dragVertex=nearest;vertexOrigin=points.get(nearest);}
            else if(points.size()<512){var hit=snap(x,y);points.add(hit==null?new MapDefinition.Point(view.worldX(x),view.worldZ(y)):hit.point());if(hit!=null)MapPolygonEditor.insert(working,hit);}
        }else if(editor&&!drawing&&button==0){
            updateHover(x,y);
            if(hovered.size()==1)selectMapRegion(hovered.get(0));
            else if(!hovered.isEmpty())choose(hovered,this::selectMapRegion);
        }
        return true;
    }
    private void pickEdge(MapPolygonEditor.Hit hit){
        if(edgeStart==null||edgeEnd!=null){edgeStart=hit;edgeEnd=null;edgePreview=List.of();reverse=false;}
        else if(edgeStart.region().equals(hit.region())&&edgeStart.polygon()==hit.polygon()&&!edgeStart.point().equals(hit.point())){edgeEnd=hit;edgePreview=MapPolygonEditor.boundary(working,edgeStart,edgeEnd,reverse);}
        edgeHover=hit;
    }
    private int dragVertex=-1;
    @Override public boolean mouseReleased(double x,double y,int button){if(dragVertex>=0&&vertexOrigin!=null&&working!=null){var hit=snap(x,y);if(hit!=null)points.set(dragVertex,hit.point());MapPolygonEditor.placeVertex(working,vertexOrigin,points.get(dragVertex),hit,linked);}dragVertex=-1;vertexOrigin=null;return super.mouseReleased(x,y,button);}
    private MapPolygonEditor.Hit snap(double x,double y){return snapping&&!hasAltDown()?MapPolygonEditor.snap(working,selectedId(),view.worldX(x),view.worldZ(y),6/view.scale):null;}
    private void selectMapRegion(String value){try{applyFields();for(int i=0;i<draft.regions.size();i++)if(draft.regions.get(i).id.equals(value))selected=i;loadFields();init();}catch(IllegalArgumentException ex){MapClient.feedback("invalid");}}
    private void updateHover(double x,double y){double wx=view.worldX(x),wz=view.worldZ(y);hovered=draft.regions.stream().filter(r->r.polygons.stream().anyMatch(p->MapDefinition.inside(p,wx,wz))||footprints.containsKey(r.id)&&footprints.get(r.id).contains(wx,wz)).map(r->r.id).toList();updateHoverLabels();}
    private void updateHoverLabels(){hoverLabels=hovered.stream().map(value->(Component)Component.translatable("icecore.map.region_state",value,Component.translatable(MapClient.unlocked.contains(value)?"icecore.map.unlocked":"icecore.map.locked"))).toList();}
    @Override public void mouseMoved(double x,double y){if(editor&&drawing&&mode.selectsEdges()&&y<mapHeight())edgeHover=MapPolygonEditor.edge(working,selectedId(),view.worldX(x),view.worldZ(y),6/view.scale,edgeEnd==null?edgeStart:null);else edgeHover=null;if(editor&&!drawing&&y<mapHeight()&&((int)x!=hoverX||(int)y!=hoverY)){hoverX=(int)x;hoverY=(int)y;updateHover(x,y);}super.mouseMoved(x,y);}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(y<mapHeight()){if(editor&&drawing&&button==0&&dragVertex>=0){var hit=snap(x,y);points.set(dragVertex,hit==null?new MapDefinition.Point(view.worldX(x),view.worldZ(y)):hit.point());return true;}if(!drawing||button==1){if(editor)nativeMap.drag(dx,dy,view);else view.drag(dx,dy);hovered=List.of();edgeHover=null;return true;}}return super.mouseDragged(x,y,button,dx,dy);}
    @Override public boolean mouseScrolled(double x,double y,double delta){if(y<mapHeight()){if(editor)nativeMap.zoom(x,y,delta);else view.zoom(delta,x,y);hovered=List.of();edgeHover=null;hoverX=Integer.MIN_VALUE;return true;}return super.mouseScrolled(x,y,delta);}
    @Override public boolean keyPressed(int key,int scan,int modifiers){if(!(getFocused() instanceof EditBox)){if(MapClient.OPEN.matches(key,scan)){onClose();return true;}if(key==GLFW.GLFW_KEY_LEFT||key==GLFW.GLFW_KEY_RIGHT||key==GLFW.GLFW_KEY_UP||key==GLFW.GLFW_KEY_DOWN)return true;}return super.keyPressed(key,scan,modifiers);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void removed(){lastPanFrame=0;if(nextAtlas!=null){nextAtlas.close();nextAtlas=null;}if(atlas!=null){atlas.close();atlas=null;}if(suspended){suspended=false;return;}footprintGeneration++;if(footprintTask!=null)footprintTask.cancel(true);footprintHash=0;if(nativeMap!=null){nativeMap.close();nativeMap=null;}}
}
