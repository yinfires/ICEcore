package com.yinfires.icecore.journal.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.yinfires.icecore.ICECore;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Registers the J (quests) and I (tutorials) key mappings. Players may rebind them in vanilla options. */
public final class JournalKeybinds {
    public static final String CATEGORY = "key.categories.icecore";

    public static final KeyMapping OPEN_QUESTS = new KeyMapping(
            "key.icecore.quests", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, CATEGORY);
    public static final KeyMapping OPEN_TUTORIALS = new KeyMapping(
            "key.icecore.tutorials", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_I, CATEGORY);

    private JournalKeybinds() {
    }

    @Mod.EventBusSubscriber(modid = ICECore.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) {
            event.register(OPEN_QUESTS);
            event.register(OPEN_TUTORIALS);
        }
    }
}
