package com.yinfires.icecore.shelf.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.yinfires.icecore.shelf.*;
import net.blay09.mods.cookingforblockheads.client.render.RenderUtils;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class ShelfRenderer implements BlockEntityRenderer<ShelfBlockEntity> {
    // Match FridgeRenderer: overlap of projected FIXED silhouettes is intentional.
    public static final float ITEM_SCALE = .3f;
    // Only the contact offset is measured. Native FIXED rotation/scale remain unchanged.
    private static final Map<BakedModel,Float> BOTTOMS=new LinkedHashMap<>() {
        @Override protected boolean removeEldestEntry(Map.Entry<BakedModel,Float> entry) { return size()>512; }
    };
    public static void clearModelCache() { ShelfSortCache.clear(); BOTTOMS.clear(); ShelfRenderCache.clear(); ShelfSceneCache.clear(); RackDisplayCache.clear(); ShelfRenderStats.clearEligibility(); }
    private static float bottom(BakedModel model) {
        PoseStack transform=new PoseStack();
        model.getTransforms().getTransform(ItemDisplayContext.FIXED).apply(false,transform);
        transform.translate(-.5,-.5,-.5);
        float min=Float.POSITIVE_INFINITY;
        RandomSource random=RandomSource.create(42);
        for(int side=0;side<7;side++) {
            random.setSeed(42);
            for(var quad:model.getQuads(null,side==6?null:Direction.values()[side],random)) {
                int[] vertices=quad.getVertices(); int stride=vertices.length/4;
                for(int i=0;i<4;i++) {
                    Vector3f point=new Vector3f(Float.intBitsToFloat(vertices[i*stride]),Float.intBitsToFloat(vertices[i*stride+1]),Float.intBitsToFloat(vertices[i*stride+2]));
                    min=Math.min(min,point.mulPosition(transform.last().pose()).y);
                }
            }
        }
        return Float.isFinite(min)?min:-.5f;
    }
    // Grilling pushes FIXED context in renderStatic, so its second model lookup is semantically significant.
    private static final boolean STATIC_CONTEXT=ShelfRenderer.class.getClassLoader().getResource(
            "cn/breezeth/kaleidoscope_grilling/mixin/ItemRendererContextMixin.class")!=null;
    private static final org.joml.Quaternionf ITEM_ROTATION=Axis.YP.rotationDegrees(45);
    private static final org.joml.Quaternionf[] FACING_ROTATIONS=new org.joml.Quaternionf[4];
    private static final double[] X=new double[72],Y=new double[72],Z=new double[72];
    static {
        for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL) FACING_ROTATIONS[direction.get2DDataValue()]=Axis.YP.rotationDegrees(180-direction.toYRot());
        for(int i=0;i<72;i++) { X[i]=ShelfLayout.x(i)/16;Y[i]=ShelfLayout.y(i)/16;Z[i]=ShelfLayout.z(i)/16; }
    }
    public ShelfRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(ShelfBlockEntity shelf,float partialTick,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if(!com.yinfires.icecore.config.ICECoreClientConfig.SHELF_DISPLAY_ENABLED.get()) return;
        var camera=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        if(!ShelfDisplayPolicy.visible(com.yinfires.icecore.config.ICECoreClientConfig.SHELF_DISPLAY_ENABLED.get(),
                com.yinfires.icecore.config.ICECoreClientConfig.SHELF_DISPLAY_DISTANCE.get(),
                camera.distanceToSqr(shelf.getBlockPos().getX()+.5,shelf.getBlockPos().getY()+.5,shelf.getBlockPos().getZ()+.5))) return;
        ShelfRenderStats.shelf();
        if(com.yinfires.icecore.compat.render.RenderDiagnostics.mode==com.yinfires.icecore.compat.render.RenderDiagnostics.Mode.HIDDEN) return;
        pose.pushPose(); pose.translate(.5,0,.5);
        pose.mulPose(FACING_ROTATIONS[shelf.getBlockState().getValue(ShelfBlock.FACING).get2DDataValue()]);
        pose.translate(-.5,0,-.5);
        var remaining=com.yinfires.icecore.compat.render.RenderDiagnostics.mode==com.yinfires.icecore.compat.render.RenderDiagnostics.Mode.NATIVE?null:ShelfSceneCache.render(shelf,pose.last(),buffers,light,slot->renderSlot(shelf,slot,pose,buffers,light));
        if(remaining==null) ShelfRenderStats.slots(0,ShelfRenderCache.slots(shelf).length);
        for(var cached:remaining==null?ShelfRenderCache.slots(shelf):remaining) {
            renderSlot(shelf,cached,pose,buffers,light);
        }
        pose.popPose();
    }
    private static void renderSlot(ShelfBlockEntity shelf,ShelfRenderCache.Slot cached,PoseStack pose,MultiBufferSource buffers,int light) {
            long nativeStart=ShelfRenderStats.start();
            int slot=cached.index; ItemStack stack=cached.stack;
            pose.pushPose();
            ShelfRenderStats.model();
            var model=Minecraft.getInstance().getItemRenderer().getModel(stack,shelf.getLevel(),null,0);
            float bottom=BOTTOMS.computeIfAbsent(model,ShelfRenderer::bottom);
            pose.translate(X[slot],Y[slot]-bottom*ITEM_SCALE,Z[slot]);
            pose.mulPose(ITEM_ROTATION);
            pose.scale(ITEM_SCALE,ITEM_SCALE,ITEM_SCALE);
            if(STATIC_CONTEXT || ShelfShaderCompatibility.acceleratedRenderingPresent() || ShelfRenderCache.nativeRequired()
                    || com.yinfires.icecore.compat.render.RenderDiagnostics.mode==com.yinfires.icecore.compat.render.RenderDiagnostics.Mode.NATIVE) {
                ShelfRenderStats.model();
                RenderUtils.renderItem(stack,light,pose,buffers,shelf.getLevel());
            } else {
                Minecraft.getInstance().getItemRenderer().render(stack,ItemDisplayContext.FIXED,false,pose,buffers,light,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,model);
            }
            pose.popPose();ShelfRenderStats.nativeTime(nativeStart);
    }
    @Override public boolean shouldRender(ShelfBlockEntity shelf,Vec3 camera) { return Vec3.atCenterOf(shelf.getBlockPos()).distanceToSqr(camera)<=(double)getViewDistance()*getViewDistance(); }
}
