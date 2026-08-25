package com.yinfires.icecore.building;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.yinfires.icecore.ICECore;
import com.yinfires.icecore.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Mod.EventBusSubscriber(modid = ICECore.MOD_ID, value = net.minecraftforge.api.distmarker.Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BuildingClientEvents {
    private static final float INVALID_PREVIEW_RED = 0.78F;
    private static final float INVALID_PREVIEW_GREEN = 0.025F;
    private static final float INVALID_PREVIEW_BLUE = 0.025F;
    private static final float INVALID_PREVIEW_ALPHA = 0.62F;
    private static int suppressPlacementPreviewTicks;
    private static InteractionHand suppressPlacementPreviewHand;
    private static ItemStack suppressPlacementPreviewStack = ItemStack.EMPTY;
    private static PoseStack lastPreviewPoseStack;
    private static WrenchPreview desiredWrenchPreview;
    private static WrenchPreview committedWrenchPreview;
    private static WrenchPreview queuedWrenchPreview;
    private static long desiredWrenchGeneration;
    private static final MultiBufferSource.BufferSource PREVIEW_BUFFER_SOURCE =
            MultiBufferSource.immediate(new BufferBuilder(RenderType.translucent().bufferSize()));

    private BuildingClientEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        // Keep the server packet authoritative while preventing local block use.
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getEntity() != minecraft.player || minecraft.player == null
                || minecraft.gameMode == null) {
            return;
        }
        if (isAdjustmentStick(event.getItemStack())
                || isAdjustmentStick(minecraft.player.getMainHandItem())
                || isAdjustmentStick(minecraft.player.getOffhandItem())) {
            // Keep the vanilla use-on packet flowing so the server can record pos2.
            // Forge treats useItem=DENY as a request to stop before creating the
            // ServerboundUseItemOnPacket, so deny the local block branch and
            // explicitly allow the item branch to preserve the packet path.
            event.setCanceled(false);
            event.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);
            event.setUseItem(net.minecraftforge.eventbus.api.Event.Result.ALLOW);
            return;
        }
        if (minecraft.gameMode.getPlayerMode() != GameType.ADVENTURE) return;
        ItemStack stack = event.getItemStack();
        // Hand-agnostic: the two-hand retry can fire this event for the empty
        // hand while the wrench is in the other hand.  Cancel whenever a wrench
        // is held and the cursor is on a managed target so BlockState.use is
        // never called locally.  The outer useItemOn still sends the packet, so
        // the server-side dismantle handler stays authoritative.
        if ((stack.is(ModItems.WRENCH.get())
                || minecraft.player.getMainHandItem().is(ModItems.WRENCH.get())
                || minecraft.player.getOffhandItem().is(ModItems.WRENCH.get()))
                && wrenchInteractionTarget(minecraft, event.getHitVec()) != null) {
            event.setCanceled(true);
            return;
        }
        if (stack.getItem() instanceof BlockItem bi && BuildingClientRules.isManaged(bi.getBlock().defaultBlockState())) {
            event.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);
            // Only arm the anti-phantom suppression when the placement is
            // actually valid.  The suppression blanks the preview for a few
            // ticks to hide the stale ghost that lingers at the just-placed
            // spot while the server ack/inventory update is in flight.  On an
            // invalid placement (red preview) the server places nothing, the
            // held stack never changes, and there is no phantom to hide, so
            // arming it would only blink the red preview out for ~150ms - the
            // "disappears, worst on failure" flash.  Skipping it there keeps the
            // red preview continuously visible.
            PlacementPreview placement = computePlacementPreview(
                    minecraft, stack, event.getHand(), event.getHitVec());
            if (placement != null && placement.allowed()) {
                suppressPlacementPreviewHand = event.getHand();
                suppressPlacementPreviewStack = stack.copy();
                suppressPlacementPreviewTicks = 3;
                // Un-hide the target coordinates synchronously, before vanilla's
                // post-placement chunk rebuild runs.  Otherwise the just-placed
                // block would be rebuilt while still marked hidden (invisible),
                // then pop in a frame later when the tick clears the hidden set.
                BuildingClientRenderState.clear();
            }
        }
    }

    /**
     * Re-assert the client-side interaction lock if another compatibility handler
     * changes the shared event after the high-priority handler.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onRightClickFinal(PlayerInteractEvent.RightClickBlock event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getEntity() != minecraft.player || minecraft.player == null
                || minecraft.gameMode == null) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (isAdjustmentStick(stack)
                || isAdjustmentStick(minecraft.player.getMainHandItem())
                || isAdjustmentStick(minecraft.player.getOffhandItem())) {
            event.setCanceled(false);
            event.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);
            event.setUseItem(net.minecraftforge.eventbus.api.Event.Result.ALLOW);
            return;
        }
        if (minecraft.gameMode.getPlayerMode() == GameType.ADVENTURE
                && (stack.is(ModItems.WRENCH.get())
                || minecraft.player.getMainHandItem().is(ModItems.WRENCH.get())
                || minecraft.player.getOffhandItem().is(ModItems.WRENCH.get()))
                && wrenchInteractionTarget(minecraft, event.getHitVec()) != null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onRightClickItemFinal(PlayerInteractEvent.RightClickItem event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getEntity() != minecraft.player || minecraft.player == null
                || minecraft.gameMode == null
                || minecraft.gameMode.getPlayerMode() != GameType.ADVENTURE) {
            return;
        }
        if (event.getItemStack().is(ModItems.WRENCH.get())) {
            event.setCanceled(false);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        // Keep the mouse event flowing into Forge's LeftClickBlock hook: vanilla uses that
        // hook to create START_DESTROY_BLOCK, which the server uses to record pos1. The
        // client-side result flags below stop the local attack without suppressing that packet.
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getEntity() != minecraft.player || minecraft.player == null) {
            return;
        }
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) {
            if (isAdjustmentStick(event.getItemStack())
                    || isAdjustmentStick(minecraft.player.getMainHandItem())
                    || isAdjustmentStick(minecraft.player.getOffhandItem())) {
                denyAdjustmentLeftClick(event, minecraft);
            }
            return;
        }
        if (isAdjustmentStick(event.getItemStack())
                || isAdjustmentStick(minecraft.player.getMainHandItem())
                || isAdjustmentStick(minecraft.player.getOffhandItem())) {
            denyAdjustmentLeftClick(event, minecraft);
        }
    }

    private static void denyAdjustmentLeftClick(PlayerInteractEvent.LeftClickBlock event, Minecraft minecraft) {
        if (minecraft.gameMode.getPlayerMode() == GameType.CREATIVE) {
            // Forge's creative branch still sends START_DESTROY_BLOCK after a canceled
            // LeftClickBlock event, while skipping the local break call.
            event.setCanceled(true);
        } else {
            // Survival/adventure use these result flags to keep the packet flowing while
            // preventing local attack/break logic.
            event.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);
            event.setUseItem(net.minecraftforge.eventbus.api.Event.Result.DENY);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onRenderHighlight(RenderHighlightEvent.Block event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null
                || isAdjustmentStick(minecraft.player.getMainHandItem())
                || isAdjustmentStick(minecraft.player.getOffhandItem())
                || minecraft.gameMode.getPlayerMode() != GameType.ADVENTURE) {
            return;
        }
        BlockHitResult hit = event.getTarget();
        if (minecraft.player.distanceToSqr(hit.getLocation().x, hit.getLocation().y, hit.getLocation().z)
                > Math.pow(minecraft.gameMode.getPickRange(), 2.0D)) {
            return;
        }
        InteractionHand hand = previewHand(minecraft.player);
        if (hand == null) {
            return;
        }
        ItemStack held = minecraft.player.getItemInHand(hand);
        if (held.is(ModItems.WRENCH.get())) {
            // Only replace the vanilla outline when a real dismantle preview is
            // available.  Invalid/unknown targets keep the normal outline and do
            // not claim that the wrench can operate there.
            if (wrenchPreview(minecraft, hit) != null) {
                event.setCanceled(true);
            }
            return;
        }
        if (held.getItem() instanceof BlockItem blockItem
                && BuildingClientRules.isManaged(blockItem.getBlock().defaultBlockState())) {
            // The preview replaces the vanilla hit outline, including when the
            // placement preview is red because one of the placement checks fails.
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onRenderHighlightFinal(RenderHighlightEvent.Block event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null
                || minecraft.gameMode.getPlayerMode() != GameType.ADVENTURE
                || isAdjustmentStick(minecraft.player.getMainHandItem())
                || isAdjustmentStick(minecraft.player.getOffhandItem())) {
            return;
        }
        InteractionHand hand = previewHand(minecraft.player);
        if (hand != null && minecraft.player.getItemInHand(hand).is(ModItems.WRENCH.get())
                && wrenchPreview(minecraft, event.getTarget()) != null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        // A render pass owns one PoseStack.  Forge/compatibility render paths
        // can dispatch the same stage more than once for that pass; submit the
        // preview only once so an invalid model cannot be blended twice at the
        // same empty target position.
        if (lastPreviewPoseStack == event.getPoseStack()) {
            return;
        }
        lastPreviewPoseStack = event.getPoseStack();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null) return;
        // Recompute the hidden-coordinate snapshot per frame from the same
        // target the preview uses.  Doing this on the client tick (20Hz) instead
        // of per-frame (60Hz+) left a window where the cursor had already moved
        // to a new target but the hidden set still pointed at the old one, so
        // the old block briefly vanished as a hole while its preview drew
        // elsewhere.  update() short-circuits when the key set is unchanged, so
        // per-frame calls only mark chunks dirty when the target actually moves.
        refreshHiddenRenderState(minecraft);
        if (BuildingClientRenderState.committedGeneration() == desiredWrenchGeneration) {
            committedWrenchPreview = desiredWrenchPreview;
            if (!Objects.equals(queuedWrenchPreview, desiredWrenchPreview)) {
                WrenchPreview queued = queuedWrenchPreview;
                queuedWrenchPreview = desiredWrenchPreview;
                requestWrenchPreview(queued);
            }
        }
        if (desiredWrenchPreview != null && !Objects.equals(desiredWrenchPreview, committedWrenchPreview)) {
            renderWrenchPreviewModel(event.getPoseStack(), minecraft, desiredWrenchPreview, true);
        }
        renderAdjustmentBorder(event.getPoseStack(), minecraft);
        if (isAdjustmentStick(minecraft.player.getMainHandItem())
                || isAdjustmentStick(minecraft.player.getOffhandItem())) {
            clearAndRenderWrench(event.getPoseStack(), minecraft);
            return;
        }
        if (minecraft.gameMode.getPlayerMode() != GameType.ADVENTURE) {
            clearAndRenderWrench(event.getPoseStack(), minecraft);
            return;
        }
        if (minecraft.hitResult == null || minecraft.player.distanceToSqr(minecraft.hitResult.getLocation().x,
                minecraft.hitResult.getLocation().y, minecraft.hitResult.getLocation().z)
                > Math.pow(minecraft.gameMode.getPickRange(), 2.0D)) {
            clearAndRenderWrench(event.getPoseStack(), minecraft);
            return;
        }
        InteractionHand heldHand = previewHand(minecraft.player);
        if (heldHand == null) {
            clearAndRenderWrench(event.getPoseStack(), minecraft);
            return;
        }
        ItemStack held = minecraft.player.getItemInHand(heldHand);
        BlockItem blockItem = held.getItem() instanceof BlockItem bi ? bi : null;
        if (!(minecraft.hitResult instanceof BlockHitResult hit)) {
            clearAndRenderWrench(event.getPoseStack(), minecraft);
            return;
        }
        if (held.is(ModItems.WRENCH.get())) {
            if (committedWrenchPreview != null) {
                renderWrenchPreviewModel(event.getPoseStack(), minecraft, committedWrenchPreview, false);
            }
            return;
        }
        if (committedWrenchPreview != null) {
            renderWrenchPreviewModel(event.getPoseStack(), minecraft, committedWrenchPreview, false);
            return;
        }
        if (blockItem == null || !BuildingClientRules.isManaged(blockItem.getBlock().defaultBlockState())) {
            clearPreviewState();
            return;
        }
        if (isPlacementPreviewSuppressed(minecraft, held, heldHand)) {
            clearPreviewState();
            return;
        }
        PlacementPreview placement = computePlacementPreview(minecraft, held, heldHand, hit);
        if (placement == null) {
            clearPreviewState();
            return;
        }
        if (placement.allowed()) {
            renderPlacementPreview(event.getPoseStack(), minecraft, placement.origin(), placement.state(),
                    RenderType.translucent());
        } else {
            renderInvalidPreview(event.getPoseStack(), minecraft, placement.origin(), placement.state(),
                    RenderType.translucent(), INVALID_PREVIEW_ALPHA);
        }
    }

    private static void renderWrenchPreviewModel(PoseStack poseStack, Minecraft minecraft,
                                                  WrenchPreview preview, boolean desired) {
        // Identical to the failed placement preview: same model renderer,
        // vertex tint, alpha and vanilla RenderType.translucent() batch.  The
        // original block is already removed from the chunk mesh by
        // RenderChunkRegionMixin, so the translucent model blends over the real
        // terrain behind the target exactly like the placement failure preview.
        // No depth mask is used: a depth-only near-offset mask would cull the
        // terrain behind the block and force the translucent model to blend
        // with the cleared sky instead.
        MultiBufferSource.BufferSource bufferSource = previewBufferSource();
        MultiBufferSource previewBuffers = new PreviewBufferSource(
                bufferSource, INVALID_PREVIEW_RED, INVALID_PREVIEW_GREEN, INVALID_PREVIEW_BLUE,
                INVALID_PREVIEW_ALPHA, true);
        for (BlockPos previewPos : previewPositions(preview.state(), preview.position())) {
            boolean render = desired
                    ? BuildingClientRenderState.shouldRenderDesired(previewPos)
                    : BuildingClientRenderState.shouldRenderCommitted(previewPos);
            if (render) {
                renderGhost(poseStack, previewPos,
                        BuildingStructure.stateAt(preview.state(), preview.position(), previewPos),
                        previewBuffers, RenderType.translucent());
            }
        }
        bufferSource.endBatch(RenderType.translucent());
    }

    private static WrenchPreview wrenchPreview(Minecraft minecraft, BlockHitResult hit) {
        BlockPos position = BuildingStructure.primaryPosition(minecraft.level, hit.getBlockPos());
        BlockState state = minecraft.level.getBlockState(position);
        if (state.isAir() || !BuildingClientRules.canBreak(minecraft.level, position, state)) {
            return null;
        }
        return new WrenchPreview(position, state);
    }

    /**
     * Finds the managed target whose interaction must be locked on the client.
     * This deliberately does not call canBreak: inventory capacity, server-side
     * reach validation, and other final removal checks must never allow vanilla
     * block use to run while a wrench target is under the cursor.
     */
    private static WrenchPreview wrenchInteractionTarget(Minecraft minecraft, BlockHitResult hit) {
        if (minecraft.level == null || hit == null) {
            return null;
        }
        BlockPos position = BuildingStructure.primaryPosition(minecraft.level, hit.getBlockPos());
        BlockState state = minecraft.level.getBlockState(position);
        if (state.isAir() || !BuildingClientRules.isManaged(state)) {
            return null;
        }
        return new WrenchPreview(position, state);
    }

    /**
     * Hand-agnostic on purpose.  Minecraft.startUseItem loops over both hands
     * and retries the other hand when the wrench hand returns PASS.  If this
     * only matched the current hand, the retry on the empty/off hand would run
     * the local BlockState.use prediction (e.g. a door swinging open).  Locking
     * on "either hand holds the wrench and the cursor is on a managed target"
     * stops the prediction for both hands.
     */
    public static boolean shouldBlockWrenchBlockUse(net.minecraft.client.player.LocalPlayer player,
                                                     InteractionHand hand, BlockHitResult hit) {
        Minecraft minecraft = Minecraft.getInstance();
        return player == minecraft.player
                && minecraft.gameMode != null
                && minecraft.gameMode.getPlayerMode() == GameType.ADVENTURE
                && (player.getMainHandItem().is(ModItems.WRENCH.get())
                || player.getOffhandItem().is(ModItems.WRENCH.get()))
                && wrenchInteractionTarget(minecraft, hit) != null;
    }

    private record WrenchPreview(BlockPos position, BlockState state) {
    }

    /** Normalized placement preview for a managed block item at a hit target. */
    private record PlacementPreview(BlockPos origin, BlockState state, boolean allowed) {
    }

    /**
     * Computes the normalized placement preview (key position, structure state
     * and whether every placement check passes) for a managed block item.  Shared
     * by the render pass, the hidden-coordinate refresh and the placement click so
     * all three agree on the same target and validity without recomputing it three
     * different ways.  Returns null when the held item is not a managed block.
     */
    private static PlacementPreview computePlacementPreview(Minecraft minecraft, ItemStack held,
                                                            InteractionHand hand, BlockHitResult hit) {
        if (minecraft.level == null || minecraft.player == null) {
            return null;
        }
        if (!(held.getItem() instanceof BlockItem blockItem)
                || !BuildingClientRules.isManaged(blockItem.getBlock().defaultBlockState())) {
            return null;
        }
        boolean placementStateValid = true;
        BlockPlaceContext context = new BlockPlaceContext(minecraft.level, minecraft.player, hand, held, hit);
        BlockPlaceContext updatedContext = blockItem.updatePlacementContext(context);
        BlockPlaceContext placementContext = updatedContext == null ? context : updatedContext;
        BlockPos pos = updatedContext == null ? context.getClickedPos() : updatedContext.getClickedPos();
        BlockState state = updatedContext == null ? null : blockItem.getBlock().getStateForPlacement(updatedContext);
        if (state == null) {
            state = blockItem.getBlock().defaultBlockState();
            placementStateValid = false;
        }
        if (updatedContext == null || !updatedContext.canPlace()) {
            placementStateValid = false;
        }
        BlockPos normalized = BuildingStructure.primaryPosition(state, pos);
        if (!normalized.equals(pos)) {
            state = BuildingStructure.primaryState(state);
            pos = normalized;
        }
        final BlockState previewState = state;
        final BlockPos previewOrigin = pos;
        boolean allowed = placementStateValid
                && !previewState.isAir()
                && previewState.canSurvive(minecraft.level, previewOrigin)
                && BuildingClientRules.canPlace(minecraft.level, previewOrigin, previewState,
                minecraft.level.getBlockState(hit.getBlockPos()), hit.getDirection())
                && BuildingStructure.positions(previewState, previewOrigin).stream().allMatch(previewPos -> canOccupy(
                        minecraft, previewPos, BuildingStructure.stateAt(previewState, previewOrigin, previewPos),
                        placementContext));
        return new PlacementPreview(previewOrigin, previewState, allowed);
    }

    private static Set<BlockPos> previewPositions(BlockState state, BlockPos origin) {
        if (state == null || state.isAir()) {
            return Set.of();
        }
        return new HashSet<>(BuildingStructure.positions(state, origin));
    }

    private static boolean canOccupy(Minecraft minecraft, BlockPos origin, BlockState state,
                                     BlockPlaceContext placementContext) {
        if (placementContext == null) {
            return false;
        }
        if (!minecraft.level.getBlockState(origin).canBeReplaced(
                BlockPlaceContext.at(placementContext, origin, placementContext.getClickedFace()))) {
            return false;
        }
        if (!minecraft.level.isUnobstructed(state, origin, CollisionContext.of(minecraft.player))) {
            return false;
        }
        return true;
    }

    private static InteractionHand previewHand(net.minecraft.client.player.LocalPlayer player) {
        if (isPreviewItem(player.getMainHandItem())) {
            return InteractionHand.MAIN_HAND;
        }
        if (isPreviewItem(player.getOffhandItem())) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    private static boolean isPreviewItem(ItemStack stack) {
        if (stack.is(ModItems.WRENCH.get())) return true;
        return stack.getItem() instanceof BlockItem blockItem
                && BuildingClientRules.isManaged(blockItem.getBlock().defaultBlockState());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (suppressPlacementPreviewTicks > 0) {
            suppressPlacementPreviewTicks--;
            if (suppressPlacementPreviewTicks == 0) {
                suppressPlacementPreviewHand = null;
                suppressPlacementPreviewStack = ItemStack.EMPTY;
            }
        }
        if (minecraft.gameMode == null || minecraft.player == null) {
            clearPreviewState();
            return;
        }
        // Hidden-coordinate refresh moved to the render path (onRender) so it
        // tracks the drawn preview frame-accurately.  The tick handler only
        // advances the placement-suppression window and adjustment cleanup.
        if (!isAdjustmentStick(minecraft.player.getMainHandItem())
                && !isAdjustmentStick(minecraft.player.getOffhandItem())) {
            return;
        }
        if (minecraft.gameMode.isDestroying()) {
            minecraft.gameMode.stopDestroyBlock();
        }
    }

    /**
     * Updates the render-only hidden-coordinate snapshot from the current
     * preview target.  Called once per frame from {@link #onRender} so the
     * desired target. The wrench path commits its visible model only after all
     * affected section meshes have uploaded, so the model and terrain change as
     * one transaction instead of exposing an asynchronous transition frame.
     */
    private static void refreshHiddenRenderState(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null
                || minecraft.gameMode.getPlayerMode() != GameType.ADVENTURE
                || isAdjustmentStick(minecraft.player.getMainHandItem())
                || isAdjustmentStick(minecraft.player.getOffhandItem())
                || minecraft.hitResult == null
                || !(minecraft.hitResult instanceof BlockHitResult hit)
                || minecraft.player.distanceToSqr(hit.getLocation().x, hit.getLocation().y, hit.getLocation().z)
                > Math.pow(minecraft.gameMode.getPickRange(), 2.0D)) {
            clearPreviewState();
            return;
        }
        InteractionHand hand = previewHand(minecraft.player);
        if (hand == null) {
            clearPreviewState();
            return;
        }
        ItemStack held = minecraft.player.getItemInHand(hand);
        if (held.is(ModItems.WRENCH.get())) {
            WrenchPreview preview = wrenchPreview(minecraft, hit);
            requestWrenchPreview(preview);
            return;
        }
        if (desiredWrenchPreview != null || committedWrenchPreview != null) {
            requestWrenchPreview(null);
            return;
        }
        if (isPlacementPreviewSuppressed(minecraft, held, hand)) {
            BuildingClientRenderState.clear();
            return;
        }
        PlacementPreview placement = computePlacementPreview(minecraft, held, hand, hit);
        if (placement == null) {
            BuildingClientRenderState.clear();
            return;
        }
        BuildingClientRenderState.update(previewPositions(placement.state(), placement.origin()));
    }

    private static void requestWrenchPreview(WrenchPreview preview) {
        if (BuildingClientRenderState.transactionPending()) {
            queuedWrenchPreview = preview;
            return;
        }
        if (Objects.equals(desiredWrenchPreview, preview)) return;
        desiredWrenchPreview = preview;
        queuedWrenchPreview = preview;
        desiredWrenchGeneration = BuildingClientRenderState.requestWrench(preview == null
                ? Set.of() : previewPositions(preview.state(), preview.position()));
    }

    private static void clearPreviewState() {
        if (desiredWrenchPreview != null || committedWrenchPreview != null) {
            requestWrenchPreview(null);
        } else {
            BuildingClientRenderState.clear();
        }
    }

    private static void clearAndRenderWrench(PoseStack poseStack, Minecraft minecraft) {
        clearPreviewState();
        if (committedWrenchPreview != null) {
            renderWrenchPreviewModel(poseStack, minecraft, committedWrenchPreview, false);
        }
    }

    private static boolean isPlacementPreviewSuppressed(Minecraft minecraft, ItemStack held, InteractionHand hand) {
        if (suppressPlacementPreviewTicks <= 0 || suppressPlacementPreviewHand == null) {
            return false;
        }
        if (minecraft.player == null || minecraft.player.getUseItemRemainingTicks() != 0) {
            return false;
        }
        ItemStack originalHand = minecraft.player.getItemInHand(suppressPlacementPreviewHand);
        boolean changed = !ItemStack.isSameItemSameTags(originalHand, suppressPlacementPreviewStack)
                || originalHand.getCount() != suppressPlacementPreviewStack.getCount();
        if (changed || hand != suppressPlacementPreviewHand) {
            // Consume the acknowledgement window on the first frame that
            // observes a changed stack or a hand switch.  That frame remains
            // hidden, preventing a replacement item from flashing at the old
            // target; the next frame can preview the new item normally.
            suppressPlacementPreviewTicks = 0;
            suppressPlacementPreviewHand = null;
            suppressPlacementPreviewStack = ItemStack.EMPTY;
        }
        // The acknowledgement and inventory update can arrive on different
        // client frames. Suppress the complete preview while the original
        // stack is pending, including an exhausted stack or hand switch.
        return true;
    }

    private static void renderPlacementPreview(PoseStack poseStack, Minecraft minecraft,
                                                 BlockPos origin, BlockState state, RenderType renderType) {
        MultiBufferSource.BufferSource bufferSource = previewBufferSource();
        MultiBufferSource previewBuffers = new PreviewBufferSource(
                bufferSource, 1.0F, 1.0F, 1.0F, 0.45F, false);
        for (BlockPos previewPos : previewPositions(state, origin)) {
            renderGhost(poseStack, previewPos, BuildingStructure.stateAt(state, origin, previewPos),
                    previewBuffers, renderType);
        }
        bufferSource.endBatch(renderType);
    }

    private static void renderInvalidPreview(PoseStack poseStack, Minecraft minecraft,
                                               BlockPos origin, BlockState state, RenderType renderType,
                                               float alpha) {
        MultiBufferSource.BufferSource bufferSource = previewBufferSource();
        MultiBufferSource previewBuffers = new PreviewBufferSource(
                bufferSource, INVALID_PREVIEW_RED, INVALID_PREVIEW_GREEN, INVALID_PREVIEW_BLUE,
                alpha, true);
        for (BlockPos previewPos : previewPositions(state, origin)) {
            renderGhost(poseStack, previewPos, BuildingStructure.stateAt(state, origin, previewPos),
                    previewBuffers, renderType);
        }
        bufferSource.endBatch(renderType);
    }

    private static MultiBufferSource.BufferSource previewBufferSource() {
        // Do not append preview vertices to LevelRenderer's particle/entity
        // BufferSource.  That source is still owned by the active render stage;
        // sharing it allows a later flush or another listener to submit the
        // same failed vertices a second time.  Both preview states use this
        // one-layer translucent source, so they have identical batch behavior.
        return PREVIEW_BUFFER_SOURCE;
    }

    private static void renderGhost(PoseStack poseStack, BlockPos pos, BlockState state,
                                    MultiBufferSource previewBuffers, RenderType renderType) {
        Minecraft minecraft = Minecraft.getInstance();
        var camera = minecraft.gameRenderer.getMainCamera().getPosition();
        poseStack.pushPose();
        poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
        minecraft.getBlockRenderer().renderSingleBlock(state, poseStack,
                previewBuffers, 0xF000F0, 0, net.minecraftforge.client.model.data.ModelData.EMPTY,
                renderType);
        poseStack.popPose();
    }

    private static final class PreviewBufferSource implements MultiBufferSource {
        private final MultiBufferSource delegate;
        private final float red;
        private final float green;
        private final float blue;
        private final float alpha;
        private final boolean replaceColor;
        private VertexConsumer buffer;

        private PreviewBufferSource(MultiBufferSource delegate, float red, float green, float blue,
                                    float alpha, boolean replaceColor) {
            this.delegate = delegate;
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.alpha = alpha;
            this.replaceColor = replaceColor;
        }

        @Override
        public VertexConsumer getBuffer(RenderType renderType) {
            if (buffer == null) {
                buffer = new PreviewVertexConsumer(delegate.getBuffer(renderType),
                        red, green, blue, alpha, replaceColor);
            }
            return buffer;
        }
    }

    private static final class PreviewVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float red;
        private final float green;
        private final float blue;
        private final float alpha;
        private final boolean replaceColor;

        private PreviewVertexConsumer(VertexConsumer delegate, float red, float green, float blue,
                                      float alpha, boolean replaceColor) {
            this.delegate = delegate;
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.alpha = alpha;
            this.replaceColor = replaceColor;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            delegate.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            int outputRed = replaceColor ? Math.round(this.red * 255.0F) : red;
            int outputGreen = replaceColor ? Math.round(this.green * 255.0F) : green;
            int outputBlue = replaceColor ? Math.round(this.blue * 255.0F) : blue;
            delegate.color(outputRed, outputGreen, outputBlue, Math.round(alpha * this.alpha));
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            delegate.uv(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            delegate.overlayCoords(u, v);
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            delegate.uv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            delegate.normal(x, y, z);
            return this;
        }

        @Override
        public void endVertex() {
            delegate.endVertex();
        }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {
            int outputRed = replaceColor ? Math.round(this.red * 255.0F) : red;
            int outputGreen = replaceColor ? Math.round(this.green * 255.0F) : green;
            int outputBlue = replaceColor ? Math.round(this.blue * 255.0F) : blue;
            delegate.defaultColor(outputRed, outputGreen, outputBlue, Math.round(alpha * this.alpha));
        }

        @Override
        public void unsetDefaultColor() {
            delegate.unsetDefaultColor();
        }
    }

    private static void renderAdjustmentBorder(PoseStack poseStack, Minecraft minecraft) {
        ItemStack stick = isAdjustmentStick(minecraft.player.getMainHandItem())
                ? minecraft.player.getMainHandItem() : minecraft.player.getOffhandItem();
        if (!isAdjustmentStick(stick)) return;
        String name = stick.getTag().getString("icecore_adjustment_region");
        RegionDefinition region = BuildingClientState.data().regions().get(name);
        if (region == null || !region.isComplete()
                || !region.dimension().equals(minecraft.level.dimension().location().toString())) return;
        int[] first = region.pos1();
        int[] second = region.pos2();
        double minX = Math.min(first[0], second[0]);
        double minY = Math.min(first[1], second[1]);
        double minZ = Math.min(first[2], second[2]);
        double maxX = Math.max(first[0], second[0]) + 1.0D;
        double maxY = Math.max(first[1], second[1]) + 1.0D;
        double maxZ = Math.max(first[2], second[2]) + 1.0D;
        var camera = minecraft.gameRenderer.getMainCamera().getPosition();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 0.55F, 0.05F, 0.95F);
        RenderSystem.disableDepthTest();
        RenderSystem.lineWidth(3.0F);
        VertexConsumer buffer = minecraft.renderBuffers().bufferSource().getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(poseStack, buffer, new AABB(minX, minY, minZ, maxX, maxY, maxZ), 1.0F, 0.55F, 0.05F, 1.0F);
        minecraft.renderBuffers().bufferSource().endBatch(RenderType.lines());
        RenderSystem.lineWidth(1.0F);
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static boolean isAdjustmentStick(ItemStack stack) {
        return stack.is(net.minecraft.world.item.Items.STICK)
                && stack.hasTag()
                && stack.getTag().contains("icecore_adjustment_region")
                && !stack.getTag().getString("icecore_adjustment_region").isEmpty();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        lastPreviewPoseStack = null;
        desiredWrenchPreview = null;
        committedWrenchPreview = null;
        queuedWrenchPreview = null;
        desiredWrenchGeneration = 0L;
        BuildingClientRenderState.reset();
        BuildingClientState.reset();
    }
}
