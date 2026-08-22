package com.yinfires.icecore.building;

import com.yinfires.icecore.ICECore;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ICECore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BuildingEvents {
    private BuildingEvents() {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        BuildingDataManager.get().start(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        BuildingDataManager.get().stop();
    }

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BuildingNetworking.sendRules(player, BuildingDataManager.get().data(), BuildingDataManager.get().revision());
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) AdjustmentModeManager.exit(player);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && AdjustmentModeManager.isActiveInAnyHand(player)) {
            AdjustmentModeManager.exit(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            AdjustmentModeManager.tick(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.level().isClientSide) {
            return;
        }
        InteractionHand hand = event.getHand();
        ItemStack stack = player.getItemInHand(hand);
        if (AdjustmentModeManager.isActiveInAnyHand(player)) {
            event.setCanceled(true);
            AdjustmentModeManager.setSecond(player, event.getPos());
            return;
        }
        if (player.gameMode.getGameModeForPlayer() != GameType.ADVENTURE) {
            return;
        }
        if (BuildingServerActions.isWrench(stack)) {
            event.setCanceled(true);
            BuildingServerActions.remove((net.minecraft.server.level.ServerLevel) player.level(), player,
                    event.getPos(), event.getFace(), hand);
            return;
        }
        // Minecraft.startUseItem retries the other hand when the wrench hand
        // returns PASS, so this event can fire for an empty/non-wrench hand
        // while the wrench is still held.  Block the vanilla interaction on that
        // hand too when the cursor is on a managed target, so a dismantle target
        // such as a door never opens.
        if (BuildingServerActions.holdsWrench(player)
                && BuildingServerActions.isManagedTarget(player.level(), event.getPos())) {
            event.setCanceled(true);
            return;
        }
        if (stack.getItem() instanceof BlockItem) {
            BlockState placing = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
            if (BuildingRuntimeCache.isManaged(placing)) {
                event.setCanceled(true);
                BuildingServerActions.place((net.minecraft.server.level.ServerLevel) player.level(), player,
                        event.getHitVec(), hand);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onRightClickFinal(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.level().isClientSide
                || player.gameMode.getGameModeForPlayer() != GameType.ADVENTURE) {
            return;
        }
        if (BuildingServerActions.holdsWrench(player)
                && BuildingServerActions.isManagedTarget(player.level(), event.getPos())) {
            // Keep later compatibility handlers from reopening vanilla block
            // interaction after the dismantle attempt, for either hand.
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) {
            if (AdjustmentModeManager.isActiveInAnyHand(player)) {
                event.setCanceled(true);
            }
            return;
        }
        if (AdjustmentModeManager.isActiveInAnyHand(player)) {
            event.setCanceled(true);
            event.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);
            event.setUseItem(net.minecraftforge.eventbus.api.Event.Result.DENY);
            AdjustmentModeManager.setFirst(player, event.getPos());
            return;
        }
        if (player.gameMode.getGameModeForPlayer() != GameType.ADVENTURE) return;
        if (BuildingServerActions.isWrench(player.getItemInHand(event.getHand()))) {
            event.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);
            event.setUseItem(net.minecraftforge.eventbus.api.Event.Result.DENY);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBreakPlace(BlockEvent.BreakEvent event) {
        // Keep the normal survival/adventure break event cancellable for managed blocks. The wrench path
        // uses the explicit server action and therefore never reaches the vanilla break pipeline.
        if (event.getPlayer() instanceof ServerPlayer player
                && AdjustmentModeManager.isActiveInAnyHand(player)) {
            event.setCanceled(true);
            return;
        }
        if (event.getPlayer() instanceof ServerPlayer player
                && player.gameMode.getGameModeForPlayer() == GameType.ADVENTURE
                && BuildingRuntimeCache.isManaged(event.getState())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        BuildingCommands.register(event.getDispatcher());
    }
}
