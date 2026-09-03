package com.yinfires.icecore.journal;

import com.yinfires.icecore.quest.QuestService;
import com.yinfires.icecore.tutorial.TutorialService;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxCommandUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Registers custom ChatBox component event types so dialogue can accept/complete quests and unlock
 * tutorials directly, without stringly-typed command lines. Dialogue authors write, on an option:
 * {@code {"type":"icecore_quest","value":"accept:fire_intro"}} (also {@code complete:<id>}) or
 * {@code {"type":"icecore_tutorial","value":"unlock:<id>"}}.
 *
 * <p>ChatBox is a forced both-sides dependency (see NpcEntity), so calling its API directly is safe.
 * The server handler re-validates the requested id and action rather than trusting client input.
 * The client handler is a no-op; all state changes run server-side (executeOnServer = true).
 */
public final class ChatBoxJournalEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String QUEST_EVENT = "icecore_quest";
    public static final String TUTORIAL_EVENT = "icecore_tutorial";

    private ChatBoxJournalEvents() {
    }

    public static void register() {
        ChatBoxCommandUtil.registerComponentEvent(QUEST_EVENT, (component, value) -> {
        }, true, ChatBoxJournalEvents::handleQuest);
        ChatBoxCommandUtil.registerComponentEvent(TUTORIAL_EVENT, (component, value) -> {
        }, true, ChatBoxJournalEvents::handleTutorial);
    }

    private static void handleQuest(ServerPlayer player, String value) {
        int colon = value.indexOf(':');
        if (colon <= 0) {
            LOGGER.warn("icecore_quest event value must be action:id, got '{}'", value);
            return;
        }
        String action = value.substring(0, colon);
        ResourceLocation id = ResourceLocation.tryParse(value.substring(colon + 1));
        if (id == null) {
            LOGGER.warn("icecore_quest event has invalid quest id in '{}'", value);
            return;
        }
        switch (action) {
            case "accept" -> QuestService.acquire(player, id);
            case "complete" -> QuestService.complete(player, id);
            case "mark" -> QuestService.setMarked(player, id);
            default -> LOGGER.warn("icecore_quest event has unknown action '{}'", action);
        }
    }

    private static void handleTutorial(ServerPlayer player, String value) {
        int colon = value.indexOf(':');
        if (colon <= 0) {
            LOGGER.warn("icecore_tutorial event value must be action:id, got '{}'", value);
            return;
        }
        String action = value.substring(0, colon);
        ResourceLocation id = ResourceLocation.tryParse(value.substring(colon + 1));
        if (id == null) {
            LOGGER.warn("icecore_tutorial event has invalid tutorial id in '{}'", value);
            return;
        }
        switch (action) {
            case "unlock" -> TutorialService.unlock(player, id);
            case "read" -> TutorialService.markRead(player, id);
            default -> LOGGER.warn("icecore_tutorial event has unknown action '{}'", action);
        }
    }
}
