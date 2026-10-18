package com.yinfires.icecore.workstation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "icecore", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TrashCanLeftClickEvents {
    public static final class PutPermission extends PlayerInteractEvent.LeftClickBlock {
        public PutPermission(Player player, BlockPos pos, net.minecraft.core.Direction face) { super(player, pos, face, Action.START); }
    }
    private TrashCanLeftClickEvents() {}
    public static boolean isTrash(Level level, BlockPos pos) {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock())
                .equals(TRASH_ID);
    }
    private static final net.minecraft.resources.ResourceLocation TRASH_ID =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "trash_can");
    public static boolean hasPriorityTool(Player player) {
        return markedTool(player.getMainHandItem()) || markedTool(player.getOffhandItem())
                || player.getMainHandItem().is(com.yinfires.icecore.item.ModItems.WRENCH.get())
                || player.getOffhandItem().is(com.yinfires.icecore.item.ModItems.WRENCH.get());
    }
    private static boolean markedTool(net.minecraft.world.item.ItemStack stack) {
        return stack.is(net.minecraft.world.item.Items.STICK) && stack.hasTag()
                && (!stack.getTag().getString("icecore_adjustment_region").isEmpty()
                || com.yinfires.icecore.compat.cozycafe.range.CozyCafeRangeAdjustmentManager.isMarked(stack, null));
    }
    public static boolean shouldIntercept(Player player) {
        return !player.getMainHandItem().isEmpty() && !player.isSpectator() && !hasPriorityTool(player);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void preventMining(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event instanceof PutPermission) && !event.getLevel().isClientSide && isTrash(event.getLevel(), event.getPos())
                && shouldIntercept(event.getEntity())) event.setCanceled(true);
    }
}
