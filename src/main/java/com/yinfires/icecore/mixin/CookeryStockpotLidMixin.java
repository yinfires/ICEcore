package com.yinfires.icecore.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ToolAction;
import org.spongepowered.asm.mixin.Mixin;

/** Overrides inherited shield behavior only on Cookery's stockpot lid. */
@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopecookery.item.StockpotLidItem", remap = false)
public abstract class CookeryStockpotLidMixin extends ShieldItem {
    protected CookeryStockpotLidMixin(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 0;
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction action) {
        return false;
    }
}
