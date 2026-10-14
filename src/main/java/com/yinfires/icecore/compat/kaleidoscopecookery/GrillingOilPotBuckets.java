package com.yinfires.icecore.compat.kaleidoscopecookery;

import cn.breezeth.kaleidoscope_grilling.TypedOilPotAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** Isolated from the Cookery-only path so Grilling remains optional. */
final class GrillingOilPotBuckets {
    private static final ResourceLocation BUCKET = ResourceLocation.fromNamespaceAndPath(
            "kaleidoscope_grilling", "canola_oil_bucket");
    private static final int OIL_PER_BUCKET = 8;

    private GrillingOilPotBuckets() {}

    static void tryFill(PlayerInteractEvent.RightClickBlock event) {
        var level = event.getLevel();
        if (!(level.getBlockEntity(event.getPos()) instanceof TypedOilPotAccess pot)
                || !"canola".equals(pot.grilling$getOilType())) return;
        // Consume even an insufficient attempt so the second hand cannot act on the pot.
        OilPotInteractions.consumeClick(event);
        int count = pot.grilling$getOilCount();
        if (level.isClientSide || count < OIL_PER_BUCKET) return;
        var bucket = ForgeRegistries.ITEMS.getValue(BUCKET);
        if (bucket == null || bucket == Items.AIR) return;
        var player = event.getEntity();
        ItemStack held = player.getMainHandItem();
        ItemStack filled = new ItemStack(bucket);
        pot.grilling$setOilCount(count - OIL_PER_BUCKET);
        held.shrink(1);
        if (held.isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, filled);
        } else if (!player.getInventory().add(filled)) {
            player.drop(filled, false);
        }
        level.playSound(null, event.getPos(), SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
    }
}
