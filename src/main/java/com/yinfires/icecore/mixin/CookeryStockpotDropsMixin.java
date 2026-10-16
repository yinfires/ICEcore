package com.yinfires.icecore.mixin;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Skips only the upstream lid-loot branch, leaving pot and ingredient loot intact. */
@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.StockpotBlock", remap = false)
public abstract class CookeryStockpotDropsMixin {
    @Redirect(method = "m_49635_(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/storage/loot/LootParams$Builder;)Ljava/util/List;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;m_61143_(Lnet/minecraft/world/level/block/state/properties/Property;)Ljava/lang/Comparable;", remap = false), remap = false)
    private Comparable<?> icecore$noLidLoot(BlockState state, Property<?> property) {
        return Boolean.FALSE;
    }
}
