package com.yinfires.icecore.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/** Only replaces the loose-skewer placement call; packed plates and disassembly remain upstream. */
@Mixin(targets = "cn.breezeth.kaleidoscope_grilling.skewer.SkewerPlatePlacement", remap = false)
public abstract class GrillingSkewerPlacementMixin {
    @Redirect(method = "onRightClickBlock(Lnet/minecraftforge/event/entity/player/PlayerInteractEvent$RightClickBlock;)V",
            at = @At(value = "INVOKE", target = "Lcn/breezeth/kaleidoscope_grilling/skewer/SkewerPlatePlacement;place(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Ljava/util/List;)Lnet/minecraft/world/InteractionResult;",
                    remap = false), remap = false)
    private static InteractionResult icecore$skipLooseSkewerPlacement(Level level, BlockPos pos, Direction face,
                                                                    Player player, ItemStack held, List<ItemStack> skewers) {
        return InteractionResult.PASS;
    }
}
