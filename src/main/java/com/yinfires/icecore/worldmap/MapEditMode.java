package com.yinfires.icecore.worldmap;

/** Explicit editing tools: selecting an edge never accepts or renders a yellow vertex. */
public enum MapEditMode {
    POINTS, SHARED_EDGE;
    public boolean showsVertices(){return this==POINTS;}
    public boolean selectsEdges(){return this==SHARED_EDGE;}
    public MapEditMode afterAcceptingEdge(){return this;}
}
