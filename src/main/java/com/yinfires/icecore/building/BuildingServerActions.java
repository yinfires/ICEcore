package com.yinfires.icecore.building;

import com.yinfires.icecore.feedback.PlayerFeedback;
import com.yinfires.icecore.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

/** Server-authoritative handling for building interaction hooks. */
public final class BuildingServerActions {
    private BuildingServerActions() {
    }

    public static void place(ServerLevel level, ServerPlayer player, BlockHitResult hit,
                             InteractionHand hand) {
        BlockPos clickedPos = hit.getBlockPos();
        Direction face = hit.getDirection();
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof BlockItem)) {
            return;
        }
        net.minecraft.world.item.context.BlockPlaceContext placementContext = new net.minecraft.world.item.context.BlockPlaceContext(
                level, player, hand, stack, hit);
        if (!player.canReach(clickedPos, 1.5D)
                || !BuildingRuntimeCache.isManaged(((BlockItem) stack.getItem()).getBlock().defaultBlockState())) {
            return;
        }
        if (!placementContext.canPlace()) {
            return;
        }
        BlockItem blockItem = (BlockItem) stack.getItem();
        net.minecraft.world.item.context.BlockPlaceContext updatedContext = blockItem.updatePlacementContext(placementContext);
        if (updatedContext == null || !updatedContext.canPlace()) {
            return;
        }
        BlockPos target = updatedContext.getClickedPos();
        BlockState support = level.getBlockState(clickedPos);
        BlockState placementState = blockItem.getBlock().getStateForPlacement(updatedContext);
        if (placementState == null) {
            return;
        }
        if (!BuildingRules.canPlace(level, target, placementState, support, face)) {
            return;
        }
        if (!BuildingStructure.positions(placementState, target).stream().allMatch(part ->
                canOccupy(level, player, placementContext, part,
                        BuildingStructure.stateAt(placementState, target, part)))) {
            return;
        }
        boolean oldMayBuild = player.getAbilities().mayBuild;
        player.getAbilities().mayBuild = true;
        try {
            // Keep the original context for Forge's placement transaction.
            // BlockItem.useOn creates and updates its placement context itself;
            // passing the already-updated context would apply a modded
            // updatePlacementContext implementation twice.
            ForgeHooks.onPlaceItemIntoWorld(placementContext);
        } finally {
            player.getAbilities().mayBuild = oldMayBuild;
        }
    }

    public static boolean remove(ServerLevel level, ServerPlayer player, BlockPos pos,
                                 Direction face, InteractionHand hand) {
        if (player.gameMode.getGameModeForPlayer() != net.minecraft.world.level.GameType.ADVENTURE
                || !isWrench(player.getItemInHand(hand)) || !player.canReach(pos, 1.5D)) {
            return false;
        }
        BlockPos keyPosition = BuildingStructure.primaryPosition(level, pos);
        BlockState state = level.getBlockState(keyPosition);
        if (state.isAir()) {
            return false;
        }
        if (!BuildingRules.canBreak(level, keyPosition, state)) {
            return false;
        }
        ItemStack blockStack = new ItemStack(state.getBlock().asItem());
        if (blockStack.isEmpty()) {
            return false;
        }
        List<ItemStack> returns = new ArrayList<>();
        returns.add(blockStack);
        BlockEntity blockEntity = level.getBlockEntity(keyPosition);
        if (blockEntity != null) {
            collectContainerItems(blockEntity, returns);
        }
        List<BlockPos> structure = BuildingStructure.positions(state, keyPosition);
        if (!canFit(player.getInventory(), returns)) {
            PlayerFeedback.show(player, net.minecraft.network.chat.Component.translatable("icecore.build.inventory_full"));
            return false;
        }
        for (BlockPos part : structure) {
            // The wrench has already copied all container contents into the
            // return list. Remove the block entity first so a modded onRemove
            // implementation cannot emit the same contents as world drops.
            if (level.getBlockEntity(part) != null) {
                level.removeBlockEntity(part);
            }
            level.removeBlock(part, false);
        }
        for (ItemStack returned : returns) {
            player.getInventory().placeItemBackInInventory(returned.copy());
        }
        player.getInventory().setChanged();
        return true;
    }

    public static boolean isWrench(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.WRENCH.get());
    }

    /**
     * Final server-side guard for the vanilla BlockState.use path.  The Forge
     * RightClickBlock handler normally cancels this path, but a failed wrench
     * removal must remain safe even if another handler changes that event after
     * our subscriber runs.
     *
     * <p>The check is hand-agnostic on purpose.  Minecraft.startUseItem loops
     * over both hands: a wrench in one hand returns PASS, so the loop retries
     * the other (empty) hand, whose use-on packet would otherwise reach
     * BlockState.use.  Blocking whenever either hand holds the wrench and the
     * target is managed closes that retry path for both hands.
     */
    public static boolean shouldBlockVanillaBlockUse(Level level, Player player,
                                                      InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)
                || serverPlayer.gameMode.getGameModeForPlayer() != net.minecraft.world.level.GameType.ADVENTURE
                || !holdsWrench(serverPlayer) || hit == null) {
            return false;
        }
        return isManagedTarget(level, hit.getBlockPos());
    }

    /** True when either hand holds the wrench. */
    public static boolean holdsWrench(Player player) {
        return isWrench(player.getMainHandItem()) || isWrench(player.getOffhandItem());
    }

    /**
     * True when the block under the cursor (normalised to its structure key)
     * belongs to a managed list.  Deliberately independent of canBreak: the
     * interaction lock must hold even when a removal would fail on inventory,
     * reach or region.
     */
    public static boolean isManagedTarget(Level level, BlockPos pos) {
        BlockPos keyPosition = BuildingStructure.primaryPosition(level, pos);
        BlockState state = level.getBlockState(keyPosition);
        return !state.isAir() && BuildingRuntimeCache.isManaged(state);
    }

    private static boolean canOccupy(ServerLevel level, ServerPlayer player,
                                     net.minecraft.world.item.context.BlockPlaceContext context,
                                     BlockPos position, BlockState state) {
        net.minecraft.world.item.context.BlockPlaceContext positionContext =
                net.minecraft.world.item.context.BlockPlaceContext.at(context, position, context.getClickedFace());
        return level.getBlockState(position).canBeReplaced(positionContext)
                && level.isUnobstructed(state, position, CollisionContext.of(player));
    }

    private static void collectContainerItems(BlockEntity entity, List<ItemStack> output) {
        List<ItemStack> containerItems = new ArrayList<>();
        if (entity instanceof Container container) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack item = container.getItem(i);
                if (!item.isEmpty()) {
                    output.add(item.copy());
                    containerItems.add(item.copy());
                }
            }
        }
        entity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack item = handler.getStackInSlot(i);
                if (item.isEmpty()) {
                    continue;
                }
                int remaining = item.getCount();
                for (int j = 0; j < containerItems.size() && remaining > 0; j++) {
                    ItemStack existing = containerItems.get(j);
                    if (!ItemStack.isSameItemSameTags(existing, item)) {
                        continue;
                    }
                    int consumed = Math.min(existing.getCount(), remaining);
                    existing.shrink(consumed);
                    remaining -= consumed;
                }
                containerItems.removeIf(ItemStack::isEmpty);
                if (remaining > 0) {
                    output.add(item.copyWithCount(remaining));
                }
            }
        });
    }

    private static boolean canFit(Inventory inventory, List<ItemStack> stacks) {
        NonNullList<ItemStack> simulated = NonNullList.withSize(inventory.items.size(), ItemStack.EMPTY);
        for (int i = 0; i < inventory.items.size(); i++) simulated.set(i, inventory.items.get(i).copy());
        for (ItemStack stack : stacks) {
            int remaining = stack.getCount();
            for (int i = 0; i < simulated.size() && remaining > 0; i++) {
                ItemStack existing = simulated.get(i);
                if (!existing.isEmpty() && ItemStack.isSameItemSameTags(existing, stack)) {
                    int room = Math.min(existing.getMaxStackSize(), stack.getMaxStackSize()) - existing.getCount();
                    int moved = Math.min(room, remaining);
                    existing.grow(moved);
                    remaining -= moved;
                }
            }
            for (int i = 0; i < simulated.size() && remaining > 0; i++) {
                if (simulated.get(i).isEmpty()) {
                    int moved = Math.min(stack.getMaxStackSize(), remaining);
                    simulated.set(i, stack.copyWithCount(moved));
                    remaining -= moved;
                }
            }
            if (remaining > 0) return false;
        }
        return true;
    }

}
