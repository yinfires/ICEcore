package com.yinfires.icecore.building;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yinfires.icecore.feedback.PlayerFeedback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;

public final class BuildingDataManager {
    private static BuildingDataManager instance;
    private MinecraftServer server;
    private BuildingData data = BuildingData.empty();
    private long revision;

    private BuildingDataManager() {
    }

    public static BuildingDataManager get() {
        if (instance == null) {
            instance = new BuildingDataManager();
        }
        return instance;
    }

    public BuildingData data() {
        return data;
    }

    public long revision() {
        return revision;
    }

    public void start(MinecraftServer server) {
        this.server = server;
        reload(null);
    }

    public void stop() {
        server = null;
        data = BuildingData.empty();
        revision++;
        AdjustmentModeManager.clear();
    }

    public boolean reload(ServerPlayer feedback) {
        if (server == null) {
            return false;
        }
        Path file = file();
        try {
            if (!Files.exists(file)) {
                BuildingData fresh = BuildingData.empty();
                write(fresh, file);
                data = fresh;
            } else {
                String text = Files.readString(file, StandardCharsets.UTF_8);
                JsonObject json = JsonParser.parseString(text).getAsJsonObject();
                data = BuildingData.fromJson(json);
            }
            revision++;
            BuildingRuntimeCache.rebuild(data, server);
            BuildingNetworking.broadcastRules(server, data, revision);
            if (feedback != null) {
                PlayerFeedback.show(feedback, Component.translatable("icecore.build.reload.success"));
            }
            return true;
        } catch (Exception exception) {
            if (feedback != null) {
                PlayerFeedback.show(feedback, Component.translatable("icecore.build.reload.failed", exception.getMessage()));
            }
            return false;
        }
    }

    public boolean mutate(java.util.function.Consumer<BuildingData> mutation, ServerPlayer feedback) {
        try {
            BuildingData copy = BuildingData.fromJson(data.toJson());
            mutation.accept(copy);
            BuildingData.fromJson(copy.toJson());
            write(copy, file());
            data = copy;
            revision++;
            BuildingRuntimeCache.rebuild(data, server);
            BuildingNetworking.broadcastRules(server, data, revision);
            return true;
        } catch (Exception exception) {
            if (feedback != null) {
                PlayerFeedback.show(feedback, Component.translatable("icecore.build.save.failed", exception.getMessage()));
            }
            return false;
        }
    }

    public Path file() {
        return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("icecore_building.json");
    }

    private static void write(BuildingData value, Path file) throws IOException {
        Files.createDirectories(file.getParent());
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temporary, BuildingData.gson().toJson(value.toJson()), StandardCharsets.UTF_8);
        try {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
