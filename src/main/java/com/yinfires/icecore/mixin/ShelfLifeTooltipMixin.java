package com.yinfires.icecore.mixin;

import com.yinfires.icecore.spoilage.*;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(ItemStack.class)
public abstract class ShelfLifeTooltipMixin {
    @Inject(method = "m_41651_(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/TooltipFlag;)Ljava/util/List;", at = @At("RETURN"))
    private void icecore$last(net.minecraft.world.entity.player.Player player, TooltipFlag flag, CallbackInfoReturnable<java.util.List<Component>> cir) {
        ItemStack stack = (ItemStack)(Object)this;
        var lines = cir.getReturnValue();
        if (stack.getItem() instanceof ExpiredFoodItem) {
            if (lines.size() > 1) lines.subList(1, lines.size()).clear();
            lines.add(Component.translatable("tooltip.icecore.expired_food").withStyle(net.minecraft.ChatFormatting.GRAY));
        } else if (player != null && !player.isCreative()
                && (net.minecraft.client.Minecraft.getInstance().gameMode == null
                || net.minecraft.client.Minecraft.getInstance().gameMode.getPlayerMode() != net.minecraft.world.level.GameType.CREATIVE)) {
            int days = ShelfLife.displayRemaining(stack, ClientBoundShelfLifePacket.currentDay);
            if (days >= 0) lines.add(Component.translatable("tooltip.icecore.shelf_life", days));
        }
    }
}
