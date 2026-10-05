package com.yinfires.icecore.compat.smartkeyprompts;

import com.mafuyu404.smartkeyprompts.init.HUD;
import com.mafuyu404.smartkeyprompts.init.KeyPrompt;

/** Loaded only from the guarded client event path when SmartKeyPrompts is installed. */
public final class AdventureGivePrompt {
    private static KeyPrompt prompt;

    private AdventureGivePrompt() {}

    public static void submit(String key) {
        // Do not mutate an entry that may still be referenced by the HUD's hash set.
        if (prompt == null || !prompt.key.equals(key)) {
            prompt = new KeyPrompt("icecore_adventure_give", key, "icecore.adventure_give.hud", true);
        }
        HUD.addCache(prompt);
    }
}
