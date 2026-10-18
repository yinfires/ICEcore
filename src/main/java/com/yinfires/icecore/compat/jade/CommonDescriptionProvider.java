package com.yinfires.icecore.compat.jade;

import com.yinfires.icecore.tooltip.ItemTooltipEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElement;

/** Insert after the actual title row, even when upstream providers add their own rows. */
public enum CommonDescriptionProvider implements IBlockComponentProvider {
    INSTANCE;
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("icecore", "common_description");
    @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        var stack = accessor.getPickedResult();
        var item = stack.isEmpty() ? accessor.getBlock().asItem() : stack.getItem();
        var lines = ItemTooltipEvents.common(ForgeRegistries.BLOCKS.getKey(accessor.getBlock()), ForgeRegistries.ITEMS.getKey(item));
        if (lines.isEmpty()) return;
        tooltip.remove(UID);
        int insertion = 0;
        for (int i = 0; i < tooltip.size(); i++) {
            boolean title = false;
            for (IElement.Align align : IElement.Align.values()) {
                for (IElement element : tooltip.get(i, align)) {
                    if (Identifiers.CORE_OBJECT_NAME.equals(element.getTag())) title = true;
                }
            }
            if (title) { insertion = i + 1; break; }
        }
        for (var line : lines) tooltip.add(insertion++, line.copy(), UID);
    }
    @Override public ResourceLocation getUid() { return UID; }
    @Override public int getDefaultPriority() { return Integer.MAX_VALUE; }
}
