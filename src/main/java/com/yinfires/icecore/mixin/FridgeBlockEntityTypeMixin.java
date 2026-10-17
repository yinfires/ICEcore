package com.yinfires.icecore.mixin;

import com.yinfires.icecore.fridge.FridgeBlockTypeAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import java.util.Set;

@Mixin(value = BlockEntityType.class, remap = false)
public abstract class FridgeBlockEntityTypeMixin implements FridgeBlockTypeAccess {
    @Shadow(aliases = "validBlocks", remap = false) @Final @Mutable private Set<Block> f_58915_;
    public Set<Block> icecore$getValidBlocks() { return f_58915_; }
    public void icecore$setValidBlocks(Set<Block> blocks) { f_58915_ = blocks; }
}
