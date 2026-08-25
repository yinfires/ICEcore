package com.yinfires.icecore.compat.cozycafe.range;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.yinfires.icecore.feedback.PlayerFeedback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;

/** Server-authoritative storage and O(1) lookup for custom CozyCafe ranges. */
public final class CozyCafeRangeDataManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final CozyCafeRangeDataManager INSTANCE = new CozyCafeRangeDataManager();

    private MinecraftServer server;
    private CozyCafeRangeData data = CozyCafeRangeData.empty();
    private long revision;

    private CozyCafeRangeDataManager() {
    }

    public static CozyCafeRangeDataManager get() {
        return INSTANCE;
    }

    public CozyCafeRangeData data() {
        return data;
    }

    public long revision() {
        return revision;
    }

    public boolean isStarted() {
        return server != null;
    }

    public void start(MinecraftServer server) {
        this.server = server;
        reload();
    }

    public void stop() {
        server = null;
        data = CozyCafeRangeData.empty();
        revision++;
        CozyCafeRangeAdjustmentManager.clear();
    }

    public boolean ensureComputer(ServerPlayer player, BlockPos computer) {
        String key = CozyCafeRangeDefinition.key(player.serverLevel().dimension(), computer);
        if (data.ranges().containsKey(key)) {
            return true;
        }
        if (data.ranges().size() >= CozyCafeRangeData.MAX_RANGES) {
            PlayerFeedback.show(player, Component.translatable("icecore.cozycafe.range.too_many",
                    CozyCafeRangeData.MAX_RANGES));
            return false;
        }
        return mutate(copy -> copy.ranges().put(key,
                new CozyCafeRangeDefinition(player.serverLevel().dimension(), computer)), player);
    }

    public boolean setFirst(ServerPlayer player, String key, BlockPos position) {
        if (!data.ranges().containsKey(key)) {
            return false;
        }
        return mutate(copy -> definition(copy, key).beginSelection(position), player);
    }

    public boolean setSecond(ServerPlayer player, String key, BlockPos position) {
        CozyCafeRangeDefinition current = data.ranges().get(key);
        if (current == null) {
            return false;
        }
        if (!current.canSetSecond(position)) {
            rangeTooLarge(player);
            return false;
        }
        return mutate(copy -> definition(copy, key).setSecond(position), player);
    }

    /** Returns null until both custom corners are present, preserving CozyCafe's original range meanwhile. */
    public BlockPos first(Level level, BlockPos computer) {
        CozyCafeRangeDefinition definition = completeDefinition(level, computer);
        return definition == null ? null : definition.firstPosition();
    }

    /** Returns null until both custom corners are present, preserving CozyCafe's original range meanwhile. */
    public BlockPos second(Level level, BlockPos computer) {
        CozyCafeRangeDefinition definition = completeDefinition(level, computer);
        return definition == null ? null : definition.secondPosition();
    }

    private CozyCafeRangeDefinition completeDefinition(Level level, BlockPos computer) {
        if (level == null || level.isClientSide) {
            return null;
        }
        CozyCafeRangeDefinition definition = data.ranges().get(
                CozyCafeRangeDefinition.key(level.dimension(), computer));
        return definition != null && definition.isComplete() ? definition : null;
    }

    private boolean reload() {
        if (server == null) {
            return false;
        }
        Path file = file();
        try {
            CozyCafeRangeData loaded;
            if (!Files.exists(file)) {
                loaded = CozyCafeRangeData.empty();
                write(loaded, file);
            } else {
                String text = Files.readString(file, StandardCharsets.UTF_8);
                JsonObject json = JsonParser.parseString(text).getAsJsonObject();
                loaded = CozyCafeRangeData.fromJson(json);
            }
            data = loaded;
            revision++;
            CozyCafeRangeNetworking.broadcast(server, data, revision);
            return true;
        } catch (Exception exception) {
            LOGGER.error("Failed to load CozyCafe range data from {}", file, exception);
            return false;
        }
    }

    private boolean mutate(Consumer<CozyCafeRangeData> mutation, ServerPlayer feedback) {
        if (server == null) {
            return false;
        }
        try {
            CozyCafeRangeData copy = CozyCafeRangeData.fromJson(data.toJson());
            mutation.accept(copy);
            CozyCafeRangeData.fromJson(copy.toJson());
            write(copy, file());
            data = copy;
            revision++;
            CozyCafeRangeNetworking.broadcast(server, data, revision);
            return true;
        } catch (Exception exception) {
            LOGGER.error("Failed to save CozyCafe range data", exception);
            if (feedback != null) {
                PlayerFeedback.show(feedback, Component.translatable(
                        "icecore.cozycafe.range.save_failed", exception.getMessage()));
            }
            return false;
        }
    }

    private static CozyCafeRangeDefinition definition(CozyCafeRangeData data, String key) {
        CozyCafeRangeDefinition definition = data.ranges().get(key);
        if (definition == null) {
            throw new IllegalArgumentException("missing CozyCafe computer range");
        }
        return definition;
    }

    private static void rangeTooLarge(ServerPlayer player) {
        PlayerFeedback.show(player, Component.translatable("icecore.cozycafe.range.too_large",
                CozyCafeRangeDefinition.MAX_SCAN_VOLUME));
    }

    private Path file() {
        return server.getWorldPath(LevelResource.ROOT).resolve("data")
                .resolve("icecore_cozycafe_ranges.json");
    }

    private static void write(CozyCafeRangeData value, Path file) throws IOException {
        Files.createDirectories(file.getParent());
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temporary, CozyCafeRangeData.gson().toJson(value.toJson()), StandardCharsets.UTF_8);
        try {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
