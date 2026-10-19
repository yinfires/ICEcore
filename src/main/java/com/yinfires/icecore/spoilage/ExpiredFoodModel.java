package com.yinfires.icecore.spoilage;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraftforge.client.model.BakedModelWrapper;
import java.util.*;

/** Wrap existing geometry; cached vertex colors affect every face, including untinted quads. */
public final class ExpiredFoodModel extends BakedModelWrapper<BakedModel> {
    private final Map<BakedQuad, BakedQuad> quads = new IdentityHashMap<>();
    public ExpiredFoodModel(BakedModel model) { super(model); }
    @Override public List<BakedQuad> getQuads(net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.core.Direction side, net.minecraft.util.RandomSource random) {
        return originalModel.getQuads(state, side, random).stream().map(this::green).toList();
    }
    private BakedQuad green(BakedQuad original) {
        if (quads.size() >= 4096) quads.clear();
        return quads.computeIfAbsent(original, quad -> {
            int[] vertices = quad.getVertices().clone();
            int stride = vertices.length / 4;
            for (int i = 0; i < 4; i++) {
                int color = vertices[i * stride + 3];
                int r = (color & 255) * 85 / 255;
                int g = ((color >>> 8) & 255) * 170 / 255;
                int b = ((color >>> 16) & 255) * 55 / 255;
                vertices[i * stride + 3] = color & 0xff000000 | b << 16 | g << 8 | r;
            }
            return new BakedQuad(vertices, -1, quad.getDirection(), quad.getSprite(), quad.isShade());
        });
    }
    @Override public List<BakedQuad> getQuads(net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.core.Direction side, net.minecraft.util.RandomSource random,
            net.minecraftforge.client.model.data.ModelData data, net.minecraft.client.renderer.RenderType type) {
        return originalModel.getQuads(state, side, random, data, type).stream().map(this::green).toList();
    }
    @Override public List<BakedModel> getRenderPasses(net.minecraft.world.item.ItemStack stack, boolean fabulous) { return List.of(this); }
    @Override public BakedModel applyTransform(net.minecraft.world.item.ItemDisplayContext context,
            com.mojang.blaze3d.vertex.PoseStack pose, boolean left) {
        originalModel.applyTransform(context, pose, left); return this;
    }
}
