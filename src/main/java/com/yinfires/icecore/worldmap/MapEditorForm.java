package com.yinfires.icecore.worldmap;

/** Input text survives rebuilds; an acknowledgement advances revision without discarding newer edits. */
public final class MapEditorForm {
    public String id="",expansion="64",feather="64",bounds="";
    private String base,pending;
    private long pendingUntil;
    private long revision;
    public MapEditorForm(MapDefinition definition,String selected,long revision){
        this.revision=revision;base=MapDefinition.JSON.toJson(definition);load(definition,selected);
    }
    public void load(MapDefinition d,String selected){
        var r=d.region(selected);id=r==null?"":r.id;expansion=Integer.toString(r==null?64:r.expansion);feather=Integer.toString(r==null?64:r.feather);
        bounds=d.minX+","+d.minZ+","+d.maxX+","+d.maxZ;
    }
    public MapDefinition apply(MapDefinition source,String selected,String dimension){
        var next=source.copy();String[] coordinates=bounds.split(",",-1);
        if(coordinates.length!=4)throw new IllegalArgumentException("four bounds required");
        next.minX=Integer.parseInt(coordinates[0].trim());next.minZ=Integer.parseInt(coordinates[1].trim());next.maxX=Integer.parseInt(coordinates[2].trim());next.maxZ=Integer.parseInt(coordinates[3].trim());
        next.dimension=dimension;
        var r=next.region(selected);if(r!=null){r.expansion=Integer.parseInt(expansion.trim());r.feather=Integer.parseInt(feather.trim());}
        next.validate();return next;
    }
    public void submitted(MapDefinition definition){pending=MapDefinition.JSON.toJson(definition);pendingUntil=System.nanoTime()+10_000_000_000L;}
    public boolean awaitingSave(){return pending!=null&&System.nanoTime()<pendingUntil;}
    public long revision(){return revision;}
    /** Whether the incoming definition may replace the current draft. */
    public boolean accept(MapDefinition incoming,long nextRevision,MapDefinition draft,String selected,boolean drawing){
        String remote=MapDefinition.JSON.toJson(incoming),local=MapDefinition.JSON.toJson(draft);
        boolean clean=!drawing&&local.equals(base)&&matches(draft,selected);
        if(remote.equals(base)||remote.equals(pending)){
            revision=nextRevision;base=remote;if(remote.equals(pending)){draft.atlas=incoming.atlas;pending=null;}
            return false;
        }
        if(clean){base=remote;revision=nextRevision;pending=null;return true;}
        return false;
    }
    private boolean matches(MapDefinition draft,String selected){
        var expected=new MapEditorForm(draft,selected,revision);
        return id.equals(expected.id)&&expansion.equals(expected.expansion)&&feather.equals(expected.feather)&&bounds.equals(expected.bounds);
    }
}
