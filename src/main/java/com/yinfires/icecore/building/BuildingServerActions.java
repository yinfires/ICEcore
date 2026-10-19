package com.yinfires.icecore.building;

import com.yinfires.icecore.feedback.PlayerFeedback;
import com.yinfires.icecore.item.ModItems;
import com.yinfires.icecore.mixing.ContainerContentAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-authoritative handling for building interaction hooks. */
public final class BuildingServerActions {
    private static final long FORCE_CONFIRM_TICKS = 60L;
    private static final Map<UUID, RemovalConfirmation> REMOVAL_CONFIRMATIONS = new ConcurrentHashMap<>();
    private record RemovalConfirmation(net.minecraft.resources.ResourceKey<Level> dimension, BlockPos position,
                                       BlockState state, int fingerprint, long expiresAt) {}
    private BuildingServerActions() {
    }

    /** Null means ordinary interaction may proceed; a context claims this click. */
    public static net.minecraft.world.item.context.BlockPlaceContext preparePlacement(
            ServerLevel level, ServerPlayer player, BlockHitResult hit, InteractionHand hand) {
        BlockPos clickedPos = hit.getBlockPos();
        Direction face = hit.getDirection();
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof BlockItem)) {
            return null;
        }
        net.minecraft.world.item.context.BlockPlaceContext placementContext = new net.minecraft.world.item.context.BlockPlaceContext(
                level, player, hand, stack, hit);
        if (!player.canReach(clickedPos, 1.5D)
                || !BuildingRuntimeCache.isManaged(((BlockItem) stack.getItem()).getBlock().defaultBlockState())) {
            return null;
        }
        if (!placementContext.canPlace()) {
            return null;
        }
        BlockItem blockItem = (BlockItem) stack.getItem();
        net.minecraft.world.item.context.BlockPlaceContext updatedContext = blockItem.updatePlacementContext(placementContext);
        if (updatedContext == null || !updatedContext.canPlace()) {
            return null;
        }
        BlockPos target = updatedContext.getClickedPos();
        BlockState support = level.getBlockState(clickedPos);
        BlockState placementState = blockItem.getBlock().getStateForPlacement(updatedContext);
        if (placementState == null || !placementState.canSurvive(level, target)) {
            return null;
        }
        if (!BuildingRules.canPlace(level, target, placementState, support, face)) {
            return null;
        }
        if (!BuildingStructure.positions(placementState, target).stream().allMatch(part ->
                canOccupy(level, player, placementContext, part,
                        BuildingStructure.stateAt(placementState, target, part)))) {
            return null;
        }
        return placementContext;
    }

    public static void executePlacement(ServerPlayer player,
            net.minecraft.world.item.context.BlockPlaceContext placementContext) {
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
        if (BuildingRules.hasDependents(level, keyPosition)) {
            clearRemovalConfirmation(player);
            PlayerFeedback.show(player, net.minecraft.network.chat.Component.translatable("icecore.build.block_depended_on"));
            return false;
        }
        ItemStack blockStack = new ItemStack(state.getBlock().asItem());
        if (blockStack.isEmpty()) {
            return false;
        }
        int contentFingerprint = structureContentFingerprint(level, state, keyPosition);
        boolean hasContent = structureHasContent(level, state, keyPosition);
        if (hasContent) {
            if (!player.isShiftKeyDown()) {
                clearRemovalConfirmation(player);
                PlayerFeedback.show(player, net.minecraft.network.chat.Component.translatable("icecore.build.container_not_empty"));
                return false;
            }
            RemovalConfirmation confirmation = REMOVAL_CONFIRMATIONS.get(player.getUUID());
            boolean confirmed = confirmation != null
                    && confirmation.dimension().equals(level.dimension())
                    && confirmation.position().equals(keyPosition)
                    && confirmation.state().equals(state)
                    && confirmation.fingerprint() == contentFingerprint
                    && level.getGameTime() <= confirmation.expiresAt();
            if (!confirmed) {
                REMOVAL_CONFIRMATIONS.put(player.getUUID(), new RemovalConfirmation(level.dimension(), keyPosition,
                        state, contentFingerprint, level.getGameTime() + FORCE_CONFIRM_TICKS));
                PlayerFeedback.show(player, net.minecraft.network.chat.Component.translatable("icecore.build.force_remove_confirm"));
                return false;
            }
        }
        clearRemovalConfirmation(player);
        List<ItemStack> returns = List.of(blockStack);
        List<BlockPos> structure = BuildingStructure.positions(state, keyPosition);
        if (com.yinfires.icecore.fridge.FridgePairing.integrated(state)
                && !com.yinfires.icecore.fridge.FridgePairing.matches(state, level.getBlockState(keyPosition.above()))) {
            return false;
        }
        if (state.getBlock() instanceof com.yinfires.icecore.shelf.ShelfBlock
                && !com.yinfires.icecore.shelf.ShelfBlock.paired(state,level.getBlockState(keyPosition.above()))) return false;
        if (!canFit(player.getInventory(), returns)) {
            PlayerFeedback.show(player, net.minecraft.network.chat.Component.translatable("icecore.build.inventory_full"));
            return false;
        }
        // Remove every entity before any structural block. Integrated blocks may remove their
        // partner in onRemove; clearing only the currently clicked half would leak partner loot.
        for (BlockPos part : structure) {
            if (com.yinfires.icecore.fridge.FridgePairing.integrated(state)
                    && level.getBlockEntity(part) instanceof net.blay09.mods.cookingforblockheads.tile.FridgeBlockEntity fridge) {
                fridge.getContainer().clearContent();
                fridge.hasIceUpgrade = false;
                fridge.hasPreservationUpgrade = false;
            }
            if (level.getBlockEntity(part) != null) {
                level.removeBlockEntity(part);
            }
        }
        for (BlockPos part : structure) {
            level.removeBlock(part, false);
        }
        for (ItemStack returned : returns) {
            player.getInventory().placeItemBackInInventory(returned.copy());
        }
        player.getInventory().setChanged();
        return true;
    }

    public static void clearRemovalConfirmation(ServerPlayer player) {
        REMOVAL_CONFIRMATIONS.remove(player.getUUID());
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
     * has dismantling permission here. Inventory and content confirmation are
     * execution checks and must not release an already claimed dismantle click.
     */
    public static boolean isManagedTarget(Level level, BlockPos pos) {
        BlockPos keyPosition = BuildingStructure.primaryPosition(level, pos);
        BlockState state = level.getBlockState(keyPosition);
        return !state.isAir() && BuildingRules.canBreak(level, keyPosition, state);
    }

    private static boolean canOccupy(ServerLevel level, ServerPlayer player,
                                     net.minecraft.world.item.context.BlockPlaceContext context,
                                     BlockPos position, BlockState state) {
        net.minecraft.world.item.context.BlockPlaceContext positionContext =
                net.minecraft.world.item.context.BlockPlaceContext.at(context, position, context.getClickedFace());
        return level.getBlockState(position).canBeReplaced(positionContext)
                && (!(com.yinfires.icecore.fridge.FridgePairing.integrated(state) || state.getBlock() instanceof com.yinfires.icecore.shelf.ShelfBlock)
                    || (!level.isOutsideBuildHeight(position) && level.getWorldBorder().isWithinBounds(position)
                        && level.mayInteract(player, position)))
                && level.isUnobstructed(state, position, CollisionContext.of(player));
    }

    private static boolean structureHasContent(ServerLevel level, BlockState state, BlockPos primary) {
        for (BlockPos part : BuildingStructure.positions(state, primary)) {
            BlockEntity entity = level.getBlockEntity(part);
            if (entity != null && hasContent(entity)) return true;
        }
        return false;
    }

    private static boolean hasContent(BlockEntity entity) {
        if (entity instanceof ContainerContentAccess access && access.icecore$hasContainerContent()) return true;
        if (entity instanceof Container container) {
            for (int i = 0; i < container.getContainerSize(); i++) if (!container.getItem(i).isEmpty()) return true;
        }
        if (entity.getCapability(ForgeCapabilities.ITEM_HANDLER).map(BuildingServerActions::hasItems).orElse(false)) return true;
        return entity.getCapability(ForgeCapabilities.FLUID_HANDLER).map(BuildingServerActions::hasFluids).orElse(false);
    }

    private static boolean hasItems(IItemHandler handler) {
        for (int i = 0; i < handler.getSlots(); i++) if (!handler.getStackInSlot(i).isEmpty()) return true;
        return false;
    }

    private static boolean hasFluids(IFluidHandler handler) {
        for (int i = 0; i < handler.getTanks(); i++) if (!handler.getFluidInTank(i).isEmpty()) return true;
        return false;
    }

    private static int structureContentFingerprint(ServerLevel level, BlockState state, BlockPos primary) {
        int hash = 1;
        for (BlockPos part : BuildingStructure.positions(state, primary)) {
            BlockEntity entity = level.getBlockEntity(part);
            if (entity == null) continue;
            if (entity instanceof ContainerContentAccess access) {
                hash = 31 * hash + access.icecore$contentFingerprint();
                continue;
            }
            if (entity instanceof Container container) {
                for (int i = 0; i < container.getContainerSize(); i++) hash = 31 * hash + stackFingerprint(container.getItem(i));
            }
            IItemHandler items = entity.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
            if (items != null) for (int i = 0; i < items.getSlots(); i++) hash = 31 * hash + stackFingerprint(items.getStackInSlot(i));
            IFluidHandler fluids = entity.getCapability(ForgeCapabilities.FLUID_HANDLER).orElse(null);
            if (fluids != null) for (int i = 0; i < fluids.getTanks(); i++) hash = 31 * hash + fluids.getFluidInTank(i).hashCode();
        }
        return hash;
    }

    private static int stackFingerprint(ItemStack stack) {
        return stack.isEmpty() ? 0 : stack.save(new CompoundTag()).hashCode();
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
