package com.yinfires.icecore.worldmap;

/** Pure coordinate/zoom policy shared by screen and tests. */
public final class MapViewport {
    public double x,z,scale=1;
    public double[] bounds;
    public int width,height;
    private double centerX=Double.NaN,centerZ=Double.NaN;
    public void nativeCamera(int physicalWidth,int physicalHeight,double guiScale,double physicalScale,double cameraX,double cameraZ){
        x=cameraX;z=cameraZ;scale=Math.max(0.000001,physicalScale/guiScale);bounds=null;
        centerX=(physicalWidth/2)/guiScale;centerZ=(physicalHeight/2)/guiScale;
    }
    public void resize(int w,int h,double[] b,boolean reset){
        width=w;height=h;bounds=b;centerX=centerZ=Double.NaN;
        if(b==null)return;
        if(reset){x=(b[0]+b[2])/2;z=(b[1]+b[3])/2;scale=minimum();}
        clamp();
    }
    public double minimum(){return bounds==null?1:Math.min(width*.94/Math.max(1,bounds[2]-bounds[0]),height*.94/Math.max(1,bounds[3]-bounds[1]));}
    private double centerX(){return Double.isNaN(centerX)?width/2.0:centerX;}
    private double centerZ(){return Double.isNaN(centerZ)?height/2.0:centerZ;}
    public double worldX(double screen){return x+(screen-centerX())/scale;}
    public double worldZ(double screen){return z+(screen-centerZ())/scale;}
    public double screenX(double world){return centerX()+(world-x)*scale;}
    public double screenZ(double world){return centerZ()+(world-z)*scale;}
    public void zoom(double amount,double mouseX,double mouseY){
        if(bounds==null)return;
        double px=worldX(mouseX),pz=worldZ(mouseY);
        scale=Math.max(minimum(),Math.min(Math.max(8,minimum()),scale*Math.pow(1.2,amount)));
        x=px-(mouseX-width/2.0)/scale;z=pz-(mouseY-height/2.0)/scale;clamp();
    }
    public void drag(double dx,double dz){if(bounds!=null){x-=dx/scale;z-=dz/scale;clamp();}}
    public void clamp(){if(bounds!=null){scale=Math.max(minimum(),scale);x=axis(x,bounds[0],bounds[2],width/scale);z=axis(z,bounds[1],bounds[3],height/scale);}}
    private static double axis(double v,double min,double max,double size){return size>=max-min?(min+max)/2:Math.max(min+size/2,Math.min(max-size/2,v));}
}
