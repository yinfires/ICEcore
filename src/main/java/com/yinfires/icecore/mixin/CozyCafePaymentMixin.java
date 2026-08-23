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
        if (amount > 0 && self.getLevel() instanceof ServerLevel level) {
            CurrencyService.add(amount, "cozycafe", "cozycafe",
                    CurrencyService.sourceAt(level, position.getCenter(), player));
        }
        return List.of();
    }
}
