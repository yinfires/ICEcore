package com.yinfires.icecore.journal;

import com.yinfires.icecore.quest.*;
import com.yinfires.icecore.quest.objective.QuestObjective;
import com.yinfires.icecore.tutorial.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class JournalGlobalDataTest {
    private static final ResourceLocation ID = ResourceLocation.parse("icecore:test");
    private static final UUID A = UUID.randomUUID(), B = UUID.randomUUID(), NEW = UUID.randomUUID();

    private static QuestDefinition quest(QuestScope scope) {
        QuestObjective objective = new QuestObjective() {
            public ResourceLocation type() { return ID; }
            public int targetCount() { return 5; }
            public Component label() { return Component.empty(); }
        };
        return new QuestDefinition(ID, new JournalText("test"), new JournalText(""), null, null,
                0, scope, false, false, true, List.of(), List.of(objective), List.of(), List.of(), List.of());
    }

    private static TutorialDefinition tutorial(QuestScope scope) {
        return new TutorialDefinition(ID, new JournalText("test"), new JournalText(""), null, null, 0, scope, List.of());
    }

    @Test void questMigrationCombinesCountsAndCompletionWithoutResurrectingReset() {
        QuestPlayerData players = new QuestPlayerData();
        QuestProgress active = new QuestProgress(QuestState.ACTIVE, 1);
        active.setCount(0, 8, 10);
        QuestProgress complete = new QuestProgress(QuestState.COMPLETE, 1);
        complete.setCount(0, 2, 10);
        players.progressFor(A).put(ID, active);
        players.progressFor(B).put(ID, complete);
        players.setMarked(A, ID);
        QuestGlobalData global = new QuestGlobalData();
        global.migrate(quest(QuestScope.GLOBAL), players);
        assertEquals(QuestState.COMPLETE, global.progress().get(ID).state());
        assertEquals(5, global.progress().get(ID).count(0));
        assertFalse(players.progressFor(A).containsKey(ID));
        players.clearMarks(ID);
        assertNull(players.marked(A));
        global.progress().remove(ID);
        global = QuestGlobalData.load(global.save(new CompoundTag()));
        players.progressFor(A).put(ID, active); // Stale copy must not undo an explicit reset.
        global.migrate(quest(QuestScope.GLOBAL), players);
        assertFalse(global.progress().containsKey(ID));
    }

    @Test void existingGlobalQuestWinsAndPlayerScopeIsUntouched() {
        QuestPlayerData players = new QuestPlayerData();
        players.progressFor(A).put(ID, new QuestProgress(QuestState.COMPLETE, 1));
        QuestGlobalData global = new QuestGlobalData();
        global.migrate(quest(QuestScope.PLAYER), players);
        assertTrue(players.progressFor(A).containsKey(ID));
        global.progress().put(ID, new QuestProgress(QuestState.ACTIVE, 1));
        global.migrate(quest(QuestScope.GLOBAL), players);
        assertEquals(QuestState.ACTIVE, global.progress().get(ID).state());
    }

    @Test void trackingWithoutPersonalQuestsSurvivesSaving() {
        QuestPlayerData players = new QuestPlayerData();
        players.setMarked(NEW, ID);
        assertEquals(ID, QuestPlayerData.load(players.save(new CompoundTag())).marked(NEW));
    }

    @Test void tutorialMigrationPreservesReadsAndNewOrOfflinePlayersSeeSharedUnlock() {
        TutorialPlayerData players = new TutorialPlayerData();
        players.setState(A, ID, TutorialState.UNLOCKED_READ);
        players.setState(B, ID, TutorialState.UNLOCKED_UNREAD);
        TutorialGlobalData global = new TutorialGlobalData();
        global.migrate(tutorial(QuestScope.GLOBAL), players);
        assertEquals(TutorialState.UNLOCKED_READ, global.state(players, A, ID));
        assertEquals(TutorialState.UNLOCKED_UNREAD, global.state(players, B, ID));
        assertEquals(TutorialState.UNLOCKED_UNREAD, global.state(players, NEW, ID));
        assertFalse(global.unlock(ID));
        assertTrue(global.reset(ID));
        global = TutorialGlobalData.load(global.save(new CompoundTag()));
        players = TutorialPlayerData.load(players.save(new CompoundTag()));
        global.migrate(tutorial(QuestScope.GLOBAL), players);
        assertEquals(TutorialState.LOCKED, global.state(players, A, ID));
        assertTrue(global.unlock(ID));
        assertEquals(TutorialState.UNLOCKED_UNREAD, global.state(players, A, ID));
        players.markGlobalRead(B, ID, global.generation(ID));
        assertEquals(TutorialState.UNLOCKED_READ, global.state(players, B, ID));
        assertEquals(TutorialState.UNLOCKED_UNREAD, global.state(players, A, ID));
        assertEquals(TutorialState.UNLOCKED_READ,
                global.state(TutorialPlayerData.load(players.save(new CompoundTag())), B, ID));
    }

    @Test void readerWithNoPersonalTutorialsIsPersistedAndPersonalScopeDoesNotMigrate() {
        TutorialPlayerData players = new TutorialPlayerData();
        players.setState(A, ID, TutorialState.UNLOCKED_READ);
        TutorialGlobalData global = new TutorialGlobalData();
        global.migrate(tutorial(QuestScope.PLAYER), players);
        assertEquals(0, global.generation(ID));
        assertEquals(TutorialState.UNLOCKED_READ, players.state(A, ID));
        global.unlock(ID);
        players.markGlobalRead(NEW, ID, global.generation(ID));
        assertEquals(TutorialState.UNLOCKED_READ,
                global.state(TutorialPlayerData.load(players.save(new CompoundTag())), NEW, ID));
    }
}
