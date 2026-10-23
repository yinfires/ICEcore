package com.yinfires.icecore.mixin;

import com.yinfires.icecore.currency.CurrencyService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.ForgeConfigSpec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/** Replaces both CozyCafe physical-coin and Numismatics payment with ICEcore's shared balance. */
@Pseudo
@Mixin(targets = "io.github.chakyl.cozycafe.blockentities.CafeMenuBlockEntity", remap = false)
public abstract class CozyCafePaymentMixin {
    @org.spongepowered.asm.mixin.Shadow private int waitTime;
    @org.spongepowered.asm.mixin.Unique private int icecore$actualWait;
    @org.spongepowered.asm.mixin.Unique private boolean icecore$alreadyPaid;

    @org.spongepowered.asm.mixin.injection.Inject(method="handlePayment(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lio/github/chakyl/cozycafe/data/CafeMenuItem;Lnet/minecraft/world/item/ItemStack;)V",at=@At("HEAD"),cancellable=true)
    private void icecore$removeSpeedPrice(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        var visit=((com.yinfires.icecore.compat.cozycafe.customer.CafeCustomerVisit)this).icecore$visit();
        icecore$alreadyPaid=visit.isEmpty()||visit.getBoolean("Settled");
        if(icecore$alreadyPaid){ci.cancel();return;}
        icecore$actualWait=waitTime;
        waitTime=io.github.chakyl.cozycafe.blockentities.CafeMenuBlockEntity.getMaxWaitTime();
    }
    @org.spongepowered.asm.mixin.injection.Inject(method="handlePayment(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lio/github/chakyl/cozycafe/data/CafeMenuItem;Lnet/minecraft/world/item/ItemStack;)V",at=@At("RETURN"))
    private void icecore$restoreWait(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {if(!icecore$alreadyPaid)waitTime=icecore$actualWait;}
    @Redirect(method = "handlePayment(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lio/github/chakyl/cozycafe/data/CafeMenuItem;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/common/ForgeConfigSpec$BooleanValue;get()Ljava/lang/Object;"),
            remap = false)
    private Object icecore$disableNumismaticsDeposit(ForgeConfigSpec.BooleanValue value) {
        return Boolean.FALSE;
    }

    @Redirect(method = "handlePayment(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lio/github/chakyl/cozycafe/data/CafeMenuItem;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At(value = "INVOKE", target = "Lio/github/chakyl/cozycafe/util/PaymentUtils;getPaymentItems(I)Ljava/util/List;"),
            remap = false)
    private List<ItemStack> icecore$depositSharedCurrency(int amount, BlockPos position, Player player,
                                                         @Coerce Object menuItem, ItemStack servedStack) {
        BlockEntity self = (BlockEntity) (Object) this;
        com.yinfires.icecore.compat.cozycafe.customer.CafeCustomers.settle(
                (io.github.chakyl.cozycafe.blockentities.CafeMenuBlockEntity)(Object)this,amount,icecore$actualWait);
        return List.of();
    }
}
