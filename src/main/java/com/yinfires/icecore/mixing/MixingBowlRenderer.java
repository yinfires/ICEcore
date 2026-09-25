package com.yinfires.icecore.mixing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.satisfy.farm_and_charm.client.model.CraftingBowlModel;
import dev.xkmc.youkaishomecoming.util.FluidRenderer;

import java.lang.reflect.Field;
public final class MixingBowlRenderer implements BlockEntityRenderer<MixingBowlBlockEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("farm_and_charm","textures/entity/crafting_bowl.png");
    private static final float SWING_BASE_Z_ROTATION = 0.0873F;
    private final ModelPart bowlPart;
    private final ModelPart swing;

    public MixingBowlRenderer(BlockEntityRendererProvider.Context context) {
        CraftingBowlModel<?> model = new CraftingBowlModel<>(context.bakeLayer(CraftingBowlModel.LAYER_LOCATION));
        bowlPart = findPart(model, "bowl");
        swing = findPart(model, "swing");
    }

    private static ModelPart findPart(CraftingBowlModel<?> model, String name) {
        try { Field field=CraftingBowlModel.class.getDeclaredField(name); field.setAccessible(true); return (ModelPart)field.get(model); }
        catch (ReflectiveOperationException exception) { throw new IllegalStateException("Farm & Charm crafting bowl model changed",exception); }
    }

    @Override public void render(MixingBowlBlockEntity bowl, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        // The server synchronizes the STIRRING stage at the start and again at
        // settlement. The client-side countdown is only a local animation hint;
        // it must not stop the rod during the remaining cumulative progress.
        boolean stirring = bowl.stage() == MixingBowlBlockEntity.Stage.STIRRING;
        swing.yRot = stirring && bowl.getLevel() != null
                ? (float) (((bowl.getLevel().getGameTime() + partialTick) * 0.36D) % (Math.PI * 2D))
                : 0F;
        swing.zRot = SWING_BASE_Z_ROTATION;
        VertexConsumer consumer=buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        pose.pushPose();
        pose.translate(.5,1.5,.5); pose.mulPose(Axis.XP.rotationDegrees(180));
        bowlPart.render(pose,consumer,light,OverlayTexture.NO_OVERLAY);
        pose.popPose();
        renderItems(bowl,pose,buffers,light,overlay);
        renderFluid(bowl, pose, buffers, light);
        pose.pushPose();
        pose.translate(.5,1.5,.5); pose.mulPose(Axis.XP.rotationDegrees(180));
        swing.render(pose,consumer,light,OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    private static void renderFluid(MixingBowlBlockEntity bowl, PoseStack pose, MultiBufferSource buffers, int light) {
        FluidStack fluid = bowl.visibleFluid();
        if (fluid.isEmpty()) return;
        float bottom = 3F / 16F;
        float top = bottom + (4.2F / 16F) * Math.min(1F, fluid.getAmount() / 1000F);
        FluidRenderer.renderFluidBox(fluid, 4F / 16F, bottom, 4F / 16F,
                12F / 16F, top, 12F / 16F, buffers, pose, light, false, 0);
    }

    private static void renderItems(MixingBowlBlockEntity bowl, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemRenderer renderer=Minecraft.getInstance().getItemRenderer();
        for(int i=0;i<bowl.visibleItems().size();i++) {
            ItemStack stack=bowl.visibleItems().get(i);
            if (stack.isEmpty()) continue;
            pose.pushPose();
            pose.translate(.5F,3F / 16F,.5F);
            pose.mulPose(Axis.YP.rotationDegrees(i * 40F));
            pose.mulPose(Axis.XP.rotationDegrees(70F));
            pose.translate(-.10F,-.10F,0F);
            pose.scale(.30F,.30F,.30F);
            renderer.renderStatic(stack,ItemDisplayContext.FIXED,light,overlay,pose,buffers,bowl.getLevel(),
                    (int) bowl.getBlockPos().asLong() + i);
            pose.popPose();
        }
    }
}
