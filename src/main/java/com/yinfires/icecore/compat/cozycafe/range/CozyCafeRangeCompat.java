package com.yinfires.icecore.compat.cozycafe.range;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

/** Optional-CozyCafe checks that do not link against CozyCafe classes. */
public final class CozyCafeRangeCompat {
    public static final String MOD_ID = "cozycafe";
    private static final ResourceLocation CAFE_MANAGER =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "cafe_manager");

    private CozyCafeRangeCompat() {
    }

    public static boolean isCafeManager(Level level, BlockPos position) {
        return level != null && position != null
                && CAFE_MANAGER.equals(ForgeRegistries.BLOCKS.getKey(level.getBlockState(position).getBlock()));
    }
}
