package com.yinfires.icecore.currency;

import com.yinfires.icecore.ICECore;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.text.NumberFormat;
import java.util.Locale;

public final class CurrencyHud {
    private static final int X = 6;
    private static final int Y = 6;
    private static final NumberFormat FORMAT = NumberFormat.getIntegerInstance(Locale.US);

    private CurrencyHud() {
    }

    @Mod.EventBusSubscriber(modid = ICECore.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void register(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("currency", (gui, graphics, partialTick, width, height) -> render(graphics));
        }
    }

    @Mod.EventBusSubscriber(modid = ICECore.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ClientEvents {
        private ClientEvents() {
        }

        @SubscribeEvent
        public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
            CurrencyClientState.reset();
        }
    }

    private static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui
                || !CurrencyClientState.initialized() || !CurrencyClientState.hudEnabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        Font font = minecraft.font;
        String visible = "$" + FORMAT.format(CurrencyClientState.displayedValue(now));
        graphics.drawString(font, visible, X, Y, 0xFFFFFFFF, true);

        long age = CurrencyClientState.changeAge(now);
        if (age >= 3_000L || CurrencyClientState.delta() == 0L) return;
        String sign = CurrencyClientState.delta() > 0L ? "+" : "-";
        String text = sign + FORMAT.format(abs(CurrencyClientState.delta())) + "$";
        float scale = age < 150L ? (float) (1.0D + 0.35D * Math.pow(1.0D - age / 150.0D, 2.0D)) : 1.0F;
        int alpha = age <= 2_400L ? 255 : Math.max(0, Math.round(255.0F * (3_000L - age) / 600.0F));
        // Font.adjustColor treats alpha values 0..3 as a missing alpha channel and forces them
        // to fully opaque. Stop before that cutoff so the last quantized fade frames cannot flash.
        if (alpha < 4) return;
        int rgb = CurrencyClientState.delta() > 0L ? 0x2F9E44 : 0xC13C3C;
        int color = (alpha << 24) | rgb;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(X + font.width(visible) + 4.0F, Y + (font.lineHeight * (1.0F - scale) / 2.0F), 0.0F);
        pose.scale(scale, scale, 1.0F);
        graphics.drawString(font, text, 0, 0, color, true);
        pose.popPose();
    }

    private static long abs(long value) {
        return value == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(value);
    }
}
