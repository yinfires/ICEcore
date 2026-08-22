package com.yinfires.icecore.item;

import com.yinfires.icecore.building.BuildingServerActions;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.List;

public final class WrenchItem extends Item {
    public WrenchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel().isClientSide
                && context.getLevel() instanceof ServerLevel level
                && context.getPlayer() instanceof ServerPlayer player
                && BuildingServerActions.remove(level, player, context.getClickedPos(),
                context.getClickedFace(), context.getHand())) {
            return InteractionResult.SUCCESS;
        }
        // The normal Forge RightClickBlock handler owns the adventure-mode
        // action and cancels vanilla block interaction.  Returning PASS here
        // keeps non-adventure use and failed validation from claiming a block
        // action when the event bridge is not involved.
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("item.icecore.wrench.tooltip"));
    }
}
