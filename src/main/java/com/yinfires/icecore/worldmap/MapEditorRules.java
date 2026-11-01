package com.yinfires.icecore.worldmap;

import java.util.List;

/** Validates a completed draft before its caller switches the published editor draft. */
public final class MapEditorRules {
    public static final List<String> ACTIONS=List.of("select","new","bounds_apply","save","draw","finish","undo","remove_contour","source","bind","cancel_draw","delete","snapshot","cancel_snapshot","edit_contour","close","tools","snap","link","trace","reverse","accept_edge","preview","back_tools","point_mode");
    private MapEditorRules(){}
    public static boolean enabled(String action,MapDefinition draft,MapDefinition.Region region,String newId,boolean drawing,int points,boolean exporting,boolean sources,boolean binding,boolean savedRegion){
        boolean idle=!drawing&&!exporting;
        return switch(action){
            case "select" -> idle&&!draft.regions.isEmpty();
            case "new" -> idle&&newId.matches("[a-z0-9_-]{1,48}")&&draft.region(newId)==null&&draft.regions.size()<128;
            case "draw" -> idle&&region!=null;
            case "finish" -> drawing&&points>=3;
            case "undo" -> drawing&&points>0;
            case "cancel_draw" -> drawing;
            case "edit_contour" -> idle&&region!=null&&!region.polygons.isEmpty();
            case "remove_contour" -> idle&&region!=null&&(!region.polygons.isEmpty()||region.runs.length>0);
            case "delete" -> idle&&region!=null;
            case "source" -> idle&&sources;
            case "bind" -> idle&&region!=null&&binding&&savedRegion;
            case "cancel_snapshot" -> exporting;
            case "save","snapshot","bounds_apply" -> idle;
            case "close" -> true;
            case "tools","back_tools","snap","link" -> !exporting;
            case "trace","point_mode" -> drawing&&!exporting;
            case "reverse","accept_edge" -> drawing&&!exporting;
            case "preview" -> idle;
            default -> throw new IllegalArgumentException("unknown editor action");
        };
    }
    public static void finish(MapDefinition next,MapDefinition.Region region,List<MapDefinition.Point> points,int replace){
        MapPolygonEditor.validate(points);
        if(points.size()<3||points.size()>512)throw new IllegalArgumentException("polygon vertex count");
        var a=points.get(0);double area=0;
        for(int i=1;i+1<points.size();i++){var b=points.get(i);var c=points.get(i+1);area+=(b.x()-a.x())*(c.z()-a.z())-(b.z()-a.z())*(c.x()-a.x());}
        if(Math.abs(area)<0.000001)throw new IllegalArgumentException("empty polygon");
        int pad=0;
        for(var p:points){
            if(!Double.isFinite(p.x())||!Double.isFinite(p.z())||Math.abs(p.x())>30000000-pad||Math.abs(p.z())>30000000-pad)throw new IllegalArgumentException("world boundary");
            next.minX=Math.min(next.minX,(int)Math.floor(p.x()-pad));next.minZ=Math.min(next.minZ,(int)Math.floor(p.z()-pad));
            next.maxX=Math.max(next.maxX,(int)Math.ceil(p.x()+pad));next.maxZ=Math.max(next.maxZ,(int)Math.ceil(p.z()+pad));
        }
        if(replace>=0)region.polygons.set(replace,List.copyOf(points));else region.polygons.add(List.copyOf(points));
        next.validate();
    }
}
