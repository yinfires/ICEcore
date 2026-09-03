package com.yinfires.icecore.journal.client;

import net.minecraft.client.Minecraft;
import com.yinfires.icecore.time.TimeClientState;

/**
 * Computes the top-right stacking offset so ICEcore's own top-right HUDs sit below one another
 * instead of overlapping. Currently the only prior occupant is {@link com.yinfires.icecore.time.TimeHud},
 * whose visible block spans roughly y=7 down through the clock line. The quest tracker reads the
 * reserved height here and drops below it; when the time HUD is hidden the tracker moves up.
 *
 * <p>This does not restructure TimeHud; it only mirrors TimeHud's own visibility rule (initialized,
 * hudEnabled, GUI not hidden) and its fixed layout height so the tracker can offset itself.
 */
public final class TopRightHudStack {
    // TimeHud draws day (y=7), weekday (y=19) and clock (y=33, height ~10); its block ends ~y=45.
    private static final int TIME_HUD_BOTTOM = 46;
    private static final int TOP_MARGIN = 7;
    private static final int GAP = 4;

    private TopRightHudStack() {
    }

    /** Y coordinate where the quest tracker HUD should begin, honouring the time HUD when visible. */
    public static int trackerStartY() {
        if (timeHudVisible()) {
            return TIME_HUD_BOTTOM + GAP;
        }
        return TOP_MARGIN;
    }

    private static boolean timeHudVisible() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return false;
        }
        return TimeClientState.initialized() && TimeClientState.config() != null
                && TimeClientState.config().hudEnabled();
    }
}
