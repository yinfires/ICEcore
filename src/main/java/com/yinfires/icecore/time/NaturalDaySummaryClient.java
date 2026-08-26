package com.yinfires.icecore.time;

import com.yinfires.icecore.ICECore;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ICECore.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NaturalDaySummaryClient {
    private static ClientBoundDaySummaryPacket packet;
    private static long lastSequence = Long.MIN_VALUE;
    private static long startedNanos;

    private NaturalDaySummaryClient() {}

    public static void accept(ClientBoundDaySummaryPacket value) {
        if (value.sequence() <= lastSequence || TimeCutsceneClient.active()) return;
        lastSequence = value.sequence();
        packet = value;
        startedNanos = System.nanoTime();
    }

    static void reset() { packet = null; }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        if (packet == null || TimeCutsceneClient.active()) { reset(); return; }
        double elapsed = (System.nanoTime() - startedNanos) / 50_000_000D;
        if (elapsed >= DaySummaryRenderer.durationTicks(packet.income(), packet.timings())) { reset(); return; }
        DaySummaryRenderer.render(event.getGuiGraphics(), packet.income(), packet.timings(), elapsed);
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
        lastSequence = Long.MIN_VALUE;
    }
}
