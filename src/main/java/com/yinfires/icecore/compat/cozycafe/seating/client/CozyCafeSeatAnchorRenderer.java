package com.yinfires.icecore.compat.cozycafe.seating.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yinfires.icecore.compat.cozycafe.seating.CozyCafeSeatAnchorEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class CozyCafeSeatAnchorRenderer extends EntityRenderer<CozyCafeSeatAnchorEntity> {
    private static final ResourceLocation EMPTY = ResourceLocation.fromNamespaceAndPath("icecore", "textures/entity/empty.png");
    public CozyCafeSeatAnchorRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public void render(CozyCafeSeatAnchorEntity entity, float yaw, float partialTick,
                                 PoseStack poseStack, MultiBufferSource buffers, int light) {}
    @Override public ResourceLocation getTextureLocation(CozyCafeSeatAnchorEntity entity) { return EMPTY; }
}
