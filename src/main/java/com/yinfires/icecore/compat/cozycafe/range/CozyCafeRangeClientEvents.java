package com.yinfires.icecore.compat.cozycafe.range;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.yinfires.icecore.ICECore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ICECore.MOD_ID, value = net.minecraftforge.api.distmarker.Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CozyCafeRangeClientEvents {
    private static PoseStack lastPoseStack;

    private CozyCafeRangeClientEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getEntity() != minecraft.player || minecraft.player == null || !holdsRangeWand(minecraft)) {
            return;
        }
        // Preserve the use-on packet so the server records pos2, but deny local block use.
        event.setCanceled(false);
        event.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);
        event.setUseItem(net.minecraftforge.eventbus.api.Event.Result.ALLOW);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onRightClickFinal(PlayerInteractEvent.RightClickBlock event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getEntity() == minecraft.player && minecraft.player != null && holdsRangeWand(minecraft)) {
            event.setCanceled(false);
            event.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);
            event.setUseItem(net.minecraftforge.eventbus.api.Event.Result.ALLOW);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getEntity() != minecraft.player || minecraft.player == null || !holdsRangeWand(minecraft)) {
            return;
        }
        if (minecraft.gameMode != null && minecraft.gameMode.getPlayerMode() == GameType.CREATIVE) {
            event.setCanceled(true);
        } else {
            event.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);
            event.setUseItem(net.minecraftforge.eventbus.api.Event.Result.DENY);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onLeftClickFinal(PlayerInteractEvent.LeftClickBlock event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getEntity() != minecraft.player || minecraft.player == null || !holdsRangeWand(minecraft)) {
            return;
        }
        if (minecraft.gameMode != null && minecraft.gameMode.getPlayerMode() == GameType.CREATIVE) {
            event.setCanceled(true);
        } else {
            event.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);
            event.setUseItem(net.minecraftforge.eventbus.api.Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.gameMode != null && holdsRangeWand(minecraft)
                && minecraft.gameMode.isDestroying()) {
            minecraft.gameMode.stopDestroyBlock();
        }
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
                || lastPoseStack == event.getPoseStack()) {
            return;
        }
        lastPoseStack = event.getPoseStack();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        ItemStack wand = rangeWand(minecraft);
        if (wand.isEmpty()) {
            return;
        }
        String key = wand.getTag().getString(CozyCafeRangeAdjustmentManager.MARKER);
        CozyCafeRangeDefinition definition = CozyCafeRangeClientState.data().ranges().get(key);
        if (definition == null || !definition.isComplete()
                || !minecraft.level.dimension().location().toString().equals(definition.dimension())) {
            return;
        }
        renderBorder(event.getPoseStack(), minecraft, definition);
    }

    private static void renderBorder(PoseStack poseStack, Minecraft minecraft,
                                     CozyCafeRangeDefinition definition) {
        int[] first = definition.pos1();
        int[] second = definition.pos2();
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
        RenderSystem.setShaderColor(0.15F, 0.85F, 1.0F, 0.95F);
        RenderSystem.disableDepthTest();
        RenderSystem.lineWidth(3.0F);
        VertexConsumer buffer = minecraft.renderBuffers().bufferSource().getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(poseStack, buffer, new AABB(minX, minY, minZ, maxX, maxY, maxZ),
                0.15F, 0.85F, 1.0F, 1.0F);
        minecraft.renderBuffers().bufferSource().endBatch(RenderType.lines());
        RenderSystem.lineWidth(1.0F);
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static boolean holdsRangeWand(Minecraft minecraft) {
        return !rangeWand(minecraft).isEmpty();
    }

    private static ItemStack rangeWand(Minecraft minecraft) {
        ItemStack main = minecraft.player.getMainHandItem();
        if (CozyCafeRangeAdjustmentManager.isMarked(main, null)) {
            return main;
        }
        ItemStack offhand = minecraft.player.getOffhandItem();
        return CozyCafeRangeAdjustmentManager.isMarked(offhand, null) ? offhand : ItemStack.EMPTY;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        lastPoseStack = null;
        CozyCafeRangeClientState.reset();
    }
}
