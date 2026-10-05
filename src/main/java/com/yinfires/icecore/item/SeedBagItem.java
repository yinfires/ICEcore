package com.yinfires.icecore.item;

import com.yinfires.icecore.farming.SeedBagCrops;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** A crop-specific replacement for direct seed planting. Players plant through SeedBagFarmingEvents. */
public final class SeedBagItem extends Item {
    private final SeedBagDefinition definition;

    public SeedBagItem(Properties properties, SeedBagDefinition definition) {
        super(properties);
        this.definition = definition;
    }

    public SeedBagDefinition definition() {
        return definition;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return SeedBagCrops.plant(context.getLevel(), context.getPlayer(), context.getHand(),
                context.getClickedPos(), definition, context.getItemInHand());
    }
}
