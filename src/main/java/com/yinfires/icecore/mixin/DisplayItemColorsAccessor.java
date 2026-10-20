package com.yinfires.icecore.mixin;

import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.Map;

@Mixin(ItemColors.class)
public interface DisplayItemColorsAccessor {
    @Accessor(value = "f_92674_", remap = false)
    Map<Holder.Reference<Item>, ItemColor> icecore$colors();
}
