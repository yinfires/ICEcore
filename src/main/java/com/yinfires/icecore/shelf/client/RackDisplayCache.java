package com.yinfires.icecore.shelf.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yinfires.icecore.compat.render.RenderDiagnostics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.*;

/** Records the upstream layout only on invalidation; never reconstructs its random layout. */
public final class RackDisplayCache {
    private static final WeakHashMap<BlockEntity,State> STATES=new WeakHashMap<>();
    private static long serial;
    private static List<ShelfRenderCache.Slot> recording;
    private static RackItemBatch batch;
    private static final class State {
        long revision=++serial;
        net.minecraft.world.level.block.state.BlockState blockState;
        ShelfRenderCache.Slot[] slots;
    }
    public static void invalidate(BlockEntity entity) {
        if(entity.getLevel()==null || !entity.getLevel().isClientSide()) return;
        STATES.remove(entity);
    }
    static void clear() { STATES.clear(); }
    static void unload(net.minecraft.world.level.LevelAccessor level,net.minecraft.world.level.ChunkPos chunk) {
        STATES.keySet().removeIf(e->e.getLevel()==level && (e.getBlockPos().getX()>>4)==chunk.x && (e.getBlockPos().getZ()>>4)==chunk.z);
    }
    @SuppressWarnings("unchecked")
    public static boolean render(Object renderer,BlockEntity entity,float partialTick,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if(recording!=null || batch!=null) return false;
        ShelfRenderStats.shelf();
        if(RenderDiagnostics.mode==RenderDiagnostics.Mode.HIDDEN) return true;
        if(RenderDiagnostics.mode==RenderDiagnostics.Mode.NATIVE || ShelfRenderCache.nativeRequired() || ShelfShaderCompatibility.acceleratedRenderingPresent()) return false;
        batch=new RackItemBatch(entity);
        try {
            if(!scene(renderer,entity,partialTick,pose,buffers,light,overlay))
                ((BlockEntityRenderer<BlockEntity>)renderer).render(entity,partialTick,pose,buffers,light,overlay);
            return true;
        } finally { batch=null; }
    }
    @SuppressWarnings("unchecked")
    private static boolean scene(Object renderer,BlockEntity entity,float partialTick,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if(!STATES.containsKey(entity) && STATES.size()>=ShelfSceneCache.MAX_SHELVES) STATES.remove(STATES.keySet().iterator().next());
        State state=STATES.computeIfAbsent(entity,e->new State());
        if(state.blockState!=entity.getBlockState()) { state.blockState=entity.getBlockState();state.slots=null;state.revision=++serial; }
        return ShelfSceneCache.render(entity,state.revision,pose.last(),buffers,light,()-> {
            if(state.slots==null) {
                recording=new ArrayList<>();
                try {
                    ((BlockEntityRenderer<BlockEntity>)renderer).render(entity,partialTick,new PoseStack(),buffers,light,overlay);
                    state.slots=recording.toArray(ShelfRenderCache.Slot[]::new);
                } finally { recording=null; }
            }
            return state.slots;
        },slot-> {
            long nativeStart=ShelfRenderStats.start();
            pose.pushPose();
            try {
                pose.last().pose().mul(slot.placement.pose());pose.last().normal().mul(slot.placement.normal());
                ShelfRenderStats.model();
                draw(net.minecraft.client.Minecraft.getInstance().getItemRenderer(),slot.stack,ItemDisplayContext.FIXED,light,overlay,pose,buffers,entity.getLevel(),slot.seed);
            } finally { pose.popPose();ShelfRenderStats.nativeTime(nativeStart); }
        })!=null;
    }
    public static void item(ItemRenderer renderer,ItemStack stack,ItemDisplayContext context,int light,int overlay,PoseStack pose,MultiBufferSource buffers,Level level,int seed) {
        if(recording==null) {
            ShelfRenderStats.slots(0,1);ShelfRenderStats.model();
            long start=ShelfRenderStats.start();
            try { draw(renderer,stack,context,light,overlay,pose,buffers,level,seed); }
            finally { ShelfRenderStats.nativeTime(start); }
        } else if(!stack.isEmpty()) {
            PoseStack copy=new PoseStack();copy.last().pose().set(pose.last().pose());copy.last().normal().set(pose.last().normal());
            recording.add(new ShelfRenderCache.Slot(recording.size(),stack.copy(),true,seed,copy.last()));
        }
    }
    private static void draw(ItemRenderer renderer,ItemStack stack,ItemDisplayContext context,int light,int overlay,PoseStack pose,MultiBufferSource buffers,Level level,int seed) {
        if(batch==null) renderer.renderStatic(stack,context,light,overlay,pose,buffers,level,seed);
        else batch.render(renderer,stack,context,light,overlay,pose,buffers,level,seed);
    }
    private RackDisplayCache() {}
}
