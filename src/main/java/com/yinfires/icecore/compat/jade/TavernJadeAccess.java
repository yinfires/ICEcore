package com.yinfires.icecore.compat.jade;

import com.github.ysbbbbbb.kaleidoscopetavern.block.brew.BarrelBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;

public final class TavernJadeAccess {
    private TavernJadeAccess() {}
    public static BlockEntity origin(BlockAccessor accessor) { return BarrelBlock.getBarrelEntity(accessor.getLevel(), accessor.getPosition(), accessor.getBlockState()); }
}
