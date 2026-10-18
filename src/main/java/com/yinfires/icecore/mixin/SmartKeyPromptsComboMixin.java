package com.yinfires.icecore.mixin;

import com.mafuyu404.smartkeyprompts.init.KeyPrompt;
import com.mafuyu404.smartkeyprompts.init.KeyRenderer;
import com.yinfires.icecore.compat.smartkeyprompts.ComboKeyLayout;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Retains native HUD layout and keycap drawing with a stable unpressed appearance. */
@Pseudo
@Mixin(targets = "com.mafuyu404.smartkeyprompts.init.HUD", remap = false)
public abstract class SmartKeyPromptsComboMixin {
    @Shadow private static volatile Font font;
    @Shadow private static volatile List<KeyPrompt> cachedDefaultPrompts;
    @Shadow private static String getCachedKeyTranslation(String key) { throw new AssertionError(); }
    @Unique private static final Map<String, ComboKeyLayout> icecore$layouts = new LinkedHashMap<>();
    @Unique private static Font icecore$font;
    @Unique private static String icecore$key;
    @Unique private static ComboKeyLayout icecore$layout;

    @Inject(method = "updateCachedPrompts", at = @At("RETURN"))
    private static void icecore$order(CallbackInfo ci) {
        int put = -1, take = -1;
        for (int i = 0; i < cachedDefaultPrompts.size(); i++) {
            String group = cachedDefaultPrompts.get(i).group;
            if ("icecore_workstation_put".equals(group)) put = i;
            if ("icecore_workstation_take".equals(group)) take = i;
        }
        if (put >= 0 && take >= 0 && put > take) {
            var ordered = new java.util.ArrayList<>(cachedDefaultPrompts);
            java.util.Collections.swap(ordered, put, take);
            cachedDefaultPrompts = List.copyOf(ordered);
        }
    }

    @Redirect(method = "renderKeyPrompts", require = 1, allow = 1, at = @At(value = "INVOKE", target = "Ljava/util/List;get(I)Ljava/lang/Object;"))
    private static Object icecore$row(List<?> prompts, int index) {
        KeyPrompt prompt = (KeyPrompt) prompts.get(index);
        icecore$layout = null;
        icecore$key = null;
        if (prompt != null) {
            icecore$key = prompt.keyAlias != null ? prompt.keyAlias : getCachedKeyTranslation(prompt.key);
            if (icecore$font != font) { icecore$layouts.clear(); icecore$font = font; }
            if (icecore$key.contains("+")) {
                if (!icecore$layouts.containsKey(icecore$key)) {
                    if (icecore$layouts.size() >= 128) icecore$layouts.clear();
                    icecore$layouts.put(icecore$key, ComboKeyLayout.measure(icecore$key, font::width));
                }
                icecore$layout = icecore$layouts.get(icecore$key);
            }
        }
        return prompt;
    }

    // The upstream jar already uses SRG names; do not remap this invoke selector.
    @Redirect(method = "renderKeyPrompts", require = 5, allow = 5, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;m_92895_(Ljava/lang/String;)I"))
    private static int icecore$width(Font currentFont, String text) {
        int width = currentFont.width(text);
        return icecore$layout == null ? width : width + icecore$layout.width() - 12 - currentFont.width(icecore$key);
    }

    @Redirect(method = "renderKeyPrompts", require = 3, allow = 3, at = @At(value = "INVOKE", target = "Lcom/mafuyu404/smartkeyprompts/init/KeyRenderer;drawKeyBoardKey(Lnet/minecraft/client/gui/GuiGraphics;IILjava/lang/String;Z)V"))
    private static void icecore$draw(GuiGraphics graphics, int x, int y, String display, boolean pressed) {
        if (icecore$layout == null) { KeyRenderer.drawKeyBoardKey(graphics, x, y, display, false); return; }
        for (int i = 0; i < icecore$layout.keys().length; i++) {
            KeyRenderer.drawKeyBoardKey(graphics, x + icecore$layout.offsets()[i], y, icecore$layout.keys()[i], false);
            if (i < icecore$layout.plusOffsets().length)
                KeyRenderer.drawText(graphics, x + icecore$layout.plusOffsets()[i], y + 3, "+");
        }
    }

    @Inject(method = "renderKeyPrompts", at = @At("RETURN"))
    private static void icecore$end(GuiGraphics graphics, com.mojang.blaze3d.vertex.PoseStack pose,
            List<KeyPrompt> prompts, int position, int screenWidth, int y, float scale,
            boolean debug, boolean crosshair, CallbackInfo ci) {
        icecore$key = null; icecore$layout = null;
    }

    @Inject(method = "clearCache", at = @At("HEAD"))
    private static void icecore$clear(CallbackInfo ci) {
        icecore$layouts.clear(); icecore$font = null; icecore$key = null; icecore$layout = null;
    }
}
