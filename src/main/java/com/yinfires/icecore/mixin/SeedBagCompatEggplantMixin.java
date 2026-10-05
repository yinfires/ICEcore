package com.yinfires.icecore.mixin;

import com.yinfires.icecore.farming.SeedBagLoot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** The eggplant crop hard-codes its drops; restore the loot-table path so the seed-bag table applies. */
@Pseudo
@Mixin(targets = "com.bmt.kaleidoscope_chinesefood.block.crop.EggplantCropBlock", remap = false)
public abstract class SeedBagCompatEggplantMixin {
    @Inject(method = "m_49635_(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/storage/loot/LootParams$Builder;)Ljava/util/List;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void icecore$lootTableDrops(BlockState state, LootParams.Builder builder,
                                        CallbackInfoReturnable<List<ItemStack>> callback) {
        callback.setReturnValue(SeedBagLoot.lootTableDrops((Block) (Object) this, state, builder));
    }
}
