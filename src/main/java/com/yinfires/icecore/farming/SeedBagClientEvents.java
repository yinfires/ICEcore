package com.yinfires.icecore.farming;

import com.yinfires.icecore.network.ICECoreNetwork;
import com.yinfires.icecore.network.ServerBoundHoeBreakCropPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Hoe left-click on a seed-bag crop. The attack input is consumed before vanilla's adventure-mode
 * restriction, and the server performs the break. Holding the button re-fires this every tick
 * through continueAttack, so one request per target is sent within a short window.
 */
@Mod.EventBusSubscriber(modid = "icecore", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SeedBagClientEvents {
    private static final long RESEND_TICKS = 5;
    private static BlockPos lastPos;
    private static long lastTick = Long.MIN_VALUE;

    private SeedBagClientEvents() {}

    @SubscribeEvent
    public static void onAttack(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null
                || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
                || !SeedBagCrops.isHoe(minecraft.player.getMainHandItem())) return;
        BlockPos pos = hit.getBlockPos();
        if (SeedBagCrops.find(minecraft.level.getBlockState(pos).getBlock()) == null) return;
        event.setCanceled(true);
        long tick = minecraft.level.getGameTime();
        if (pos.equals(lastPos) && tick - lastTick < RESEND_TICKS) {
            event.setSwingHand(false);
            return;
        }
        lastPos = pos.immutable();
        lastTick = tick;
        ICECoreNetwork.sendToServer(new ServerBoundHoeBreakCropPacket(lastPos));
    }
}
