package com.yinfires.icecore.quest;

import com.yinfires.icecore.network.ClientBoundQuestSyncPacket;
import com.yinfires.icecore.network.ICECoreNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds and sends the client quest snapshot: the player's ACTIVE quests (PLAYER and GLOBAL scope)
 * plus their marked id. Installed as the {@link QuestSyncHook} at startup.
 */
public final class QuestNetworking {
    private QuestNetworking() {
    }

    public static void install() {
        QuestService.installSyncHook(QuestNetworking::syncPlayer);
    }

    public static void syncPlayer(ServerPlayer player) {
        if (!QuestService.isReady()) {
            return;
        }
        List<QuestSnapshot> snapshots = new ArrayList<>();
        collect(player, QuestService.playerData().progressFor(player.getUUID()), snapshots, QuestScope.PLAYER);
        collect(player, QuestService.globalData().progress(), snapshots, QuestScope.GLOBAL);
        ResourceLocation marked = QuestService.marked(player);
        ICECoreNetwork.sendToPlayer(new ClientBoundQuestSyncPacket(snapshots, marked), player);
    }

    private static void collect(ServerPlayer player, Map<ResourceLocation, QuestProgress> map,
                                List<QuestSnapshot> out, QuestScope scope) {
        map.forEach((questId, progress) -> {
            if (progress.state() != QuestState.ACTIVE) {
                return;
            }
            QuestDefinition def = QuestDefinitionManager.INSTANCE.get(questId);
            // Only surface quests whose current definition still matches this scope map.
            if (def == null || def.scope() != scope) {
                return;
            }
            out.add(new QuestSnapshot(questId, progress.counts()));
        });
    }
}
