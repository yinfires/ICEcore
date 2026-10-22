package com.yinfires.icecore.compat.cozycafe;

/** Physical serving state determines layout independently of the customer's next course. */
public final class CafeDisplayRules {
    private CafeDisplayRules() {}
    public static int course(boolean plated,boolean drink,int course) {
        return plated ? 2 : drink ? 1 : course;
    }
    public static boolean hideMenu(boolean drink,boolean pending,boolean eating,boolean plated) {
        return !drink && (pending || eating && !plated);
    }
    public static float plateScale(double width,double depth) {
        return (float)Math.min(.9,.75/Math.max(.0001,Math.max(width,depth)));
    }
    public static double plateOffset(double nativeHeight,double plateTop,double minimumY,double scale) {
        return plateTop+.001-nativeHeight-minimumY*scale;
    }
}
