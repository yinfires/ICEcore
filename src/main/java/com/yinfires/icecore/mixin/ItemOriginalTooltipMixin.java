package com.yinfires.icecore.mixin;

import com.yinfires.icecore.tooltip.ItemTooltipEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/** Filter the item's own body before Forge listeners add contextual information. */
@Mixin(ItemStack.class)
public abstract class ItemOriginalTooltipMixin {
    @Redirect(method = "m_41651_(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/TooltipFlag;)Ljava/util/List;",
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/event/ForgeEventFactory;onItemTooltip(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Ljava/util/List;Lnet/minecraft/world/item/TooltipFlag;)Lnet/minecraftforge/event/entity/player/ItemTooltipEvent;", remap = false), remap = false)
    private ItemTooltipEvent icecore$filterOriginal(ItemStack stack, Player player, List<Component> lines, TooltipFlag flag) {
        ItemTooltipEvents.filterOriginal(stack, player, lines);
        return ForgeEventFactory.onItemTooltip(stack, player, lines, flag);
    }
}
