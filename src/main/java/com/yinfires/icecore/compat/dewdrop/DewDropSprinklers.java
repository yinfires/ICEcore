package com.yinfires.icecore.compat.dewdrop;

import cool.bot.botslib.util.Util;
import cool.bot.dewdropfarmland.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmBlock;

public final class DewDropSprinklers {
    private record Placement(BlockPos origin, Block block, int radius) {}
    private static final ThreadLocal<Placement> PENDING = new ThreadLocal<>();
    private static final java.util.List<TagKey<Block>> TIERS = java.util.stream.IntStream.rangeClosed(1, 4)
            .mapToObj(i -> TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("dew_drop_farmland_growth", "sprinkler_tier_" + i)))
            .toList();
    private DewDropSprinklers() {}

    public static void begin(UseOnContext context) {
        PENDING.remove();
        if (!(context.getItemInHand().getItem() instanceof BlockItem item)
                || !(context.getLevel() instanceof ServerLevel level) || !Config.checkSprinklers) return;
        int radius = 0;
        for (int i = 0; i < TIERS.size(); i++) if (item.getBlock().defaultBlockState().is(TIERS.get(i))) radius = i + 1;
        if (radius == 0) return;
        var placement = new net.minecraft.world.item.context.BlockPlaceContext(context);
        PENDING.set(new Placement(placement.getClickedPos().immutable(), item.getBlock(), radius));
    }

    public static void placed(UseOnContext context, boolean success) {
        Placement pending = PENDING.get();
        PENDING.remove();
        if (!success || pending == null || !(context.getLevel() instanceof ServerLevel level)) return;
        BlockPos origin = pending.origin();
        int radius = pending.radius();
        if (!level.getBlockState(origin).is(pending.block())) return;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = -1; y <= 0; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    if (level.hasChunkAt(cursor) && level.getBlockState(cursor).getBlock() instanceof FarmBlock
                            && Util.isDryWaterable(level, cursor)) Util.setMoist(level, cursor);
                }
            }
        }
    }
}
