package com.yinfires.icecore.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Vector2i;
import org.lwjgl.opengl.GL11;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.FormattedCharSequence;
import com.yinfires.icecore.compat.cozycafe.client.CafePageFeedbackLayout;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Routes ICEcore opening failures into CozyCafe's existing page feedback area. */
@Pseudo
@Mixin(targets = "io.github.chakyl.cozycafe.gui.CafeManagerScreen", remap = false)
public abstract class CozyCafeManagerScreenErrorMixin implements com.yinfires.icecore.compat.cozycafe.client.CafeFeedbackResources {
    @Shadow private Component errorMessage;
    @Shadow private int errorDisplayTicks;
    @Shadow private ImageButton toggleOpenButton;
    @Unique private long icecore$errorStarted;
    @Unique private Component icecore$cachedError;
    @Unique private List<FormattedCharSequence> icecore$errorLines = List.of();
    @Unique private List<Component> icecore$wrappedMessage = List.of();
    @Shadow private List<Component> wrapErrorText(Font font, Component message) { throw new AssertionError(); }
    @Unique private TextureTarget icecore$fadeTarget;
    @Unique private final ClientTooltipPositioner icecore$positioner = (sw, sh, bx, by, tw, th) ->
            new Vector2i(CafePageFeedbackLayout.left(bx, tw), Math.max(4, by - 6 - th));

    @Inject(method = "setErrorMessage(Ljava/lang/Byte;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void icecore$validMenuSeatHud(Byte code, CallbackInfo callback) {
        icecore$errorStarted = System.nanoTime();
        icecore$cachedError = null;
        if (code != null && (code.byteValue() == 5 || code.byteValue() == 6)) {
            errorMessage = Component.translatable(code.byteValue() == 5
                    ? "icecore.cozycafe.no_valid_menu_seat" : "icecore.cozycafe.closed_hours");
            errorDisplayTicks = 120;
            callback.cancel();
        }
    }

    @Inject(method = "setErrorMessage(Ljava/lang/Byte;)V", at = @At("RETURN"), remap = false)
    private void icecore$limitOriginalMessageDuration(Byte code, CallbackInfo callback) {
        errorDisplayTicks = 120;
    }

    @Inject(method = "m_88315_(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("HEAD"), remap = false)
    private void icecore$expirePageFeedback(CallbackInfo callback) {
        if (errorMessage != null && CafePageFeedbackLayout.alpha(System.nanoTime() - icecore$errorStarted) <= 0.0F) {
            errorMessage = null;
            errorDisplayTicks = 0;
        }
        if (errorMessage == null && icecore$cachedError != null) {
            icecore$cachedError = null;
            icecore$errorLines = List.of();
            icecore$wrappedMessage = List.of();
        }
    }

    @Redirect(method = "m_88315_(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            at = @At(value = "INVOKE", target = "Lio/github/chakyl/cozycafe/gui/CafeManagerScreen;wrapErrorText(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;)Ljava/util/List;"), remap = false)
    private List<Component> icecore$cacheNativeWrapping(@Coerce Object screen, Font font, Component message) {
        if (icecore$cachedError != message) {
            icecore$cachedError = message;
            icecore$wrappedMessage = wrapErrorText(font, message);
            icecore$errorLines = icecore$wrappedMessage.stream().map(Component::getVisualOrderText).toList();
        }
        return icecore$wrappedMessage;
    }

    @Redirect(method = "m_88315_(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;m_280666_(Lnet/minecraft/client/gui/Font;Ljava/util/List;II)V", ordinal = 0), remap = false)
    private void icecore$drawAboveOpenButton(GuiGraphics graphics, Font font, List<Component> original, int x, int y) {
        if (errorMessage == null || toggleOpenButton == null) return;
        float alpha = CafePageFeedbackLayout.alpha(System.nanoTime() - icecore$errorStarted);
        if (alpha <= 0.0F || errorDisplayTicks <= 0) {
            errorMessage = null;
            errorDisplayTicks = 0;
            icecore$cachedError = null;
            icecore$errorLines = List.of();
            icecore$wrappedMessage = List.of();
            return;
        }
        int center = toggleOpenButton.getX() + toggleOpenButton.getWidth() / 2;
        if (alpha >= 1.0F) {
            graphics.renderTooltip(font, icecore$errorLines, icecore$positioner, center, toggleOpenButton.getY());
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        var main = minecraft.getMainRenderTarget();
        graphics.flush();
        if (icecore$fadeTarget == null) {
            icecore$fadeTarget = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
            icecore$fadeTarget.setClearColor(0, 0, 0, 0);
        } else if (icecore$fadeTarget.width != main.width || icecore$fadeTarget.height != main.height) {
            icecore$fadeTarget.resize(main.width, main.height, Minecraft.ON_OSX);
        }
        icecore$fadeTarget.clear(Minecraft.ON_OSX);
        icecore$fadeTarget.bindWrite(false);
        try {
            graphics.renderTooltip(font, icecore$errorLines, icecore$positioner, center, toggleOpenButton.getY());
            graphics.flush();
        } finally {
            main.bindWrite(false);
        }
        // Fade the native renderer's pixels, without replacing its content, styles or hooks.
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, icecore$fadeTarget.getColorTextureId());
        RenderSystem.setShaderColor(alpha, alpha, alpha, alpha);
        try {
            BufferBuilder buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            var matrix = graphics.pose().last().pose();
            float right = graphics.guiWidth(), bottom = graphics.guiHeight();
            buffer.vertex(matrix, 0, bottom, 400).uv(0, 0).endVertex();
            buffer.vertex(matrix, right, bottom, 400).uv(1, 0).endVertex();
            buffer.vertex(matrix, right, 0, 400).uv(1, 1).endVertex();
            buffer.vertex(matrix, 0, 0, 400).uv(0, 1).endVertex();
            BufferUploader.drawWithShader(buffer.end());
        } finally {
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
        }
    }

    @Inject(method = "m_88315_(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("HEAD"), remap = false)
    private void icecore$releaseExpiredTarget(CallbackInfo callback) {
        if (errorMessage == null && icecore$fadeTarget != null) {
            icecore$fadeTarget.destroyBuffers();
            icecore$fadeTarget = null;
        }
    }

    public void icecore$releaseFeedbackTarget() {
        if (icecore$fadeTarget != null) {
            icecore$fadeTarget.destroyBuffers();
            icecore$fadeTarget = null;
        }
    }
}
