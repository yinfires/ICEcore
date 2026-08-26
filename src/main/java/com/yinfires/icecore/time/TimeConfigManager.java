package com.yinfires.icecore.time;

import com.google.gson.JsonParser;
import com.yinfires.icecore.building.BuildingDataManager;
import com.yinfires.icecore.feedback.PlayerFeedback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;

public final class TimeConfigManager {
    private static final TimeConfigManager INSTANCE = new TimeConfigManager();
    private MinecraftServer server;
    private TimeConfigData data = TimeConfigData.empty();
    private long revision;
    private TimeConfigManager() {}
    public static TimeConfigManager get() { return INSTANCE; }
    public TimeConfigData data() { return data; }
    public long revision() { return revision; }
    public MinecraftServer server() { return server; }

    public void start(MinecraftServer value) { server = value; reload(null); }
    public void stop() { server = null; data = TimeConfigData.empty(); revision++; }
    public boolean reload(ServerPlayer feedback) {
        if (server == null) return false;
        try {
            Path path = file();
            TimeConfigData loaded;
            if (!Files.exists(path)) {
                loaded = TimeConfigData.empty();
                // A fresh world may not have a list named beds yet. Persist the useful default,
                // but defer cross-file validity until an administrator binds a real list.
                write(loaded, path);
            } else {
                loaded = TimeConfigData.fromJson(JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject());
                loaded.validateReferences(BuildingDataManager.get().data());
            }
            data = loaded;
            revision++;
            TimeNetworking.broadcastSnapshot();
            if (feedback != null) PlayerFeedback.show(feedback, Component.translatable("icecore.time.reload.success"));
            return true;
        } catch (Exception exception) {
            if (feedback != null) PlayerFeedback.show(feedback, Component.translatable("icecore.time.reload.failed", exception.getMessage()));
            return false;
        }
    }
    public boolean mutate(Consumer<TimeConfigData> mutation, ServerPlayer feedback) {
        try {
            TimeConfigData copy = TimeConfigData.fromJson(data.toJson());
            mutation.accept(copy);
            copy.validateBasic();
            copy.validateReferences(BuildingDataManager.get().data());
            write(copy, file());
            data = copy;
            revision++;
            TimeService.resetFraction();
            TimeNetworking.broadcastSnapshot();
            return true;
        } catch (Exception exception) {
            if (feedback != null) PlayerFeedback.show(feedback, Component.translatable("icecore.time.save.failed", exception.getMessage()));
            return false;
        }
    }
    public void referencesChanged() {
        TimeVoteManager.revalidate();
        TimeNetworking.broadcastSnapshot();
    }
    public Path file() { return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("icecore_time.json"); }
    private static void write(TimeConfigData value, Path file) throws Exception {
        Files.createDirectories(file.getParent());
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temporary, TimeConfigData.gson().toJson(value.toJson()), StandardCharsets.UTF_8);
        try { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException exception) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
    }
}
