package com.yinfires.icecore.fridge;

import net.minecraft.world.level.block.Block;
import java.util.Set;

public interface FridgeBlockTypeAccess {
    Set<Block> icecore$getValidBlocks();
    void icecore$setValidBlocks(Set<Block> blocks);
}
