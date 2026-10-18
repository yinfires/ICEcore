package com.yinfires.icecore.workstation;

import com.yinfires.icecore.network.ICECoreNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "icecore", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TrashCanClientEvents {
    private static long lastRequest;
    private static boolean claimedPress;
    private static net.minecraft.client.multiplayer.ClientLevel claimedLevel;
    private TrashCanClientEvents() {}

    @SubscribeEvent
    public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        claimedPress = false;
        claimedLevel = null;
        lastRequest = 0;
    }

    /** Called before both vanilla attack paths; a consumed hand stays claimed until release. */
    public static boolean attack(boolean down, boolean start) {
        var minecraft = Minecraft.getInstance();
        if (!down || minecraft.level != claimedLevel || minecraft.screen != null) {
            claimedPress = false;
            claimedLevel = minecraft.level;
        }
        if (!down) return false;
        if (start) claimedPress = false;
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null || minecraft.player.isSpectator()
                || minecraft.player.isUsingItem() || minecraft.player.isHandsBusy()) return claimedPress;
        if (claimedPress && minecraft.player.getMainHandItem().isEmpty()) return true;
        if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
                || !TrashCanLeftClickEvents.isTrash(minecraft.level, hit.getBlockPos())
                || !TrashCanLeftClickEvents.shouldIntercept(minecraft.player)) return claimedPress;
        claimedPress = true;
        if (com.yinfires.icecore.interaction.InteractionEvents.blocked(minecraft.level, minecraft.player, hit.getBlockPos())) return true;
        long now = System.nanoTime();
        if (now - lastRequest < 250_000_000L) return true;
        var permission = net.minecraftforge.client.ForgeHooksClient.onClickInput(0, minecraft.options.keyAttack,
                net.minecraft.world.InteractionHand.MAIN_HAND);
        if (permission.isCanceled()) return true;
        lastRequest = now;
        ICECoreNetwork.sendToServer(new ServerBoundTrashPutPacket(hit.getBlockPos(), hit.getDirection()));
        return true;
    }

    /** Holding attack must never enter vanilla's continued mining path. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void preventMining(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event instanceof TrashCanLeftClickEvents.PutPermission) && event.getLevel().isClientSide
                && TrashCanLeftClickEvents.isTrash(event.getLevel(), event.getPos())
                && TrashCanLeftClickEvents.shouldIntercept(event.getEntity())) event.setCanceled(true);
    }
}
