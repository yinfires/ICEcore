package com.yinfires.icecore.worldmap;

/** Frame-time camera displacement, bounded after pauses and normalized diagonally. */
public final class MapKeyboardPan {
    private MapKeyboardPan(){}
    public static double pixels(int axis,int otherAxis,double seconds){
        if(axis==0||!Double.isFinite(seconds))return 0;
        return axis*240*Math.max(0,Math.min(.05,seconds))/(otherAxis==0?1:Math.sqrt(2));
    }
}
