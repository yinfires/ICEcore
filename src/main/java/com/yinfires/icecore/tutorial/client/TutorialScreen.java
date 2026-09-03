package com.yinfires.icecore.tutorial.client;

import com.yinfires.icecore.journal.JournalType;
import com.yinfires.icecore.journal.client.EntryView;
import com.yinfires.icecore.journal.client.JournalScreen;
import com.yinfires.icecore.network.ICECoreNetwork;
import com.yinfires.icecore.network.ServerBoundMarkTutorialReadPacket;
import com.yinfires.icecore.tutorial.TutorialDefinition;
import com.yinfires.icecore.tutorial.TutorialDefinitionManager;
import com.yinfires.icecore.tutorial.TutorialState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Tutorial journal (I). Lists only unlocked tutorials; a NEW badge shows until the entry's detail
 * is opened, at which point it is marked read (server sync removes the badge). No completion state.
 */
public final class TutorialScreen extends JournalScreen {
    public TutorialScreen() {
        super(Component.translatable("gui.icecore.journal.tutorials"));
    }

    @Override
    protected JournalType journalType() {
        return JournalType.TUTORIAL;
    }

    @Override
    protected Component emptyMessage() {
        return Component.translatable("gui.icecore.journal.tutorials.empty");
    }

    @Override
    protected Component noSelectionMessage() {
        return Component.translatable("gui.icecore.journal.tutorials.none_selected");
    }

    @Override
    protected List<EntryView> entries() {
        List<EntryView> views = new ArrayList<>();
        TutorialClientState.states().forEach((id, state) -> {
            if (state == TutorialState.LOCKED) {
                return;
            }
            TutorialDefinition def = TutorialDefinitionManager.INSTANCE.get(id);
            if (def == null) {
                return;
            }
            boolean unread = state == TutorialState.UNLOCKED_UNREAD;
            views.add(new EntryView(def.id(), def.category(), def.order(),
                    def.title().resolve(), def.content().resolve(), unread, List.of(), def.images()));
        });
        return views;
    }

    @Override
    protected void onSelect(ResourceLocation entryId) {
        // Opening a tutorial marks it read and clears the NEW badge — locally at once (so the red
        // dot disappears immediately), then confirmed by the server sync.
        if (TutorialClientState.state(entryId) == TutorialState.UNLOCKED_UNREAD) {
            TutorialClientState.markReadLocal(entryId);
            rebuildTree();
            ICECoreNetwork.sendToServer(new ServerBoundMarkTutorialReadPacket(entryId));
        }
    }
}
