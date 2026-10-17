package com.yinfires.icecore.client;

import com.github.ysbbbbbb.kaleidoscopecookery.client.resources.ItemRenderReplacer;
import com.github.ysbbbbbb.kaleidoscopecookery.client.resources.ItemRenderReplacerReloadListener;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Loaded only when the optional kitchen model provider is present. */
public final class CuttingBoardCookeryModel {
    private CuttingBoardCookeryModel() {}
    public static BakedModel model(BlockEntity entity, ItemStack item) {
        return ItemRenderReplacer.getModel(entity.getLevel(), item, ItemRenderReplacerReloadListener.INSTANCE.pot());
    }
}
