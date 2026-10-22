package com.yinfires.icecore.shelf.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Per-rack, per-render batches. Capture through the complete native item pipeline. */
final class RackItemBatch {
    private final ShelfRenderCache.Slot[] slots = new ShelfRenderCache.Slot[6];
    private final BlockEntity rack;

    RackItemBatch(BlockEntity rack) { this.rack = rack; }

    void render(ItemRenderer renderer, ItemStack stack, ItemDisplayContext context, int light, int overlay,
                PoseStack pose, MultiBufferSource buffers, Level level, int seed) {
        if (seed < 0 || seed >= slots.length || context != ItemDisplayContext.FIXED) {
            renderer.renderStatic(stack, context, light, overlay, pose, buffers, level, seed);
            return;
        }
        var slot=slots[seed];
        if(slot==null) slots[seed]=slot=new ShelfRenderCache.Slot(seed,stack.copy(),true,seed,null);
        var mesh=ShelfRenderCache.mesh(slot,rack);
        if(mesh==null) renderer.renderStatic(stack,context,light,overlay,pose,buffers,level,seed);
        else mesh.replay(pose.last(),buffers,light);
    }
}
