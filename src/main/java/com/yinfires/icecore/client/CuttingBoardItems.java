package com.yinfires.icecore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.yinfires.icecore.workstation.CuttingBoardAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import java.util.List;

public final class CuttingBoardItems {
    private static final net.minecraft.util.RandomSource ROTATIONS = net.minecraft.util.RandomSource.create(0);
    private CuttingBoardItems() {}
    public static void render(BlockEntity entity, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        CuttingBoardAccess board = (CuttingBoardAccess)entity;
        List<ItemStack> products = board.icecore$products();
        int size = products.isEmpty() ? 1 : products.size();
        var renderer = Minecraft.getInstance().getItemRenderer();
        ROTATIONS.setSeed(entity.getBlockPos().asLong());
        boolean cookery = net.minecraftforge.fml.ModList.get().isLoaded("kaleidoscope_cookery");
        pose.pushPose();
        pose.translate(.5, .1, .5);
        pose.mulPose(Axis.YP.rotationDegrees(entity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).get2DDataValue() * 90));
        pose.mulPose(Axis.XN.rotationDegrees(90)); pose.scale(.5F, .5F, .5F);
        for (int i = 0; i < size; i++) {
            int angle = 90 + ROTATIONS.nextInt(90);
            ItemStack item = products.isEmpty() ? board.icecore$inventory().getStackInSlot(0) : products.get(i);
            if (item.isEmpty()) continue;
            pose.pushPose();
            // The wok's seeded per-index rotation; empty entries retain their original layout positions.
            pose.translate(0, 0, i * .025);
            pose.mulPose(Axis.ZP.rotationDegrees(i * angle));
            var model = cookery
                    ? CuttingBoardCookeryModel.model(entity, item) : renderer.getModel(item, entity.getLevel(), null, 0);
            // FIXED generated-item transforms turn the sprite around; keep its front facing up.
            if (!model.isGui3d()) pose.mulPose(Axis.YP.rotationDegrees(180));
            renderer.render(item, ItemDisplayContext.FIXED, false, pose, buffer, light, overlay, model);
            pose.popPose();
        }
        pose.popPose();
    }
}
