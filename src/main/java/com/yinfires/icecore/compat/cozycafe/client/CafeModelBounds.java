package com.yinfires.icecore.compat.cozycafe.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3f;
import java.util.*;

/** Resource-generation scoped geometry cache; no quad or pixel scans on cache hits. */
public final class CafeModelBounds {
    private static final Map<BakedModel,Map<Key,Float>> CACHE=new IdentityHashMap<>();
    private static final Map<TextureAtlasSprite,float[]> INK=new IdentityHashMap<>();
    private static final Map<BakedModel,net.minecraft.world.phys.AABB> BLOCKS=new IdentityHashMap<>();
    private record Key(ItemDisplayContext context,boolean block) {}
    private CafeModelBounds() {}
    public static void clear() { CACHE.clear(); INK.clear(); BLOCKS.clear(); }
    public static net.minecraft.world.phys.AABB blockBounds(BakedModel model) {
        if(BLOCKS.size()>=512 && !BLOCKS.containsKey(model)) clear();
        return BLOCKS.computeIfAbsent(model,m->{
            double minX=Double.POSITIVE_INFINITY,minY=minX,minZ=minX,maxX=Double.NEGATIVE_INFINITY,maxY=maxX,maxZ=maxX;
            for(int side=0;side<7;side++) for(BakedQuad quad:m.getQuads(null,side==6?null:Direction.values()[side],RandomSource.create(42),net.minecraftforge.client.model.data.ModelData.EMPTY,null)) {
                int[] data=quad.getVertices(); int stride=data.length/4;
                for(int v=0;v<4;v++) {
                    float x=f(data,v*stride),y=f(data,v*stride+1),z=f(data,v*stride+2);
                    minX=Math.min(minX,x);minY=Math.min(minY,y);minZ=Math.min(minZ,z);
                    maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);maxZ=Math.max(maxZ,z);
                }
            }
            return Double.isFinite(minX)?new net.minecraft.world.phys.AABB(minX,minY,minZ,maxX,maxY,maxZ):new net.minecraft.world.phys.AABB(0,0,0,1,1,1);
        });
    }
    public static float minimumY(BakedModel model,ItemDisplayContext context,boolean block) {
        if (CACHE.size()>=512 && !CACHE.containsKey(model)) clear();
        return CACHE.computeIfAbsent(model,k->new HashMap<>()).computeIfAbsent(new Key(context,block),k->measure(model,k));
    }
    private static float measure(BakedModel original,Key key) {
        PoseStack pose=new PoseStack();
        BakedModel model=key.block ? original : original.applyTransform(key.context,pose,false);
        if (!key.block) pose.translate(-.5,-.5,-.5);
        float min=Float.POSITIVE_INFINITY;
        for (int side=0;side<7;side++) {
            Direction direction=side==6 ? null : Direction.values()[side];
            for (BakedQuad quad:model.getQuads(null,direction,RandomSource.create(42),net.minecraftforge.client.model.data.ModelData.EMPTY,null)) {
                int[] data=quad.getVertices(); int stride=data.length/4;
                float[] ink=!key.block && !model.isGui3d() ? INK.computeIfAbsent(quad.getSprite(),CafeModelBounds::ink) : null;
                for (int v=0;v<4;v++) {
                    Vector3f point=new Vector3f(f(data,v*stride),f(data,v*stride+1),f(data,v*stride+2));
                    // Generated front/back quads cover transparent sprite margins. Clip their UV rectangle.
                    if (ink!=null && quad.getDirection().getAxis()==Direction.Axis.Z) {
                        int next=(v+1)%4, other=(v+3)%4;
                        float u=f(data,v*stride+4), w=f(data,v*stride+5);
                        float targetU=Math.max(ink[0],Math.min(ink[2],u));
                        float targetV=Math.max(ink[1],Math.min(ink[3],w));
                        adjust(point,data,stride,v,next,targetU-u,targetV-w);
                        adjust(point,data,stride,v,other,targetU-u,targetV-w);
                    }
                    pose.last().pose().transformPosition(point);
                    min=Math.min(min,point.y);
                }
            }
        }
        return Float.isFinite(min) ? min : 0;
    }
    private static float f(int[] data,int i) { return Float.intBitsToFloat(data[i]); }
    private static void adjust(Vector3f point,int[] data,int stride,int from,int to,float du,float dv) {
        float eu=f(data,to*stride+4)-f(data,from*stride+4), ev=f(data,to*stride+5)-f(data,from*stride+5);
        float t=Math.abs(eu)>Math.abs(ev) ? du/eu : dv/ev;
        if (!Float.isFinite(t)) return;
        point.add(t*(f(data,to*stride)-f(data,from*stride)),t*(f(data,to*stride+1)-f(data,from*stride+1)),t*(f(data,to*stride+2)-f(data,from*stride+2)));
    }
    private static float[] ink(TextureAtlasSprite sprite) {
        var contents=sprite.contents(); int width=contents.width(),height=contents.height();
        int minX=width,minY=height,maxX=-1,maxY=-1;
        int[] frames=contents.getUniqueFrames().toArray();
        for(int frame:frames) for(int y=0;y<height;y++) for(int x=0;x<width;x++) if(!contents.isTransparent(frame,x,y)) {
            minX=Math.min(minX,x);minY=Math.min(minY,y);maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);
        }
        if(maxX<0) return new float[]{sprite.getU0(),sprite.getV0(),sprite.getU1(),sprite.getV1()};
        return new float[]{sprite.getU(minX*16.0/width),sprite.getV(minY*16.0/height),sprite.getU((maxX+1)*16.0/width),sprite.getV((maxY+1)*16.0/height)};
    }
}
