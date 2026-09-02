package com.yinfires.icecore.npc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yinfires.icecore.npc.NpcEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.EntityHitResult;

public final class NpcRenderer extends MobRenderer<NpcEntity, PlayerModel<NpcEntity>> {
    private final PlayerModel<NpcEntity> wideModel;
    private final PlayerModel<NpcEntity> slimModel;

    public NpcRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        wideModel = model;
        slimModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        showOuterLayers(wideModel);
        showOuterLayers(slimModel);
    }

    private static void showOuterLayers(PlayerModel<?> model) {
        model.hat.visible = true; model.jacket.visible = true;
        model.leftSleeve.visible = true; model.rightSleeve.visible = true;
        model.leftPants.visible = true; model.rightPants.visible = true;
    }

    @Override
    public void render(NpcEntity npc, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        model = npc.isSlim() ? slimModel : wideModel;
        super.render(npc, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override public ResourceLocation getTextureLocation(NpcEntity npc) { return npc.getTexture(); }

    @Override
    protected boolean shouldShowName(NpcEntity npc) {
        return Minecraft.getInstance().hitResult instanceof EntityHitResult hit && hit.getEntity() == npc;
    }
}
