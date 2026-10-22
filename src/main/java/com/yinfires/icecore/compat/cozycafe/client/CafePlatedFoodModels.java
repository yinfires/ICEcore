package com.yinfires.icecore.compat.cozycafe.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import com.yinfires.icecore.compat.cozycafe.CafeDisplayRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.QuadTransformers;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** A single normalized, horizontal food geometry for held and placed plates. */
public final class CafePlatedFoodModels {
    private record Key(BakedModel source,boolean block) {}
    private static final Map<Key,BakedModel> CACHE=new HashMap<>();
    private CafePlatedFoodModels() {}
    public static void clear() { CACHE.clear(); }
    public static BakedModel model(ItemStack stack) {
        var mc=Minecraft.getInstance();
        BlockState state=null;
        if(!CafeTableRendering.icon(stack)) {
            if(stack.getItem() instanceof BlockItem block) state=block.getBlock().defaultBlockState();
            else {
                var itemId=ForgeRegistries.ITEMS.getKey(stack.getItem());
                var id=stack.is(Items.PUMPKIN_PIE)?ResourceLocation.parse("farmersdelight:pumpkin_pie"):itemId;
                // Farmer's Delight registers full pies as ordinary items, with separate placeable blocks.
                if(id!=null && id.getNamespace().equals("farmersdelight") && Set.of("pumpkin_pie","apple_pie","sweet_berry_cheesecake","chocolate_pie").contains(id.getPath())
                    && ForgeRegistries.BLOCKS.containsKey(id)) state=ForgeRegistries.BLOCKS.getValue(id).defaultBlockState();
            }
        }
        BakedModel source=state==null?mc.getItemRenderer().getModel(stack,null,null,0):mc.getBlockRenderer().getBlockModel(state);
        Key key=new Key(source,state!=null);
        if(CACHE.size()>=512 && !CACHE.containsKey(key)) clear();
        return CACHE.computeIfAbsent(key,CafePlatedFoodModels::normalize);
    }
    private static BakedModel normalize(Key key) {
        return normalize(key.source,key.block);
    }
    static BakedModel normalize(BakedModel input,boolean block) {
        PoseStack pose=new PoseStack();
        BakedModel source=input;
        if(!block) {
            pose.mulPose(Axis.XP.rotationDegrees(90));
            source=source.applyTransform(ItemDisplayContext.FIXED,pose,false);
            pose.translate(-.5,-.5,-.5);
        }
        var first=QuadTransformers.applying(new Transformation(pose.last().pose()));
        List<BakedQuad> oriented=new ArrayList<>();
        for(int side=0;side<7;side++) for(var q:source.getQuads(null,side==6?null:Direction.values()[side],RandomSource.create(42),ModelData.EMPTY,null)) oriented.add(first.process(q));
        double minX=Double.POSITIVE_INFINITY,minY=minX,minZ=minX,maxX=Double.NEGATIVE_INFINITY,maxZ=maxX;
        for(var q:oriented) {
            int[] d=q.getVertices();int stride=d.length/4;
            for(int i=0;i<4;i++) {
                float x=Float.intBitsToFloat(d[i*stride]),y=Float.intBitsToFloat(d[i*stride+1]),z=Float.intBitsToFloat(d[i*stride+2]);
                minX=Math.min(minX,x);minY=Math.min(minY,y);minZ=Math.min(minZ,z);maxX=Math.max(maxX,x);maxZ=Math.max(maxZ,z);
            }
        }
        if(!Double.isFinite(minX)) return source;
        float scale=CafeDisplayRules.plateScale(maxX-minX,maxZ-minZ);
        PoseStack fit=new PoseStack();fit.scale(scale,scale,scale);fit.translate(-(minX+maxX)/2,-minY,-(minZ+maxZ)/2);
        var transform=QuadTransformers.applying(new Transformation(fit.last().pose()));
        List<BakedQuad> quads=oriented.stream().map(transform::process).toList();
        return new BakedModelWrapper<BakedModel>(source) {
            @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random,ModelData data,net.minecraft.client.renderer.RenderType type) { return side==null?quads:List.of(); }
            @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random) { return side==null?quads:List.of(); }
            @Override public BakedModel applyTransform(ItemDisplayContext context,PoseStack pose,boolean left) {
                // ItemRenderer always adds (-.5,-.5,-.5) after this callback; geometry is already centered.
                pose.translate(.5,.5,.5);
                return this;
            }
            @Override public boolean isCustomRenderer() { return false; }
            @Override public List<BakedModel> getRenderPasses(ItemStack stack,boolean fabulous) { return List.of(this); }
        };
    }
}
