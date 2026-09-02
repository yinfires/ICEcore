package com.yinfires.icecore.mixin;

import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.deco.TextBlockEntity;
import com.github.ysbbbbbb.kaleidoscopetavern.client.render.block.TextBlockEntityRender;
import com.mojang.blaze3d.vertex.PoseStack;
import com.yinfires.icecore.compat.cozycafe.board.CozyCafeBoardService;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.DyeColor;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Renders bound status independently so the name keeps the board's dye color. */
@Pseudo
@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopetavern.client.render.block.TextBlockEntityRender", remap = false)
public abstract class TavernTextRendererMixin {
    @Shadow @Final protected Font font;
    @Shadow protected abstract float getPosX(TextBlockEntity block, int maxWidth, int lineWidth);
    @Shadow protected abstract int getDarkColor(DyeColor color, boolean glowing);
    @Shadow protected abstract boolean isOutlineVisible(net.minecraft.core.BlockPos pos, int color);

    private static final int CACHE_LIMIT = 256;
    private static final Map<LayoutKey, List<FormattedCharSequence>> LAYOUTS = new LinkedHashMap<>(32, 0.75F, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<LayoutKey, List<FormattedCharSequence>> eldest) {
            return size() > CACHE_LIMIT;
        }
    };

    @Inject(method = "doTextRender(Lcom/github/ysbbbbbb/kaleidoscopetavern/blockentity/deco/TextBlockEntity;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILjava/lang/String;IFII)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void icecore$renderBound(TextBlockEntity board, PoseStack poseStack, MultiBufferSource buffer,
                                     int packedLight, String ignored, int maxWidth, float scale,
                                     int maxLines, int lineHeight, CallbackInfo ci) {
        if (!CozyCafeBoardService.isBound(board)) return;
        ci.cancel();
        poseStack.scale(scale, -scale, scale);
        String name = CozyCafeBoardService.name(board);
        int nameLimit = Math.max(1, maxLines - 2);
        List<FormattedCharSequence> lines = layout(name, maxWidth, nameLimit);
        int nameColor = board.isGlowing() ? board.getColor().getTextColor() : getDarkColor(board.getColor(), false);
        int light = board.isGlowing() ? 15728880 : packedLight;
        boolean outline = board.isGlowing() && isOutlineVisible(board.getBlockPos(), nameColor);
        for (int i = 0; i < lines.size(); i++) draw(board, lines.get(i), i, maxWidth, lineHeight,
                nameColor, getDarkColor(board.getColor(), board.isGlowing()), outline, light, poseStack, buffer);

        Component status = Component.translatable(CozyCafeBoardService.isOpen(board)
                ? "icecore.cozycafe.board.open" : "icecore.cozycafe.board.closed");
        FormattedCharSequence statusLine = status.getVisualOrderText();
        int statusColor = CozyCafeBoardService.isOpen(board) ? 0xFF55FF55 : 0xFFFF5555;
        draw(board, statusLine, Math.min(maxLines - 1, lines.size() + 1), maxWidth, lineHeight,
                statusColor, statusColor & 0xFF3F3F3F, false, light, poseStack, buffer);
    }

    private void draw(TextBlockEntity board, FormattedCharSequence line, int index, int maxWidth, int lineHeight,
                      int color, int dark, boolean outline, int light, PoseStack stack, MultiBufferSource buffer) {
        float x = getPosX(board, maxWidth, font.width(line));
        float y = index * lineHeight - 19.0F;
        Matrix4f pose = stack.last().pose();
        if (outline) font.drawInBatch8xOutline(line, x, y, color, dark, pose, buffer, light);
        else font.drawInBatch(line, x, y, color, false, pose, buffer, DisplayMode.POLYGON_OFFSET, 0, light);
    }

    private List<FormattedCharSequence> layout(String name, int width, int limit) {
        LayoutKey key = new LayoutKey(name, width, limit);
        return LAYOUTS.computeIfAbsent(key, ignored -> {
            List<FormattedCharSequence> original = font.split(Component.literal(name), width);
            if (original.size() <= limit) return List.copyOf(original);
            String candidate = name;
            while (!candidate.isEmpty()) {
                candidate = candidate.substring(0, candidate.offsetByCodePoints(candidate.length(), -1));
                List<FormattedCharSequence> clipped = font.split(Component.literal(candidate + "…"), width);
                if (clipped.size() <= limit) return List.copyOf(clipped);
            }
            return List.of(Component.literal("…").getVisualOrderText());
        });
    }

    private record LayoutKey(String name, int width, int lines) {}
}
