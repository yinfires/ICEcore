package com.yinfires.icecore.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Removes the byproduct before either Create insertion or world ejection. */
@Mixin(targets = "cn.breezeth.kaleidoscope_grilling.oil.OilPressBlockEntity", remap = false)
public abstract class GrillingOilPressMixin {
    @Shadow private int cakes;

    @Redirect(method={"pressWithTool(Lnet/minecraft/world/entity/player/Player;I)Z","addProgress(I)Z"},
        at=@At(value="FIELD",target="Lcn/breezeth/kaleidoscope_grilling/oil/OilPressBlockEntity;cakes:I",opcode=180))
    private int icecore$allowPartialBatch(@org.spongepowered.asm.mixin.injection.Coerce Object press) {
        // Native checks compare to a full batch of four; any nonempty batch is valid.
        return cakes>0?4:0;
    }

    @ModifyArg(method="tryFinish()Z",at=@At(value="INVOKE",
        target="Lcn/breezeth/kaleidoscope_grilling/OilPressContainerApi;insertNearby(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;I)Lcn/breezeth/kaleidoscope_grilling/OilPressContainerApi$TransferResult;"),index=2)
    private int icecore$oneBucketPerCake(int ignored) { return cakes; }

    @Redirect(method = "tryFinish()Z", at = @At(value = "NEW",
            target = "(Lnet/minecraft/world/level/ItemLike;I)Lnet/minecraft/world/item/ItemStack;",
            remap = false), remap = false)
    private ItemStack icecore$omitOilResidue(ItemLike item, int count) {
        return ItemStack.EMPTY;
    }
}
